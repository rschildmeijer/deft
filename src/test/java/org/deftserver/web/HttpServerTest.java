package org.deftserver.web;

import org.junit.Test;


public class HttpServerTest {

	@Test(expected=IllegalArgumentException.class)
	public void testPortInRange_low() {
		int port = 0;
		HttpServer server = new HttpServer(null);
		server.listen(port);
	}
	

	@Test(expected=IllegalArgumentException.class)
	public void testPortInRange_high() {
		int port = 65536;
		HttpServer server = new HttpServer(null);
		server.listen(port);
	}
	
	public void testPortInRange_ok() {
		int port = 34;
		HttpServer server = new HttpServer(null);
		server.listen(port);
	}
	
}
