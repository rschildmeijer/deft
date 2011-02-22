package org.deftserver.io;

import java.io.IOException;
import java.nio.channels.SelectionKey;


/**
 * TODO RS 101024 Add javadoc 
 *
 */
public interface IOHandler {

	void handleAccept(SelectionKey key) throws IOException;

	void handleConnect(SelectionKey key) throws IOException;
	
	void handleRead(SelectionKey key) throws IOException;
	
	void handleWrite(SelectionKey key);
	
}
