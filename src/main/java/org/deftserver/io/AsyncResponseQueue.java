package org.deftserver.io;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.deftserver.web.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncResponseQueue {

	private static final Logger LOG = LoggerFactory.getLogger(AsyncResponseQueue.class);
	private final LinkedBlockingQueue<HttpResponse> queue;
/*	private final AsyncCallback cb = new AsyncCallback() {

		@Override
		public void onCallback() {
			sendQueuedResponses();
		}
	};*/
	private Boolean planned;
	
	private final AtomicInteger ai = new AtomicInteger();

	public AsyncResponseQueue() {
		queue = new LinkedBlockingQueue<HttpResponse>();
		planned = false;
	}
	
	public void planify(){
		ai.incrementAndGet();
		if (!planned){
		//	addTimeout();
			planned = true;
		}
	}

	public void pushResponseToSend(HttpResponse response) {
		try {
			queue.put(response);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	 void sendQueuedResponses() {
		if (ai.get()<= 0 || queue.isEmpty()){
			return;
		}
		LOG.debug("About to send queued responses.");
		
		HttpResponse resp;
		while ((resp = queue.poll()) != null) {
			resp.finish();
			ai.decrementAndGet();
		}

		if (ai.intValue()== 0){
			LOG.debug("All queued responses sent.");
			planned = false;
		} 
		
	}

/*	private void addTimeout() {
		IOLoopFactory.getLoopController().addTimeout( new Timeout(10,cb));
	}*/
}
