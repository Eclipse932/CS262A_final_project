package database;

import java.time.Instant;

enum AccessMode {
	READ,WRITE
}
public class LeaseLock {
	Long ownerTransactionID;
	AccessMode mode;
	Instant expirationTime;
	int lockedKey;
	
	public LeaseLock(long ownerTransactionID, AccessMode mode, Instant expirationTime, int lockedKey) {
		this.ownerTransactionID = ownerTransactionID;
		this.mode = mode;
		this.expirationTime = expirationTime;
		this.lockedKey = lockedKey;
	}
	
}
