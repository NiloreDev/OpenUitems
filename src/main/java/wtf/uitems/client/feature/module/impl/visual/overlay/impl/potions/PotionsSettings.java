package wtf.uitems.client.feature.module.impl.visual.overlay.impl.potions;

import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.ScreenPositionProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;

public final class PotionsSettings {

    private final BooleanProperty enabled = new BooleanProperty("Enabled", true);
    private final ScreenPositionProperty screenPosition = new ScreenPositionProperty("Screen Position", 0.98F, 0.32F);
    private final BooleanProperty lowercase = new BooleanProperty("Lowercase", true);
    private final BooleanProperty glow = new BooleanProperty("Background glow", false);

    public PotionsSettings(final HUDModule module) {
        screenPosition.setSnapEnabled(false);
        module.addProperties(new GroupProperty("Potion HUD", enabled, screenPosition, lowercase, glow));
    }

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public ScreenPositionProperty getScreenPosition() {
        return screenPosition;
    }

    public boolean isLowercase() {
        return lowercase.getValue();
    }

    public boolean isGlow() {
        return glow.getValue();
    }
}
