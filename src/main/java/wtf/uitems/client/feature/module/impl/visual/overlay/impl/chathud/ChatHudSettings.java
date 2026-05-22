package wtf.uitems.client.feature.module.impl.visual.overlay.impl.chathud;

import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;

public final class ChatHudSettings {

    private final BooleanProperty enabled = new BooleanProperty("Enabled", false);
    private final BooleanProperty glow = new BooleanProperty("Background glow", false);

    public ChatHudSettings(final HUDModule module) {
        module.addProperties(new GroupProperty("Chat HUD", enabled, glow));
    }

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public boolean isGlow() {
        return glow.getValue();
    }
}
