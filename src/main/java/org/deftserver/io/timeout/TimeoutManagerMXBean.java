package org.deftserver.io.timeout;

public interface TimeoutManagerMXBean {

	int getNumberOfTimeouts();
	
	int getNumberOfKeepAliveTimeouts();
	
}
