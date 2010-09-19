package org.ulme.web.protocol;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class HttpProtocolImpl implements HttpProtocol {

	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		System.out.println("accept");
	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleWrite(SelectionKey key) throws IOException {
		// TODO Auto-generated method stub

	}

}
