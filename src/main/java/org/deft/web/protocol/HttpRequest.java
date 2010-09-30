package org.deft.web.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.deft.util.ArrayUtil;
import org.deft.web.HttpVerb;

public class HttpRequest {

	private final static Charset CHAR_SET = Charset.forName("US-ASCII");

	private final String requestLine;
	private final HttpVerb method;
	private final String requestedPath;	// correct name?
	private final String version; 
	private Map<String, String> headers;
	private Map<String, String> parameters;

	public HttpRequest(String requestLine, Map<String, String> headers) {
		this.requestLine = requestLine;
		String[] elements = requestLine.split(" ");
		method = HttpVerb.valueOf(elements[0]);
		requestedPath = elements[1];
		version = elements[2];
		this.headers = headers;
		parameters = parseParameters(requestedPath);
	}
	
	public static HttpRequest of(ByteBuffer buffer) {
		String raw = new String(buffer.array(), CHAR_SET);
		String[] fields = raw.split("\\r\\n");
		fields = ArrayUtil.dropFromEndWhile(fields, "");
		
		String requestLine = fields[0];
		Map<String, String> generalHeaders = new HashMap<String, String>();
		for (int i = 1; i < fields.length; i++) {
			String[] header = fields[i].split(": ");
			generalHeaders.put(header[0], header[1]);
		}
		return new HttpRequest(requestLine, generalHeaders);
	}

	public String getRequestLine() {
		return requestLine;
	}
	
	public String getRequestedPath() {
		return requestedPath;
	}

	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}
	
	public HttpVerb getMethod() {
		return method;
	}
	
	public String getParameter(String name) {
		return parameters.get(name);
	}
	
	public Map<String, String> getParameters() {
		return parameters;
	}
	
	@Override
	public String toString() {
		String result = "METHOD: " + method + "\n";
		result += "VERSION: " + version + "\n";
		result += "PATH: " + requestedPath + "\n";
		
		result += "--- HEADER --- \n";
		for (String key : headers.keySet()) {
			String value = headers.get(key);
			result += key + ":" + value + "\n";
		}
		
		result += "--- PARAMETERS --- \n";
		for (String key : parameters.keySet()) {
			String value = parameters.get(key);
			result += key + ":" + value + "\n";
		}
		return result;
	}
	
	private Map<String, String> parseParameters(String requestLine) {
		Map<String, String> params = new HashMap<String, String>();
		String[] str = requestLine.split("\\?");
		
		//Parameters exist
		if (str.length > 1) {
			String[] paramArray = str[1].split("\\&"); //TODO JP support ; delimiter
			for (String keyValue : paramArray) {
				String[] keyValueArray = keyValue.split("=");
				String name = keyValueArray[0];
				String value = null;
				
				//We need to check if the parameter has a value associated with it.
				if (keyValueArray.length > 1) {
					value = keyValueArray[1];
				}
				params.put(name, value);
			}
		}
		return params;
	}
	
	//Enumeration getParameterNames();
}
