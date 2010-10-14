package org.deftserver.web;

import org.deftserver.web.handler.RequestHandler;
import org.junit.Test;

import com.google.common.collect.Maps;


public class HttpServerTest {
	
	private final Application application = new Application(Maps.<String, RequestHandler>newHashMap());
	
	@Test(expected=IllegalArgumentException.class)
	public void testPortInRange_low() {
		int port = 0;
		HttpServer server = new HttpServer(application);
		server.listen(port);
	}
	

	@Test(expected=IllegalArgumentException.class)
	public void testPortInRange_high() {
		int port = 65536;
		HttpServer server = new HttpServer(application);
		server.listen(port);
	}
	
	public void testPortInRange_ok() {
		int port = 34;
		HttpServer server = new HttpServer(application);
		server.listen(port);
	}
	
}
