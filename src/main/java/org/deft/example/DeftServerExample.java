package org.deft.example;

import java.util.HashMap;

import org.deft.web.Application;
import org.deft.web.HttpContext;
import org.deft.web.HttpServer;
import org.deft.web.RequestHandler;

public class DeftServerExample {
	
	private static class ExampleRequestHandler implements RequestHandler {

		@Override
		public void get(HttpContext ctx) {
			// ctx.getHttpResponse.write("hello world");
		}

	}

	public static void main(String[] args) {
		Application application = new Application(
				new HashMap<String, RequestHandler>() {{ put("/", new ExampleRequestHandler()); }}
		);
		HttpServer server = new HttpServer(application);
		server.listen(8080).getIOLoop().start();
	}
}
