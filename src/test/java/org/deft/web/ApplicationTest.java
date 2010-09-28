package org.deft.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.deft.web.handler.RequestHandler;
import org.deft.web.protocol.HttpRequest;
import org.deft.web.protocol.HttpResponse;
import org.junit.Test;


public class ApplicationTest {
	
	@Test
	public void simpleApplicationTest() {
		Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
		final RequestHandler handler1 = new RequestHandler() {
			@Override public void get(HttpRequest request, HttpResponse response) { }
		};
		final RequestHandler handler2 = new RequestHandler() {
			@Override public void get(HttpRequest request, HttpResponse response) { }
		};
		final RequestHandler handler3 = new RequestHandler() {
			@Override public void get(HttpRequest request, HttpResponse response) { }
		};
		final RequestHandler handler4 = new RequestHandler() {
			@Override public void get(HttpRequest request, HttpResponse response) { }
		};
		
		handlers.put("/", handler1);
		handlers.put("/persons/([0-9]+)", handler2);
		handlers.put("/persons/phone_numbers", handler3);
		handlers.put("/pets/([0-9]{0,3})", handler4);
		Application app = new Application(handlers);
		
		assertNotNull(app.getHandler("/"));
		assertNotNull(app.getHandler("/persons/1911"));
		assertNotNull(app.getHandler("/persons/phone_numbers"));
		assertNotNull(app.getHandler("/pets/123"));
		
		assertEquals(handler1, app.getHandler("/"));
		assertEquals(handler2, app.getHandler("/persons/1911"));
		assertEquals(handler3, app.getHandler("/persons/phone_numbers"));
		assertEquals(handler4, app.getHandler("/pets/123"));
	}
}
