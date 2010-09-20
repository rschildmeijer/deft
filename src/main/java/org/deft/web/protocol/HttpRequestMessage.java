package org.deft.web.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestMessage {

	private final static Logger logger = LoggerFactory.getLogger(HttpRequestMessage.class);
	
	public static HttpRequestMessage of(ByteBuffer buffer) {
		logger.debug(new String(buffer.array(), Charset.forName("US-ASCII")));
		return new HttpRequestMessage();
	}
	
}
