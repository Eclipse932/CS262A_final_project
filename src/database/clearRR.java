package database;

import java.rmi.Naming;
import java.rmi.RemoteException;

public class clearRR {

	static String TERRATEST = "128.32.48.222";
	
	public static void main(String[] args){
		
		RemoteRegistryIntf obj = null;
		System.out.println("Trying to contact terratest.eecs.berkeley.edu");
	
		try {
			obj = (RemoteRegistryIntf) Naming.lookup("//" + TERRATEST + "/RemoteRegistry");
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Error, terratest.eecs.berkeley.edu ");
		}
		
		try {
			obj.reset();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}
	
}
