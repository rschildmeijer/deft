package org.deftserver.example;

import static java.lang.System.out;

import org.deftserver.io.IOLoop;
import org.deftserver.io.timeout.Timeout;
import org.deftserver.web.AsyncCallback;
import org.deftserver.web.AsyncResult;
import org.deftserver.web.http.client.AsynchronousHttpClient;
import org.deftserver.web.http.client.Response;

public class AsynchronousHttpClientExample {

	public static void main(String[] args) {
		AsynchronousHttpClient client = new AsynchronousHttpClient();
		client.fetch("http://sunet.se/", 
				new AsyncResult<Response>() {
					public void onFailure(Throwable caught) { out.println("exception:\n" + caught);} 
					public void onSuccess(Response response) { out.println("http resonse:\n" + response);} 
				}
		);
		IOLoop.INSTANCE.addTimeout(
				new Timeout(
						System.currentTimeMillis() + 1000, 
						new AsyncCallback() { public void onCallback() { IOLoop.INSTANCE.stop(); }}
				)
		);
		IOLoop.INSTANCE.start();
	}


}
