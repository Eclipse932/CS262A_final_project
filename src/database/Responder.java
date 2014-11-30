package database;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Set;

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
	
	//added by Jane; we need a method to get reference to the leader replica(s) from the remote registry
	public Set<Replica> getLeaderReplica(){
		//not implemented yet
		return null;
	}
	
}
