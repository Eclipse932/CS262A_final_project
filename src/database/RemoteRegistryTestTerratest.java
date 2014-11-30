package database;

import static org.junit.Assert.*;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RemoteRegistryTestTerratest {

	String TERRATEST = "128.32.48.222";
	RemoteRegistryIntf obj;
	
	

	public RemoteRegistryTestTerratest() {
	}

	@Before
	public void setUp() throws Exception {
		System.out.println("Trying to contact terratest.eecs.berkeley.edu");

		try {
			this.obj = (RemoteRegistryIntf) Naming
					.lookup("//" + TERRATEST + "/RemoteRegistry");
		} catch (RemoteException e) {
			System.out.println("Error, terratest.eecs.berkeley.edu ");
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void TestTerratestRegisterName() {
		try {
			this.obj.registerNetworkName("//128.32.14.28/fish1", "fish1");
		} catch (RemoteException e) {
			e.printStackTrace();
			fail("Unable to register");
		}

		try {
			assert (this.obj.getNetworkName("fish1")
					.equals("//128.32.14.28/fish1"));
		} catch (RemoteException e) {
			e.printStackTrace();
			fail("Unable to getNetworkName in RegisterName");
		}
	}

	@Test
	public void TestTerratestUnRegisterName() {
		try {
			this.obj.registerNetworkName("//128.32.14.28/fish1", "fish1");
		} catch (RemoteException e) {
			e.printStackTrace();
			fail("Unable to register");
		}

		try {
			this.obj.unRegisterRemoteName("fish1");
		} catch (RemoteException e) {
			e.printStackTrace();
			fail("Unable to unregister");
		}

		try {
			assert (this.obj.hasRemoteName("fish1") == false);
		} catch (RemoteException e) {
			e.printStackTrace();
			fail("Unable to use hasNetworkName");
		}
	}
}

