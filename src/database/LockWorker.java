package database;

public class LockWorker implements Runnable{
	LockTable lockTable;
	public void run() {
		
	}
	
	public LockWorker(LockTable lockTable) {
		this.lockTable = lockTable;
	}
}
