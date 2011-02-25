package org.deftserver.web.http;

import java.nio.charset.CharacterCodingException;
import java.util.Map;

import org.deftserver.io.buffer.DynamicByteBuffer;


/**
 * Represents an unfinished "dummy" HTTP request, e.g, an HTTP POST request where the entire payload hasn't been 
 * received.
 * (E.g. because the size of the underlying (OS) socket's read buffer has a fixed size.)
 * 
 */

public class PartialHttpRequest extends HttpRequest {
	

	private StringBuffer unfinishedBody;

	private boolean finished = false;
	
	public PartialHttpRequest(String[] requestLine, Map<String, String> generalHeaders, DynamicByteBuffer body) {
		super(requestLine, generalHeaders, body);
		
	}

	
	public PartialHttpRequest(String requestLine, Map<String, String> generalHeaders, String body) {
		super(requestLine, generalHeaders);
		unfinishedBody = new StringBuffer(body);
	}

	public void appendBody(String nextChunk) {
		unfinishedBody.append(nextChunk);
	}
	
	public void finish(){
		finished = true;
	}
	

	
	public boolean isFinished(){
		return finished;
	}
	
}
