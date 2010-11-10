package org.deftserver.web.handler;

import org.deftserver.web.http.HttpRequest;
import org.deftserver.web.http.HttpResponse;

public class BadRequestRequestHandler extends RequestHandler {

private final static BadRequestRequestHandler instance = new BadRequestRequestHandler();
	
	private BadRequestRequestHandler() { }
	
	public static final BadRequestRequestHandler getInstance() {
		return instance;
	}
	
	@Override
	public void get(HttpRequest request, HttpResponse response) {
		response.setStatusCode(400);
		response.setHeader("Connection", "close");
		response.write("HTTP 1.1 requests must include the Host: header");
	}
}
