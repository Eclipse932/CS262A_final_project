package database;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.time.Duration;
import java.time.Instant;

public class Replica extends UnicastRemoteObject implements ReplicaIntf {
	String RMIRegistryAddress;
	boolean isLeader;
	String name;

	Log dataLog;
	Replica leader;

	Thread leaseKiller;
	LockTable lockTable;
	List<LeaseLock> committingWrites;
	
	// the lock lease interval is 10 milliseconds across replicas.
	static Duration LOCK_LEASE_INTERVAL = Duration.ofSeconds(10);

	public Replica(String RMIRegistryAddress, boolean isLeader, String name)
			throws RemoteException {
		super();
		this.RMIRegistryAddress = RMIRegistryAddress;
		this.isLeader = isLeader;
		this.name = name;
		this.lockTable = new LockTable();
		this.committingWrites = new LinkedList<LeaseLock>();
		this.leaseKiller = new Thread(new LeaseKiller(lockTable,
				committingWrites));
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
		 * 1. validate the heldLocks against locks in LockTable, if not all valid, return "abort"; else
		 * 2. add the LeaseLocks to commitingWrites so that LeaseKiller won't remove locks from lockTable 
		 * 3. commit through Raft
		 * 4. release all locks in the lockTable and remove TransactionBirthDate through releaseLocks
		*/
		return "abort";
	}
	
	public Instant beginTransaction(long transactionID) throws RemoteException{
		//TODO implement this
		
		return null;
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
