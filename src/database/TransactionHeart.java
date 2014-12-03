package database;

import java.rmi.RemoteException;
import java.util.List;

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

			boolean renewStatus = false;
			try {
				renewStatus = leader.keepTransactionAlive(myPairedTransaction
						.getMyLocks());
			} catch (RemoteException r) {
				System.out
						.println("Remote Exception in transactionHeart in thread "
								+ Thread.currentThread());
				System.out
						.println("Setting alive in associated transaction to false");
				System.out.println(r);
				myPairedTransaction.setAlive(false);
			}
			
			if(renewStatus == false) {
				myPairedTransaction.setAlive(false);
			}

		}
		return;
	}

}
