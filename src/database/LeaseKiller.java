package database;

import java.util.List;

public class LeaseKiller implements Runnable {
	LockTable lockTable;
	List<LeaseLock> committingWrites;
	public LeaseKiller(LockTable lockTable, List committingWrites) {
		this.lockTable = lockTable;
		this.committingWrites = committingWrites;
	}
	
	public void run() {
		
	}

}
