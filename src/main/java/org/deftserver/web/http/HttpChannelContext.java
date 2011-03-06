package org.deftserver.web.http;

import java.nio.ByteBuffer;

import org.deftserver.io.ChannelContext;
import org.deftserver.io.IOHandler;

public class HttpChannelContext implements ChannelContext<PartialHttpRequest> {

    private Long id;

    private ByteBuffer bufferIn;

    private ByteBuffer bufferOut;

    private final IOHandler handler;

    private PartialHttpRequest request;

    private boolean keepAlive = false;

    public HttpChannelContext(IOHandler id) {
        this.handler = id;
    }

    public HttpChannelContext(IOHandler id, Long idLong) {
        this(id);
        this.id = idLong;
    }

    public HttpChannelContext(IOHandler id, ByteBuffer buff, Long idLong) {
        this(id, idLong);
        bufferIn = buff;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public ByteBuffer getBufferIn() {
        return bufferIn;
    }

    public void setBufferIn(ByteBuffer buff) {
        this.bufferIn = buff;
    }

    @Override
    public ByteBuffer getBufferOut() {
        return bufferOut;
    }

    public void setBufferOut(ByteBuffer buff) {
        this.bufferOut = buff;
    }

    @Override
    public PartialHttpRequest getContext() {
        return request;
    }

    public void setContext(PartialHttpRequest req) {
        request = req;
    }

    @Override
    public IOHandler getHandler() {
        return handler;
    }

    public void clear() {

        if (bufferOut != null) {
            bufferOut.clear();
        }
        if (bufferIn != null) {
            bufferIn.clear();
        }
        request = null;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
}
