package org.ulme.web;


public class HttpServer {
	
	private static final int MIN_PORT_NUMBER = 1;
	private static final int MAX_PORT_NUMBER = 65535;

	private final IOLoop ioLoop = new IOLoop();
	private final Application application;
	
	public HttpServer(Application app) {
		application = app;
	}

	public IOLoop getIOLoop() {
		return ioLoop;
	}

	/**
	 * @return this for chaining purposes
	 */
	public HttpServer listen(int port) {
		if (port <= MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
			throw new IllegalArgumentException("Invalid port number. Valid range: [" + 
					MIN_PORT_NUMBER + ", " + MAX_PORT_NUMBER + ")");
		}
		
		ioLoop.listen(port);
		return this;
	}

}
