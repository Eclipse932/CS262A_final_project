package database;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ClientApp {

	protected ResponderIntf entryPoint;
	protected Log InterfaceLog;

	private static String REMOTEREGISTRYIP = "128.32.48.222";
	private static RemoteRegistryIntf terraTestRemoteRegistry = null;

	public ClientApp() {
	}

	static void getInput(String responderName) throws MalformedURLException,
			NotBoundException {
		String firstTransaction = "write x=1; write y=4;";
		String secondTransaction = "read x; x++; read  y; y--; write x; write y";
		List<String> commands = (List<String>) new ArrayList<String>();
		commands.add("declare var1 2");
		commands.add("declare var2 2");
		commands.add("declare var3 0");
		commands.add("add var3 var1 var2");
		commands.add("write var3 25");

		ResponderIntf obj;

		String commandReturn = null;
		String TERRATEST = "128.32.48.222";

		try {
			RemoteRegistryIntf RRI = (RemoteRegistryIntf) Naming.lookup("//"
					+ TERRATEST + "/RemoteRegistry");
			RRI.registerNetworkName("//128.32.14.28/Responder0", "Responder0");
			String NN = RRI.getNetworkName(responderName);
			// obj = (ResponderIntf) Naming.lookup(NN);
			// commandReturn = obj.PRWTransaction(commands);

			System.out.println(NN);
		} catch (RemoteException e) {
			System.out.println("Error, unable to startup.");
		} catch (Exception e) {
			System.out.println(e);
			fail("Exception in testAddTransaction");
		}

		// assertTrue(commandReturn.equals("commit") ||
		// commandReturn.equals("abort"));
	}

	// Format of arguments: <Responder remote name> , <filename for getInput>
	public static void main(String[] args) {

		int expectedNumArgs = 2;

		ClientApp me = new ClientApp();

		if (args.length < expectedNumArgs) {
			System.out.println("Incorrect number of arguments.");
			System.out
					.println("Required format is <Responder remote name> , <filename for getInput>");
			System.exit(1);
		}

		String responderRemoteName = args[0];
		String inputFilename = args[1];

		// Acquire remoteRegistry to lookup the Responder
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

		// Use the remoteRegistry to lookup the responder's networkname
		String responderNetworkName = null;
		boolean firstIteration = true;

		do {
			if (!firstIteration) {
				// Give the responder a chance to register itself and try again
				System.out.println("Waiting for responder to be registered...");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException i) {
					System.out.println("Thread sleep has been interupted");
				}
			}
			try {
				responderNetworkName = terraTestRemoteRegistry
						.getNetworkName(responderRemoteName);
			} catch (Exception e) {
				System.out
						.println("Unable to connect to RemoteRegistry during lookup of leaderNetworkName");
				System.exit(1);
			}
			firstIteration = false;
		} while (responderNetworkName == null);

		// Use the responder's networkname to get its remote object
		try {
			me.entryPoint = (ResponderIntf) Naming.lookup(responderNetworkName);
		} catch (Exception e) {
			System.out
					.println("Unable to acquire the responder's remote object");
			System.out.println(e);
			System.exit(1);
		}

		try {
			getInput(inputFilename);
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}

	}
}
