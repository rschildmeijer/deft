package org.deft.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.deft.web.Application;
import org.deft.web.HttpContext;
import org.deft.web.RequestHandler;


public class ApplicationTest {
	
	@Test
	public void simpleApplicationTest() {
		Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
		final RequestHandler handler = new RequestHandler() {
			@Override public void get(HttpContext ctx) { }
		};
		
		handlers.put("/", handler);
		Application app = new Application(handlers);
		
		assertNotNull(app.getHandler("/"));
		assertEquals(handler, app.getHandler("/"));
	}
}
