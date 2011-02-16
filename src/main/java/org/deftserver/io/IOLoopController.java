package org.deftserver.io;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.deftserver.io.timeout.Timeout;
import org.deftserver.web.AsyncCallback;

public interface IOLoopController {

	  SelectionKey addHandler(SelectableChannel channel,
			IOHandler handler, int interestOps, Object attachment);

	  void removeHandler(SelectableChannel channel);

	  void addKeepAliveTimeout(SocketChannel channel,
			Timeout keepAliveTimeout);

	  boolean hasKeepAliveTimeout(SelectableChannel channel);

	  void addTimeout(Timeout timeout);

	  void addCallback(AsyncCallback callback);


}