package database;

enum AccessMode {
	READ,WRITE
}
public class LeaseLock {
	long ownerTransactionID;
	AccessMode mode;
	long expirationTime;
	int lockedKey;
	
	public LeaseLock(long ownerTransactionID, AccessMode mode, long expirationTime, int lockedKey) {
		this.ownerTransactionID = ownerTransactionID;
		this.mode = mode;
		this.expirationTime = expirationTime;
		this.lockedKey = lockedKey;
	}
	
}
