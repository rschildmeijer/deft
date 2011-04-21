package org.deftserver.io.timeout;

import java.nio.channels.SelectableChannel;


public interface TimeoutManager {

	void addTimeout(Timeout timeout);
	
	void addKeepAliveTimeout(SelectableChannel channel, Timeout timeout);
	
	boolean hasKeepAliveTimeout(SelectableChannel channel);
	
	/**
	 * 
	 * @return the positive number (>0) in milliseconds until the deadline for the next scheduled timeout.
	 */
	long execute();
	
}
