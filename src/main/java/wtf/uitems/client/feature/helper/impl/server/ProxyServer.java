package wtf.uitems.client.feature.helper.impl.server;

import wtf.uitems.client.feature.helper.IHelper;

public abstract class ProxyServer implements IHelper {
    private final String name;

    protected ProxyServer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isHandlingEvents() {
        return false;
    }
}
