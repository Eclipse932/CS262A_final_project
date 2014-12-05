package database;

import java.util.List;
import java.util.Map;

public class LeaseKiller implements Runnable {
	LockTable lockTable;
	Map<Long, List<LeaseLock>> committingWrites;
	public LeaseKiller(LockTable lockTable, Map<Long, List<LeaseLock>>committingWrites) {
		this.lockTable = lockTable;
		this.committingWrites = committingWrites;
	}
	
	public void run() {
		
	}

}
