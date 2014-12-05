package database;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.time.Instant;

public class LockTable {
	Map<Integer, List<LeaseLock>> lockMap;
	Map<Long, Instant> transactionBirthdates;
	Map<Integer, PriorityQueue<LockAndCondition>> waitingLocks;
	Map<Long, List<LeaseLock>> committingWrites;
	
	public LockTable() {
		this.lockMap = new HashMap<Integer, List<LeaseLock>>();
		this.transactionBirthdates = new HashMap<Long, Instant>();
		this.waitingLocks = new HashMap<Integer, PriorityQueue<LockAndCondition>>();
		this.committingWrites = new HashMap<Long, List<LeaseLock>>();
	}
	
	synchronized Instant extendLockLeases(List<LeaseLock> locks) {
		Instant newLeaseEnd = Instant.now().plus(Replica.LOCK_LEASE_INTERVAL); 
		if (locks.size() == 0) return newLeaseEnd;
		Long transactionID = ((LeaseLock) locks.get(0)).ownerTransactionID;
		//the following check should not be required, but just to be more safe
		if (committingWrites.containsKey(transactionID)) return newLeaseEnd;
		//if transactionBirthDate is no longer found, it shows the transaction is already aborted
		if (transactionBirthdates.get(transactionID)== null) return null;
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
						sameKeyLock.expirationTime = newLeaseEnd;
						break;
					}
				}
				if (!isFound) {
					return null;
				}
			}
		}
		return newLeaseEnd;
	}
	
	synchronized Instant getTransactionBirthDate(LeaseLock lock) {
		return transactionBirthdates.get(lock.ownerTransactionID);
	}
	
	synchronized void setTransactionBirthDate(Long transactionID, Instant transactionBirthDate) {
		transactionBirthdates.put(transactionID, transactionBirthDate);
	}
	
	synchronized void wakeUpNextLock(Integer key) {
		//TODO
		return;
	}
	
	synchronized void cleanUpLockTable() {
		Instant cleanUpStartTime  = Instant.now();
		for (List<LeaseLock> sameKeyLocks: lockMap.values()) {
			if (sameKeyLocks != null && sameKeyLocks.size() > 0) {
				for (LeaseLock sameKeyLock: sameKeyLocks) {
					if (transactionBirthdates.get(sameKeyLock.ownerTransactionID) == null) {
						sameKeyLocks.remove(sameKeyLock);
						wakeUpNextLock(sameKeyLock.lockedKey);
					}
					else if (sameKeyLock.expirationTime.isBefore(cleanUpStartTime)) {
						sameKeyLocks.remove(sameKeyLock);
						transactionBirthdates.remove(sameKeyLock.ownerTransactionID);
						wakeUpNextLock(sameKeyLock.lockedKey);
					}
				}
			}
		}
	}
	
	synchronized void releaseTableLocks(List<LeaseLock> locks, Long ownerTransactionID) {
		transactionBirthdates.remove(ownerTransactionID);
		for (LeaseLock lock: locks) {
			List <LeaseLock> sameKeyLocks = lockMap.get(lock.lockedKey);
			if (sameKeyLocks != null) {
				for (LeaseLock sameKeyLock: sameKeyLocks) {
					if (sameKeyLock.equals(lock)) {
						sameKeyLocks.remove(sameKeyLock);
						wakeUpNextLock(sameKeyLock.lockedKey);
						break;
					}
				}
			}
		}
		committingWrites.remove(ownerTransactionID);
		return ;
	}
	
	 synchronized boolean validateTableLock(List<LeaseLock> locks, Long ownerTransactionID) {
		//if transactionBirthDate is no longer found, it shows the transaction is already aborted
		if (transactionBirthdates.get(ownerTransactionID)== null) {
			releaseTableLocks(locks, ownerTransactionID);
			return false;
		}
		Instant validateStartTime = Instant.now();
		for (LeaseLock lock: locks) {
			List <LeaseLock> sameKeyLocks = lockMap.get(lock.lockedKey);
			//if no entry, it shows the lock has already been removed by LeaseKiller so no longer valid
			if (sameKeyLocks == null) {
				releaseTableLocks(locks, ownerTransactionID);
				return false;
			}else {
				boolean isFound = false;
				for (LeaseLock sameKeyLock: sameKeyLocks) {
					if (sameKeyLock.equalForValidatingLocks(lock)) {
						isFound = true;
						if (sameKeyLock.expirationTime.isBefore(validateStartTime)) {
							releaseTableLocks(locks, ownerTransactionID);
							return false;
						}
						break;
					}
				}
				if (!isFound) {
					releaseTableLocks(locks, ownerTransactionID);
					return false;
				}
			}
		}
		committingWrites.put(ownerTransactionID, locks);
		return true;
	}
	 
	 /*public static void main(String[] args) {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.put(0, null);
		for (Integer value: map.values()) {
			System.out.println(value == null);
		}
	 }*/
	 
	
}
