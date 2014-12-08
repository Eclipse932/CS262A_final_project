package database;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.time.Duration;

public class Replica extends UnicastRemoteObject implements ReplicaIntf {
	String RMIRegistryAddress;
	boolean isLeader;
	String remoteName;
	String ipAddress;
	int numOfReplicas;

	Log dataLog;
	ReplicaIntf leader;
	ArrayList<ReplicaIntf> replicas = null; // Only initialized to something
											// other than null in main if this
											// is the leader

	// Start both sequence numbers at -1 so they will be incremented to zero for
	// the first sequence use.
	Long paxosSequenceNumber = new Long(-1); // This value should only be used
												// by
												// the leader

	Long replicaSequenceNumber = new Long(-1); // This value should only be used
												// by a paxos slave replica

	Integer[] sequenceToMemAddr;
	ConcurrentHashMap<Integer, ValueAndTimestamp> dataMap;
	static int SEQUENCETRACKINGRANGE = 400;

	Thread leaseKiller;
	LockTable lockTable;
	Object serializedCommitLock;

	private static double paxosFailRate = 0.01; // Default value. Set this in
												// main.

	private Long getPaxosSequenceNumber() {
		return paxosSequenceNumber;
	}

	private Long incrementPaxosSequenceNumber() {
		return ++this.paxosSequenceNumber;
	}

	private Long getReplicaSequenceNumber() {
		return replicaSequenceNumber;
	}

	private void setReplicaSequenceNumber(Long replicaSequenceNumber) {
		this.replicaSequenceNumber = replicaSequenceNumber;
	}

	// the lock lease interval is 10 milliseconds across replicas.
	static Duration LOCK_LEASE_INTERVAL = Duration.ofMillis(1000);

	public Replica(String RMIRegistryAddress, boolean isLeader, String remoteName, int numOfReplicas, String ipAddress)
			throws RemoteException {
		super();
		this.dataMap = new ConcurrentHashMap<Integer, ValueAndTimestamp>();
		this.sequenceToMemAddr = new Integer[SEQUENCETRACKINGRANGE];
		this.RMIRegistryAddress = RMIRegistryAddress;
		this.isLeader = isLeader;
		this.remoteName = remoteName;
		this.numOfReplicas = numOfReplicas;
		this.ipAddress = ipAddress;
		this.dataLog = new Log();
		if (this.isLeader == true) {
			leader = this;
			this.replicas = new ArrayList<ReplicaIntf>();
			this.lockTable = new LockTable();
			this.serializedCommitLock = new Object();
			this.leaseKiller = new Thread(new LeaseKiller(lockTable));
			leaseKiller.start();
			
		} 
	}


	public Instant keepTransactionAlive(List<LeaseLock> locks)
			throws RemoteException {
		return lockTable.extendLockLeases(locks);
	}

	public String RWTcommit(Long transactionID, List<LeaseLock> heldLocks,
			HashMap<Integer, Integer> memaddrToValue) throws RemoteException {
		/*
		 * commit needs to : 1. validate the heldLocks against locks in
		 * LockTable： if not all valid, return false and abort; else add the
		 * LeaseLocks to committingWrites so that LeaseKiller and wakeUpNextLock
		 * won't remove locks from lockTable and return true 2. commit through
		 * paxos, set transaction status to "commit" 3. release all locks in the
		 * lockTable, remove TransactionBirthDate and remove the entry in
		 * committingWrites through releaseLockss
		 */
		synchronized (serializedCommitLock) {
			boolean result = lockTable.validateTableLock(heldLocks,
					transactionID);
			if (result == false) {
				lockTable.releaseTableLocks(heldLocks, transactionID);
				return "abort";
			} else {

				Instant timestamp = Instant.now();
				String returnStatus = "";

				for (Integer memAddr : memaddrToValue.keySet()) {
					try {
						result = paxosWrite(memAddr,
								memaddrToValue.get(memAddr), timestamp);
					} catch (RemoteException r) {
						System.out
								.println("Unable to perform paxosWrite on timestamp: "
										+ timestamp
										+ " memAddr: "
										+ memAddr
										+ " value: "
										+ memaddrToValue.get(memAddr));
						System.out.println("Aborting transaction "
								+ transactionID);
						System.out.println(r);
						lockTable.releaseTableLocks(heldLocks, transactionID);
						return "abort";
					}
				}

				if (result == false) {
					lockTable.releaseTableLocks(heldLocks, transactionID);
					return "abort";
				} else {

					while (!TrueTime.after(timestamp)) {
						try {
							Thread.sleep(1); // Experiment with this value
						} catch (InterruptedException i) {
							System.out.println(i);
						}
					}

					// If we fall through the error cases, that means we
					// successfully wrote!
					lockTable.releaseTableLocks(heldLocks, transactionID);
					return "commit";
				}
			}
		}

	}

	private boolean paxosWrite(Integer memAddr, Integer value, Instant timestamp)
			throws RemoteException {
		synchronized (paxosSequenceNumber) {
			Long sn = this.getPaxosSequenceNumber() + 1;
			ArrayList<ReplicaIntf> quorum = new ArrayList<ReplicaIntf>();
			boolean hasPromised = false;
			int majority = (this.replicas.size() / 2) + 1;

			while (quorum.size() < majority) {
				quorum.clear();
				for (ReplicaIntf contactReplica : replicas) {
					try {
						hasPromised = contactReplica.prepare(sn);
					} catch (RemoteException r) {
						System.out
								.println("Aborting - unable to prepare Replica "
										+ contactReplica);
						System.out.println(r);
						return false;
					}
					if (hasPromised) {
						quorum.add(contactReplica);
					}
				}
			}

			for (ReplicaIntf participatingReplica : quorum) {

				boolean successfulDuplication = false;
				try {
					successfulDuplication = participatingReplica
							.paxosSlaveDuplicate(sn, memAddr, value, timestamp);
				} catch (RemoteException r) {
					System.out
							.println("Aborting - unable to paxosSlaveDuplicate with Replica "
									+ participatingReplica);
					System.out.println(r);
					return false;
				}
				if (!successfulDuplication) {
					// I don't know how this could happen, but leave the check
					// in to be safe.
					System.out
							.println("Aborting - Duplication falied with Replica "
									+ participatingReplica);
					return false;
				}

			}

			// Make the increment in sequence number official
			this.incrementPaxosSequenceNumber();

			// Update the local dataMap.
			ValueAndTimestamp vat = new ValueAndTimestamp(value, timestamp);
			dataMap.put(memAddr, vat);

			// Update the sequencenumber array
			int arraySpot = (int) (sn % SEQUENCETRACKINGRANGE);
			sequenceToMemAddr[arraySpot] = memAddr;
		}
		return true;
	}

	public boolean prepare(Long sequenceNumber) throws RemoteException {

		Long expectedReplicaSequenceNumber = this.getReplicaSequenceNumber() + 1;
		if (sequenceNumber == expectedReplicaSequenceNumber) {
			// This replica is up-to-date
			// Fall through to error injection
		} else {

			if ((sequenceNumber - expectedReplicaSequenceNumber) <= SEQUENCETRACKINGRANGE) {
				// The leader will send the missing values to copy into the
				// local dataMap
				ConcurrentHashMap<Integer, ValueAndTimestamp> freshMemAddrToValue = null;
				try {
					freshMemAddrToValue = leader.requestSequenceData(
							expectedReplicaSequenceNumber, sequenceNumber);
				} catch (RemoteException r) {
					System.out.println(r);
					return false;
				}

				if (freshMemAddrToValue == null) {
					// This is how the leader signifies that it was given bad
					// arguments.
					System.out
							.println("leader.requestSequenceData returned null. "
									+ "Returning false in prepare");
					return false;
				}
				for (Integer freshMemAddr : freshMemAddrToValue.keySet()) {
					dataMap.put(freshMemAddr,
							freshMemAddrToValue.get(freshMemAddr));
				}

			} else {
				// The leader will send the entire dataMap data structure to
				// replace the local one because this replica is too far behind.
				try {
					this.dataMap = leader.requestSequenceData(
							expectedReplicaSequenceNumber, sequenceNumber);
					// requests data from the specified argument up to sn (this
					// includes the new value)!
				} catch (RemoteException r) {
					System.out.println(r);
					return false;
				}
			}
		}

		// If we fell through we decide whether or not to inject a network
		// failure
		if (paxosFailRate < Math.random()) {
			// Inject a network failure
			return false;
		} else {
			return true;
		}

	}

	public ConcurrentHashMap<Integer, ValueAndTimestamp> requestSequenceData(
			Long replicaExpectedSn, Long leaderNewSequenceNumber)
			throws RemoteException {

		if (!((leaderNewSequenceNumber - replicaExpectedSn) <= 1)) {
			System.out
					.println("Incorrect arguments given to requestSequenceData");
			System.out.println("Returning null");
			return null;
		}

		// All the data the replica needs to get up to date is in the
		// sequenceToMemAddr array
		if (((leaderNewSequenceNumber - replicaExpectedSn) <= SEQUENCETRACKINGRANGE)) {
			ConcurrentHashMap<Integer, ValueAndTimestamp> missingDataMap = new ConcurrentHashMap<Integer, ValueAndTimestamp>();

			for (Long i = replicaExpectedSn; i < leaderNewSequenceNumber; i++) {
				int sequenceAddr = (int) (i % SEQUENCETRACKINGRANGE);
				Integer memAddr = sequenceToMemAddr[sequenceAddr];
				missingDataMap.put(memAddr, dataMap.get(memAddr));
			}
			return missingDataMap;

		} else {
			// give a snapshot, i.e. the entire dataMap
			// Note that no other threads are capable of modifying the dataMap
			// right now:
			// The leader is currently blocked on a function call to
			// replica.prepare() in paxosWrite where it has
			// exclusive access to the only code that modifies the dataMap.
			return dataMap;
		}

	}

	public boolean paxosSlaveDuplicate(Long sequenceNumber, Integer memAddr,
			Integer value, Instant timestamp) throws RemoteException {

		this.setReplicaSequenceNumber(sequenceNumber);
		this.dataMap.put(memAddr, new ValueAndTimestamp(value, timestamp));
		return true;
	}

	public Instant beginTransaction(long transactionID) throws RemoteException {
		Instant transactionBirthDate = Instant.now();
		lockTable.setTransactionBirthDate(transactionID, transactionBirthDate);
		return transactionBirthDate;
	}

	// A true return value indicates that the locks have been acquired, false
	// means that this transaction must abort
	public Instant getReplicaLock(LeaseLock lock) throws RemoteException,
			InterruptedException {
		Object leaseLockCondition = new Object();
		synchronized (leaseLockCondition) {
			Instant transactionBirthDate = lockTable
					.getTransactionBirthDate(lock);
			if (transactionBirthDate == null)
				return null;
			LockAndCondition lc = new LockAndCondition(lock,
					leaseLockCondition, transactionBirthDate);
			LockWorker lockWorker = new LockWorker(lockTable, lc);
			Thread lockWorkerThread = new Thread(lockWorker);
			lockWorkerThread.start();
			leaseLockCondition.wait();
			return lc.lockLeaseEnd;
		}
	}

	// It is the calling Responder's responsibility to have acquired the read
	// lock for this databasekey
	public Integer RWTread(Integer databaseKey) throws RemoteException {
		if (dataMap.contains(databaseKey)) {
			return dataMap.get(databaseKey).getValue();
		} else {
			// If this value is not in the database, return null. The responder
			// will check for this and turn nulls into zeroes.
			return null;
		}

	}

	public static void main(String[] args){
		// set the paxos fail rate as a probability between 0 and 1
		paxosFailRate = Math.random();
		if (args.length != 5) {
			System.out.println("Incorrect number of command line arguments.");
			System.out.println("Correct form: RMIRegistryAddress, isLeader, remoteName, numOfReplicas, ipAddress");
			System.exit(1);
		}
		String myRemoteName = args[2];
		String myIpAddress = args[4];
		boolean leaderOrNot;
		if (args[1] == "true") {
			leaderOrNot = true;
			myRemoteName = "Replica0";
		} else if (args[1] == "false") {
			leaderOrNot = false;
		} else {
			System.out.println("type in wrong isLeader argument! Default initialization is non-leader replica");
			leaderOrNot = false;
		}
		
		System.out.println("Attempting to start local RMI Server for "
				+ myRemoteName + " at " + myIpAddress);
		try { // special exception handler for registry creation
			LocateRegistry.createRegistry(1099);
			System.out.println("java RMI registry created.");
		} catch (RemoteException e) {
			// do nothing, error means registry already exists
			System.out.println("java RMI registry already exists.");
		}

		Replica me = null;
		try {
			Replica replica = new Replica(args[0], leaderOrNot, myRemoteName, Integer.parseInt(args[3]), myIpAddress);
		} catch (RemoteException r) {
			System.out.println("Unable to start local server");
			System.out.println(r);
			System.exit(1);
		}
		// Bind this object's instance to the local name on the local RMI
		// registry
		try {
			Naming.rebind("//" + myIpAddress + "/" + myRemoteName, me);
		} catch (Exception e) {
			System.out.println("Unable to bind this Responder to local server");
			System.out.println(e);
			System.exit(1);
		}
		System.out.println(myRemoteName
				+ " successfully bound in local registry");

		// Acquire remoteRegistry to first register this responder and second to
		// lookup the leader.
		System.out.println("Trying to contact terratest.eecs.berkeley.edu");
		RemoteRegistryIntf terraTestRemoteRegistry = null;
		try {
			terraTestRemoteRegistry = (RemoteRegistryIntf) Naming.lookup("//"
					+ args[0] + "/RemoteRegistry");
		} catch (Exception e) {
			System.out.println("Error, terratest.eecs.berkeley.edu.");
			System.out
					.println("Please check to make sure you're connected to the internet.");
			System.out.println(e);
			System.exit(1);
		}
		
		if (me.isLeader == true) {
			
		} else {
			//the replica is a non-leader
			// Use the remoteRegistry to lookup the leader's networkname
			String leaderNetworkName = null;
			boolean firstIteration = true;

			do {
				if (!firstIteration) {
					// Give the leader a chance to register itself and try again
					System.out.println("Waiting for leader replica to be registered...");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException i) {
						System.out.println("Thread sleep in non-leader initialization has been interupted");
					}
				}
				try {
					leaderNetworkName = terraTestRemoteRegistry
							.getNetworkName("Replica0");
				} catch (Exception e) {
					System.out
							.println("Unable to connect to RemoteRegistry during lookup of leaderNetworkName");
					System.exit(1);
				}
				firstIteration = false;
			} while (leaderNetworkName == null);
			
			try {
				me.leader = (ReplicaIntf) Naming.lookup(leaderNetworkName);
			} catch (Exception e) {
				System.out.println("Unable to acquire the leader's remote object");
				System.out.println(e);
				System.exit(1);
			}
			
			boolean registrationStatus = false;
			try {
				registrationStatus = terraTestRemoteRegistry.registerNetworkName(
						"//" + myIpAddress + "/" + myRemoteName, myRemoteName);
			} catch (RemoteException e) {
				System.out.println("Unable to register " + myRemoteName);
				e.printStackTrace();
				System.exit(1);
			}
			if (registrationStatus == false) {
				System.out.println("Error, RemoteName:" + myRemoteName
						+ " has already been taken");
				System.exit(1);
			}
			
		}
	}

}
