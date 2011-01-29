package org.deftserver.io.callback;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

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

}
