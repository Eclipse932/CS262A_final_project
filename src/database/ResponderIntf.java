package database;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.List;
import java.util.Set;

//Responder -M
//1.	Fields
//	a.	String RMIRegistryAddress
//	b.	Private Threadpool Transactions
//	c.	Private Threadpool TransactionHearts
//	d.	Set<Replica> Leaders ---shared with Transaction and TransactionHeart
//2.	Methods
//	a.	Private Boolean startNewTransaction( List<String> Actions, Integer transactionID )
//	i.	This is the method that starts new Transaction threads
//	b.	String PRWTransaction( List<String> Actions)
//	c.	String BRWTransaction( List<String> Actions)
//	d.	TODO support Read only transactions
//3.	Comments
//	a.	The two threadpools must be the same size

public interface ResponderIntf extends Remote {

	public String LPRWTransaction(List<String> Actions) throws RemoteException, BadTransactionRequestException;
	
	public String LBRWTransaction(List<String> Actions) throws RemoteException, BadTransactionRequestException;

	public String OPRWTransaction(List<String> Actions) throws RemoteException, BadTransactionRequestException;
	
	public String OBRWTransaction(List<String> Actions) throws RemoteException, BadTransactionRequestException;

	
	//added by Jane; we need a method to get reference to the leader replica(s) from the remote registry
	//public Set<Replica> getLeaderReplica();
}
