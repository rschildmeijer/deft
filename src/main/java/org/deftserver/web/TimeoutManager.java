package org.deftserver.web;

import java.nio.channels.SelectableChannel;


public interface TimeoutManager {

	void addTimeout(Timeout timeout);
	
	void addKeepAliveTimeout(SelectableChannel channel, Timeout timeout);
	
	boolean hasKeepAliveTimeout(SelectableChannel channel);
	
	void touch(SelectableChannel channel); 
	
}
