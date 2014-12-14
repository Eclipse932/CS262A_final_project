package database;

import java.io.Serializable;
import java.time.Instant;

public class KeyAndTimestamp implements Serializable {

	@Override
	public String toString() {
		return "KeyAndTimestamp [key=" + key + ", timestamp=" + timestamp + "]";
	}
	/**
	 * Default serialization id
	 */
	private static final long serialVersionUID = 1L;
	Integer key;
	Instant timestamp;
	

	
	public KeyAndTimestamp(Integer key, Instant timestamp) {
		super();
		this.key = key;
		this.timestamp = timestamp;
	}
	public Integer getKey() {
		return key;
	}
	public void setKey(Integer Key) {
		this.key = key;
	}
	public Instant getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
	
}
