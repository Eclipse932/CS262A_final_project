package database;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

public class Replica extends UnicastRemoteObject{
	String RMIRegistryAddress;
	boolean isLeader;
	int startOfKeyRange;
	int endOfKeyRange;
	String name;
	
	Log dataLog;
	Set<Replica> leaderSet;
	
	public Replica(String RMIRegistryAddress, boolean isLeader, int startOfKeyRange, int endOfKeyRange, String name) throws RemoteException {
		super();
		this.RMIRegistryAddress = RMIRegistryAddress;
		this.isLeader = isLeader;
		this.startOfKeyRange = startOfKeyRange;
		this.endOfKeyRange = endOfKeyRange;
		this.name = name;
		
	}
	
	public void setLog(Log dataLog) {
		this.dataLog = dataLog;
	}
	
	public void setLeaderSet(Set<Replica> leaderSet) {
		this.leaderSet = leaderSet;
	}
	
	
	
}
