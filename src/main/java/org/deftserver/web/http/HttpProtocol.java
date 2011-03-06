package org.deftserver.web.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;

import org.deftserver.io.IOHandler;
import org.deftserver.io.IOLoopFactory;
import org.deftserver.util.Closeables;
import org.deftserver.web.Application;
import org.deftserver.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProtocol implements IOHandler {

    private final static Logger logger = LoggerFactory
            .getLogger(HttpProtocol.class);

    private final Application application;

    private final AtomicLong alConnections;

    // a queue of half-baked (pending/unfinished) HTTP post request
    // private final Map<SelectableChannel, PartialHttpRequest> partials =
    // Maps.newHashMap();

    public HttpProtocol(Application app) {
        application = app;
        alConnections = new AtomicLong();

    }

    @Override
    public void handleAccept(SocketChannel clientChannel) throws IOException {
        Long id = alConnections.incrementAndGet();
        logger.debug("handle accept on {} #id: {}", clientChannel, id);

        // SocketChannel clientChannel = ((ServerSocketChannel)
        // clientChannel.channel()).accept();
        // clientChannel.configureBlocking(false);

        IOLoopFactory.getLoopController().registerReadHandler(clientChannel,
                this, new HttpChannelContext(this, id));
    }

    @Override
    public void handleRead(SelectionKey key) throws IOException {
        logger.debug("#id: {} - handle read...{}",
                ((HttpChannelContext) key.attachment()).getId(), key);
        SocketChannel clientChannel = (SocketChannel) key.channel();
        HttpRequest request = getHttpRequest(key, clientChannel);
        logger.debug("#id: {} - After get request : {}",
                ((HttpChannelContext) key.attachment()).getId(), request);

        if (request != null && request.isFinished()) {
            logger.debug("#id: {} - Process the complete parsed request.",
                    ((HttpChannelContext) key.attachment()).getId());
            request.getBodyBuffer().flip();
            HttpResponse response = new HttpResponse(this, key,
                    request.isKeepAlive());
            RequestHandler rh = application.getHandler(request);
            HttpRequestDispatcher.dispatch(rh, request, response);
            setKeepAlive(key, request.isKeepAlive());
            // Only close if not async. In that case its up to RH to close it (+
            // don't close if it's a partial request).
            if (!rh.isMethodAsynchronous(request.getMethod())) {
                response.finish();
                logger.debug(
                        "#id: {} - Response finished and closed after synchronous request handler.",
                        ((HttpChannelContext) key.attachment()).getId());
            } else {

                logger.debug(
                        "#id: {} - Response not sent after asynchronous request handler.",
                        ((HttpChannelContext) key.attachment()).getId());
            }

        } else {
            logger.debug(
                    "#id: {} - Nothing to do since request is null or not completely parsed",
                    ((HttpChannelContext) key.attachment()).getId());
        }
    }

    @Override
    public void handleWrite(SelectionKey key) {
        logger.debug("#id: {} - handle write...",
                ((HttpChannelContext) key.attachment()).getId());
        HttpChannelContext ctx = (HttpChannelContext) key.attachment();
        logger.debug("#id: {} - pending data about to be written", ctx.getId());
        ByteBuffer toSend = ctx.getBufferOut();
        try {
            toSend.flip(); // prepare for write
            long bytesWritten = ((SocketChannel) key.channel()).write(toSend);
            logger.debug("#id: {} - sent {} bytes to wire", bytesWritten,
                    ctx.getId());
            if (!toSend.hasRemaining()) {
                logger.debug("#id: {} - sent all data in toSend buffer",
                        ctx.getId());
                closeOrRegisterForRead(key); // should probably only be done if
                                             // the HttpResponse is finished
            } else {
                toSend.compact(); // make room for more data be "read" in
            }
        } catch (IOException e) {
            logger.error("#id: {} - Failed to send data to client: {}",
                    e.getMessage(), ctx.getId());
            Closeables.cancelAndCloseQuietly(key);
        }
    }

    public void closeOrRegisterForRead(SelectionKey key) {
        if (key.isValid() && isKeepAlive(key)) {
            try {
                key.channel().register(key.selector(), SelectionKey.OP_READ,
                        reuseAttachment(key));
                // key.channel().register(key.selector(), SelectionKey.OP_READ,
                // ByteBuffer.allocate(READ_BUFFER_SIZE));
                logger.debug(
                        "#id: {} - keep-alive connection. registrating for read.",
                        ((HttpChannelContext) key.attachment()).getId());
            } catch (ClosedChannelException e) {
                logger.debug(
                        "#id: {} - ClosedChannelException while registrating key for read: {}",
                        e.getMessage(),
                        ((HttpChannelContext) key.attachment()).getId());
                Closeables.cancelAndCloseQuietly(key);
            }
        } else {
            // http request should be finished and no 'keep-alive' => close
            // connection
            logger.debug("#id: {} - Closing finished http connection",
                    ((HttpChannelContext) key.attachment()).getId());
            Closeables.cancelAndCloseQuietly(key);

        }
    }

    /**
     * Clears the buffer (prepares for reuse) attached to the given
     * SelectionKey.
     * 
     * @return A cleared (position=0, limit=capacity) ByteBuffer which is ready
     *         for new reads
     */
    private HttpChannelContext reuseAttachment(SelectionKey key) {
        HttpChannelContext ctx = (HttpChannelContext) key.attachment();
        ctx.clear(); // prepare for reuse
        return ctx;
    }

    private boolean isKeepAlive(SelectionKey key) {
        return ((HttpChannelContext) key.attachment()).isKeepAlive();
    }

    private void setKeepAlive(SelectionKey key, boolean keep) {
        ((HttpChannelContext) key.attachment()).setKeepAlive(keep);
    }

    private HttpRequest getHttpRequest(SelectionKey key,
            SocketChannel clientChannel) {
        HttpChannelContext ctx = (HttpChannelContext) key.attachment();
        int i = 0;
        try {
            i = clientChannel.read(ctx.getBufferIn());
        } catch (IOException e) {
            logger.error("Could not read buffer", e);
            Closeables.cancelAndCloseQuietly(key);

            return null;
        }
        if (i < 0) {
            logger.debug(
                    "#id: {} - Nothing to read in this buffer so close it !",
                    ((HttpChannelContext) key.attachment()).getId());
            Closeables.cancelAndCloseQuietly(key);
            return null;
        }
        ctx.getBufferIn().flip();
        return doGetHttpRequest(key, clientChannel, ctx);
    }

    private HttpRequest doGetHttpRequest(SelectionKey key,
            SocketChannel clientChannel, HttpChannelContext ctx) {
        // do we have any unfinished http post requests for this channel?
        HttpRequest request = null;
        if (ctx.getContext() != null) {
            request = HttpRequest.continueParsing(ctx.getBufferIn(),
                    ctx.getContext());
            // if (! (request instanceof PartialHttpRequest)) { // received the
            // entire payload/body
            // partials.remove(clientChannel);
            // }
        } else {
            request = HttpRequest.of(ctx.getBufferIn());
            if (request instanceof PartialHttpRequest) {
                ctx.setContext((PartialHttpRequest) request);
            }

        }
        ctx.getBufferIn().clear();
        return request;
    }

    @Override
    public String toString() {
        return "HttpProtocol";
    }

}
