package database;

import java.time.Instant;

public class ValueAndTimestamp {

	Integer value;
	Instant timestamp;
	
	
	public ValueAndTimestamp(Integer value, Instant timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}
}
