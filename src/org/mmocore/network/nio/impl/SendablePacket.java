package org.mmocore.network.nio.impl;

import java.nio.ByteBuffer;

public abstract class SendablePacket<T extends MMOClient> extends org.mmocore.network.nio.SendablePacket<T> {

    @Override
    protected ByteBuffer getByteBuffer() {
        return ((SelectorThread) Thread.currentThread()).getWriteBuffer();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getClient() {
        return (T) ((SelectorThread) Thread.currentThread()).getWriteClient();
    }

    @Override
    protected abstract boolean write();
}
