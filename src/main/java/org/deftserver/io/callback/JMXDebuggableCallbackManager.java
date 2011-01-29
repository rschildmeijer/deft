package org.deftserver.io.callback;

import java.util.List;

import org.deftserver.util.MXBeanUtil;
import org.deftserver.web.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class JMXDebuggableCallbackManager implements CallbackManager, CallbackManagerMXBean {

	private final Logger logger = LoggerFactory.getLogger(JMXDebuggableCallbackManager.class);
	
	private final List<AsyncCallback> callbacks = Lists.newLinkedList();
	
	{ 	// instance initialization block
		MXBeanUtil.registerMXBean(this, "org.deftserver.io.callback:type=JMXDebuggableCallbackManager"); 
	}
	
	@Override
	public int getNumberOfCallbacks() {
		return callbacks.size();
	}

	@Override
	public void addCallback(AsyncCallback callback) {
		callbacks.add(callback);
		logger.debug("Callback added");
	}

	@Override
	public boolean execute() {
		// makes a defensive copy to avoid (1) CME (new callbacks are added this iteration) and (2) IO starvation.
		List<AsyncCallback> defensive = Lists.newLinkedList(callbacks);
		callbacks.clear();
		for (AsyncCallback callback : defensive) {
			callback.onCallback();
			logger.debug("Callback executed");
		}
		return !callbacks.isEmpty();
	}
	

}
