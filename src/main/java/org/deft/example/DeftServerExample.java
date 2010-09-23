package org.deft.example;

import java.util.HashMap;

import org.deft.web.Application;
import org.deft.web.HttpServer;
import org.deft.web.handler.RequestHandler;
import org.deft.web.protocol.HttpRequest;
import org.deft.web.protocol.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class DeftServerExample {
	
	private final static Logger logger = LoggerFactory.getLogger(DeftServerExample.class);
	private final static int PORT = 8080;
	
	private static class ExampleRequestHandler extends RequestHandler {

		@Override
		public void get(HttpRequest request, HttpResponse response) {
			response.write("hello world");
		}

	}

	public static void main(String[] args) {
		Application application = new Application(
				new HashMap<String, RequestHandler>() {{ put("/", new ExampleRequestHandler()); }}
		);
		logger.debug("Starting up server on port: " + PORT);
		HttpServer server = new HttpServer(application);
		server.listen(PORT).getIOLoop().start();
	
	}
}
