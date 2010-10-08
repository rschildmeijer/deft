package org.deftserver.web.protocol;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface HttpProtocol {

	void handleAccept(SelectionKey key) throws IOException;
	void handleRead(SelectionKey key) throws IOException;
	void handleCallback();
	
}
