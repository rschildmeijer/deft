package org.deftserver.io.callback;

import org.deftserver.io.IOLoop;
import org.deftserver.io.timeout.Timeout;
import org.deftserver.web.AsyncCallback;

public class PeriodicCallback {
	
	private final IOLoop ioLoop;
	private final AsyncCallback cb;
	private final long period;
	private boolean active = true;
	
	/** 
	 * A periodic callback that will execute its callback once every period.
	 * @param cb 
	 * @param period The period in ms
	 */
	public PeriodicCallback(AsyncCallback cb, long period) {
		this(IOLoop.INSTANCE, cb, period);
	}
	
	public PeriodicCallback(IOLoop ioLoop, AsyncCallback cb, long period) {
		this.ioLoop = ioLoop;
		this.cb = cb;
		this.period = period;
	}
	
	/**
	 * Start the {@code PeriodicCallback}
	 */
	public void start() {
		ioLoop.addTimeout(
				new Timeout(
						System.currentTimeMillis() + period, 
						new AsyncCallback() { @Override public void onCallback() { run(); }}
				)
		);
	}
	
	private void run() {
		if (active) {
			cb.onCallback();
			start();	// reschedule
		}
	}
	
	/**
	 * Cancel the {@code PeriodicCallback}. (No way to resume the cancellation, you will need to create a new
	 * {@code PeriodicCallback}).
	 */
	public void cancel() {
		this.active = false;
	}
	
}
