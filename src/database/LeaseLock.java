package database;

enum AccessMode {
	READ,WRITE
}
public class LeaseLock {
	long ownerTransactionID;
	AccessMode mode;
	long expirationTime;
	long transactionBirthdate;
	int lockedKey;
	
	public LeaseLock(long ownerTransactionID, AccessMode mode, long expirationTime, long transactionBirthdate, int lockedKey) {
		this.ownerTransactionID = ownerTransactionID;
		this.mode = mode;
		this.expirationTime = expirationTime;
		this.transactionBirthdate = transactionBirthdate;
		this.lockedKey = lockedKey;
		
	}
	
}
