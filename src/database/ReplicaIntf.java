package database;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public interface ReplicaIntf extends Remote {

	public Instant keepTransactionAlive(List<LeaseLock> locks)
			throws RemoteException;

	public String RWTcommit(Long transactionID, List<LeaseLock> heldLocks,
			HashMap<Integer, Integer> memaddrToValue, String replicationMode) throws RemoteException;

	public Instant getReplicaLock(LeaseLock lock) throws RemoteException,
			InterruptedException;

	public Integer RWTread(Integer databaseKey) throws RemoteException;

	public Instant beginTransaction(long transactionID) throws RemoteException;
	
	public void emmulateByzCommunication() throws RemoteException;

	// called by leader, served by replica
	// returns true if the replica was simulated as responsive
	public boolean byzPrepare(Long sequenceNumber, int numOfReplicasFromLeader)
			throws Exception;
	
	public boolean paxosPrepare(Long sequenceNumber)
			throws RemoteException;

	// called by leader, served by replica
	// returns true if the replica updated it's local dataMap
	public boolean paxosSlaveDuplicate(Long sequenceNumber, Integer memAddr,
			Integer value, Instant timestamp) throws RemoteException;

	// requests data from the specified argument up to before the newest sn
	// (this does not include the new value)!
	// called by replica, served by leader
	public ConcurrentHashMap<Integer, ValueAndTimestamp> requestSequenceData(
			Long replicaExpectedSn, Long leaderNewSequenceNumber)
			throws RemoteException;

}
