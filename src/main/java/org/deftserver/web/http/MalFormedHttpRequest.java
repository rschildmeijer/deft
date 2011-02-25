package org.deftserver.web.http;

import org.deftserver.io.buffer.DynamicByteBuffer;

import com.google.common.collect.Maps;


public class MalFormedHttpRequest extends HttpRequest {

	public static final MalFormedHttpRequest instance = new MalFormedHttpRequest();
	
	/* Dummy HttpRequest that represents a malformed client HTTP request */
	private MalFormedHttpRequest() {
		super(new String []{"GET", "/ Mal formed request", "HTTP/1.1"}, Maps.<String, String>newHashMap(), DynamicByteBuffer.allocate(100));
	}

}
