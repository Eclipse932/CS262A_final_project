package database;

import java.time.Instant;

public class ValueAndTimestamp {

	Integer value;
	Instant timestamp;
	
	
	public ValueAndTimestamp(Integer value, Instant timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}


	public Integer getValue() {
		return value;
	}


	public void setValue(Integer value) {
		this.value = value;
	}


	public Instant getTimestamp() {
		return timestamp;
	}


	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
}
