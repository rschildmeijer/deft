package org.deftserver.io;

import static com.google.common.collect.Collections2.transform;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deftserver.io.timeout.JMXConfigurableTimeoutManager;
import org.deftserver.io.timeout.Timeout;
import org.deftserver.io.timeout.TimeoutManager;
import org.deftserver.util.MXBeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public enum IOLoop implements IOLoopMXBean {
	
	INSTANCE;
	
	private final Logger logger = LoggerFactory.getLogger(IOLoop.class);

	private static final long TIMEOUT = 250;	// 0.25s in ms

	private Selector selector;
	
	private final Map<SelectableChannel, IOHandler> handlers = Maps.newHashMap();
	
	private final TimeoutManager tm = new JMXConfigurableTimeoutManager();
	
	private IOLoop() {
		try {
			selector = Selector.open();
		} catch (IOException e) {
			logger.error("Could not open selector: {}", e.getMessage());
		}
		MXBeanUtil.registerMXBean(this, "org.deftserver.web:type=IOLoop");
	}
	
	public void start() {
		Thread.currentThread().setName("I/O-LOOP");

		while (true) {
			try {
				if (selector.select(TIMEOUT) == 0) {
					tm.execute();
					continue;
				}

				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
				while (keys.hasNext()) {
					SelectionKey key = keys.next();
					IOHandler handler = handlers.get(key.channel());
					if (key.isAcceptable()) {
						handler.handleAccept(key);
					}
					if (key.isReadable()) {
						handler.handleRead(key);
					}
					if (key.isValid() && key.isWritable()) {
						handler.handleWrite(key);
					}
					keys.remove();
				}
				tm.execute();

			} catch (IOException e) {
				logger.error("Exception received in IOLoop: {}", e);			
			}
		}
	}

	public SelectionKey addHandler(SelectableChannel channel, IOHandler handler, int interestOps, Object attachment) {
		handlers.put(channel, handler);
		return registerChannel(channel, interestOps, attachment);		
	}
	
	public void removeHandler(SelectableChannel channel) {
		handlers.remove(channel);
	}
	
//	public <T> void addCallback(AsyncResult<T> cb) {
//		// TODO RS 101022 store this timeout 
//	}

	private SelectionKey registerChannel(SelectableChannel channel, int interestOps, Object attachment) {
		try {
			return channel.register(selector, interestOps, attachment);
		} catch (ClosedChannelException e) {
			removeHandler(channel);
			logger.error("Could not register channel: {}", e.getMessage());		
		}		
		return null;
	}
	
	public void addKeepAliveTimeout(SocketChannel channel, Timeout keepAliveTimeout) {
		tm.addKeepAliveTimeout(channel, keepAliveTimeout);
	}
	
	public boolean hasKeepAliveTimeout(SelectableChannel channel) {
		return tm.hasKeepAliveTimeout(channel);
	}
	
	public void addTimeout(Timeout timeout) {
		tm.addTimeout(timeout);
	}
	
// implements IOLoopMXBean
	@Override
	public int getNumberOfRegisteredIOHandlers() {
		return handlers.size();
	}

	@Override
	public List<String> getRegisteredIOHandlers() {
		Map<SelectableChannel, IOHandler> defensive = new HashMap<SelectableChannel, IOHandler>(handlers);
		Collection<String> readables = transform(defensive.values(), new Function<IOHandler, String>() {
			@Override public String apply(IOHandler handler) { return handler.toString(); }
		});
		return Lists.newLinkedList(readables);
	}

	@Override
	public long getSelectorTimeout() {
		return TIMEOUT;
	}

}
