package org.deftserver.io.timeout;

import java.nio.channels.SelectableChannel;


public interface TimeoutManager {

	void addTimeout(Timeout timeout);
	
	void addKeepAliveTimeout(SelectableChannel channel, Timeout timeout);
	
	boolean hasKeepAliveTimeout(SelectableChannel channel);
	
	void touch(SelectableChannel channel); 
	
	/**
	 * 
	 * @return the positive number of milliseconds until next scheduled timeout.
	 */
	long execute();
	
}
