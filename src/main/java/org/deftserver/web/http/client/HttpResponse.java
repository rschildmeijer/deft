package org.deftserver.web.http.client;

import java.util.Map;

import com.google.common.collect.Maps;

public class HttpResponse {
	
	private final long requestTime;
	
	public HttpResponse(long requestStarted) {
		requestTime = System.currentTimeMillis() - requestStarted;
	}
	
	private String statusLine;
	private final Map<String, String> headers = Maps.newHashMap();
	private String body;
	
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
	
}
