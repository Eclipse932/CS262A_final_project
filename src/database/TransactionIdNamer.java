package database;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class TransactionIdNamer extends UnicastRemoteObject implements
		TransactionIdNamerIntf {
	//static TransactionIdNamer TIDN;
	//static long lastGUID;
	static Long count;
	static int objectPortOnTerratest = 1049;
	static String myRemoteName = "TransactionIdNamer";
	static String myIP = "128.32.48.222"; //terratest

	public TransactionIdNamer() throws RemoteException {
		//TIDN = this;
		super(objectPortOnTerratest);
		count = new Long(0);

	}

	// Matt: Note that this needs to be synchronized but it can't be
	// synchronized in the interface
	// I'm not sure if this is a problem...
	public synchronized Long createNewGUID() throws RemoteException {
		count++;
		return count;
	}

	public static void main(String[] args) {
		// Start local RMI server.
		System.out.println("Attempting to start local RMI Server for "
				+ myRemoteName + " at terratest.");
		try { // special exception handler for registry creation
			LocateRegistry.createRegistry(1099);
			System.out.println("java RMI registry created.");
		} catch (RemoteException e) {
			// do nothing, error means registry already exists
			System.out.println("java RMI registry already exists.");
		}
		
		
		TransactionIdNamer me = null;
		try {
			me = new TransactionIdNamer();
		} catch (RemoteException r) {
			System.out.println("Unable to start local TransactionIdNamer server");
			System.out.println(r);
			System.exit(1);
		}
		// Bind this object's instance to the local name on the local RMI
		// registry
		try {
			Naming.rebind("//" + myIP + "/" + myRemoteName, me);
		} catch (Exception e) {
			System.out.println("Unable to bind TransactionIdNamer to local server");
			System.out.println(e);
			System.exit(1);
		}
		
		System.out.println(myRemoteName
				+ " successfully bound in local registry");
		
	
	}
}
