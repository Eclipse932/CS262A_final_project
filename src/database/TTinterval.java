package database;

import java.time.Instant;

public class TTinterval {

	public final Instant earliest;
	public final Instant latest;
	
	public TTinterval( Instant earliest, Instant latest) {
		this.earliest = earliest;
		this.latest = latest;
	}
}
