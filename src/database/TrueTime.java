package database;

import java.time.Instant;

public class TrueTime {

	
	//Computed PTP error
	private static long epsilonNanos = 640000; 
	
	//returns TTinterval: [earliest, latest]
	public static TTinterval now() {
		Instant thisMoment = Instant.now();
		TTinterval retVal = new TTinterval(thisMoment.minusNanos(epsilonNanos),
				thisMoment.plusNanos(epsilonNanos));
		return retVal;
	}

	//returns true if t has definitely passed
	public static boolean after(Instant t) {
		Instant thisMoment = Instant.now();
		if (thisMoment.minusNanos(epsilonNanos).isAfter(t)) {
			return true;
		} else {
			return false;
		}
	}

	//returns true if t has definitely not arrived
	public static boolean before(Instant t) {
		Instant thisMoment = Instant.now();
		if (thisMoment.plusNanos(epsilonNanos).isBefore(t)) {
			return true;
		} else {
			return false;
		}
	}

}
