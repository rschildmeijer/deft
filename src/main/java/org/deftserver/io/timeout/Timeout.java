package org.deftserver.io.timeout;

import org.deftserver.web.AsyncCallback;


public class Timeout {

	private final long timeout;
	private final AsyncCallback cb;
	private boolean cancelled = false;
	
	public Timeout(long timeout, AsyncCallback cb) {
		this.timeout = timeout;
		this.cb = cb;
	}

	public long getTimeout() {
		return timeout;
	}
	
	public void cancel() {
		cancelled = true;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}

	public AsyncCallback getCallback() {
		return cancelled ? AsyncCallback.nopCb : cb;
	}

}
