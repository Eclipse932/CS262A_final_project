package database;

import static org.junit.Assert.*;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResponderTest {

	Responder me;
	ResponderIntf obj;
	static final String COMMIT = "commit";
	static final String ABORT = "abort";

	public ResponderTest() {
	}

	@Before
	public void setUp() throws Exception {

		try { // special exception handler for registry creation
			LocateRegistry.createRegistry(1099);
			System.out.println("Responder server created.");
		} catch (RemoteException e) {
			// do nothing, error means registry already exists
			System.out.println("Responder server already exists.");
		}

		try {
			this.me = new Responder();
			Naming.rebind("//localhost/Responder", me);
		} catch (RemoteException e) {
			System.out.println("Error, unable to bind.");
		}

		try {
			this.obj = (ResponderIntf) Naming.lookup("//localhost/Responder");
		} catch (RemoteException e) {
			System.out.println("Error, unable to startup.");
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	// declare <variable name> <integer constant initial value>
	// read <variable name> <memory address to be read from>
	// write <variable name> <memory address to be written to>
	// add <variable name sum> <variable name addend> <variable name addend>
	// addc <variable name sum> <variable name> <integer constant addend>

	@Test
	public void testWriteOnlyTransaction() {
		List<String> commands = (List<String>) new ArrayList<String>();
		commands.add("declare var1 0");
		commands.add("write var1 300 ");

		String commandReturn = null;

		try {
			commandReturn = this.obj.LPRWTransaction(commands);
			System.out.println(commandReturn);
		} catch (Exception e) {
			System.out.println(e);
			fail("Exception in testWriteOnlyTransaction");
		}

		assertTrue(commandReturn.equals(COMMIT) || commandReturn.equals(ABORT));
	}

	@Test
	public void testNullTransaction() {
		List<String> commands = null;
		boolean BTRE = false;
		try{
			this.obj.LPRWTransaction(commands);
		} catch (RemoteException r) {
			System.out.println(r);
			fail("Remote Exception in testNullTransaction");
		} catch (BadTransactionRequestException b) {
			BTRE = true;
		}
		assertTrue(BTRE);
	}
	
	@Test
	public void testBadTransactionCommand() {
		List<String> commands = new ArrayList<String>();
		commands.add("");
		boolean BTRE = false;
		try{
			this.obj.LPRWTransaction(commands);
		} catch (RemoteException r) {
			System.out.println(r);
			fail("Remote Exception in testNullTransaction");
		} catch (BadTransactionRequestException b) {
			BTRE = true;
		}
		assertTrue(BTRE);
	}
	
	
	@Test
	public void testAddTransaction() {
		List<String> commands = (List<String>) new ArrayList<String>();
		commands.add("declare var1 2");
		commands.add("declare var2 2");
		commands.add("declare var3 0");
		commands.add("add var3 var1 var2");
		commands.add("write var3 25");

		String commandReturn = null;

		try {
			commandReturn = this.obj.LPRWTransaction(commands);
			System.out.println(commandReturn);
		} catch (Exception e) {
			System.out.println(e);
			fail("Exception in testAddTransaction");
		}

		assertTrue(commandReturn.equals(COMMIT) || commandReturn.equals(ABORT));
	}
	
	@Test
	public void testAddcTransaction() {
		List<String> commands = (List<String>) new ArrayList<String>();
		commands.add("declare var1 0");
		commands.add("addc var1 var1 7");
		commands.add("write var1 250");

		String commandReturn = null;

		try {
			commandReturn = this.obj.LPRWTransaction(commands);
			System.out.println(commandReturn);
		} catch (Exception e) {
			System.out.println(e);
			fail("Exception in testAddcTransaction");
		}

		assertTrue(commandReturn.equals(COMMIT) || commandReturn.equals(ABORT));
	}
}