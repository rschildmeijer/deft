package org.deftserver.web.http;

import java.util.Map;


/**
 * Represents an unfinished "dummy" HTTP request, e.g, an HTTP POST request where the entire payload hasn't been 
 * received.
 * (E.g. because the size of the underlying (OS) socket's read buffer has a fixed size.)
 * 
 */

public class PartialHttpRequest extends HttpRequest {
	
	private final String requestLine;
	private String unfinishedBody;

	public PartialHttpRequest(String requestLine, Map<String, String> generalHeaders, String body) {
		super("POST <> Unfinished request\r\n", generalHeaders);
		this.requestLine = requestLine;
		this.unfinishedBody = body;
	}

	public void appendBody(String nextChunk) {
		unfinishedBody += nextChunk;
	}
	
	@Override
	public String getBody() {
		return unfinishedBody;
	}
	
	@Override
	public String getRequestLine() {
		return requestLine;
	}
	
}
