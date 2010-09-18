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
		assert (port > MIN_PORT_NUMBER && port <= MAX_PORT_NUMBER)  : "Port out of range";
		ioLoop.listen(port);
		return this;
	}

}
