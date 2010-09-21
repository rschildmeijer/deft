package org.deft.web.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.deft.util.ArrayUtil;

public class HttpRequest {

	private final static Charset CHAR_SET = Charset.forName("US-ASCII");

	private final String requestLine;
	private final String method;
	private final String requestedPath;	// correct name?
	private final String version; 
	private Map<String, String> headers;

	public HttpRequest(String requestLine, Map<String, String> headers) {
		this.requestLine = requestLine;
		String[] elements = requestLine.split(" ");
		method = elements[0];
		requestedPath = elements[1];
		version = elements[2];
		this.headers = headers;
	}
	
	public static HttpRequest of(ByteBuffer buffer) {
		String raw = new String(buffer.array(), CHAR_SET);
		String[] fields = raw.split("\\r\\n");
		fields = ArrayUtil.removeTrailingEmptyStrings(fields);
		
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

}
