package org.deftserver.io.timeout;

import java.nio.channels.SelectableChannel;
import java.util.Iterator;
import java.util.Map;

import org.deftserver.util.MXBeanUtil;
import org.deftserver.web.http.HttpServerDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultiset;


public class JMXDebuggableTimeoutManager implements TimeoutManager, TimeoutManagerMXBean {

	private final Logger logger = LoggerFactory.getLogger(JMXDebuggableTimeoutManager.class);

	private final TreeMultiset<DecoratedTimeout> timeouts = TreeMultiset.create();
	private final Map<SelectableChannel, DecoratedTimeout> index = Maps.newHashMap();

	{ 	// instance initialization block
		MXBeanUtil.registerMXBean(this, "TimeoutManager"); 
	}

	@Override
	public void addKeepAliveTimeout(SelectableChannel channel, Timeout timeout) {
		logger.debug("added keep-alive timeout: {}", timeout);
		DecoratedTimeout oldTimeout = index.get(channel);
		if (oldTimeout != null) {
			timeouts.remove(oldTimeout);
		}
		DecoratedTimeout decorated = new DecoratedTimeout(channel, timeout);
		timeouts.add(decorated);
		index.put(channel, decorated);
	}

	@Override
	public void addTimeout(Timeout timeout) {
		logger.debug("added generic timeout: {}", timeout);
		timeouts.add(new DecoratedTimeout(timeout));		
	}

	@Override
	public boolean hasKeepAliveTimeout(SelectableChannel channel) {
		return index.containsKey(channel);
	}

	@Override
	public void touch(SelectableChannel channel) {
		//Prolong/reset keep-alive timeout associated with channel
		DecoratedTimeout oldTimeout = index.get(channel);
		timeouts.remove(oldTimeout);
		Timeout newTimeout = new Timeout(
				System.currentTimeMillis() + HttpServerDescriptor.KEEP_ALIVE_TIMEOUT, 
				oldTimeout.timeout.getCallback()
		);
		timeouts.add(new DecoratedTimeout(channel, newTimeout));
	}

	@Override
	public long execute() {
		// makes a defensive copy to avoid (1) CME (new timeouts are added this iteration) and (2) IO starvation.
		TreeMultiset<DecoratedTimeout> defensive = TreeMultiset.create(timeouts);
		Iterator<DecoratedTimeout> iter = defensive.iterator();
		final long now = System.currentTimeMillis();
		while (iter.hasNext()) {
			DecoratedTimeout candidate = iter.next();
			if (candidate.timeout.getTimeout() > now) { break; }
			candidate.timeout.getCallback().onCallback();
			index.remove(candidate.channel);
			iter.remove();
			timeouts.remove(candidate);
			logger.debug("Timeout triggered: {}", candidate.timeout);
		}
		return timeouts.isEmpty() ? Long.MAX_VALUE : Math.max(1, timeouts.iterator().next().timeout.getTimeout() - now);
	}

	// implements TimoutMXBean
	@Override
	public int getNumberOfKeepAliveTimeouts() {
		return index.size();
	}

	@Override
	public int getNumberOfTimeouts() {
		return timeouts.size();
	}

	private class DecoratedTimeout implements Comparable<DecoratedTimeout> {

		public final SelectableChannel channel;
		public final Timeout timeout;

		public DecoratedTimeout(SelectableChannel channel, Timeout timeout) {
			this.channel = channel;
			this.timeout = timeout;
		}

		public DecoratedTimeout(Timeout timeout) {
			this(null, timeout);
		}

		@Override
		public int compareTo(DecoratedTimeout that) {
			long diff = timeout.getTimeout() - that.timeout.getTimeout();
			if (diff < 0) {
				return -1;
			} else if (diff > 0) {
				return 1; 
			} 
			if (channel != null && that.channel != null) {
				return channel.hashCode() - that.channel.hashCode(); 
			} else if (channel == null && that.channel != null){
				return -1;
			} else if (channel != null && that.channel == null){
				return -1;
			} else {
				return 0;
			}
		}
		
	}

}
