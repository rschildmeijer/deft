package org.deftserver.io;

import static com.google.common.collect.Collections2.transform;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deftserver.io.IOLoopFactory.Mode;
import org.deftserver.io.callback.CallbackManager;
import org.deftserver.io.callback.JMXDebuggableCallbackManager;
import org.deftserver.io.timeout.JMXDebuggableTimeoutManager;
import org.deftserver.io.timeout.Timeout;
import org.deftserver.io.timeout.TimeoutManager;
import org.deftserver.util.MXBeanUtil;
import org.deftserver.web.AsyncCallback;
import org.deftserver.web.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public enum IOLoop implements IOLoopMXBean, IOLoopController {

    INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(IOLoop.class);

    private Selector selector;

    private final TimeoutManager tm = new JMXDebuggableTimeoutManager();
    private final CallbackManager cm = new JMXDebuggableCallbackManager();
    private final AsyncResponseQueue responseQueue;

    private IOLoop() {

        try {
            selector = Selector.open();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MXBeanUtil.registerMXBean(this, "org.deftserver.web:type=IOLoop");
        responseQueue = new AsyncResponseQueue();
    }

    public void start() {
        Thread.currentThread().setName("I/O-LOOP");
        IOLoopFactory.setMode(Mode.SINGLE_THREADED);

        while (true) {
            long selectorTimeout = 250; // 250 ms
            try {
                if (selector.select(selectorTimeout) == 0) {
                    long ms = tm.execute();
                    selectorTimeout = Math.min(ms, selectorTimeout);
                    if (cm.execute()) {
                        selectorTimeout = 0;
                    }
                    responseQueue.sendQueuedResponses();
                    continue;
                }
                responseQueue.sendQueuedResponses();
                Iterator<SelectionKey> keys = selector.selectedKeys()
                        .iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    IOHandler handle = ((ChannelContext) key.attachment())
                            .getHandler();
                    if (key.isAcceptable()) {
                        this.handleAccept(key);
                    }
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

            } catch (IOException e) {
                logger.error("Exception received in IOLoop: {}", e);
            }
        }
    }

    private void handleAccept(SelectionKey key) {

        try {
            SocketChannel clientChannel = ((ServerSocketChannel) key.channel())
                    .accept();
            clientChannel.configureBlocking(false);
            IOHandler handle = ((ChannelContext) key.attachment()).getHandler();
            handle.handleAccept(clientChannel);

            logger.trace("Accepted clientChannel {}", clientChannel);

        } catch (IOException e) {
            logger.error(
                    "I/O Unable to accept new connection from selected key", e);
        }
    }

    public SelectionKey addHandler(SelectableChannel channel,
            IOHandler handler, int interestOps, Object attachment) {
        // this.handler = handler;
        // if (attachment != null && attachment instanceof ChannelContext) {
        // ((ChannelContext) attachment).setHandler(handler);
        // }
        return registerChannel(channel, interestOps, attachment);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SelectionKey registerReadHandler(SelectableChannel channel,
            IOHandler handler, ChannelContext ctx) {
        if (ctx.getBufferIn() == null) {
            ctx.setBufferIn(ByteBuffer.allocate(1500));
        }
        // ctx.setHandler(handler);
        return registerChannel(channel, SelectionKey.OP_READ, ctx);
    }

    public void removeHandler(SelectableChannel channel) {
        // handlers.remove(channel);

    }

    private SelectionKey registerChannel(SelectableChannel channel,
            int interestOps, Object attachment) {
        try {

            return channel.register(selector, interestOps, attachment);
        } catch (ClosedChannelException e) {
            removeHandler(channel);
            logger.error("Could not register channel: {}", e.getMessage());
        }
        return null;
    }

    public void addKeepAliveTimeout(SocketChannel channel,
            Timeout keepAliveTimeout) {
        tm.addKeepAliveTimeout(channel, keepAliveTimeout);
    }

    public boolean hasKeepAliveTimeout(SelectableChannel channel) {
        return tm.hasKeepAliveTimeout(channel);
    }

    public void addTimeout(Timeout timeout) {
        tm.addTimeout(timeout);
    }

    public void addCallback(AsyncCallback callback) {
        cm.addCallback(callback);
    }

    // implements IOLoopMXBean
    @Override
    public int getNumberOfRegisteredIOHandlers() {
        return 1;
    }

    @Override
    public List<String> getRegisteredIOHandlers() {
        Map<SelectableChannel, IOHandler> defensive = new HashMap<SelectableChannel, IOHandler>();
        Collection<String> readables = transform(defensive.values(),
                new Function<IOHandler, String>() {
                    @Override
                    public String apply(IOHandler handler) {
                        return handler.toString();
                    }
                });
        return Lists.newLinkedList(readables);
    }

    @Override
    public void planifyResponse() {
        responseQueue.planify();
    }

    @Override
    public void pushResponse(HttpResponse response) {
        responseQueue.pushResponseToSend(response);

    }

}
