package org.deftserver.io;

import java.nio.channels.SelectableChannel;

public class JMXConfigurableTimeoutManager implements TimeoutManager {

	@Override
	public void addKeepAliveTimeout(SelectableChannel channel, Timeout timeout) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTimeout(Timeout timeout) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasKeepAliveTimeout(SelectableChannel channel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void touch(SelectableChannel channel) {
		// TODO Auto-generated method stub
	}

}
