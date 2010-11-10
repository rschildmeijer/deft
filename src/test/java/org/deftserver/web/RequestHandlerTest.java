package org.deftserver.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.deftserver.web.handler.RequestHandler;
import org.deftserver.web.http.HttpRequest;
import org.deftserver.web.http.HttpResponse;
import org.junit.Test;


public class RequestHandlerTest {


	static class RequestHandler1 extends RequestHandler {

		@Override
		@Asynchronous
		public void get(HttpRequest request, HttpResponse response) {}

	}

	static class RequestHandler2 extends RequestHandler {

		@Override
		public void get(HttpRequest request, HttpResponse response) { }

		
		@Override
		@Asynchronous
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

}
