package org.deftserver.web.http.client;

import java.util.Map;

import com.google.common.collect.Maps;

public class Response {
	
	private final long requestTime;
	private String statusLine;
	private final Map<String, String> headers = Maps.newHashMap();
	private String body = "";
	
	public Response(long requestStarted) {
		requestTime = System.currentTimeMillis() - requestStarted;
	}
	
	public void setStatuLine(String statusLine) {
		this.statusLine = statusLine;
	}
	
	public String getStatusLine() {
		return statusLine;
	}
	
	public void setHeader(String key, String value) {
		headers.put(key, value);
	}
	
	public String getHeader(String key) {
		return headers.get(key);
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public String getBody() {
		return body;
	}
	
	/**
	 * @return The total execution time of the request/response round trip.
	 */
	public long getRequestTime() {
		return requestTime;
	}
	
	@Override
	public String toString() {
		return "HttpResponse [body=" + body + ", headers=" + headers
				+ "\n, statusLine=" + statusLine + "]\n" + ", request time: " + requestTime +"ms";
	}

	void addChunk(String chunk) {
		body += chunk;
	}
	
}
