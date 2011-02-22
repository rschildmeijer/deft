package org.deftserver.io.callback;

import org.deftserver.io.IOLoop;
import org.deftserver.io.timeout.Timeout;
import org.deftserver.web.AsyncCallback;

public class PeriodicCallback {
	
	private final AsyncCallback cb;
	private final long period;
	private boolean active = true;
	
	/** 
	 * A periodic callback that will execute its callback once every period.
	 * @param cb 
	 * @param period The period in ms
	 */
	public PeriodicCallback(AsyncCallback cb, long period) {
		this.cb = cb;
		this.period = period;
	}
	
	public void start() {
		IOLoop.INSTANCE.addTimeout(
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
	
	public void cancel() {
		this.active = false;
	}
	
}
