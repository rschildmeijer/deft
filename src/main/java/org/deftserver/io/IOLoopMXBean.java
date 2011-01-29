package org.deftserver.io;

import java.util.List;



public interface IOLoopMXBean {
	
	int getNumberOfRegisteredIOHandlers();
	
	List<String> getRegisteredIOHandlers();
	
}
