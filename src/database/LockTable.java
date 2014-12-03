package database;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.time.Instant;

public class LockTable {
	private Map<Integer, List<LeaseLock>> lockMap;
	private Map<Long, Instant> transactionBirthdates;
	private Map<Integer, PriorityQueue<LockAndCondition>> waitingLocks;
	
	public LockTable() {
		this.lockMap = new HashMap<Integer, List<LeaseLock>>();
		this.transactionBirthdates = new HashMap<Long, Instant>();
		this.waitingLocks = new HashMap<Integer, PriorityQueue<LockAndCondition>>();
	}
	
	
	
}
