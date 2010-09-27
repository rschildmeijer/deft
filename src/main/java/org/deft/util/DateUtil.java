package org.deft.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {

	private final static Locale LOCALE = Locale.US;
	private final static TimeZone GMT_ZONE;
	private final static String RFC_1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private final static DateFormat RFC_1123_FORMAT;
	private final static Calendar CALENDAR;
	
	static {
		RFC_1123_FORMAT = new SimpleDateFormat(RFC_1123_PATTERN, LOCALE);
		GMT_ZONE = TimeZone.getTimeZone("GMT");
		RFC_1123_FORMAT.setTimeZone(GMT_ZONE);
		CALENDAR = new GregorianCalendar(GMT_ZONE, LOCALE);
	}
		
	public static String getGurrentAsString() {
		return RFC_1123_FORMAT.format(CALENDAR.getTime());
	}
	
}