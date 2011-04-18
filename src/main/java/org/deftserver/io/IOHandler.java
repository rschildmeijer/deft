package org.deftserver.io;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * {@code IOHandler}s are added to the IOLoop via {@link IOLoop#addHandler} method.
 * The callbacks defined in the {@code IOHandler} will be invoked by the {@code IOLoop} when io is ready.
 *
 */
public interface IOHandler {

	void handleAccept(SelectionKey key) throws IOException;

	void handleConnect(SelectionKey key) throws IOException;
	
	void handleRead(SelectionKey key) throws IOException;
	
	void handleWrite(SelectionKey key);
	
}
