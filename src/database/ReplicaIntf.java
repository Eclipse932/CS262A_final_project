package database;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

public interface ReplicaIntf extends Remote{
	
	
	public boolean keepTransactionAlive(List<LeaseLock> locks) throws RemoteException;
	
	public String RWTcommit(Long transactionID, List<LeaseLock> heldLocks,
			HashMap<Integer, Integer> memaddrToValue) throws RemoteException;
	
	
}
