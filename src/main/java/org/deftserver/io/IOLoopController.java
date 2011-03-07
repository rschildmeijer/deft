package org.deftserver.io;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.deftserver.io.timeout.Timeout;
import org.deftserver.web.AsyncCallback;
import org.deftserver.web.http.HttpResponse;

public interface IOLoopController {

    SelectionKey addHandler(SelectableChannel channel, IOHandler handler,
            int interestOps, Object attachment);

    /**
     * Registers Read OPS on the given channel setting the context as attachment
     * if the context does not contains a input buffer then the controller will
     * set its default buffer.
     * 
     * @param channel
     * @param handler
     * @param attachment
     * @return
     */
    SelectionKey registerReadHandler(SelectableChannel channel,
            IOHandler handler, ChannelContext ctx);

    void updateHandler(SelectableChannel channel, int newInterestOps);

    void removeHandler(SelectableChannel channel);

    void addKeepAliveTimeout(SocketChannel channel, Timeout keepAliveTimeout);

    boolean hasKeepAliveTimeout(SelectableChannel channel);

    void addTimeout(Timeout timeout);

    void addCallback(AsyncCallback callback);

    void pushResponse(HttpResponse response);

    void planifyResponse();
}