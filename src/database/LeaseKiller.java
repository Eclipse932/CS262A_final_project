package database;


public class LeaseKiller implements Runnable {
	LockTable lockTable;
	long cleanUpInterval = 400000000;
	
	public LeaseKiller(LockTable lockTable) {
		this.lockTable = lockTable;
	}
	
	public void run() {
		while (true) {
			try{
				Thread.sleep(cleanUpInterval);
			} catch(InterruptedException e) {
				System.out.println("LeaseKiller was unable to sleep");
			}
			lockTable.cleanUpLockTable();
		}
	}

}
