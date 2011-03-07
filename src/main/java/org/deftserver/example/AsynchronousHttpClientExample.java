package org.deftserver.example;

import static java.lang.System.out;

import org.deftserver.io.IOLoop;
import org.deftserver.web.AsyncResult;
import org.deftserver.web.http.client.AsynchronousHttpClient;
import org.deftserver.web.http.client.HttpResponse;

public class AsynchronousHttpClientExample {

	public static void main(String[] args) {
		AsynchronousHttpClient client = new AsynchronousHttpClient();
		client.fetch("http://tt.se/start/", new ResultCallback());
		IOLoop.INSTANCE.start();
	}
	
	private static class ResultCallback implements AsyncResult<HttpResponse> {

		@Override public void onFailure(Throwable caught) { out.println("exception caught: " + caught); }

		@Override public void onSuccess(HttpResponse response) { out.println("http resonse:\n" + response); }
		
	}

}
