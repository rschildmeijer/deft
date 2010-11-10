package org.deftserver.io;

import java.util.List;



public interface IOLoopMXBean {
	
	int getNumberOfRegisteredIOHandlers();
	
	List<String> getRegisteredIOHandlers();
	
	int getNumberOfTimeouts();
	
	int getNumberOfKeepAliveTimeouts();
	
	long getSelectorTimeout();
	
	long getTimeoutCallbackPeriod();

}
