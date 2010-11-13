package org.deftserver.io.timeout;

import java.nio.channels.SelectableChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deftserver.util.MXBeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class JMXConfigurableTimeoutManager implements TimeoutManager, TimeoutMXBean {
	
	private final Logger logger = LoggerFactory.getLogger(JMXConfigurableTimeoutManager.class);
	
	private static final long TIMEOUT_CALLBACK_PERIOD = 2 * 1000;	//2s in ms
	
	private long maxTime = 100;	//ms
	private int maxOperations = Integer.MAX_VALUE;

	private final List<Timeout> timeouts = Lists.newLinkedList();
	private final Map<SelectableChannel, Timeout> keepAliveTimeouts = Maps.newHashMap();
	
	private long lastIteration;
	
	{ 	// instance initialization block
		MXBeanUtil.registerMXBean(this, "org.deftserver.io.timeout:type=JMXConfigurableTimeoutManager"); 
	}
	
	@Override
	public void addKeepAliveTimeout(SelectableChannel channel, Timeout timeout) {
		logger.debug("added keep-alive timeout: {}", timeout);
		keepAliveTimeouts.put(channel, timeout);		
	}
	
	@Override
	public void addTimeout(Timeout timeout) {
		logger.debug("added generic timeout: {}", timeout);
		timeouts.add(timeout);		
	}

	@Override
	public boolean hasKeepAliveTimeout(SelectableChannel channel) {
		return keepAliveTimeouts.containsKey(channel);
	}

	@Override
	public void touch(SelectableChannel channel) {
		// TODO RS101113 Prolong/reset keep-alive timeout associated with channel
	}
	
	@Override
	public void execute() {
		long now = System.currentTimeMillis();
		long deadline = now + maxTime;
		if (now >= lastIteration + TIMEOUT_CALLBACK_PERIOD) {
			int operations = 0;
			operations = triggerTimeouts(timeouts, now, deadline, operations);
			triggerTimeouts(keepAliveTimeouts.values(), now, deadline, operations);
			lastIteration = now;
		}
	}
	
	private int triggerTimeouts(Collection<Timeout> timeouts, long now, long deadline, int operations) {
		Iterator<Timeout> iter = timeouts.iterator();
		while (iter.hasNext()) {
			if (now > deadline || operations > maxOperations) { break; }
			Timeout candidate = iter.next();
			if (candidate.getTimeout() <= now) {
				candidate.getCallback().onCallback();
				operations++;
				iter.remove();
				logger.debug("Timeout triggered: {}", candidate);
			}
		}
		return operations;
	}

	// implements TimoutMXBean
	@Override
	public long getMaxTimePerIterationInMs() {
		return maxTime;
	}

	@Override
	public void setMaxTimePerIterationInMs(long newMaxTime) {
		maxTime = newMaxTime;
	}
	
	@Override
	public int getMaxNumberOfTimeoutOperationsPerIteration() {
		return maxOperations;
	}
	
	@Override
	public void setMaxNumberOfTimeoutOperationsPerIteration(int newMaxOperations) {
		maxOperations = newMaxOperations;
	}

	@Override
	public int getNumberOfKeepAliveTimeouts() {
		return keepAliveTimeouts.size();
	}

	@Override
	public int getNumberOfTimeouts() {
		return timeouts.size();
	}

	@Override
	public long getTimeoutCallbackPeriod() {
		return TIMEOUT_CALLBACK_PERIOD;
	}

}
