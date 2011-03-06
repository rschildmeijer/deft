package org.deftserver.io;

import java.nio.ByteBuffer;

public class DefaultChannelContext implements ChannelContext<Object> {

    private ByteBuffer bufferIn;

    private ByteBuffer bufferOut;

    private final IOHandler handler;

    private Object context;

    public DefaultChannelContext(IOHandler handler) {
        this.handler = handler;
    }

    public DefaultChannelContext(ByteBuffer bufferIn, IOHandler handler,
            Object context) {
        super();
        this.bufferIn = bufferIn;
        this.handler = handler;
        this.context = context;
    }

    @Override
    public IOHandler getHandler() {
        return handler;
    }

    @Override
    public ByteBuffer getBufferIn() {

        return bufferIn;
    }

    @Override
    public void setBufferIn(ByteBuffer buffer) {
        this.bufferIn = buffer;
    }

    @Override
    public ByteBuffer getBufferOut() {

        return bufferOut;
    }

    public void setBufferOut(ByteBuffer bufferOut) {
        this.bufferOut = bufferOut;
    }

    @Override
    public Object getContext() {

        return this.context;
    }

}
