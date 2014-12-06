package database;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.time.Instant;
import java.time.Duration;

public class Replica extends UnicastRemoteObject implements ReplicaIntf {
	String RMIRegistryAddress;
	boolean isLeader;
	String name;

	Log dataLog;
	Replica leader;

	Thread leaseKiller;
	LockTable lockTable;
	Object serializedCommitLock;
	
	// the lock lease interval is 10 milliseconds across replicas.
	static Duration LOCK_LEASE_INTERVAL = Duration.ofMillis(1000);

	public Replica(String RMIRegistryAddress, boolean isLeader, String name)
			throws RemoteException {
		super();
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
		// TODO implement this method
		/*commit needs to :
		 * 1. validate the heldLocks against locks in LockTable：
		 * 	if not all valid, return false and abort; else
			add the LeaseLocks to committingWrites so that LeaseKiller and wakeUpNextLock won't remove locks from lockTable and return true
		 * 2. commit through paxos, set transaction status to "commit"
		 * 3. release all locks in the lockTable, remove TransactionBirthDate and remove the entry in committingWrites through releaseLockss
		*/
		synchronized(serializedCommitLock) {
			boolean result = lockTable.validateTableLock(heldLocks, transactionID);
			if (result == false) {
				return "abort";
			} else {
				//committing through paxos protocol
				lockTable.releaseTableLocks(heldLocks, transactionID);
				return "abort or commit depending on the result of Raft";
			}
		}
		
	}
	
	public Instant beginTransaction(long transactionID) throws RemoteException{
		//TODO implement this
		Instant transactionBirthDate = Instant.now();
		lockTable.setTransactionBirthDate(transactionID, transactionBirthDate);
		return transactionBirthDate;
	}

	// A true return value indicates that the locks have been acquired, false
	// means that this transaction must abort
	public Instant getReplicaLock(LeaseLock lock) throws RemoteException, InterruptedException {
		// TODO implement this method
		Object leaseLockCondition = new Object();
		synchronized (leaseLockCondition) {
			Instant transactionBirthDate = lockTable.getTransactionBirthDate(lock);
			if (transactionBirthDate == null) return null;
			LockAndCondition lc= new LockAndCondition(lock, leaseLockCondition, transactionBirthDate);
			LockWorker lockWorker = new LockWorker(lockTable, lc);
			Thread lockWorkerThread = new Thread(lockWorker);
			lockWorkerThread.start();
			leaseLockCondition.wait();
			return lc.lockLeaseEnd;
		}
	}
		
	public Integer RWTread( Integer databaseKey) throws RemoteException	{
		// TODO implement this method
		return null;
	}
	
	
}
