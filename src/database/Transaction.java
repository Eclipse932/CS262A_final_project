package database;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.time.Instant;

public class Transaction {

	// Final Integer (transactionID) ----structure shared with TransactionHeart
	// b. TransactionHeart myHeart
	// c. List<Locks> myLocks ----- structure shared with the heart
	// d. Boolean alive ----- shared with heart
	// e. Set<Replica> Leaders --- shared with responder and TransactionHeart

	Long transactionID = null;
	HashMap<Integer, LeaseLock> myLocks = new HashMap<Integer, LeaseLock>();
	boolean alive = true;
	Responder myResponder;
	TTinterval myBirthDate;

	public Transaction(Long transactionIDinput, Responder myResponder,
			TTinterval birthdate) {
		this.transactionID = transactionIDinput;
		this.myResponder = myResponder;
		this.myBirthDate = birthdate;
	}

	public synchronized Long getTransactionID() {
		return transactionID;
	}

	public synchronized void setTransactionID(Long transactionID) {
		this.transactionID = transactionID;
	}

	//THIS METHOD SHOULD NOT BE CALLED
	//If you want to examine locks create a deep copy.
//	public synchronized ArrayList<LeaseLock> getMyLocks() {
//		ArrayList<LeaseLock> listView = new ArrayList<LeaseLock>(
//				myLocks.values());
//		return listView;
//	}

	public synchronized HashMap<Integer, LeaseLock> deepCopyMyLocks() {
		HashMap<Integer, LeaseLock> copiedMyLocks = new HashMap<Integer, LeaseLock>();
		for (Integer toCopyKey : myLocks.keySet()) {
			LeaseLock toCopy = myLocks.get(toCopyKey);
			LeaseLock addLock = new LeaseLock(toCopy.getOwnerTransactionID(),
					toCopy.getMode(), toCopy.getExpirationTime(),
					toCopy.getLockedKey());
			copiedMyLocks.put(toCopyKey, addLock);
		}
		return copiedMyLocks;
	}

	public synchronized void setMyLocks(ArrayList<LeaseLock> myLocks) {
		for (LeaseLock setLock : myLocks) {
			this.myLocks.put(setLock.getLockedKey(), setLock);
		}
	}

	public synchronized void addLocks(List<LeaseLock> myLocks) {
		for (LeaseLock aLock : myLocks) {
			this.myLocks.put(aLock.getLockedKey(), aLock);
		}
	}

	public synchronized void upgradeReadLockToWrite(Integer lockKey)
			throws BadTransactionRequestException {
		if (!(myLocks.containsKey(lockKey) && myLocks.get(lockKey).getMode() == AccessMode.READ)) {
			BadTransactionRequestException b = new BadTransactionRequestException(
					"Lock on "
							+ lockKey
							+ " that upgradeReadLockToWrite is attempting to change a lock that either "
							+ "does not exist in transaction or is not initialy in READ mode "
							+ transactionID + "'s list of locks");
			throw b;
		}

		myLocks.get(lockKey).setMode(AccessMode.WRITE);
	}

	public synchronized boolean changeLockExp(Integer key, Instant newExp)
			throws BadTransactionRequestException {
		// Note, this implementation must extend the lock regardless of lock
		// mode
		if (!myLocks.containsKey(key)) {
			BadTransactionRequestException b = new BadTransactionRequestException(
					"Lock on "
							+ key
							+ " that changeLockExp is attempting to change does not exist "
							+ "in transaction " + transactionID
							+ "'s list of locks");
			throw b;
		}
		myLocks.get(key).setExpirationTime(newExp);
		return true;
	}

	public synchronized boolean isAlive() {
		return alive;
	}

	public synchronized void setAlive(boolean alive) {
		this.alive = alive;
	}

}
