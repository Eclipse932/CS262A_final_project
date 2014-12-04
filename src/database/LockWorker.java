package database;
import java.time.Instant;
import java.util.PriorityQueue;

public class LockWorker implements Runnable{
	LockTable lockTable;
	LockAndCondition leaseLockCondition;
	
	public void run() {
		synchronized(leaseLockCondition.leaseLockCondition) {
			synchronized(lockTable) {
				PriorityQueue<LockAndCondition> waitingQueue = lockTable.waitingLocks.get(leaseLockCondition.leaseLock.lockedKey);
				if (waitingQueue == null) {
					waitingQueue = new PriorityQueue<LockAndCondition>();
					lockTable.waitingLocks.put(leaseLockCondition.leaseLock.lockedKey, waitingQueue);
				}
				waitingQueue.add(leaseLockCondition);
				lockTable.wakeUpNextLock();
			}
		}
	}
	
	public LockWorker(LockTable lockTable, LockAndCondition leaseLockCondition) {
		this.lockTable = lockTable;
		this.leaseLockCondition = leaseLockCondition;
	}
}
