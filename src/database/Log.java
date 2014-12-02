package database;
import java.util.*;

//log should provide a synchronized method interface
public class Log {
	static List<String> commands;
	
	public static boolean Append(String action){
		commands.add(action);
		return true;
	}
}
