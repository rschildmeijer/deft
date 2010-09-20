package org.deft.example;

import java.util.HashMap;


import org.deft.web.Application;
import org.deft.web.HttpContext;
import org.deft.web.HttpServer;
import org.deft.web.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class DeftServerExample {
	
	private final static Logger logger = LoggerFactory.getLogger(DeftServerExample.class);
	private final static int PORT = 8080;
	
	private static class ExampleRequestHandler implements RequestHandler {

		@Override
		public void get(HttpContext ctx) {
			// ctx.getHttpResponse.write("hello world");
		}

	}

	public static void main(String[] args) {
		
		//Log debug
		// assume SLF4J is bound to logback in the current environment
		//LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    // print logback's internal status
	    //StatusPrinter.print(lc);
		
		Application application = new Application(
				new HashMap<String, RequestHandler>() {{ put("/", new ExampleRequestHandler()); }}
		);
		logger.debug("Starting up server on port: " + PORT);
		HttpServer server = new HttpServer(application);
		server.listen(PORT).getIOLoop().start();
	
	}
}
