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
	
	public LeaseLock(Long ownerTransactionID, AccessMode mode, Instant expirationTime, int lockedKey) {
		this.ownerTransactionID = ownerTransactionID;
		this.mode = mode;
		this.expirationTime = expirationTime;
		this.lockedKey = lockedKey;
	}
	
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof LeaseLock)) {
			return false;
		} else {
			LeaseLock lock = (LeaseLock) obj;
			return ownerTransactionID.equals(lock.ownerTransactionID) && (mode == lock.mode) && (lockedKey == lock.lockedKey);
		}
	}
	
	
	
	 public int hashCode() { 
		 	int hash = 1;
		    return (int) ((hash * 31 + ownerTransactionID) * 31 + lockedKey) * 31 + mode.hashCode();
		   
	 }


}
