package org.deft.example;

import org.deft.web.AsyncCallback;
import org.deft.web.Asynchronous;
import org.deft.web.handler.RequestHandler;
import org.deft.web.protocol.HttpRequest;
import org.deft.web.protocol.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncDbHandler extends RequestHandler{

	Logger logger = LoggerFactory.getLogger(AsyncDbHandler.class);
	
	@Asynchronous
	public void get(HttpRequest request, HttpResponse response) {
		logger.debug("Entering AsyncDbHandler.get");
		new AsyncDbApi().getNameFromId("123", new MyCallback());
		logger.debug("Leaving AsyncDbHandler.get");
	}
	

	private class MyCallback implements AsyncCallback<String> {

		@Override
		public void onFailure(Throwable caught) {
			logger.debug("Exception: " + caught);
		}

		@Override
		public void onSuccess(String result) {
			logger.debug("MyCallback.onSuccess, retrieved name: " + result);
		}
		
	}
}
