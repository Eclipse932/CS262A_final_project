package database;

import java.time.Instant;

public class LockAndCondition implements Comparable {
	LeaseLock leaseLock;
	Object leaseLockCondition;
	Instant transactionBirthDate;
	Instant lockLeaseEnd;
	
	public LockAndCondition(LeaseLock leaseLock, Object leaseLockCondition, Instant transactionBirthDate) {
		this.leaseLock = leaseLock;
		this.leaseLockCondition = leaseLockCondition;
		this.transactionBirthDate = transactionBirthDate;
	}
	
	public int compareTo(Object otherLC) {
		LockAndCondition other = (LockAndCondition) otherLC;
		
		int result = this.transactionBirthDate.compareTo(other.transactionBirthDate);
		if (result != 0) {
			return result;
		} else {
			//break ties on TransactionID
			return this.leaseLock.ownerTransactionID.compareTo(other.leaseLock.ownerTransactionID);
		}
		
	}

}
