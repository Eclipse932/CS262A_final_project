package database;

import static org.junit.Assert.*;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RemoteRegistryTestLocal {

	RemoteRegistry me;
	RemoteRegistryIntf obj;
	
	public RemoteRegistryTestLocal() {
	}

	@Before
	public void setUp() throws Exception {
		System.out.println("RemoteRegistry server started");
		try { // special exception handler for registry creation
			LocateRegistry.createRegistry(1099);
			System.out.println("java RMI registry created.");
		} catch (RemoteException e) {
			// do nothing, error means registry already exists
			System.out.println("java RMI registry already exists.");
		}

		try {
			this.me = new RemoteRegistry();
			Naming.rebind("//localhost/RemoteRegistry", me);
		} catch (RemoteException e) {
			System.out.println("Error, unable to bind.");
		}

		try {
			this.obj = (RemoteRegistryIntf) Naming
					.lookup("//localhost/RemoteRegistry");
		} catch (RemoteException e) {
			System.out.println("Error, unable to startup.");
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void TestLocalRegisterName() {
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
	public void TestLocalUnRegisterName() {
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
