package org.deftserver.web.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.deftserver.web.Asynchronous;
import org.deftserver.web.Authenticated;
import org.deftserver.web.HttpVerb;
import org.deftserver.web.handler.RequestHandler;
import org.deftserver.web.http.HttpRequest;
import org.deftserver.web.http.HttpResponse;
import org.junit.Test;


public class RequestHandlerTest {


	static class RequestHandler1 extends RequestHandler {

		@Override
		@Asynchronous
		@Authenticated
		public void get(HttpRequest request, HttpResponse response) {}

	}

	static class RequestHandler2 extends RequestHandler {

		@Override
		public void get(HttpRequest request, HttpResponse response) { }

		
		@Override
		@Asynchronous
		@Authenticated
		public void post(HttpRequest request, HttpResponse response) { }

		
	}

	@Test
	public void testAsynchronousAnnotations() {
		RequestHandler rh1 = new RequestHandler1();
		RequestHandler rh2 = new RequestHandler2();

		assertTrue(rh1.isMethodAsynchronous(HttpVerb.GET));
		
		assertFalse(rh2.isMethodAsynchronous(HttpVerb.GET));
		assertTrue(rh2.isMethodAsynchronous(HttpVerb.POST));
	}
	
	@Test
	public void testAuthenticatedAnnotations() {
		RequestHandler rh1 = new RequestHandler1();
		RequestHandler rh2 = new RequestHandler2();

		assertTrue(rh1.isMethodAuthenticated(HttpVerb.GET));
		assertFalse(rh1.isMethodAuthenticated(HttpVerb.POST));
		assertFalse(rh1.isMethodAuthenticated(HttpVerb.DELETE));
		
		assertFalse(rh2.isMethodAuthenticated(HttpVerb.GET));
		assertFalse(rh2.isMethodAuthenticated(HttpVerb.PUT));
		assertTrue(rh2.isMethodAuthenticated(HttpVerb.POST));
	}

}
