package org.deftserver.util;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import org.deftserver.io.IOHandler;
import org.deftserver.io.IOLoop;
import org.deftserver.web.AsyncCallback;

public class AcceptUtil {

	public static void accept(ServerSocketChannel server, final AsyncCallback cb) {
		accept(IOLoop.INSTANCE, server, cb);
	}
	
	public static void accept(IOLoop ioLoop, ServerSocketChannel server, final AsyncCallback cb) {
		ioLoop.addHandler(
				server, 
				new AcceptingIOHandler() {public void handleAccept(SelectionKey key) { cb.onCallback(); }},
				SelectionKey.OP_ACCEPT, 
				null
		);
	}
	
	private static abstract class AcceptingIOHandler implements IOHandler {

		public void handleConnect(SelectionKey key) throws IOException {}

		public void handleRead(SelectionKey key) throws IOException {}

		public void handleWrite(SelectionKey key) {}
		
	}

}
