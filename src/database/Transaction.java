package database;

import java.util.ArrayList;
import java.util.List;

public class Transaction {
	
//	Final Integer (transactionID) ----structure shared with TransactionHeart
//	b.	TransactionHeart myHeart
//	c.	List<Locks> myLocks ----- structure shared with the heart
//	d.	Boolean alive ----- shared with heart
//	e.	Set<Replica> Leaders --- shared with responder and TransactionHeart
	
	
	Long transactionID = null;
	ArrayList<LeaseLock> myLocks = new ArrayList<LeaseLock>();
	boolean alive = true;
	Responder myResponder;
	TTinterval myBirthDate;
			
	public Transaction(Long transactionIDinput, Responder myResponder, TTinterval birthdate){
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

	public synchronized ArrayList<LeaseLock> getMyLocks() {
		return myLocks;
	}

	public synchronized void setMyLocks(ArrayList<LeaseLock> myLocks) {
		this.myLocks = myLocks;
	}
	
	public synchronized void addLocks(List<LeaseLock> myLocks) {
		for(LeaseLock aLock: myLocks){
			this.myLocks.add(aLock);
		}
	}

	public synchronized boolean isAlive() {
		return alive;
	}

	public synchronized void setAlive(boolean alive) {
		this.alive = alive;
	}
	
	
	
	
}
