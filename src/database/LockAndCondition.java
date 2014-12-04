package database;

import java.time.Instant;

public class LockAndCondition implements Comparable {
	LeaseLock leaseLock;
	Object leaseLockCondition;
	Instant transactionBirthDate;
	
	public LockAndCondition(LeaseLock leaseLock, Object leaseLockCondition, Instant transactionBirthDate) {
		this.leaseLock = leaseLock;
		this.leaseLockCondition = leaseLockCondition;
		this.transactionBirthDate = transactionBirthDate;
	}
	
	public int compareTo(Object otherLC) {
		LockAndCondition other = (LockAndCondition) otherLC;
		return this.transactionBirthDate.compareTo(other.transactionBirthDate);
		
	}

}
