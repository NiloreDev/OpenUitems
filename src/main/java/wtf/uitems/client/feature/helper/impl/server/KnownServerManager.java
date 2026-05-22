package wtf.uitems.client.feature.helper.impl.server;

import net.minecraft.client.network.ServerAddress;
import wtf.uitems.event.EventDispatcher;

public final class KnownServerManager {

    private KnownServer currentServer;

    public void identifyServer(final ServerAddress address) {
        this.currentServer = null;
    }

    public KnownServer getCurrentServer() {
        return currentServer;
    }

    public void resetServer() {
        this.currentServer = null;
    }

    public void setServer(final KnownServer currentServer) {
        if (this.currentServer != currentServer) {
            this.currentServer = currentServer;
            EventDispatcher.subscribe(currentServer);
        }
    }

    private static final KnownServerIdentifier[] SERVER_IDENTIFIERS = {};

}
