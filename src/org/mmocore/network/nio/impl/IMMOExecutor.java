package org.mmocore.network.nio.impl;

public interface IMMOExecutor<T extends MMOClient> {

    public void execute(Runnable r);
}
