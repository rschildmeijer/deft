package org.deft.web;

import org.deft.web.protocol.HttpRequest;
import org.deft.web.protocol.HttpResponse;

public interface RequestHandler {
	
	void get(HttpRequest request, HttpResponse response);

}
