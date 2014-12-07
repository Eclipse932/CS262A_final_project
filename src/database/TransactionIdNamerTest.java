package database;

import static org.junit.Assert.*;

import java.rmi.Naming;
import java.rmi.RemoteException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TransactionIdNamerTest {

	String TERRATEST = "128.32.48.222";
	TransactionIdNamerIntf obj;

	@Before
	public void setUp() throws Exception {
		System.out.println("Trying to contact terratest.eecs.berkeley.edu");

		try {
			this.obj = (TransactionIdNamerIntf) Naming.lookup("//" + TERRATEST
					+ "/TransactionIdNamer");
		} catch (RemoteException e) {
			System.out.println("Error, terratest.eecs.berkeley.edu ");
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void getNewGUID() {
		Long guid = null;
		try {
			guid = this.obj.createNewGUID();
		} catch (RemoteException e) {
			e.printStackTrace();
			fail("Unable to getNewGUID");
		}

		assert (guid != null);
		System.out.println("New Guid is: " + guid);
	}
}
