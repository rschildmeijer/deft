package org.deftserver.io.timeout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.deftserver.web.AsyncCallback;
import org.junit.Test;

public class TimeoutTest {

	@Test
	public void simpleTimeoutConstructorTest() {
		final AsyncCallback cb = new AsyncCallback() {
			@Override public void onCallback() {}
		};
		
		Timeout t = new Timeout(1492, cb);

		assertEquals(1492, t.getTimeout());
		assertEquals(cb, t.getCallback());
	}
	
	@Test
	public void timeoutCancelledTest() {
		final long now = System.currentTimeMillis();
		final AsyncCallback cb = new AsyncCallback() { @Override public void onCallback() { /*nop*/} };
		Timeout t1 = new Timeout(now + 2000, cb);
		assertTrue(t1.getCallback() == cb);

		t1.cancel();
		
		assertTrue(t1.getCallback() != cb);
		assertTrue(t1.isCancelled());
	}
	
}
