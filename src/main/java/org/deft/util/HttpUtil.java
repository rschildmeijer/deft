package org.deft.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

	public static String createInitialLineAndHeaders(int returnCode) {
		String s = createInitialLine(returnCode);

		s += s + "Content-Type: text/html\r\n";
		s += s + "Connection: close\r\n"; // we can't handle persistent connections
		return s;
	}
	
	// e.g. HTTP/1.0 200 OK or HTTP/1.0 404 Not Found (HTTP version + response status code + reason phrase)
	public static String createInitialLine(int statusCode) {
		String initialLine = "HTTP/1.1 ";

		switch (statusCode) {
		case 200:
			initialLine += "200 OK";
			break;
		case 400:
			initialLine += "400 Bad Request";
			break;
		case 403:
			initialLine += "403 Forbidden";
			break;
		case 404:
			initialLine += "404 Not Found";
			break;
		case 500:
			initialLine += "500 Internal Server Error";
			break;
		case 501:
			initialLine += "501 Not Implemented";
			break;
		default:
			logger.error("Uknonwn Http status code: " + statusCode);
			throw new IllegalArgumentException("Unknow Http status code: " + statusCode);
		}
		return initialLine + "\r\n";
	}

}
