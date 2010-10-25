package org.deftserver.example;

import org.deftserver.web.AsyncResult;
import org.deftserver.web.Asynchronous;
import org.deftserver.web.handler.RequestHandler;
import org.deftserver.web.protocol.HttpRequest;
import org.deftserver.web.protocol.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncDbHandler extends RequestHandler{

	Logger logger = LoggerFactory.getLogger(AsyncDbHandler.class);
	
	@Asynchronous
	public void get(HttpRequest request, HttpResponse response) {
		logger.debug("Entering AsyncDbHandler.get");
		new AsyncDbApi().getNameFromId("123", new MyCallback(request, response));
		logger.debug("Leaving AsyncDbHandler.get");
	}
	

	private class MyCallback implements AsyncResult<String> {

		HttpRequest request; 
		HttpResponse response;
		public MyCallback(HttpRequest request, HttpResponse response) {
			this.request = request;
			this.response = response;
		}
		
		@Override
		public void onFailure(Throwable caught) {
			logger.debug("Exception: " + caught);
			response.finish();
		}

		@Override
		public void onSuccess(String result) {
			response.write("Name: " + result);
			logger.debug("MyCallback.onSuccess, retrieved name: " + result);
			response.finish();
		}
		
	}
}
