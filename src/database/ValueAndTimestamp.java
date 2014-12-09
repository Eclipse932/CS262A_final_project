package database;

import java.io.Serializable;
import java.time.Instant;

public class ValueAndTimestamp implements Serializable{

	/**
	 * Default serialization id
	 */
	private static final long serialVersionUID = 1L;
	Integer value;
	Instant timestamp;
	
	
	public ValueAndTimestamp(Integer value, Instant timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}


	public Integer getValue() {
		return value;
	}


	@Override
	public String toString() {
		return "ValueAndTimestamp [value=" + value + ", timestamp=" + timestamp
				+ "]";
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
