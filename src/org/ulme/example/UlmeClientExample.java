package org.ulme.example;

import org.ulme.web.HttpServer;
import org.ulme.web.UlmeApplication;

/**
 * 
 * @author schildmeijer
*/

public class UlmeClientExample {

	public static void main(String[] args) {
		UlmeApplication application = new UlmeApplication("/", new ExampleRequestHandler());
		HttpServer server = new HttpServer(application);
		server.listen(8080);
		server.getIOLoop().start();
	}

}
