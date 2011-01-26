package org.deftserver.web.handler;

import org.deftserver.web.http.HttpRequest;
import org.deftserver.web.http.HttpResponse;

public class ForbiddenRequestHandler extends RequestHandler {

private final static ForbiddenRequestHandler instance = new ForbiddenRequestHandler();
	
	private ForbiddenRequestHandler() { }
	
	public static final ForbiddenRequestHandler getInstance() {
		return instance;
	}
	
	@Override
	public void get(HttpRequest request, HttpResponse response) {
		response.setStatusCode(403);
		response.setHeader("Connection", "close");
		response.write("Authentication failed");
	}
}