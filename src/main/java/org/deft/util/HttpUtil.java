package org.deft.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

	public static String createHttpHeader(int returnCode) {
		String s = "HTTP/1.1 ";

		switch (returnCode) {
		case 200:
			s = s + "200 OK";
			break;
		case 400:
			s = s + "400 Bad Request";
			break;
		case 403:
			s = s + "403 Forbidden";
			break;
		case 404:
			s = s + "404 Not Found";
			break;
		case 500:
			s = s + "500 Internal Server Error";
			break;
		case 501:
			s = s + "501 Not Implemented";
			break;
		default:
			logger.error("Uknonwn Http-code: " + returnCode);
			throw new IllegalArgumentException("Unknow Http-code: " + returnCode);
		}

		s = s + "\r\n";
		s = s + "Content-Type: text/html\r\n";
		s = s + "Connection: close\r\n"; // we can't handle persistent connections
		s = s + "\r\n"; // this marks the end of the httpheader
		return s;
	}

}
