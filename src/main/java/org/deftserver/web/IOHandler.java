package org.deftserver.web;

import java.io.IOException;
import java.nio.channels.SelectionKey;


/**
 * TODO RS 101024 Add javadoc 
 *
 */
public interface IOHandler {

	void handleAccept(SelectionKey key) throws IOException;
	
	void handleRead(SelectionKey key) throws IOException;
	
	void handleWrite(SelectionKey key);
	
}
