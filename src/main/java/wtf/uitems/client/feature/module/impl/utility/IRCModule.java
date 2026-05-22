package wtf.uitems.client.feature.module.impl.utility;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;

public final class IRCModule extends Module {

    public IRCModule() {
        super("IRC", "Lets you chat with other Uitems users.", ModuleCategory.UTILITY);
        setEnabled(true);
    }

}
