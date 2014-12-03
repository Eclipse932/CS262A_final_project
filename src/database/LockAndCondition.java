package database;

public class LockAndCondition implements Comparable {
	LeaseLock leaseLock;
	Object leaseLockCondition;
	Long transactionBirthDate;
	
	public LockAndCondition(LeaseLock leaseLock, Object leaseLockCondition, long transactionBirthDate) {
		this.leaseLock = leaseLock;
		this.leaseLockCondition = leaseLockCondition;
		this.transactionBirthDate = transactionBirthDate;
	}
	
	public int compareTo(Object otherLC) {
		LockAndCondition other = (LockAndCondition) otherLC;
		return this.transactionBirthDate.compareTo(other.transactionBirthDate);
		
	}

}
