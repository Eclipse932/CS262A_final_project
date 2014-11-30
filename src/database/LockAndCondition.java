package database;

import java.util.concurrent.locks.Condition;

public class LockAndCondition {
	LeaseLock leaseLock;
	Condition leaseLockCondition;
	
	public LockAndCondition(LeaseLock leaseLock, Condition leaseLockCondition) {
		this.leaseLock = leaseLock;
		this.leaseLockCondition = leaseLockCondition;
	}

}
