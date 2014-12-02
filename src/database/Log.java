package database;
import java.util.*;

//log should provide a synchronized method interface
public class Log {
	protected List<String> commands;
	
	public boolean Append(String action){
		return true;
	}
}
