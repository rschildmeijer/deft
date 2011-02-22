package org.deftserver.io.callback;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.deftserver.io.IOLoop;
import org.deftserver.web.AsyncCallback;
import org.junit.Test;

public class PeriodicCallbackTest {
	
	@Test
	public void testPeriodicCallback() throws InterruptedException {
		// start the IOLoop from a new thread so we dont block this test.
		new Thread(new Runnable() {

			@Override public void run() { IOLoop.INSTANCE.start(); }
		
		}).start();
		
		final CountDownLatch latch = new CountDownLatch(200);
		long period = 10; // 10ms (=> ~100times / s)
		AsyncCallback cb = new AsyncCallback() {
			@Override public void onCallback() { latch.countDown(); }
		};
		PeriodicCallback pcb = new PeriodicCallback(cb, period);
		pcb.start();
		
		latch.await(5, TimeUnit.SECONDS);
		pcb.cancel();
		IOLoop.INSTANCE.stop();
		// TODO wait?
		assertEquals(0, latch.getCount());
	}
	
}
