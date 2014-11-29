package database;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class Responder extends UnicastRemoteObject implements ResponderIntf {

//	private ArrayList<Transaction> Transactions;
//	private ArrayList<TransactionHeart> TransactionHearts
	
	public Responder() throws RemoteException {
		super();
	}
	
	//Paxos Read Write Transaction
	//Returns "commit" if the transaction is succesful and "abort" if the transaction failed.
	public String PRWTransaction(List<String> Actions) throws RemoteException{
		//TODO implement this
		return "";
	}

}
