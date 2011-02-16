package org.deftserver.io;


import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.deftserver.io.IOLoopFactory.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;


public class IOAcceptLoop {

	private static final Logger LOG = LoggerFactory
			.getLogger(IOAcceptLoop.class);

	private Selector selector;

	private List<IOWorkerLoop> workers;
	
	private Iterator<IOWorkerLoop> workersIterator;
	
	private ExecutorService executor = Executors.newCachedThreadPool();

	public IOAcceptLoop(IOHandler handler, int workerCount) {

		try {
			selector = Selector.open();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		workers = Lists.newArrayList();
		int i = 0;
		while (i < workerCount){
			workers.add(new DefaultIOWorkerLoop(handler));
			i++;
		}
	}

	public void start() {
		Thread.currentThread().setName("I/O-AcceptLoop");
		IOLoopFactory.setMode(Mode.MULTI_THREADED);
		
		for (IOWorkerLoop worker : workers){
			executor.execute(worker);	
		}
		LOG.info("{} I/O Worker Loop started", this.workers.size());
		workersIterator = workers.iterator();
		while (true) {
			long selectorTimeout = 250; // 250 ms
			try {
				if (selector.select(selectorTimeout) > 0) {

					Iterator<SelectionKey> keys = selector.selectedKeys()
							.iterator();
					while (keys.hasNext()) {
						SelectionKey key = keys.next();
						if (key.isValid() && key.isAcceptable()) {
							handleAccept(key);
						}
						keys.remove();
					}
					
					for (IOWorkerLoop  worker: workers){
						worker.commitAddedChannels();
					}
				}
			} catch (IOException e) {
				LOG.error("Exception received in IOLoop: {}", e);
			}
		}
	}

	public void registerAcceptChannel(SelectableChannel channel) {
		try {
			channel.register(selector, SelectionKey.OP_ACCEPT, null);
		} catch (ClosedChannelException e) {

			LOG.error("Could not register channel: {}", e.getMessage());
		}
	}

	public void setWorkers(List<IOWorkerLoop> workers) {
		this.workers = workers;
		this.workersIterator = workers.iterator();
	}
	
	private void handleAccept(SelectionKey key){
		LOG.debug("handle accept on key {}", key);
		try {
			SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
			clientChannel.configureBlocking(false);
			
			// Add the accepted channel to a worker thread
			IOWorkerLoop worker;
			if (!workersIterator.hasNext()){
				workersIterator = workers.iterator();
			}
			worker = workersIterator.next();
			worker.addChannel(clientChannel);
			LOG.trace("Accepted clientChannel {} added to worker {}", clientChannel, worker);
			
		} catch (IOException e) {
			LOG.error("I/O Unable to accept new connection from selected key", e);
		}
	}
	
}
