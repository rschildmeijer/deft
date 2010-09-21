package org.deft.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.deft.web.protocol.HttpRequest;
import org.deft.web.protocol.HttpResponse;
import org.junit.Test;


public class ApplicationTest {
	
	@Test
	public void simpleApplicationTest() {
		Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
		final RequestHandler handler = new RequestHandler() {
			@Override public void get(HttpRequest request, HttpResponse response) { }
		};
		
		handlers.put("/", handler);
		Application app = new Application(handlers);
		
		assertNotNull(app.getHandler("/"));
		assertEquals(handler, app.getHandler("/"));
	}
}
