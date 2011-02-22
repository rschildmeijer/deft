package org.deftserver.example;

import java.util.Map;

import org.deftserver.io.IOLoop;
import org.deftserver.web.Application;
import org.deftserver.web.HttpServer;
import org.deftserver.web.handler.RequestHandler;
import org.deftserver.web.http.HttpRequest;
import org.deftserver.web.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;



public class DeftServerExample {
	
	private final static Logger logger = LoggerFactory.getLogger(DeftServerExample.class);
	private final static int PORT = 8080;
	
	private static class ExampleRequestHandler extends RequestHandler {

		@Override
		public void get(HttpRequest request, HttpResponse response) {
			response.write("hello world");
		}
		
		@Override
		public void post(HttpRequest request, HttpResponse response) {
			response.write("hello post world\nbody: " + request.getBody());
		}

	}

	public static void main(String[] args) {
		Map<String, RequestHandler> handlers = Maps.newHashMap();
		handlers.put("/", new ExampleRequestHandler());
		handlers.put("/mySql", new AsyncDbHandler());
		
		Application application = new Application(handlers);
		application.setStaticContentDir("static");
		
		//HttpServerDescriptor.KEEP_ALIVE_TIMEOUT = 30 * 1000;	// 30s  
		//HttpServerDescriptor.READ_BUFFER_SIZE = 1500;			// 1500 bytes 
		//HttpServerDescriptor.WRITE_BUFFER_SIZE = 1500;			// 1500 bytes 
		

		logger.debug("Starting up server on port: " + PORT);
		HttpServer server = new HttpServer(application);
		server.listen(PORT);
		IOLoop.INSTANCE.start();
	
	}
}
