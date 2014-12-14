package database;

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

	private String replicationMode;
	private Replica owner;

	public LockTable(String replicationMode, Replica owner) {
		this.lockMap = new HashMap<Integer, List<LeaseLock>>();
		this.transactionBirthdates = new HashMap<Long, Instant>();
		this.waitingLocks = new HashMap<Integer, PriorityQueue<LockAndCondition>>();
		this.committingWrites = new HashMap<Long, List<LeaseLock>>();
		this.replicationMode = replicationMode;
		this.owner = owner;
		System.out.println("replicationMode is: " + replicationMode);
	}

	synchronized Instant extendLockLeases(List<LeaseLock> locks) {

		if (replicationMode.equals("byz")) {
			try {
				owner.emulateLeaderByzReplicateState();
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Killing LockTable thread");
				System.exit(1);
			}
		}

		if (Replica.debugMode) {
			System.out.println("extendingLockLeases for " + locks);
		}

		Instant newLeaseEnd = Instant.now().plus(Replica.LOCK_LEASE_INTERVAL);
		if (locks.size() == 0)
			return newLeaseEnd;
		Long transactionID = ((LeaseLock) locks.get(0)).ownerTransactionID;
		// the following check should not be required, but just to be more safe
		if (committingWrites.containsKey(transactionID))
			return newLeaseEnd;
		// if transactionBirthDate is no longer found, it shows the transaction
		// is already aborted
		if (!transactionBirthdates.containsKey(transactionID))
			return null;
		for (LeaseLock lock : locks) {
			List<LeaseLock> sameKeyLocks = lockMap.get(lock.lockedKey);
			// if no entry, it shows the lock has already been removed by
			// LeaseKiller so no longer valid
			if (sameKeyLocks == null) {
				return null;
			} else {
				boolean isFound = false;
				for (LeaseLock sameKeyLock : sameKeyLocks) {
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

	synchronized void setTransactionBirthDate(Long transactionID,
			Instant transactionBirthDate) {
		transactionBirthdates.put(transactionID, transactionBirthDate);
	}

	synchronized void wakeUpNextLock(Integer key) {
		if (replicationMode.equals("byz")) {
			try {
				owner.emulateLeaderByzReplicateState();
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Killing LockTable thread");
				System.exit(1);
			}
		}
		
		
		if (Replica.debugMode) {
			System.out.println("Calling waking up next lock for key: " + key);
		}

		PriorityQueue<LockAndCondition> queue = waitingLocks.get(key);
		if (queue != null && queue.size() > 0) {
			List<LeaseLock> sameKeyLocks = lockMap.get(key);
			LockAndCondition nextLockHolderCandidate = queue.peek();

			// If the next candidate for control of the lock has been aborted,
			// tell it to abort by
			// setting lockLeaseEnd to null and wake it so it can return this
			// information to the calling Responder
			if (!transactionBirthdates
					.containsKey(nextLockHolderCandidate.leaseLock.ownerTransactionID)) {
				nextLockHolderCandidate = queue.poll();
				nextLockHolderCandidate.lockLeaseEnd = null;
				synchronized (nextLockHolderCandidate.leaseLockCondition) {
					nextLockHolderCandidate.leaseLockCondition.notify();
				}
				wakeUpNextLock(key);

				// Easy case: There are currently no locks in the lock table for
				// this memory address
				// and there has never been a list of locks here in the history
				// of the system.
			} else if (sameKeyLocks == null) {
				sameKeyLocks = new LinkedList<LeaseLock>();
				lockMap.put(key, sameKeyLocks);
				wakeUpNextLockHelper(queue, sameKeyLocks, key);

				// If there has never been a list of locks in the lock table at
				// this memory address
			} else if (sameKeyLocks.size() == 0) {
				wakeUpNextLockHelper(queue, sameKeyLocks, key);

				// At least one lock is currently in the lock table
			} else {
				if (nextLockHolderCandidate.leaseLock.mode == AccessMode.READ
						&& sameKeyLocks.size() >= 1
						&& sameKeyLocks.get(0).mode == AccessMode.READ) {
					wakeUpNextLockHelper(queue, sameKeyLocks, key);
				} else if (nextLockHolderCandidate.leaseLock.mode == AccessMode.READ
						&& sameKeyLocks.size() == 1) {
					wakeUpNextLockOnlyOneCurrentLockHolderHelper(sameKeyLocks,
							queue, key);
				} else if (nextLockHolderCandidate.leaseLock.mode == AccessMode.WRITE
						&& sameKeyLocks.size() == 1
						&& sameKeyLocks.get(0).mode == AccessMode.WRITE) {
					wakeUpNextLockOnlyOneCurrentLockHolderHelper(sameKeyLocks,
							queue, key);
				} else if (nextLockHolderCandidate.leaseLock.mode == AccessMode.WRITE
						&& sameKeyLocks.size() >= 1) {
					LeaseLock toBeUpgrade = null;
					int finalCompareResult = Integer.MAX_VALUE;
					List<LeaseLock> toBeRemovedLocks = new LinkedList<LeaseLock>();
					for (LeaseLock sameKeyLock : sameKeyLocks) {
						if (!transactionBirthdates
								.containsKey(sameKeyLock.ownerTransactionID)) {
							toBeRemovedLocks.add(sameKeyLock);
						} else {
							if (committingWrites
									.containsKey(sameKeyLock.ownerTransactionID)) {
								finalCompareResult = -1;
							} else {
								int currentResult = wakeUpNextLockCompareHelper(
										sameKeyLock, nextLockHolderCandidate);
								finalCompareResult = Math.min(
										finalCompareResult, currentResult);
								if (currentResult == 0)
									toBeUpgrade = sameKeyLock;

								if (Replica.debugMode) {
									System.out
											.println("currentCompareResult : "
													+ currentResult
													+ ";transaction "
													+ sameKeyLock.ownerTransactionID);
								}
							}
						}
					}
					for (LeaseLock toBeRemovedLock : toBeRemovedLocks) {
						if (Replica.debugMode) {
							System.out.println("transaction "
									+ toBeRemovedLock.ownerTransactionID + " : " + toBeRemovedLock.lockedKey + 
									 " already aborted in wound wait one write multiple read and transaction birth date not valid");
						}
						sameKeyLocks.remove(toBeRemovedLock);
					}

					if (Replica.debugMode) {
						System.out.println("finalCompareResult : "
								+ finalCompareResult);
					}
					if (finalCompareResult >= 0) {
						for (LeaseLock sameKeyLock : sameKeyLocks) {
							if (Replica.debugMode) {
								System.out.println("transaction "
										+ sameKeyLock.ownerTransactionID + " : " + sameKeyLock.lockedKey + " : " + sameKeyLock.mode +
										 " already aborted in wound wait one write multiple read");
							}
							if (!sameKeyLock.equals(toBeUpgrade)) {
								transactionBirthdates
										.remove(sameKeyLock.ownerTransactionID);
							} else {
								if (Replica.debugMode) {
									System.out.println("transaction "
											+ toBeUpgrade.ownerTransactionID + " : " + toBeUpgrade.lockedKey + " : " + toBeUpgrade.mode +
											 " already aborted in wound wait one write multiple read upgrade");
								}
							}
						}
						
						sameKeyLocks.clear();
						wakeUpNextLockHelper(queue, sameKeyLocks, key);
					} else
						return;
				} else {
					System.out.println("this case should never happen");
					return;
				}
			}
		}
	}

	void wakeUpNextLockOnlyOneCurrentLockHolderHelper(
			List<LeaseLock> sameKeyLocks,
			PriorityQueue<LockAndCondition> queue, Integer key) {
		LeaseLock currentLockHolder = sameKeyLocks.get(0);
		if (!transactionBirthdates
				.containsKey(currentLockHolder.ownerTransactionID)) {
			sameKeyLocks.remove(currentLockHolder);
			if (Replica.debugMode) {
				System.out.println("transaction "
						+ currentLockHolder.ownerTransactionID + " : " + currentLockHolder.lockedKey + 
						 " already aborted in wound wait on one current lock holder case and transaction birth date not valid");
			
		}
			wakeUpNextLockHelper(queue, sameKeyLocks, key);
		} else {
			int compareResult = wakeUpNextLockCompareHelper(currentLockHolder,
					queue.peek());
			if (compareResult < 0) {
				return;
			} else if (compareResult > 0) {
				if (!committingWrites
						.containsKey(currentLockHolder.ownerTransactionID)) {
					// kill currentLockHolder
					if (Replica.debugMode) {
							System.out.println("transaction "
									+ currentLockHolder.ownerTransactionID + " : " + currentLockHolder.lockedKey + 
									 " already aborted in wound wait on one current lock holder case");
						
					}
					sameKeyLocks.remove(currentLockHolder);
					transactionBirthdates
							.remove(currentLockHolder.ownerTransactionID);
					wakeUpNextLockHelper(queue, sameKeyLocks, key);
				} else
					return;
			} else {
				System.out.println("this case should never happen");
				return;
			}
		}
	}

	/*
	 * a return result of 0 indicates it's the same transaction trying to
	 * upgrade a read lock to a write lock;a return result of >0 indicates the
	 * currentLockHolder is younger than the waiting transaction and should be
	 * killeda return result of <0 indicates the currentLockHolder is older than
	 * the waiting transaction and the waiting transaction should be waiting
	 */
	int wakeUpNextLockCompareHelper(LeaseLock currentLockHolder,
			LockAndCondition nextLockHolderCandidate) {
		Instant currentLockHolderBirthdate = transactionBirthdates
				.get(currentLockHolder.ownerTransactionID);
		if (Replica.debugMode) {
			System.out.println("currentLockHolderBirthdate: "
					+ currentLockHolderBirthdate + "nextLockHolderCandidate :"
					+ nextLockHolderCandidate);
		}
		int result = currentLockHolderBirthdate
				.compareTo(nextLockHolderCandidate.transactionBirthDate);
		if (result != 0) {
			return result;
		} else {
			// break ties on TransactionID
			return currentLockHolder.ownerTransactionID
					.compareTo(nextLockHolderCandidate.leaseLock.ownerTransactionID);
		}
	}

	void wakeUpNextLockHelper(PriorityQueue<LockAndCondition> queue,
			List<LeaseLock> sameKeyLocks, Integer key) {
		LockAndCondition nextLockHolderCandidate = queue.poll();
		sameKeyLocks.add(nextLockHolderCandidate.leaseLock);
		Instant leaseEnd = Instant.now().plus(Replica.LOCK_LEASE_INTERVAL);
		nextLockHolderCandidate.leaseLock.expirationTime = leaseEnd;
		nextLockHolderCandidate.lockLeaseEnd = leaseEnd;
		synchronized (nextLockHolderCandidate.leaseLockCondition) {
			nextLockHolderCandidate.leaseLockCondition.notify();
		}
		if (nextLockHolderCandidate.leaseLock.mode == AccessMode.READ) {
			wakeUpNextLock(key);
		}
	}

	synchronized void cleanUpLockTable() {
		if (replicationMode.equals("byz")) {
			try {
				owner.emulateLeaderByzReplicateState();
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Killing LockTable thread");
				System.exit(1);
			}
		}
		Instant cleanUpStartTime = Instant.now();
		for (List<LeaseLock> sameKeyLocks : lockMap.values()) {
			if (sameKeyLocks != null && sameKeyLocks.size() > 0) {
				int lockedKey = sameKeyLocks.get(0).lockedKey;
				List<LeaseLock> toBeRemovedLocks = new LinkedList<LeaseLock>();
				for (LeaseLock sameKeyLock : sameKeyLocks) {
					if (!transactionBirthdates
							.containsKey(sameKeyLock.ownerTransactionID)) {
						toBeRemovedLocks.add(sameKeyLock);
						if (Replica.debugMode) {
							System.out.println("transaction "
									+ sameKeyLock.ownerTransactionID + " : " + sameKeyLock.lockedKey 
									+ " already aborted in lease killer and transaction birth date not valid");
						}
					} else if (sameKeyLock.expirationTime
							.isBefore(cleanUpStartTime)
							&& !committingWrites
									.containsKey(sameKeyLock.ownerTransactionID)) {
						toBeRemovedLocks.add(sameKeyLock);
						transactionBirthdates
								.remove(sameKeyLock.ownerTransactionID);
						if (Replica.debugMode) {
							System.out.println("transaction "
									+ sameKeyLock.ownerTransactionID + " : " + sameKeyLock.lockedKey 
									+ " already aborted in lease killer");
						}
					}
				}
				for (LeaseLock toBeRemovedLock : toBeRemovedLocks) {
					sameKeyLocks.remove(toBeRemovedLock);
				}
				if (toBeRemovedLocks.size() > 0)
					wakeUpNextLock(lockedKey);
			}
		}
	}

	synchronized void releaseTableLocks(List<LeaseLock> locks,
			Long ownerTransactionID) {
		if (replicationMode.equals("byz")) {
			try {
				owner.emulateLeaderByzReplicateState();
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Killing LockTable thread");
				System.exit(1);
			}
		}
		
		
		if (Replica.debugMode) {
			System.out.println("Calling realeaseTableLocks for Transaction: "
					+ ownerTransactionID);
			System.out.println("It has locks: " + locks);
		}

		transactionBirthdates.remove(ownerTransactionID);
		for (LeaseLock lock : locks) {
			List<LeaseLock> sameKeyLocks = lockMap.get(lock.lockedKey);
			if (sameKeyLocks != null) {
				LeaseLock toBeRemovedLock = null;
				for (LeaseLock sameKeyLock : sameKeyLocks) {
					if (sameKeyLock.equals(lock)) {
						toBeRemovedLock = sameKeyLock;
						break;
					}
				}
				if (toBeRemovedLock != null) {
					sameKeyLocks.remove(toBeRemovedLock);
					wakeUpNextLock(toBeRemovedLock.lockedKey);
				}
			}
		}
		committingWrites.remove(ownerTransactionID);
		return;
	}

	synchronized boolean validateTableLock(List<LeaseLock> locks,
			Long ownerTransactionID) {
		// if transactionBirthDate is no longer found, it shows the transaction
		// is already aborted
		if (!transactionBirthdates.containsKey(ownerTransactionID)) {
			if (Replica.debugMode) {
				System.out.println("transaction birth date not valid in validateTableLock");
			}
			return false;
		}
		Instant validateStartTime = Instant.now();
		for (LeaseLock lock : locks) {
			List<LeaseLock> sameKeyLocks = lockMap.get(lock.lockedKey);
			// if no entry, it shows the lock has already been removed by
			// LeaseKiller so no longer valid
			if (sameKeyLocks == null) {
				System.out.println("sameKeyLocks null in validateTableLock");
				return false;
			} else {
				boolean isFound = false;
				for (LeaseLock sameKeyLock : sameKeyLocks) {
					if (sameKeyLock.equalForValidatingLocks(lock)) {
						isFound = true;
						if (sameKeyLock.expirationTime
								.isBefore(validateStartTime)) {
							if (Replica.debugMode) {
								System.out.println("transaction "
										+ sameKeyLock.ownerTransactionID + " : " + sameKeyLock.lockedKey 
										+ " already aborted in validateTableLock");
							}
							return false;
						}
						break;
					}
				}
				if (!isFound) {
					System.out.println("lock not found in validateTableLock");
					return false;
				}
			}
		}
		committingWrites.put(ownerTransactionID, locks);
		return true;
	}

}
