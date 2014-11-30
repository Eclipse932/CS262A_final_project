package database;

import java.util.concurrent.locks.Condition;

public class LockAndCondition implements Comparable {
	LeaseLock leaseLock;
	Condition leaseLockCondition;
	Long transactionBirthDate;
	
	public LockAndCondition(LeaseLock leaseLock, Condition leaseLockCondition, long transactionBirthDate) {
		this.leaseLock = leaseLock;
		this.leaseLockCondition = leaseLockCondition;
		this.transactionBirthDate = transactionBirthDate;
	}
	
	public int compareTo(Object otherLC) {
		LockAndCondition other = (LockAndCondition) otherLC;
		return this.transactionBirthDate.compareTo(other.transactionBirthDate);
		
	}

}
