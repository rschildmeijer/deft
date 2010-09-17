package org.ulme.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.ulme.web.Application;
import org.ulme.web.Context;
import org.ulme.web.RequestHandler;


public class ApplicationTest {
	
	@Test
	public void simpleApplicationTest() {
		Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
		final RequestHandler handler = new RequestHandler() {
			@Override public void get(Context ctx) { }
		};
		
		handlers.put("/", handler);
		Application app = new Application(handlers);
		
		assertNotNull(app.getHandler("/"));
		assertEquals(handler, app.getHandler("/"));
	}
}
