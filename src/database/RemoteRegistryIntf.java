package database;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistryIntf extends Remote {
	public String getNetworkName(String RemoteObjectName)
			throws RemoteException;

	public boolean registerNetworkName(String NetworkName,
			String RemoteObjectName) throws RemoteException;

	public boolean unRegisterRemoteName(String RemoteObjectName)
			throws RemoteException;

	public boolean hasRemoteName(String RemoteObjectName)
			throws RemoteException;
	
}
