package org.deftserver.web.handler;

import org.deftserver.web.protocol.HttpRequest;
import org.deftserver.web.protocol.HttpResponse;


public class NotFoundRequestHandler extends RequestHandler {

	private final static NotFoundRequestHandler instance = new NotFoundRequestHandler();
	
	private NotFoundRequestHandler() { }
	
	public static final NotFoundRequestHandler getInstance() {
		return instance;
	}
	
	@Override
	public void get(HttpRequest request, HttpResponse response) {
		response.setStatusCode(404);
		response.setHeader("Connection", "close");
		response.write("Requested URL: " + request.getRequestedPath() + " was not found");
	}
	
}
