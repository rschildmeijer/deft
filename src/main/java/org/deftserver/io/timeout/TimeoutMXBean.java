package org.deftserver.io.timeout;

public interface TimeoutMXBean {

	long getMaxTimePerIterationInMs();
	
	void setMaxTimePerIterationInMs(long time);
	
	int getMaxNumberOfTimeoutOperationsPerIteration();

	void setMaxNumberOfTimeoutOperationsPerIteration(int operations);
	
	int getNumberOfTimeouts();
	
	int getNumberOfKeepAliveTimeouts();
	
	long getTimeoutCallbackPeriod();

}
