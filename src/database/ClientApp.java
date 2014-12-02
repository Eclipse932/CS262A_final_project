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
	protected String RMIRegistryAddress;
	protected Responder entryPoint;
	protected Log InterfaceLog;
	
	static void getInput(String responderName) throws MalformedURLException, NotBoundException{
		String firstTransaction="write x=1; write y=4;";
		String secondTransaction= "read x; x++; read  y; y--; write x; write y";
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
			RemoteRegistryIntf RRI = (RemoteRegistryIntf)Naming.lookup("//" + TERRATEST + "/RemoteRegistry");
			RRI.registerNetworkName("//128.32.14.28/Responder0", "Responder0");
			String NN = RRI.getNetworkName(responderName);
			//obj = (ResponderIntf) Naming.lookup(NN);
			//commandReturn = obj.PRWTransaction(commands);
			
			System.out.println(NN);
		} catch (RemoteException e) {
			System.out.println("Error, unable to startup.");
		}catch (Exception e) {
			System.out.println(e);
			fail("Exception in testAddTransaction");
		} 
		
		//assertTrue(commandReturn.equals("commit") || commandReturn.equals("abort"));
	}
	public static void main(String[]args){
		String argsTemp = "Responder0";
		try {
			getInput(argsTemp);
		} catch (MalformedURLException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
