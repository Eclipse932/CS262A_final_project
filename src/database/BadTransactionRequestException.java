package database;

public class BadTransactionRequestException extends Exception {
	
	public BadTransactionRequestException(String badCommand){
		super(badCommand);
	}
}
