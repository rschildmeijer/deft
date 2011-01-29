package org.deftserver.io.callback;

import org.deftserver.web.AsyncCallback;

public interface CallbackManager {

	void addCallback(AsyncCallback callback);
	
	/**
	 * 
	 * @return true if there are callbacks scheduled to be executed during the next IO loop iteration.
	 */
	boolean execute();
	
}
