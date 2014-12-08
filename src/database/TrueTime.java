package database;

import java.time.Instant;

public class TrueTime {

	
	//NTP error should be on the order of milliseconds
	private static long epsilonNanos = 10000; 
	// TODO replace this with a value determined from ptpd
	
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
