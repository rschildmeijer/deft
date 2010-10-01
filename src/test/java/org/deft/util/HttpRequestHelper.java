package org.deft.util;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * This class can be used to create HttpRequests (and corresponding byte representations)
 */
public class HttpRequestHelper {
		
//Default request will look like this:
/*
GET / HTTP/1.0
Host: localhost:8080
User-Agent: Mozilla/5.0
From: abcde@qwert.com
 */
	
	enum ParameterDelimMode {
		AMPERSAND,
		SEMICOLON,
		MIXED
	}
	
	private ParameterDelimMode paramDelimMode = ParameterDelimMode.MIXED;
	
	private String protocol = "HTTP";
	private String method = "GET";
	private String version = "1.1";
	private String requestedPath = "/";
	private Map<String, String> headers = new HashMap<String, String>();
	private Map<String, String> getParameters = new HashMap<String, String>();
	
	public HttpRequestHelper() {
		headers.put("Host", "localhost:8080");
		headers.put("User-Agent", "Mozilla/5.0");
		headers.put("From", "abcde@qwert.com");
	}
	
	
	public String getRequestAsString() {
		String requestLine = createRequestLine();
		String headerString = createHeaders();
		//TODO Body
		String request = requestLine + headerString;
		return request;
	}


	public byte[] getRequestAsBytes() {
		String request = getRequestAsString();
		return request.getBytes();
	}
	
	public ByteBuffer getRequestAsByteBuffer() {
		return ByteBuffer.wrap(getRequestAsBytes());
	}
	
	public String addHeader(String name, String value) {
		return headers.put(name, value);
	}
	
	public String addGetParameter(String name, String value) {
		return getParameters.put(name, value);
	}
	
	public void setRequestedPath(String path) {
		requestedPath = path;
	}
	
	public void setParameterDelimMode(ParameterDelimMode mode) {
		paramDelimMode = mode;
	}
	
	private String getParameterDelimiter() {
		String delim;
		switch (paramDelimMode) {
		case AMPERSAND :
			delim = "&";
			break;
		case SEMICOLON :
			delim = ";";
			break;
		case MIXED:
			if (Math.random() > 0.5) {
				delim ="&"; 
			}
			else {
				delim = ";";
			}
			break;
		default : 
			delim = ";";
		}
		return delim;
	}
	
	
	/**
	 * Creates the initial request line, i.e:
	 * GET / HTTP/1.0
	 * 
	 * It also add \r\n to the end of the line
	 */
	private String createRequestLine() {
		String requestedPathWithParams = requestedPath;
		
		if (!getParameters.isEmpty()) { //Add get parameters
			requestedPathWithParams += "?";
			for (String paramName : getParameters.keySet()) {
				String delimiter = getParameterDelimiter();
				String value = getParameters.get(paramName);
				value = value == null? "" : value;
				requestedPathWithParams += paramName + "=" + value + delimiter;
			}
			//Remove last &
			requestedPathWithParams = requestedPathWithParams.substring(0, requestedPathWithParams.length()-1);
		}
		String reqLine = method + " " + requestedPathWithParams + " " + protocol + "/" + version + "\r\n";
		return reqLine;
	}
	
	/**
	 * Creates the header lines, i.e:
	 *  Host: localhost:8080
	 *	User-Agent: Mozilla/5.0
	 *	From: abcde@qwert.com
	 * 
	 * It also add \r\n to the end of the line
	 */
	private String createHeaders() {
		String result = "";
		for(String headerKey : headers.keySet()) {
			String headerValue = headers.get(headerKey);
			result += headerKey + ": " + headerValue + "\r\n"; 
		}
		result += "\r\n";
		return result;
	}
}
