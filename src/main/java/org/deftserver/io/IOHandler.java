package org.deftserver.io;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


/**
 * TODO RS 101024 Add javadoc 
 *
 */
public interface IOHandler {

	void handleAccept(SocketChannel clientChannel) throws IOException;
	
	void handleRead(SelectionKey key) throws IOException;
	
	void handleWrite(SelectionKey key);
	
}
