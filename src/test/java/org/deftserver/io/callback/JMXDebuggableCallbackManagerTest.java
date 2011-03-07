package org.deftserver.io.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.deftserver.web.AsyncCallback;
import org.junit.Test;


public class JMXDebuggableCallbackManagerTest {
	
	private final JMXDebuggableCallbackManager cm = new JMXDebuggableCallbackManager();
	
	@Test
	public void simpleJMXDeubggableCallbackManagerTest() {
		final CountDownLatch latch = new CountDownLatch(3);
		
		final AsyncCallback cb1 = new AsyncCallback() {
			@Override
			public void onCallback() { latch.countDown(); }
		};
		
		final AsyncCallback cb2 = new AsyncCallback() {
			@Override
			public void onCallback() { latch.countDown(); }
		};
		
		final AsyncCallback cb3 = new AsyncCallback() {
			@Override
			public void onCallback() { 
				latch.countDown();
				cm.addCallback(cb1); 	// adding a new callback that should be scheduled for execution 
										// during the next iteration (i.e next call to execute)
			}
		};
		
		cm.addCallback(cb1);
		cm.addCallback(cb2);
		cm.addCallback(cb3);
		
		assertEquals(3, cm.getNumberOfCallbacks());
		
		boolean pending = cm.execute();
		
		assertEquals(true, pending);
		assertEquals(1, cm.getNumberOfCallbacks());
		assertEquals(0, latch.getCount());
	}
	
	@Test
	public void concurrencyTest() {
		final int nThreads = 25;
		final int n = 20 * 1000;
		final int[] exceptionThrown = {0};
		Runnable ioLoopTask = new Runnable() {
			
			public void run() { 
				try {
					cm.execute(); 
				} catch (Exception e) {
					exceptionThrown[0] = 1;
				}
			}
			
		};

		ScheduledExecutorService ioLoop = Executors.newSingleThreadScheduledExecutor();
		ioLoop.scheduleAtFixedRate(ioLoopTask, 0, 1, TimeUnit.MILLISECONDS);

		final CountDownLatch latch = new CountDownLatch(n);
		ScheduledExecutorService workerThreads = Executors.newScheduledThreadPool(nThreads);
		Runnable work = new Runnable() { 

			public void run() { 
				cm.addCallback(AsyncCallback.nopCb); 
				latch.countDown();
			}

		};

		for (int i = 0; i < 25; i++) {
			workerThreads.scheduleWithFixedDelay(work, 0, 1, TimeUnit.MILLISECONDS);
		}

		try {
			latch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			assertTrue(false);
		}
		assertEquals(0, latch.getCount());
		assertEquals(0, exceptionThrown[0]);
		ioLoop.shutdownNow();
		workerThreads.shutdownNow();
	}

}
