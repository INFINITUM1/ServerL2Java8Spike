package org.mmocore.network.nio.impl;

public interface IClientFactory<T extends MMOClient> {

    public T create(MMOConnection<T> con);
}
