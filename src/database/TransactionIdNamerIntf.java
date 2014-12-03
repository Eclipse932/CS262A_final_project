package database;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TransactionIdNamerIntf extends Remote{

	public Long createNewGUID() throws RemoteException;
}
