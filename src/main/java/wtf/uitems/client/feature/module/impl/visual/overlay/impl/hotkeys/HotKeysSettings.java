package wtf.uitems.client.feature.module.impl.visual.overlay.impl.hotkeys;

import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.ScreenPositionProperty;

public final class HotKeysSettings {

    private final BooleanProperty enabled = new BooleanProperty("Enabled", true);
    private final ScreenPositionProperty screenPosition = new ScreenPositionProperty("Screen Position", 0.98F, 0.50F);
    private final BooleanProperty glow = new BooleanProperty("Background glow", false);

    public HotKeysSettings(final HUDModule module) {
        screenPosition.setSnapEnabled(false);
        module.addProperties(new GroupProperty("Hot Keys", enabled, screenPosition, glow));
    }

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public ScreenPositionProperty getScreenPosition() {
        return screenPosition;
    }

    public boolean isGlow() {
        return glow.getValue();
    }
}
