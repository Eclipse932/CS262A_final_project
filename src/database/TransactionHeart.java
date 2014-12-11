package database;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.time.Instant;
import java.util.ArrayList;

public class TransactionHeart implements Runnable {

	private static int leaseRenewInterval = 500;

	private Transaction myPairedTransaction;

	public TransactionHeart(Transaction myPairedTransaction) {
		this.myPairedTransaction = myPairedTransaction;
	}

	@Override
	public void run() {

//		System.out.println("Transaction Heart alive in thread "
//				+ Thread.currentThread());
 
		while (myPairedTransaction.isAlive()) {
			try {
				Thread.sleep(leaseRenewInterval);
			} catch (InterruptedException i) {
				System.out.println("TransactionHeart was unable to sleep");
			}
//			System.out.println("Transaction Heart in thread "
//					+ Thread.currentThread() + "should be"
//					+ " renewing its transaction now");

			ReplicaIntf leader = myPairedTransaction.myResponder.getLeader();

			Instant newLeaseEnd = null;
			HashMap<Integer, LeaseLock> currentlyHeldLocksMap = myPairedTransaction
					.deepCopyMyLocks();
			ArrayList<LeaseLock> currentlyHeldLocks = new ArrayList<LeaseLock>(
					currentlyHeldLocksMap.values());

			// This if statement is crucial because it checks to make sure we
			// don't try to call
			// keepTransactionAlive if the leader's lock table does not actually
			// have any locks for this transaction.
			if (!myPairedTransaction.deepCopyMyLocks().isEmpty()) {
				try {
					newLeaseEnd = leader
							.keepTransactionAlive(currentlyHeldLocks);
				} catch (RemoteException r) {
					System.out
							.println("Remote Exception in transactionHeart in thread "
									+ Thread.currentThread());
					System.out
							.println("Setting alive in associated transaction to false");
					System.out.println(r);
					myPairedTransaction.setAlive(false);
				}

				// If the leader returns a value of null, that means the extend
				// lease operation failed.
				if (newLeaseEnd == null) {
					myPairedTransaction.setAlive(false);
					break;
				}

				// Use the Instant returned by the extendLease method to update
				// the lease expirationtimes.
				boolean willBreak = false;
				for (LeaseLock newTimeLock : currentlyHeldLocks) {
					try {
						myPairedTransaction.changeLockExp(
								newTimeLock.getLockedKey(), newLeaseEnd);
					} catch (BadTransactionRequestException b) {
						// Abort
						System.out.println(b);
						System.out.println("aborting transaction ");
						myPairedTransaction.setAlive(false);
						willBreak = true;
					}
				}
				if (willBreak)
					break;
			}
		}
		return;
	}

}
