package org.deftserver.example.kv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

import org.deftserver.io.DefaultChannelContext;
import org.deftserver.io.IOHandler;
import org.deftserver.io.IOLoopFactory;
import org.deftserver.util.Closeables;
import org.deftserver.web.AsyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

public class KeyValueStoreIOHandler implements IOHandler {

    private final Logger logger = LoggerFactory
            .getLogger(KeyValueStoreIOHandler.class);

    // Read callbacks (callbacks that are supposed to be invoked upon a
    // successful read)
    private final Map<SelectionKey, AsyncResult<String>> rcbs = Maps
            .newHashMap();

    @Override
    public void handleAccept(SocketChannel clientChannel) throws IOException {
        // logger.debug("[KeyValueStoreHandler] handle accept...");
        // SocketChannel clientChannel = ((ServerSocketChannel)
        // key.channel()).accept();
        // clientChannel.configureBlocking(false);
        // IOLoopFactory.getLoopController().addHandler(clientChannel, this,
        // SelectionKey.OP_READ, ByteBuffer.allocate(1024));
    }

    @Override
    public void handleRead(SelectionKey key) throws IOException {
        logger.debug("[KeyValueStoreHandler] handle read...");
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ((DefaultChannelContext) key.attachment())
                .getBufferIn();
        try {
            long bytesRead = channel.read(buffer);
            logger.debug("[KeyValueStoreHandler] read data: {} bytes",
                    bytesRead);
        } catch (IOException e) {
            if (rcbs.containsKey(key)) {
                rcbs.get(key).onFailure(e);
            }
            logger.debug("[KeyValueStoreHandler] could not read data: ",
                    e.getMessage());
            Closeables.closeQuietly(channel);
        }
        int length = buffer.position();
        buffer.flip();
        String data = new String(buffer.array(), 0, length, Charsets.UTF_8);
        logger.debug("[KeyValueStoreHandler] KeyValueStore server sent: {}",
                data);
        Closeables.closeQuietly(channel);
        logger.debug("[KeyValueStoreHandler] closed connection to KeyValueStore");
        if (rcbs.containsKey(key)) {
            rcbs.get(key).onSuccess(data);
        }
    }

    @Override
    public void handleWrite(SelectionKey key) {
        logger.debug("[KeyValueStoreHandler] handle write...");
        ByteBuffer toSend = ((DefaultChannelContext) key.attachment())
                .getBufferOut();

        SocketChannel channel = (SocketChannel) key.channel();
        logger.debug("[KeyValueStoreHandler] about to send: {} bytes",
                toSend.position());
        try {
            toSend.flip(); // prepare for write
            long bytesWritten = channel.write(toSend);
            logger.debug("[KeyValueStoreHandler] sent: {} bytes", bytesWritten);
            if (!toSend.hasRemaining()) {
            } else {
                toSend.compact(); // make room for more data be "read" in
            }
        } catch (IOException e) {
            logger.error(
                    "[KeyValueStoreHandler] Failed to send data to client: {}",
                    e.getMessage());
        }
        IOLoopFactory.getLoopController()
                .addHandler(
                        channel,
                        this,
                        SelectionKey.OP_READ,
                        new DefaultChannelContext(ByteBuffer.allocate(1024),
                                this, null));
    }

    void addReadCallback(SelectionKey key, AsyncResult<String> cb) {
        rcbs.put(key, cb);
    }

    @Override
    public void handleConnect(SelectionKey key) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public String toString() {
        return "KeyValueStoreIOHandler";
    }

}
