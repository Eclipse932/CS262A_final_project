package database;

enum AccessMode {
	READ,WRITE
}
public class LeaseLock {
	long ownerTransactionID;
	AccessMode mode;
	long expirationTime;
	long transactionBirthdate;
	int startOfKeyRange;
	
}
