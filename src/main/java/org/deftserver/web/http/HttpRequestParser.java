package org.deftserver.web.http;

import java.nio.ByteBuffer;

public class HttpRequestParser {

	
	public HttpRequest parseRequestBuffer(ByteBuffer buffer){
		String method = readHttpMethod(buffer);
		HttpRequest res = null;
		
		return res;
	}
	
	
	
	protected String readHttpMethod(ByteBuffer buffer){
		
		return "";
	}

}
