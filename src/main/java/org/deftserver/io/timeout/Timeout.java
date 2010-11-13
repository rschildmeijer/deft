package org.deftserver.io.timeout;

import org.deftserver.web.AsyncCallback;


public class Timeout {

	private final long timeout;
	private final AsyncCallback cb;

	public Timeout(long timeout, AsyncCallback cb) {
		this.timeout = timeout;
		this.cb = cb;
	}

	public long getTimeout() {
		return timeout;
	}

	public AsyncCallback getCallback() {
		return cb;
	}

}
