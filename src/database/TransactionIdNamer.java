package database;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

public class TransactionIdNamer extends UnicastRemoteObject implements TransactionIdNamerIntf{
	static TransactionIdNamer TIDN;
	static long lastGUID;
	static Long count;
	
	public TransactionIdNamer() throws RemoteException{
		TIDN = this;
		this.count = new Long(0);
	}
	
	//Matt: Note that this needs to be synchronized but it can't be synchronized in the interface
	// I'm not sure if this is a problem...
	public synchronized Long createNewGUID() throws RemoteException{
		count++;
		return count;
	}

}
