package database;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.time.Instant;

public class LockTable {
	private Map<Integer, List<LeaseLock>> lockMap;
	private Map<Long, Instant> transactionBirthdates;
	private Map<Integer, PriorityQueue<LockAndCondition>> waitingLocks;
	
	public LockTable() {
		this.lockMap = new HashMap<Integer, List<LeaseLock>>();
		this.transactionBirthdates = new HashMap<Long, Instant>();
		this.waitingLocks = new HashMap<Integer, PriorityQueue<LockAndCondition>>();
	}
	
	synchronized Instant extendLockLeases(List<LeaseLock> locks) {
		Instant newLeaseEnd = null; 
		for (LeaseLock lock: locks) {
			List <LeaseLock> sameKeyLocks = lockMap.get(lock.lockedKey);
			//if no entry, it shows the lock has already been removed by LeaseKiller so no longer valid
			if (sameKeyLocks == null) {
				return null;
			}else {
				boolean isFound = false;
				for (LeaseLock sameKeyLock: sameKeyLocks) {
					if (sameKeyLock.equals(lock)) {
						isFound = true;
						if (newLeaseEnd == null) {
							//here we're being sloppy and not using TrueTime
							newLeaseEnd = Instant.now().plus(Replica.LOCK_LEASE_INTERVAL);
						}
						sameKeyLock.expirationTime = newLeaseEnd;
					}
				}
				if (!isFound) return null;
			}
		}
		return newLeaseEnd;
	}
	
	synchronized Instant getTransactionBirthDate(LeaseLock lock) {
		return transactionBirthdates.get(lock.ownerTransactionID);
	}
	
	
	
}
