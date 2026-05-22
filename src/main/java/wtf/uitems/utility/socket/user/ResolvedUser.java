package wtf.uitems.utility.socket.user;

import wtf.uitems.client.feature.module.impl.visual.CapeModule;

public final class ResolvedUser extends User {

    private final CapeModule.CapeType capeType;

    public ResolvedUser(final String name, final UserRole role, final CapeModule.CapeType capeType) {
        super(name, role);
        this.capeType = capeType;
    }

    public ResolvedUser(final String name, final UserRole role) {
        this(name, role, null);
    }

    public CapeModule.CapeType getCapeType() {
        return capeType;
    }

}
