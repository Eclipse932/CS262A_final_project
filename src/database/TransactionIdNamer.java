package database;

public class TransactionIdNamer {
	static TransactionIdNamer TIDN;
	static long lastGUID;
	static long count;
	
	public TransactionIdNamer(){
		TIDN = this;
		this.count = 0;
	}
	
	protected synchronized long createNewGUID(){
		count++;
		return count;
	}

}
