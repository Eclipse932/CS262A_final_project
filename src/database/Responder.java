package database;

import static org.junit.Assert.fail;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Set;

public class Responder extends UnicastRemoteObject implements ResponderIntf {

	// private ArrayList<Transaction> Transactions;
	// private ArrayList<TransactionHeart> TransactionHearts

	private static String REMOTEREGISTRYIP = "128.32.48.222";

	private static RemoteRegistryIntf terraTestRemoteRegistry = null;
	private static ReplicaIntf leader;

	public Responder() throws RemoteException {
		super();
	}

	// Paxos Read Write Transaction
	// Returns "commit" if the transaction is succesful and "abort" if the
	// transaction failed.
	public String PRWTransaction(List<String> Actions) throws RemoteException {
		// TODO implement this
		return "";
	}

	// arg0 = this computer's ip address
	// arg1 = the remoteName of this Responder process (e.g. Responder6)
	public static void main(String[] args) {
		String myIP = args[0];
		String myRemoteName = args[1];

		// TODO Fix the bug where if this exits after registering itself with
		// the
		// RemoteRegistry its registered name doesn't get removed!

		// Start local RMI server.
		System.out.println("Attempting to start local RMI Server for "
				+ myRemoteName + " at " + myIP);
		try { // special exception handler for registry creation
			LocateRegistry.createRegistry(1099);
			System.out.println("java RMI registry created.");
		} catch (RemoteException e) {
			// do nothing, error means registry already exists
			System.out.println("java RMI registry already exists.");
		}

		Responder me = null;
		try {
			me = new Responder();
		} catch (RemoteException r) {
			System.out.println("Unable to start local server");
			System.out.println(r);
			System.exit(1);
		}
		// Bind this object's instance to the local name on the local RMI registry
		try {
			Naming.rebind("//" + myIP + "/" + myRemoteName, me);
		} catch (Exception e) {
			System.out.println("Unable to bind this Responder to local server");
			System.out.println(e);
			System.exit(1);
		}
		System.out.println(myRemoteName
				+ " successfully bound in local registry");

		// Acquire remoteRegistry to first register this responder and second to
		// lookup the leader.
		System.out.println("Trying to contact terratest.eecs.berkeley.edu");
		try {
			terraTestRemoteRegistry = (RemoteRegistryIntf) Naming.lookup("//"
					+ REMOTEREGISTRYIP + "/RemoteRegistry");
		} catch (Exception e) {
			System.out.println("Error, terratest.eecs.berkeley.edu.");
			System.out
					.println("Please check to make sure you're connected to the internet.");
			System.out.println(e);
			System.exit(1);
		}

		// Use the remoteRegistry to lookup the leader's networkname
		String leaderNetworkName = null;
		boolean firstIteration = true;

		do {
			if (!firstIteration) {
				// Give the leader a chance to register itself and try again
				System.out.println("Waiting for leader to be registered...");
				try{
					Thread.sleep(2000);
				} catch (InterruptedException i){
					System.out.println("Thread sleep has been interupted");
				}
			}
			try {
				leaderNetworkName = terraTestRemoteRegistry
						.getNetworkName("Leader");
			} catch (Exception e) {
				System.out
						.println("Unable to connect to RemoteRegistry during lookup of leaderNetworkName");
				System.exit(1);
			}
		} while (leaderNetworkName == null);

		// Use the leader's networkname to get its remote object
		try {
			leader = (ReplicaIntf) Naming.lookup(leaderNetworkName);
		} catch (Exception e) {
			System.out.println("Unable to acquire the leader's remote object");
			System.out.println(e);
			System.exit(1);
		}
		
		// Register this Responder with the RemoteRegistry
		// Note that this must be done last, only after the Responder server is ready to field requests.
		boolean registrationStatus = false;
		try {
			registrationStatus = terraTestRemoteRegistry.registerNetworkName(
					"//" + myIP + "/" + myRemoteName, myRemoteName);
		} catch (RemoteException e) {
			System.out.println("Unable to register " + myRemoteName);
			e.printStackTrace();
			System.exit(1);
		}
		if (registrationStatus == false) {
			System.out.println("Error, RemoteName:" + myRemoteName
					+ " has already been taken");
			System.exit(1);
		}

	}
}
