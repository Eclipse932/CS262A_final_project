package database;
import java.time.Instant;

public class LockWorker implements Runnable{
	LockTable lockTable;
	Instant lockLeaseEnd;
	LockAndCondition leaseLockCondition;
	public void run() {
		
	}
	
	public LockWorker(LockTable lockTable, LockAndCondition leaseLockCondition) {
		this.lockTable = lockTable;
		this.leaseLockCondition = leaseLockCondition;
	}
}
