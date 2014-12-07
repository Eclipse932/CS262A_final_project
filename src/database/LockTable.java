package database;
import java.util.HashSet;
import java.util.LinkedList;
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
		if (!transactionBirthdates.containsKey(transactionID)) return null;
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
		PriorityQueue<LockAndCondition> queue = waitingLocks.get(key);
		if (queue != null && queue.size() > 0) {
			List<LeaseLock> sameKeyLocks = lockMap.get(key);
			LockAndCondition nextLockHolderCandidate = queue.peek();
			if (!transactionBirthdates.containsKey(nextLockHolderCandidate.leaseLock.ownerTransactionID)) {
				nextLockHolderCandidate = queue.poll();
				nextLockHolderCandidate.lockLeaseEnd = null;
				synchronized(nextLockHolderCandidate.leaseLockCondition) {
					nextLockHolderCandidate.leaseLockCondition.notify();
				}
				wakeUpNextLock(key);
			}else if (sameKeyLocks == null) {
				sameKeyLocks = new LinkedList<LeaseLock>();
				lockMap.put(key, sameKeyLocks);
				wakeUpNextLockHelper(queue, sameKeyLocks, key);
			} else if (sameKeyLocks.size() == 0){
				wakeUpNextLockHelper(queue, sameKeyLocks, key);
			} else {
				if (nextLockHolderCandidate.leaseLock.mode == AccessMode.READ && sameKeyLocks.size() >= 1 && sameKeyLocks.get(0).mode == AccessMode.READ) {
					wakeUpNextLockHelper(queue, sameKeyLocks, key);
				} else if (nextLockHolderCandidate.leaseLock.mode == AccessMode.READ && sameKeyLocks.size() == 1) {
					wakeUpNextLockOnlyOneCurrentLockHolderHelper(sameKeyLocks, queue, key);
				} else if (nextLockHolderCandidate.leaseLock.mode == AccessMode.WRITE && sameKeyLocks.size() == 1 && sameKeyLocks.get(0).mode == AccessMode.WRITE) {
					wakeUpNextLockOnlyOneCurrentLockHolderHelper(sameKeyLocks, queue, key);
				} else if (nextLockHolderCandidate.leaseLock.mode == AccessMode.WRITE && sameKeyLocks.size() >=1) {
					LeaseLock toBeUpgrade = null;
					int finalCompareResult = Integer.MIN_VALUE;
					for (LeaseLock sameKeyLock: sameKeyLocks) {
						if (!transactionBirthdates.containsKey(sameKeyLock.ownerTransactionID))
						int currentResult = wakeUpNextLockCompareHelper(sameKeyLock, nextLockHolderCandidate);
						if (currentResult == 0) {
							if (transactionBirthdates.containsKey)
							toBeUpgrade = sameKeyLock;
						}
						if (transactionBirthdates.containsKey(sameKeyLock.ownerTransactionID) && !committingWrites.containsKey(sameKeyLock.ownerTransactionID)) {
							finalCompareResult = Math.max(finalCompareResult, currentResult);
						}
					}
					
				} else {
					System.out.println("this case should never happen");
					return;
				}
			}
		}
	}
	
	void wakeUpNextLockOnlyOneCurrentLockHolderHelper(List<LeaseLock> sameKeyLocks, PriorityQueue<LockAndCondition> queue, Integer key) {
		LeaseLock currentLockHolder = sameKeyLocks.get(0);
		int compareResult = wakeUpNextLockCompareHelper(currentLockHolder, queue.peek());
		if (compareResult < 0 ) {
			if (!transactionBirthdates.containsKey(currentLockHolder.ownerTransactionID)){
				sameKeyLocks.remove(currentLockHolder);
				wakeUpNextLockHelper(queue, sameKeyLocks, key);
			} else return;
		} else if (compareResult > 0) {
			if (!committingWrites.containsKey(currentLockHolder.ownerTransactionID)) {
				//kill currentLockHolder
				sameKeyLocks.remove(currentLockHolder);
				transactionBirthdates.remove(currentLockHolder.ownerTransactionID);
				wakeUpNextLockHelper(queue, sameKeyLocks, key);
			}else return;
		} else {
			System.out.println("this case should never happen");
			return;
		}
	}
	
	/*a return result of 0 indicates it's the same transaction trying to upgrade a read lock to a write lock;
	 *a return result of >0 indicates the currentLockHolder is younger than the waiting transaction and should be killed
	 *a return result of <0 indicates the currentLockHolder is older than the waiting transaction and the waiting transaction should be waiting
	*/
	int wakeUpNextLockCompareHelper(LeaseLock currentLockHolder, LockAndCondition nextLockHolderCandidate) {
		Instant currentLockHolderBirthdate = transactionBirthdates.get(currentLockHolder.ownerTransactionID);
		int result = currentLockHolderBirthdate.compareTo(nextLockHolderCandidate.transactionBirthDate);
		if (result != 0) {
			return result;
		} else {
			//break ties on TransactionID
			return currentLockHolder.ownerTransactionID.compareTo(nextLockHolderCandidate.leaseLock.ownerTransactionID);
		}
	}
	
	
	void wakeUpNextLockHelper(PriorityQueue<LockAndCondition> queue, List<LeaseLock> sameKeyLocks, Integer key) {
		LockAndCondition nextLockHolderCandidate = queue.poll();
		sameKeyLocks.add(nextLockHolderCandidate.leaseLock);
		Instant leaseEnd = Instant.now().plus(Replica.LOCK_LEASE_INTERVAL);
		nextLockHolderCandidate.leaseLock.expirationTime = leaseEnd;
		nextLockHolderCandidate.lockLeaseEnd = leaseEnd;
		synchronized(nextLockHolderCandidate.leaseLockCondition) {
			nextLockHolderCandidate.leaseLockCondition.notify();
		}
		if (nextLockHolderCandidate.leaseLock.mode == AccessMode.READ) {
			wakeUpNextLock(key);
		}
	}
	
	synchronized void cleanUpLockTable() {
		Instant cleanUpStartTime  = Instant.now();
		for (List<LeaseLock> sameKeyLocks: lockMap.values()) {
			if (sameKeyLocks != null && sameKeyLocks.size() > 0) {
				int lockedKey = sameKeyLocks.get(0).lockedKey;
				List<LeaseLock> toBeRemovedLocks = new LinkedList<LeaseLock>();
				for (LeaseLock sameKeyLock: sameKeyLocks) {
					if (!transactionBirthdates.containsKey(sameKeyLock.ownerTransactionID)) {
						toBeRemovedLocks.add(sameKeyLock);
					}
					else if (sameKeyLock.expirationTime.isBefore(cleanUpStartTime) && !committingWrites.containsKey(sameKeyLock.ownerTransactionID)) {
						toBeRemovedLocks.add(sameKeyLock);
						transactionBirthdates.remove(sameKeyLock.ownerTransactionID);
					}
				}
				for (LeaseLock toBeRemovedLock: toBeRemovedLocks) {
					sameKeyLocks.remove(toBeRemovedLock);
				}
				if (toBeRemovedLocks.size() > 0) wakeUpNextLock(lockedKey);
			}
		}
	}
	
	synchronized void releaseTableLocks(List<LeaseLock> locks, Long ownerTransactionID) {
		transactionBirthdates.remove(ownerTransactionID);
		for (LeaseLock lock: locks) {
			List <LeaseLock> sameKeyLocks = lockMap.get(lock.lockedKey);
			if (sameKeyLocks != null) {
				List<LeaseLock> toBeRemovedLocks = new LinkedList<LeaseLock>();
				for (LeaseLock sameKeyLock: sameKeyLocks) {
					if (sameKeyLock.equals(lock)) {
						toBeRemovedLocks.add(sameKeyLock);
						wakeUpNextLock(sameKeyLock.lockedKey);
						break;
					}
				}
				for (LeaseLock toBeRemovedLock: toBeRemovedLocks) {
					sameKeyLocks.remove(toBeRemovedLock);
				}
			}
		}
		committingWrites.remove(ownerTransactionID);
		return ;
	}
	
	 synchronized boolean validateTableLock(List<LeaseLock> locks, Long ownerTransactionID) {
		//if transactionBirthDate is no longer found, it shows the transaction is already aborted
		if (!transactionBirthdates.containsKey(ownerTransactionID)) {
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
