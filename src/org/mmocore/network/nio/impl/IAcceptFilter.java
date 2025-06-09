package org.mmocore.network.nio.impl;

import java.nio.channels.SocketChannel;

public interface IAcceptFilter {

    public boolean accept(SocketChannel sc);
}
