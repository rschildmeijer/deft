package org.deftserver.example;

import java.util.HashMap;
import java.util.Map;

import org.deftserver.web.Application;
import org.deftserver.web.HttpServer;
import org.deftserver.web.handler.RequestHandler;
import org.deftserver.web.protocol.HttpRequest;
import org.deftserver.web.protocol.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class DeftServerExample {
	
	private final static Logger logger = LoggerFactory.getLogger(DeftServerExample.class);
	private final static int PORT = 8080;
	
	private static class ExampleRequestHandler extends RequestHandler {

		@Override
		public void get(HttpRequest request, HttpResponse response) {
			response.write("hello world");
			//response.flush();
			//response.write("world");
			//response.flush();
			//response.write("!");
		}

	}

	public static void main(String[] args) {
		Map<String, RequestHandler> reqHandlers = new HashMap<String, RequestHandler>();
		reqHandlers.put("/", new ExampleRequestHandler());
		reqHandlers.put("/mySql", new AsyncDbHandler());
		
		Application application = new Application(reqHandlers);

		logger.debug("Starting up server on port: " + PORT);
		HttpServer server = new HttpServer(application);
		server.listen(PORT).getIOLoop().start();
	
	}
}
