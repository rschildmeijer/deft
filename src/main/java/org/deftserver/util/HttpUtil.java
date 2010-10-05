package org.deftserver.util;

import org.deftserver.web.protocol.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

	private static final String _200_OK 		 			= "HTTP/1.1 200 OK\r\n"; 
	private static final String _400_BAD_REQUEST			= "HTTP/1.1 400 Bad Request\r\n"; 
	private static final String _403_FORBIDDEN 			 	= "HTTP/1.1 403 Forbidden\r\n"; 
	private static final String _404_NOT_FOUND 	 			= "HTTP/1.1 404 Not Found\r\n"; 
	private static final String _500_INTERNAL_SERVER_ERROR	= "HTTP/1.1 500 Internal Server Error\r\n"; 
	private static final String _501_NOT_IMPLEMENTED		= "HTTP/1.1 501 Not Implemented\r\n"; 
	
	// e.g. HTTP/1.0 200 OK or HTTP/1.0 404 Not Found (HTTP version + response status code + reason phrase)
	public static String createInitialLine(int statusCode) {

		switch (statusCode) {
		case 200:
			return _200_OK;
		case 400:
			return _400_BAD_REQUEST;
		case 403:
			return _403_FORBIDDEN;
		case 404:
			return _404_NOT_FOUND;
		case 500:
			return _500_INTERNAL_SERVER_ERROR;
		case 501:
			return _501_NOT_IMPLEMENTED;
		default:
			logger.error("Uknonwn Http status code: " + statusCode);
			throw new IllegalArgumentException("Unknow Http status code: " + statusCode);
		}
	}
	
	
	public static boolean verifyRequest(HttpRequest request) {
		String version = request.getVersion();
		boolean requestOk = true;
		if (version.equals("HTTP/1.1")) { //TODO might be optimized? Could do version.endsWith("1"), or similar
			requestOk =  (request.getHeader("Host") != null);
		}
		
		return requestOk;
	}

}
