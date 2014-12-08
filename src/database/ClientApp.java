package database;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.*;
import java.nio.charset.Charset;
public class ClientApp {
	protected String RMIRegistryAddress;
	protected Responder entryPoint;
	protected Log InterfaceLog;
	
	static void getInput(String responderName, String testFileName) throws NotBoundException, IOException{
		String firstTransaction="write x=1; write y=4;";
		String secondTransaction= "read x; x++; read  y; y--; write x; write y";
		List<String> commands = (List<String>) new ArrayList<String>();
		BufferedReader br = null;
		FileInputStream in = null;
		String         line;
		ResponderIntf obj;
		String commandReturn = null;
		String TERRATEST = "128.32.48.222";
		
		try{
			in = new FileInputStream("test-file/"+testFileName);
			br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
			while ((line = br.readLine()) != null) {
				if(line.equals("ENDING TRANSACTION")){
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
					commands.clear();
				}
				commands.add(line);
			}
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if (in != null) {
				in.close();
				br.close();
				br = null;
				in = null;
			}
	    }
		//assertTrue(commandReturn.equals("commit") || commandReturn.equals("abort"));
	}
	public static void main(String[]args) throws IOException{
		String argsTemp = "Responder0";
		try {
			getInput(argsTemp);
		} catch (MalformedURLException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
