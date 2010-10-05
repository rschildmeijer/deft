package org.deftserver.example;

import org.deftserver.web.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy class representing a call to an async DB-lib used in development
 *
 */
public class AsyncDbApi {
	
	Logger logger = LoggerFactory.getLogger(AsyncDbApi.class);
	private final int DELAY = 2000;
	
	public void getNameFromId(final String id, final AsyncCallback<String> callback) {
		logger.debug("Getting name from database...");
		
		Runnable runnable = new Runnable( ) {
			
			@Override
			public void run() {
				logger.debug("sleeping for " + DELAY +" seconds");
				try {
					Thread.sleep(DELAY);
					String result = "Jim" + id;
					callback.onSuccess(result);
				} catch (InterruptedException e) {
					callback.onFailure(e);
				}
			}
		};
		Thread t = new Thread(runnable);
		t.start();
	}
}
