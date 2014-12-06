package database;
import java.time.Instant;
import java.util.PriorityQueue;

public class LockWorker implements Runnable{
	LockTable lockTable;
	LockAndCondition leaseLockAndCondition;
	
	public void run() {
		synchronized(leaseLockAndCondition.leaseLockCondition) {
			synchronized(lockTable) {
				PriorityQueue<LockAndCondition> waitingQueue = lockTable.waitingLocks.get(leaseLockAndCondition.leaseLock.lockedKey);
				if (waitingQueue == null) {
					waitingQueue = new PriorityQueue<LockAndCondition>();
					lockTable.waitingLocks.put(leaseLockAndCondition.leaseLock.lockedKey, waitingQueue);
				}
				waitingQueue.add(leaseLockAndCondition);
				lockTable.wakeUpNextLock(leaseLockAndCondition.leaseLock.lockedKey);
			}
		}
	}
	
	public LockWorker(LockTable lockTable, LockAndCondition leaseLockCondition) {
		this.lockTable = lockTable;
		this.leaseLockAndCondition = leaseLockCondition;
	}
}
