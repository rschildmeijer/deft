package org.deftserver.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {

	private final static Locale LOCALE = Locale.US;
	private final static TimeZone GMT_ZONE;
	private final static String RFC_1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private final static DateFormat RFC_1123_FORMAT;
	
	static {
		RFC_1123_FORMAT = new SimpleDateFormat(RFC_1123_PATTERN, LOCALE);
		GMT_ZONE = TimeZone.getTimeZone("GMT");
		RFC_1123_FORMAT.setTimeZone(GMT_ZONE);
	}
		
	public static String getCurrentAsString() {
		return RFC_1123_FORMAT.format(new Date());
	}
	
}