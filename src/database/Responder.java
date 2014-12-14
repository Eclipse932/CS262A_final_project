package database;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Responder extends UnicastRemoteObject implements ResponderIntf {

	// private ArrayList<Transaction> Transactions;
	// private ArrayList<TransactionHeart> TransactionHearts
	public static boolean debugMode = false;

	private static String REMOTEREGISTRYIP = "128.32.48.222";
	private static String TRANSACTIONIDNAMERIP = "128.32.48.222";

	private static RemoteRegistryIntf terraTestRemoteRegistry = null;
	private static ReplicaIntf leader;
	private static ReplicaIntf nearestReplica;
	private static TransactionIdNamerIntf TIdNamer;

	// TODO figure out where this server is running

	public Responder() throws RemoteException {
		super();
	}

	public synchronized ReplicaIntf getLeader() {
		return this.leader;
	}

	private synchronized void setLeader(ReplicaIntf newLeader) {
		this.leader = newLeader;
	}

	private ReplicaIntf getNearestReplica() {
		return nearestReplica;
	}

	private void setNearestReplica(ReplicaIntf nearestReplica) {
		Responder.nearestReplica = nearestReplica;
	}

	private String processActions(List<String> actions,
			Transaction meTransaction, String replicationMode,
			String controlMode) throws BadTransactionRequestException {

		HashMap<String, Integer> variableTable = new HashMap<String, Integer>();
		HashMap<Integer, Integer> writeCache = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> readCache = new HashMap<Integer, Integer>();

		// Only used if in optimistic concurrency mode
		ArrayList<KeyAndTimestamp> readSet = new ArrayList<KeyAndTimestamp>();

		if (actions == null) {
			BadTransactionRequestException b = new BadTransactionRequestException(
					"Null list of arguments");
			throw b;
		}

		for (String action : actions) {

			if (Responder.debugMode) {
				System.out.println(action);
			}

			if (!meTransaction.isAlive()) {
				return "abort";
			}

			// Parse Action
			String[] elements = action.split(" ");
			if (elements.length < 1) {
				BadTransactionRequestException b = new BadTransactionRequestException(
						"Null list of arguments");
				throw b;
			}
			String command = elements[0];

			// declare <variable name> <integer constant initial value>
			if (command.equals("declare")) {
				if (elements.length != 3) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"declare has the wrong number of arguments\n"
									+ "correct form: declare <variable name> <integer constant initial value>");
					throw b;
				}

				String variableName = elements[1];
				Integer initialValue = null;
				try {
					initialValue = Integer.parseInt(elements[2]);
				} catch (NumberFormatException n) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"Argument 2 of declare does not parse as an integer");
					throw b;
				}

				// If variableName has not already been declared, add it to the
				// variableTable
				if (variableTable.containsKey(variableName)) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							variableName + " has already been declared");
					throw b;
				} else {
					variableTable.put(variableName, initialValue);
				}

				// TODO read is unable to handle the case where the replica
				// returns null for an uninitialized variable
				// TODO refactor this section. This could probably help the
				// above TODO

				// read <variable name> <memory address to be read from>
			} else if (command.equals("read")) {
				if (elements.length != 3) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"read has the wrong number of arguments\n"
									+ "correct form: read <variable name> <memory address to be read from>");
					throw b;
				}

				String variableName = elements[1];
				Integer memAddr = null;
				Integer valueAtMemAddr = null;

				try {
					memAddr = Integer.parseInt(elements[2]);
				} catch (NumberFormatException n) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"Argument 2 of read does not parse as an integer");
					throw b;
				}

				HashMap<Integer, LeaseLock> copiedLocks = null;
				if (controlMode.equals("lock")) {
					// Use this to check if we already have a read or write lock
					// on
					// the value
					copiedLocks = meTransaction.deepCopyMyLocks();
				}

				// First check if we already have this memAddr in the
				// writeBuffer
				if (writeCache.containsKey(memAddr)) {
					variableTable.put(variableName, writeCache.get(memAddr));
				}

				// Next check if we already have this memAddr in the readCache
				// Note that this must happen after we check the writeCache.
				else if (readCache.containsKey(memAddr)) {
					variableTable.put(variableName, readCache.get(memAddr));
				}

				// Otherwise we need to get it from a replica. Check for
				// optimistic first because it has the fewest subcases
				else if (controlMode.equals("opt")) {

					ValueAndTimestamp valueAndTimestampAtMemAddr = null;

					try {
						valueAndTimestampAtMemAddr = nearestReplica
								.optimisticRead(memAddr, replicationMode);
					} catch (Exception r) {
						System.out.println("Exception while trying to read "
								+ memAddr + " in"
								+ meTransaction.getTransactionID());
						System.out.println("Returning \"abort\"");
						System.out.println(r);
						r.printStackTrace();
						return "abort";
					}

					// If this memAddr has not been written before by any
					// transaction, default to reading a zero
					if (valueAndTimestampAtMemAddr == null
							|| valueAndTimestampAtMemAddr.getValue() == null
							|| valueAndTimestampAtMemAddr.getTimestamp() == null) {
						variableTable.put(variableName, 0);
					} else {
						variableTable.put(variableName,
								valueAndTimestampAtMemAddr.getValue());
					}

					KeyAndTimestamp memAddrAndTimestamp = new KeyAndTimestamp(
							memAddr, valueAndTimestampAtMemAddr.getTimestamp());
					readSet.add(memAddrAndTimestamp);
					readCache.put(memAddr,
							valueAndTimestampAtMemAddr.getValue());

				}

				// Attempt to acquire lock. Note that this may take a
				// long time
				// Also the expiration time for the lock created here is
				// given as null and must
				// be set by the leader.
				else if (!copiedLocks.containsKey(memAddr)) {
					LeaseLock lockForRead = new LeaseLock(
							meTransaction.getTransactionID(), AccessMode.READ,
							null, memAddr);
					Instant leaseLockExpiration = null;
					try {
						leaseLockExpiration = leader.getReplicaLock(
								lockForRead, replicationMode);
					} catch (Exception r) {
						System.out
								.println("Remote Exception or Interrupted Exception while trying to acquire LeaseLock in"
										+ meTransaction.getTransactionID());
						System.out.println("Returning \"abort\"");
						System.out.println(r);
						r.printStackTrace();
						return "abort";
					}

					// Check to see if the transaction has been aborted
					if (leaseLockExpiration == null) {
						return "abort";
					}

					// Add this lock to the transaction's list of locks.
					// This must happen AFTER we've acquired the lock in the
					// leader's lock table
					lockForRead.setExpirationTime(leaseLockExpiration);
					meTransaction.addLock(lockForRead);

					// Get the value from the database and associate it with the
					// variable
					// Note that I don't require that this variable already be
					// declared

					try {
						valueAtMemAddr = leader.RWTread(memAddr,
								replicationMode);
					} catch (Exception r) {
						System.out.println("Exception while trying to read "
								+ memAddr + " in"
								+ meTransaction.getTransactionID());
						System.out.println("Returning \"abort\"");
						System.out.println(r);
						r.printStackTrace();
						return "abort";
					}

					// If this memAddr has not been written before by any
					// transaction, default to reading a zero
					if (valueAtMemAddr == null) {
						variableTable.put(variableName, 0);
					} else {
						variableTable.put(variableName, valueAtMemAddr);
					}
					readCache.put(memAddr, valueAtMemAddr);

				} else if (copiedLocks.containsKey(memAddr)
						&& copiedLocks.get(memAddr).getMode() == AccessMode.READ) {

					// Don't attempt to acquire a new lock.
					// Still read from database because we might have lost the
					// old value

					// Get the value from the database and associate it with the
					// variable
					// Note that I don't require that this variable already be
					// declared
					valueAtMemAddr = null;
					try {
						valueAtMemAddr = leader.RWTread(memAddr,
								replicationMode);
					} catch (Exception r) {
						System.out
								.println("Remote Exception while trying to read "
										+ memAddr
										+ " in"
										+ meTransaction.getTransactionID());
						System.out.println("Returning \"abort\"");
						System.out.println(r);
						r.printStackTrace();
						return "abort";
					}

					// If this memAddr has not been written before by any
					// transaction, default to reading a zero
					if (valueAtMemAddr == null) {
						variableTable.put(variableName, 0);
					} else {
						variableTable.put(variableName, valueAtMemAddr);
					}

					readCache.put(memAddr, valueAtMemAddr);

				} else if ((copiedLocks.containsKey(memAddr) && copiedLocks
						.get(memAddr).getMode() == AccessMode.WRITE)) {
					// This should not happen because in this implementation
					// write locks are only acquired just before commit
					System.out
							.println("Error: A read operation in transaction "
									+ meTransaction.getTransactionID()
									+ " is attempting to read after we already acquired"
									+ " a write lock on that memory address ");
					System.out.println("Aborting");
					return "abort";

				}

				// write <variable name> <memory address to be written to>
			} else if (command.equals("write")) {
				if (elements.length != 3) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"write has the wrong number of arguments\n"
									+ "correct form: write <variable name> <memory address to be written to");
					throw b;
				}

				if (!variableTable.containsKey(elements[1])) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"write is trying to access a variable that doesn't exist");
					throw b;
				}
				Integer currentValueOfVariable = variableTable.get(elements[1]);

				Integer memAddr = null;
				try {
					memAddr = Integer.parseInt(elements[2]);
				} catch (NumberFormatException n) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"Argument 2 of write does not parse as an integer");
					throw b;
				}

				writeCache.put(memAddr, currentValueOfVariable);
				// Note that we acquire write locks and commit this structure
				// at the end of the transaction.

				// sub <variable name difference> <variable name minuend> <variable name subtrahend>
				// FOR CLARIFICATION: difference = minuend - subtrahend
			} else if (command.equals("sub")) {
				if (elements.length != 4) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"sub has the wrong number of arguments\n"
									+ "correct form: <variable name difference> <variable name minuend> <variable name subtrahend>");
					throw b;
				}

				String differenceName = elements[1];
				String minuendName = elements[2];
				String subtrahendName = elements[3];


				if(Responder.debugMode){
					System.out.println("Starting debug of sub");
					System.out.println("command parses as: " + "sub space " + differenceName + " space " + subtrahendName + " space " + minuendName);
				}
				
				// Get the terms from the variableTable and write their difference to
				// the table
				if (variableTable.containsKey(differenceName)
						&& variableTable.containsKey(minuendName)
						&& variableTable.containsKey(subtrahendName)) {
					Integer difference = variableTable.get(minuendName)
							- variableTable.get(subtrahendName);
					variableTable.put(differenceName, difference);
				} else {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"One of the arguments to sub has not already been declared.");
					throw b;
				}
				
				
				// add <variable name sum> <variable name addend> <variable name addend>
				// addend>
			} else if (command.equals("add")) {
				if (elements.length != 4) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"add has the wrong number of arguments\n"
									+ "correct form: add <variable name sum> <variable name addend> <variable name addend>");
					throw b;
				}

				String sumName = elements[1];
				String addendOneName = elements[2];
				String addendTwoName = elements[3];

				if (Responder.debugMode) {
					System.out.println("Starting debug of add");
					System.out.println("command parses as: " + "add space "
							+ sumName + " space " + addendOneName + " space "
							+ addendTwoName);
				}

				// Get the addends from the variableTable and write their sum to
				// the table
				if (variableTable.containsKey(sumName)
						&& variableTable.containsKey(addendOneName)
						&& variableTable.containsKey(addendTwoName)) {
					Integer sum = variableTable.get(addendOneName)
							+ variableTable.get(addendTwoName);
					variableTable.put(sumName, sum);
				} else {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"One of the arguments to add has not already been declared.");
					throw b;
				}

				// addc <variable name sum> <variable name> <integer constant
				// addend>
			} else if (command.equals("addc")) {
				if (elements.length != 4) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"addc has the wrong number of arguments\n"
									+ "correct form: addc <variable name sum> <variable name> <integer constant addend>");
					throw b;
				}

				String sumName = elements[1];
				String addendOneName = elements[2];
				Integer addendTwo = null;

				try {
					addendTwo = Integer.parseInt(elements[3]);
				} catch (NumberFormatException n) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"Argument 3 of addc does not parse as an integer");
					throw b;
				} catch (Exception e) {
					e.printStackTrace();
					throw e;
				}

				if (Responder.debugMode) {
					System.out.println("Starting debug of addc");
					System.out.println("command parses as: " + "addc space "
							+ sumName + " space " + addendOneName + " space "
							+ addendTwo);
					System.out
							.println("also variableTable.get(addendOneName) gives"
									+ variableTable.get(addendOneName));
				}

				if (variableTable.containsKey(sumName)
						&& variableTable.containsKey(addendOneName)) {
					Integer sum = variableTable.get(addendOneName) + addendTwo;
					variableTable.put(sumName, sum);
				} else {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"One of the arguments to addc has not already been declared.");
					throw b;
				}

				// wait <integer time to sleep in milliseconds>
			} else if (command.equals("wait")) {
				if (elements.length != 2) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"wait has the wrong number of arguments\n"
									+ "correct form: wait <integer time to sleep in milliseconds>");
					throw b;
				}

				Integer waitTime = null;
				try {
					waitTime = Integer.parseInt(elements[1]);
				} catch (NumberFormatException n) {
					BadTransactionRequestException b = new BadTransactionRequestException(
							"Argument 1 of wait does not parse as an integer");
					throw b;
				}
				if (waitTime > 0) {
					try {
						Thread.sleep(waitTime);
					} catch (InterruptedException i) {
						BadTransactionRequestException b = new BadTransactionRequestException(
								"Interrupted Exception caught during Thread.sleep in wait.");
						throw b;
					}
				}

				// Invalid command
			} else {
				BadTransactionRequestException b = new BadTransactionRequestException(
						command + " is an unsupported command.");
				throw b;
			}

		}

		// Perform buffered writes

		// Unecessary line, but this name is more informative for the role of
		// the map from here on
		HashMap<Integer, Integer> addrToVariableValue = writeCache;

		// Acquire write locks for these memory addresses and add them to the
		// transaction
		ArrayList<LeaseLock> addThisLockList;
		String commitStatus = "abort";

		if (controlMode.equals("lock")) {
			addThisLockList = new ArrayList<LeaseLock>();
			HashMap<Integer, LeaseLock> localCopyOfLocks = meTransaction
					.deepCopyMyLocks();

			for (Integer lockKey : addrToVariableValue.keySet()) {
				if (localCopyOfLocks.containsKey(lockKey)
						&& localCopyOfLocks.get(lockKey).getMode() == AccessMode.READ) {

					// Note that this is a copy, not the actual lock held in
					// this
					// transaction
					LeaseLock ll = localCopyOfLocks.get(lockKey);
					ll.setMode(AccessMode.WRITE);

					Instant expirationTime = null;
					try {
						expirationTime = leader.getReplicaLock(ll,
								replicationMode); // upgrade
					} catch (Exception r) {
						System.out
								.println("Remote Exception or Interrupted Exception while trying to acquire (upgrading) Write lock in Transaction "
										+ meTransaction.getTransactionID());
						System.out.println("Returning \"abort\"");
						System.out.println(r);
						r.printStackTrace();
						return "abort";
					}

					// Check to see if the transaction has been aborted
					if (expirationTime == null) {
						return "abort";
					}

					ll.setExpirationTime(expirationTime);

					// Upgrade the lock in this transaction's list to a Write
					// lock
					meTransaction.upgradeReadLockToWrite(lockKey);

				} else if (localCopyOfLocks.containsKey(lockKey)
						&& localCopyOfLocks.get(lockKey).getMode() == AccessMode.WRITE) {
					// do nothing, most certainly don't get a new lock
					// Although it's hard to think of a case where this would
					// happen
				} else if (!localCopyOfLocks.containsKey(lockKey)) {
					LeaseLock ll = new LeaseLock(
							meTransaction.getTransactionID(), AccessMode.WRITE,
							null, lockKey);
					Instant expirationTime = null;
					try {
						expirationTime = leader.getReplicaLock(ll,
								replicationMode);
					} catch (Exception r) {
						System.out
								.println("Remote Exception or Interrupted Exception while trying to acquire Write lock in Transaction "
										+ meTransaction.getTransactionID());
						System.out.println("Returning \"abort\"");
						System.out.println(r);
						r.printStackTrace();
						return "abort";
					}

					// Check to see if the transaction has been aborted
					if (expirationTime == null) {
						return "abort";
					}

					ll.setExpirationTime(expirationTime);
					addThisLockList.add(ll); // batch the write locks we'll be
												// getting
				}
			}
			meTransaction.addLocks(addThisLockList);
			// Add the batched write locks to this transaction's list of locks
			// Note that this MUST happen after we already acquire the write
			// locks

			// TODO check to make sure I didn't miss anything in this method
			ArrayList<LeaseLock> listOfLocks = new ArrayList<LeaseLock>(
					meTransaction.deepCopyMyLocks().values());

			if (meTransaction.isAlive()) {
				try {
					commitStatus = leader.RWTcommit(
							meTransaction.getTransactionID(), listOfLocks,
							addrToVariableValue, replicationMode);
				} catch (Exception r) {
					System.out
							.println("Remote Exception while trying to commit Transaction "
									+ meTransaction.getTransactionID());
					System.out.println("Returning \"abort\"");
					System.out.println(r);
					r.printStackTrace();
					return "abort";
				}
			}
		}

		// Control mode is opt
		else {
			try {
				commitStatus = nearestReplica.optimisticCommit(
						meTransaction.getTransactionID(), readSet,
						addrToVariableValue, replicationMode);
			} catch(Exception r){
				System.out.println(r);
				r.printStackTrace();
				return "abort";
			}
		}

		return commitStatus;
	}

	public String OBRWTransaction(List<String> actions)
			throws BadTransactionRequestException, RemoteException {

		String transactionStatus = null;
		try {
			transactionStatus = doOptimisticTransaction(actions, "byz");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return transactionStatus;
	}
	
	public String OPRWTransaction(List<String> actions)
			throws BadTransactionRequestException, RemoteException {

		String transactionStatus = null;
		try {
			transactionStatus = doOptimisticTransaction(actions, "pax");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return transactionStatus;
	}

	public String LPRWTransaction(List<String> actions)
			throws BadTransactionRequestException, RemoteException {

		String transactionStatus = null;
		try {
			transactionStatus = doLockingTransaction(actions, "pax");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return transactionStatus;
	}

	public String LBRWTransaction(List<String> actions)
			throws BadTransactionRequestException, RemoteException {

		String transactionStatus = null;
		try {
			transactionStatus = doLockingTransaction(actions, "byz");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return transactionStatus;
	}

	// Optimistic Read Write Transaction
	// Returns "commit" if the transaction is successful and "abort" if the
	// transaction failed.
	// If the list of actions given to this function is ill formed it throws a
	// BadTransactionRequestException

	private String doOptimisticTransaction(List<String> actions,
			String replicationMode) throws BadTransactionRequestException,
			RemoteException {

		// Get this transaction's GUID transactionID
		Long myTransactionID = TIdNamer.createNewGUID();

		Instant birthdateOnResponder = Instant.now();

		Transaction meTransaction = new Transaction(myTransactionID, this,
				birthdateOnResponder);

		// Don't start a transactionHeart for this transaction

		String transactionReturnStatus;
		try {
			transactionReturnStatus = processActions(actions, meTransaction,
					replicationMode, "opt");
		} catch (BadTransactionRequestException b) {
			throw b;
		}

		return transactionReturnStatus;
	}

	// Locking Read Write Transaction
	// Returns "commit" if the transaction is successful and "abort" if the
	// transaction failed.
	// If the list of actions given to this function is ill formed it throws a
	// BadTransactionRequestException

	private String doLockingTransaction(List<String> actions,
			String replicationMode) throws BadTransactionRequestException,
			RemoteException {
		// Get this transaction's GUID transactionID
		Long myTransactionID = TIdNamer.createNewGUID();

		Instant birthdateOnLeader = null;

		try {
			birthdateOnLeader = leader.beginTransaction(myTransactionID);
		} catch (RemoteException r) {
			System.out.println(r);
			return "abort";
		}

		Transaction meTransaction = new Transaction(myTransactionID, this,
				birthdateOnLeader);

		// Start a transactionHeart for this transaction
		TransactionHeart myHeart = new TransactionHeart(meTransaction);
		Thread myHeartThread = new Thread(myHeart);
		myHeartThread.start();

		String transactionReturnStatus;
		try {
			transactionReturnStatus = processActions(actions, meTransaction,
					replicationMode, "lock");
		} catch (BadTransactionRequestException b) {
			meTransaction.setAlive(false);
			throw b;
		} finally {
			// Kills the transactionHeart
			meTransaction.setAlive(false);
		}

		return transactionReturnStatus;
	}

	// arg0 = this computer's ip address
	// arg1 = the remoteName of this Responder process (e.g. responder6)
	// arg2 = debug mode true or false
	// arg3 = remoteName of neareastReplica

	public static void main(String[] args) {

		if (args.length != 4) {
			System.out.println("Incorrect number of command line arguments.");

			System.out
					.println("Correct form: IPaddress myRemoteName debugMode(True | False) remoteNameOfNearestReplica");

			System.exit(1);
		}
		String myIP = args[0];
		String myRemoteName = args[1];
		if (args[2].equals("true")) {
			Responder.debugMode = true;
		} else if (args[2].equals("false")) {
			Responder.debugMode = false;
		} else {
			System.out
					.println("type in isLeader is wrong argument! Default initialization is non-leader replica");
			Responder.debugMode = false;
		}

		String remoteNameOfNearestReplica = args[3];

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
		// Bind this object's instance to the local name on the local RMI
		// registry
		try {
			Naming.rebind("//" + myIP + "/" + myRemoteName, me);
		} catch (Exception e) {
			System.out.println("Unable to bind this Responder to local server");
			System.out.println(e);
			e.printStackTrace();
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
			e.printStackTrace();
			System.exit(1);
		}

		// Use the remoteRegistry to lookup the leader's networkname
		String leaderNetworkName = null;
		boolean firstIteration = true;

		do {
			if (!firstIteration) {
				// Give the leader a chance to register itself and try again
				System.out.println("Waiting for leader to be registered...");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException i) {
					System.out.println("Thread sleep has been interupted");
				}
			}
			try {
				leaderNetworkName = terraTestRemoteRegistry
						.getNetworkName("leader");
			} catch (Exception e) {
				System.out
						.println("Unable to connect to RemoteRegistry during lookup of leaderNetworkName");
				e.printStackTrace();
				System.exit(1);
			}
			firstIteration = false;
		} while (leaderNetworkName == null);

		// Use the leader's networkname to get its remote object
		try {
			me.setLeader((ReplicaIntf) Naming.lookup(leaderNetworkName));
		} catch (Exception e) {
			System.out.println("Unable to acquire the leader's remote object");
			System.out.println(e);
			e.printStackTrace();
			System.exit(1);
		}

		// Use the remoteRegistry to lookup the nearestReplica's networkname
		String nearestReplicaNetworkName = null;
		firstIteration = true;

		do {
			if (!firstIteration) {
				// Give the nearest replica a chance to register itself and try
				// again
				System.out
						.println("Waiting for nearestReplica to be registered...");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException i) {
					System.out.println("Thread sleep has been interupted");
				}
			}
			try {
				nearestReplicaNetworkName = terraTestRemoteRegistry
						.getNetworkName(remoteNameOfNearestReplica);
			} catch (Exception e) {
				System.out
						.println("Unable to connect to RemoteRegistry during lookup of nearestReplicaNetworkName ");
				e.printStackTrace();
				System.exit(1);
			}
			firstIteration = false;
		} while (nearestReplicaNetworkName == null);

		// Use the nearestReplica's networkname to get its remote object
		try {
			me.setNearestReplica((ReplicaIntf) Naming
					.lookup(nearestReplicaNetworkName));
		} catch (Exception e) {
			System.out
					.println("Unable to acquire the nearest replica's remote object");
			System.out.println(e);
			e.printStackTrace();
			System.exit(1);
		}

		// Acquire the TransactionIdNamer on terratest
		System.out
				.println("Trying to contact terratest.eecs.berkeley.edu for TransactionIdNamer");
		try {
			TIdNamer = (TransactionIdNamerIntf) Naming.lookup("//"
					+ TRANSACTIONIDNAMERIP + "/TransactionIdNamer");
		} catch (Exception e) {
			System.out.println("Error, terratest.eecs.berkeley.edu.");
			System.out
					.println("Please check to make sure you're connected to the internet.");
			System.out.println(e);
			e.printStackTrace();
			System.exit(1);
		}

		// Register this Responder with the RemoteRegistry
		// Note that this must be done last, only after the Responder server is
		// ready to field requests.
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
