package org.ulme.example;

import java.util.HashMap;

import org.ulme.web.Application;
import org.ulme.web.HttpServer;
import org.ulme.web.RequestHandler;

public class UlmeClientExample {

	public static void main(String[] args) {
		Application application = new Application(
				new HashMap<String, RequestHandler>() {{ put("/", new ExampleRequestHandler()); }}
		);
		HttpServer server = new HttpServer(application);
		server.listen(8080).getIOLoop().start();
	}

}
