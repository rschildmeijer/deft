package org.deftserver.web.protocol;

import com.google.common.collect.Maps;


public class MalFormedHttpRequest extends HttpRequest {

	public static final MalFormedHttpRequest instance = new MalFormedHttpRequest();
	
	/* Dummy HttpRequest that represents a malformed client HTTP request */
	private MalFormedHttpRequest() {
		super("GET / Mal formed request\r\n", Maps.<String, String>newHashMap());
	}

}
