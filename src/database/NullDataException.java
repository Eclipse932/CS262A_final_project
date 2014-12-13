package database;

public class NullDataException extends Exception {

	public NullDataException(String badCommand){
		super(badCommand);
	}
}
