package org.deftserver.io.timeout;

import java.nio.channels.SelectableChannel;

import org.deftserver.util.Closeables;
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
	
	public static Timeout newKeepAliveTimeout(final SelectableChannel clientChannel, long keepAliveTimeout) {
		return new Timeout(
				System.currentTimeMillis() + keepAliveTimeout,
				new AsyncCallback() { public void onCallback() { Closeables.closeQuietly(clientChannel); } }
		);
	}

}
