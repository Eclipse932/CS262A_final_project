package database;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;

public class Replica extends UnicastRemoteObject implements ReplicaIntf{
	String RMIRegistryAddress;
	boolean isLeader;
	int startOfKeyRange;
	int endOfKeyRange;
	String name;
	
	Log dataLog;
	Set<Replica> leaderSet;
	
	Thread leaseKiller;
	LockTable lockTable;
	List<LeaseLock> committingWrites;
	
	public Replica(String RMIRegistryAddress, boolean isLeader, int startOfKeyRange, int endOfKeyRange, String name) throws RemoteException {
		super();
		this.RMIRegistryAddress = RMIRegistryAddress;
		this.isLeader = isLeader;
		this.startOfKeyRange = startOfKeyRange;
		this.endOfKeyRange = endOfKeyRange;
		this.name = name;
		this.lockTable = new LockTable();
		this.committingWrites = new LinkedList<LeaseLock>();
		this.leaseKiller = new Thread(new LeaseKiller(lockTable, committingWrites));
		leaseKiller.start();
		
	}
	
	public void setLog(Log dataLog) {
		this.dataLog = dataLog;
	}
	
	public void setLeaderSet(Set<Replica> leaderSet) {
		this.leaderSet = leaderSet;
	}
	
	
	
}
