package database;

import java.rmi.RemoteException;
import java.util.List;
import java.time.Instant;

public class TransactionHeart implements Runnable {

	private static int leaseRenewInterval = 500;

	private Transaction myPairedTransaction;

	public TransactionHeart(Transaction myPairedTransaction) {
		this.myPairedTransaction = myPairedTransaction;
	}

	@Override
	public void run() {

		System.out.println("Transaction Heart alive in thread "
				+ Thread.currentThread());

		while (myPairedTransaction.isAlive()) {
			try {
				Thread.sleep(leaseRenewInterval);
			} catch (InterruptedException i) {
				System.out.println("TransactionHeart was unable to sleep");
			}
			System.out.println("Transaction Heart in thread "
					+ Thread.currentThread() + "should be"
					+ " renewing its transaction now");

			ReplicaIntf leader = myPairedTransaction.myResponder.getLeader();

			Instant newLeaseEnd = null;
			try {
				newLeaseEnd = leader.keepTransactionAlive(myPairedTransaction
						.getMyLocks());
				//TODO change the expirationTime in lock in Transaction
			} catch (RemoteException r) {
				System.out
						.println("Remote Exception in transactionHeart in thread "
								+ Thread.currentThread());
				System.out
						.println("Setting alive in associated transaction to false");
				System.out.println(r);
				myPairedTransaction.setAlive(false);
			}
			
			if(newLeaseEnd == null) {
				myPairedTransaction.setAlive(false);
			}

		}
		return;
	}

}
