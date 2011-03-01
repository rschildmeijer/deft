package org.deftserver.web.http;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.HashMap;
import java.util.Map;

import org.deftserver.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestFactory {

	private static final Logger LOG = LoggerFactory.getLogger(HttpRequest.class);
	

	public static HttpRequest of(ByteBuffer buffer) {
		try {
	
			String raw =  new String(buffer.array(), buffer.position(), buffer.limit());
			
			String[] headersAndBody = raw.split("\\r\\n\\r\\n"); //
			String[] headerFields = headersAndBody[0].split("\\r\\n");
			headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");
            
			String requestLine = headerFields[0];
			Map<String, String> generalHeaders = new HashMap<String, String>();
			for (int i = 1; i < headerFields.length; i++) {
				String[] header = headerFields[i].split(": ");
				generalHeaders.put(header[0].toLowerCase(), header[1]);
			}
            
			String body = "";
			for (int i = 1; i < headersAndBody.length; ++i) { //First entry contains headers
				body += headersAndBody[i];
			}
			
			if (requestLine.contains("POST")) {
				int contentLength = Integer.parseInt(generalHeaders.get("content-length"));
				if (contentLength > body.length()) {
					return new PartialHttpRequest(requestLine, generalHeaders, body);
				}
			}
			return new HttpRequest(requestLine, generalHeaders, body);
		} catch (Exception t) {
			LOG.error("Bad HTTP format", t);
			return MalFormedHttpRequest.instance;
		}
	}
	
	public static HttpRequest continueParsing(ByteBuffer buffer, PartialHttpRequest unfinished) {
	
		String nextChunk = null;
		
			nextChunk = new String(buffer.array(), buffer.position(), buffer.limit());
	
		unfinished.appendBody(nextChunk);
		
		int contentLength = Integer.parseInt(unfinished.getHeader("Content-Length"));
		if (contentLength > unfinished.getBody().length()) {
			return unfinished;
		} else {
			return new HttpRequest(unfinished.getRequestLine(), unfinished.getHeaders(), unfinished.getBody());
		}
	}
	
}
