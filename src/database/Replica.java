package database;

import java.rmi.RemoteException;
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
	String name;

	Log dataLog;
	Replica leader;
	ArrayList<ReplicaIntf> replicas = null; // Only initialized to something
											// other than null in main if this
											// is the leader

	Long paxosSequenceNumber = new Long(0); // This value should only be used by
											// the leader

	private Long getPaxosSequenceNumber() {
		return paxosSequenceNumber;
	}

	private Long incrementPaxosSequenceNumber() {
		return ++this.paxosSequenceNumber;
	}

	ConcurrentHashMap<Integer, ValueAndTimestamp> dataMap;

	Thread leaseKiller;
	LockTable lockTable;
	Object serializedCommitLock;

	// the lock lease interval is 10 milliseconds across replicas.
	static Duration LOCK_LEASE_INTERVAL = Duration.ofMillis(1000);

	public Replica(String RMIRegistryAddress, boolean isLeader, String name)
			throws RemoteException {
		super();
		this.dataMap = new ConcurrentHashMap<Integer, ValueAndTimestamp>();
		this.RMIRegistryAddress = RMIRegistryAddress;
		this.isLeader = isLeader;
		this.name = name;
		this.lockTable = new LockTable();
		this.serializedCommitLock = new Object();
		this.leaseKiller = new Thread(new LeaseKiller(lockTable));
		leaseKiller.start();

	}

	public void setLog(Log dataLog) {
		this.dataLog = dataLog;
	}

	public void setLeader(Replica leader) {
		this.leader = leader;
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
						result = paxosWrite(timestamp, memAddr,
								memaddrToValue.get(memAddr));
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

	// TODO implement this method
	private boolean paxosWrite() throws RemoteException {
		synchronized (paxosSequenceNumber) {
			Long sn = this.getPaxosSequenceNumber() + 1;
			ArrayList<ReplicaIntf> quorum = new ArrayList<ReplicaIntf>();
			boolean hasPromised = false;
			int majority = (this.replicas.size() / 2) + 1;

			while (quorum.size() < majority) {
				quorum.clear();
				for (ReplicaIntf contactReplica : replicas) {
					try {
						hasPromised = contactReplica.prepare(sn, key, value);
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
				try {
					participatingReplica
							.paxosSlaveDuplicate(sn, key, value);
				} catch (RemoteException r) {
					System.out
							.println("Aborting - unable to paxosSlaveDuplicate with Replica "
									+ participatingReplica);
					System.out.println(r);
					return false;
				}
			}
			
			//Make the increment in sequence number official
			this.incrementPaxosSequenceNumber();
			return true;
		}
	}

	public Instant beginTransaction(long transactionID) throws RemoteException {
		// TODO implement this
		Instant transactionBirthDate = Instant.now();
		lockTable.setTransactionBirthDate(transactionID, transactionBirthDate);
		return transactionBirthDate;
	}

	// A true return value indicates that the locks have been acquired, false
	// means that this transaction must abort
	public Instant getReplicaLock(LeaseLock lock) throws RemoteException,
			InterruptedException {
		// TODO implement this method
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
		return dataMap.get(databaseKey).getValue();
	}

}
