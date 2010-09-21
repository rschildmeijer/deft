package org.deft.web.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestMessage {

	private final static Logger logger = LoggerFactory.getLogger(HttpRequestMessage.class);
	private final static Charset CHAR_SET = Charset.forName("US-ASCII");

	private String requestLine;
	private Map<String, String> headers;

	public HttpRequestMessage(String requestLine, Map<String, String> headers) {
		this.requestLine = requestLine;
		this.headers = headers;
	}
	
	public static HttpRequestMessage of(ByteBuffer buffer) {
		
		String raw = new String(buffer.array(), CHAR_SET);
		String[] fields = raw.split("\\r\\n");

		String requestLine = fields[0];
		Map<String, String> generalHeaders = new HashMap<String, String>();
		for (int i = 1; i < fields.length; i++) {
			if (fields[i].trim().equals("")) {
				continue;
			}
			String[] header = fields[i].split(": ");
			generalHeaders.put(header[0], header[1]);
		}
		return new HttpRequestMessage(requestLine, generalHeaders);
	}

	public String getRequestLine() {
		return requestLine;
	}

	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}

}
