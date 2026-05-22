package wtf.uitems.event.impl.client;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.event.EventCancellable;

public final class ModuleToggleEvent extends EventCancellable {
    private final Module module;
    private final boolean enabled;

    public ModuleToggleEvent(Module module, boolean enabled) {
        this.module = module;
        this.enabled = enabled;
    }

    public Module getModule() {
        return module;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
