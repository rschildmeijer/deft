package org.deftserver.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.deftserver.io.callback.CallbackManager;
import org.deftserver.io.callback.JMXDebuggableCallbackManager;
import org.deftserver.io.timeout.JMXDebuggableTimeoutManager;
import org.deftserver.io.timeout.Timeout;
import org.deftserver.io.timeout.TimeoutManager;
import org.deftserver.web.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultIOWorkerLoop implements IOWorkerLoop, IOLoopController {
	
	private static final Logger LOG = LoggerFactory.getLogger(IOWorkerLoop.class);
	
	private final ConcurrentLinkedQueue<SocketChannel> newChannels = new ConcurrentLinkedQueue<SocketChannel>(); 
	
	private Selector selector;
	
	private IOHandler handler;

	private final TimeoutManager tm ;
	private final CallbackManager cm;
	
	
	private static final ThreadLocal<DefaultIOWorkerLoop> tlocal = new ThreadLocal<DefaultIOWorkerLoop>();

	public DefaultIOWorkerLoop(IOHandler handler) {
		try {
			selector = Selector.open();
			tm = new JMXDebuggableTimeoutManager();
			cm = new JMXDebuggableCallbackManager();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.handler = handler;
	}
	
	/**
	 * This method can be invoked from another thread
	 */
	@Override
	public void addChannel(SocketChannel clientChannel) {
		
		newChannels.add(clientChannel);
	}
	
	
	@Override
	public void commitAddedChannels() {
		if (newChannels.size() > 0) {
			this.selector.wakeup();
		}
		
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.deftserver.io.IOLoopController#addHandler(java.nio.channels.SelectableChannel, org.deftserver.io.IOHandler, int, java.lang.Object)
	 */
	@Override
	public SelectionKey addHandler(SelectableChannel channel,
			IOHandler handler, int interestOps, Object attachment) {
		this.handler = handler;
		return registerChannel(channel, interestOps, attachment);
	}

	/* (non-Javadoc)
	 * @see org.deftserver.io.IOLoopController#removeHandler(java.nio.channels.SelectableChannel)
	 */
	@Override
	public void removeHandler(SelectableChannel channel) {
		// Nothing to do here 

	}

	/**
	 * Register all pending SocketChannel accepted and added from AcceptLoop 
	 * in the {@link #newChannels} using {@link #addChannel(SocketChannel)} method
	 * 
	 * @param channel
	 * @param interestOps
	 * @param attachment
	 * @return
	 */
	private SelectionKey registerChannel(SelectableChannel channel,
			int interestOps, Object attachment) {
		try {
		
			return channel.register(selector, interestOps, attachment);
		} catch (ClosedChannelException e) {
			removeHandler(channel);
			LOG.error("Could not register channel: {}", e.getMessage());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.deftserver.io.IOLoopController#addKeepAliveTimeout(java.nio.channels.SocketChannel, org.deftserver.io.timeout.Timeout)
	 */
	@Override
	public void addKeepAliveTimeout(SocketChannel channel,
			Timeout keepAliveTimeout) {
		tm.addKeepAliveTimeout(channel, keepAliveTimeout);
	}

	/* (non-Javadoc)
	 * @see org.deftserver.io.IOLoopController#hasKeepAliveTimeout(java.nio.channels.SelectableChannel)
	 */
	@Override
	public boolean hasKeepAliveTimeout(SelectableChannel channel) {
		return tm.hasKeepAliveTimeout(channel);
	}

	/* (non-Javadoc)
	 * @see org.deftserver.io.IOLoopController#addTimeout(org.deftserver.io.timeout.Timeout)
	 */
	@Override
	public void addTimeout(Timeout timeout) {
		tm.addTimeout(timeout);
	}

	/* (non-Javadoc)
	 * @see org.deftserver.io.IOLoopController#addCallback(org.deftserver.web.AsyncCallback)
	 */
	@Override
	public void addCallback(AsyncCallback callback) {
		cm.addCallback(callback);
	}
	
	private void registerAddedChannels(){
		SocketChannel chan = null;
		while ((chan = newChannels.poll()) != null){
			
			try {
				this.handler.handleAccept(chan);
			} catch (IOException e) {
				LOG.error("IO Handler failed #handleAccept method", e);
			}
		}
	}
	
	
	public static IOLoopController getInstance() {
		return tlocal.get();
	}
	
	/**
	 * Runs the eternal loop on one core
	 */
	@Override
	public void run() {
	
		// Store thread local IOLoop instance 
		tlocal.set(this);
		
		while (true) {
			long selectorTimeout = 250; // 250 ms
			try {
				if (selector.select(selectorTimeout) == 0) {
					long ms = tm.execute();
					selectorTimeout = Math.min(ms, selectorTimeout);
					cm.execute();
					registerAddedChannels();
					continue;
				}
				// Accepts all pending SocketChannel from accept loop
				registerAddedChannels();
				
				Iterator<SelectionKey> keys = selector.selectedKeys()
						.iterator();
				while (keys.hasNext()) {
					SelectionKey key = keys.next();
						if (key.isValid() && key.isReadable()) {
							handler.handleRead(key);
						}

						if (key.isValid() && key.isWritable()) {
							handler.handleWrite(key);
						}

					keys.remove();
				}
				long ms = tm.execute();
				selectorTimeout = Math.min(ms, selectorTimeout);
				if (cm.execute()) {
					selectorTimeout = 0;
				}

			} catch (IOException e) {
				LOG.error("Exception received in IOLoop: {}", e);
			}
		}
		
	}
}
