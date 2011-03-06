package org.deftserver.io;

import static org.deftserver.web.http.HttpServerDescriptor.READ_BUFFER_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import org.deftserver.util.Closeables;
import org.deftserver.web.AsyncCallback;
import org.deftserver.web.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultIOWorkerLoop implements IOWorkerLoop, IOLoopController {

    private static final Logger LOG = LoggerFactory
            .getLogger(IOWorkerLoop.class);

    private final ConcurrentLinkedQueue<ChannelWrapper> newChannels = new ConcurrentLinkedQueue<ChannelWrapper>();

    private Selector selector;

    private final TimeoutManager tm;
    private final CallbackManager cm;

    private final AsyncResponseQueue responseQueue;

    private final ByteBuffer inputBuffer;

    private static final ThreadLocal<DefaultIOWorkerLoop> tlocal = new ThreadLocal<DefaultIOWorkerLoop>();

    public DefaultIOWorkerLoop() {
        try {
            selector = Selector.open();
            tm = new JMXDebuggableTimeoutManager();
            cm = new JMXDebuggableCallbackManager();
            responseQueue = new AsyncResponseQueue();
            inputBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * This method can be invoked from another thread
     */
    @Override
    public void addChannel(SocketChannel clientChannel, ChannelContext ctx) {

        newChannels.add(new ChannelWrapper(clientChannel, ctx.getHandler()));
    }

    @Override
    public void commitAddedChannels() {
        if (!newChannels.isEmpty()) {
            this.selector.wakeup();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.deftserver.io.IOLoopController#addHandler(java.nio.channels.
     * SelectableChannel, org.deftserver.io.IOHandler, int, java.lang.Object)
     */
    @Override
    public SelectionKey addHandler(SelectableChannel channel,
            IOHandler handler, int interestOps, Object attachment) {
        // this.handler = handler;
        // if (attachment != null && attachment instanceof ChannelContext) {
        // ((ChannelContext) attachment).setHandler(handler);
        // }
        return registerChannel(channel, interestOps, attachment);
    }

    @Override
    public SelectionKey registerReadHandler(SelectableChannel channel,
            IOHandler handler, @SuppressWarnings("rawtypes") ChannelContext ctx) {
        if (ctx.getBufferIn() == null) {
            ctx.setBufferIn(inputBuffer);
        }
        // ctx.setHandler(handler);
        return registerChannel(channel, SelectionKey.OP_READ, ctx);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.deftserver.io.IOLoopController#removeHandler(java.nio.channels.
     * SelectableChannel)
     */
    @Override
    public void removeHandler(SelectableChannel channel) {
        // Nothing to do here

    }

    /**
     * Register all pending SocketChannel accepted and added from AcceptLoop in
     * the {@link #newChannels} using {@link #addChannel(SocketChannel)} method
     * 
     * @param channel
     * @param interestOps
     * @param attachment
     * @return
     */
    private SelectionKey registerChannel(SelectableChannel channel,
            int interestOps, Object attachment) {
        try {
            LOG.debug("Registering channel for {} on channel {}", interestOps,
                    channel);
            return channel.register(selector, interestOps, attachment);
        } catch (ClosedChannelException e) {
            LOG.error("Could not register channel: {}", e.getMessage());
            Closeables.closeQuietly(channel);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.deftserver.io.IOLoopController#addKeepAliveTimeout(java.nio.channels
     * .SocketChannel, org.deftserver.io.timeout.Timeout)
     */
    @Override
    public void addKeepAliveTimeout(SocketChannel channel,
            Timeout keepAliveTimeout) {
        tm.addKeepAliveTimeout(channel, keepAliveTimeout);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.deftserver.io.IOLoopController#hasKeepAliveTimeout(java.nio.channels
     * .SelectableChannel)
     */
    @Override
    public boolean hasKeepAliveTimeout(SelectableChannel channel) {
        return tm.hasKeepAliveTimeout(channel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.deftserver.io.IOLoopController#addTimeout(org.deftserver.io.timeout
     * .Timeout)
     */
    @Override
    public void addTimeout(Timeout timeout) {
        tm.addTimeout(timeout);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.deftserver.io.IOLoopController#addCallback(org.deftserver.web.
     * AsyncCallback)
     */
    @Override
    public void addCallback(AsyncCallback callback) {
        cm.addCallback(callback);
    }

    private void registerAddedChannels() {
        ChannelWrapper chan = null;
        while ((chan = newChannels.poll()) != null) {

            try {
                chan.handler.handleAccept(chan.channel);
            } catch (IOException e) {
                LOG.error("IO Handler failed #handleAccept method", e);
            }
        }
    }

    public static IOLoopController getInstance() {
        return tlocal.get();
    }

    @Override
    public void planifyResponse() {
        responseQueue.planify();
    }

    @Override
    public void pushResponse(HttpResponse response) {
        responseQueue.pushResponseToSend(response);

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
            // LOG.debug("begin select loop");
            try {
                if (selector.select(selectorTimeout) == 0) {
                    long ms = tm.execute();
                    selectorTimeout = Math.min(ms, selectorTimeout);
                    cm.execute();
                    registerAddedChannels();
                    responseQueue.sendQueuedResponses();
                    continue;
                }
                // Accepts all pending SocketChannel from accept loop
                registerAddedChannels();
                responseQueue.sendQueuedResponses();

                Iterator<SelectionKey> keys = selector.selectedKeys()
                        .iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    IOHandler handle = ((ChannelContext) key.attachment())
                            .getHandler();
                    if (key.isValid() && key.isReadable()) {
                        handle.handleRead(key);
                    }

                    if (key.isValid() && key.isWritable()) {
                        handle.handleWrite(key);
                    }

                    keys.remove();
                }
                long ms = tm.execute();
                selectorTimeout = Math.min(ms, selectorTimeout);
                if (cm.execute()) {
                    selectorTimeout = 0;
                }
                // registerAddedChannels();

            } catch (IOException e) {
                LOG.error("Exception received in IOLoop: {}", e);
            } catch (Exception e) {
                LOG.error("Exception received in IOLoop:", e);
            }
        }

    }

    private class ChannelWrapper {
        final SocketChannel channel;
        final IOHandler handler;

        public ChannelWrapper(SocketChannel chan, IOHandler handler) {
            this.channel = chan;
            this.handler = handler;
        }

    }
}
