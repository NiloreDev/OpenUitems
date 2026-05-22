package wtf.uitems.client.feature.module.impl.visual.overlay.impl.notifications;

import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;

public final class NotificationSettings {

    private final BooleanProperty enabled;
    private final BooleanProperty moduleToggleNotifications;
    private final BooleanProperty moduleToggleSound;
    private final BooleanProperty themeIconColor;
    private final BooleanProperty glow;

    NotificationSettings(final HUDModule module) {
        this.enabled = new BooleanProperty("Enabled", true);
        this.moduleToggleNotifications = new BooleanProperty("On module toggle", false);
        this.moduleToggleSound = new BooleanProperty("Toggle sound", false).hideIf(() -> !this.moduleToggleNotifications.getValue());
        this.themeIconColor = new BooleanProperty("Theme icon color", false);
        this.glow = new BooleanProperty("Background glow", false);
        module.addProperties(new GroupProperty("Notifications", moduleToggleNotifications, moduleToggleSound, themeIconColor, glow));
    }

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public boolean isModuleToggleNotifications() {
        return moduleToggleNotifications.getValue();
    }

    public boolean isThemeIconColor() {
        return themeIconColor.getValue();
    }

    public boolean isModuleToggleSound() {
        return moduleToggleSound.getValue();
    }

    public boolean isGlow() {
        return glow.getValue();
    }

}
