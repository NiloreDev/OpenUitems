package wtf.uitems.client.feature.module;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.binding.IBindable;
import wtf.uitems.client.feature.helper.impl.render.ScreenPositionManager;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.notifications.NotificationSettings;
import wtf.uitems.client.feature.module.property.IPropertyListProvider;
import wtf.uitems.client.feature.module.property.Property;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.ScreenPositionProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModuleMode;
import wtf.uitems.client.notification.NotificationType;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.client.ModuleToggleEvent;
import wtf.uitems.event.subscriber.IEventSubscriber;
import wtf.uitems.client.verify.VerifyManager;
import net.minecraft.sound.SoundEvents;

import static wtf.uitems.client.Constants.mc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Module implements IBindable, IPropertyListProvider, IEventSubscriber {

    private final String name, description;

    @Expose
    @SerializedName("name")
    private final String id;

    private final ModuleCategory category;

    @Expose
    @SerializedName("enabled")
    private boolean enabled;

    @Expose
    @SerializedName("visible")
    private boolean visible = true;

    @Expose
    @SerializedName("properties")
    private final List<Property<?>> propertyList = new ArrayList<>();

    private final List<ModuleMode<?>> moduleModeList = new ArrayList<>();
    private ModeProperty<?> modeProperty;

    private boolean expanded;
    private int propertyIndex;

    protected Module(final String name, final String description, final ModuleCategory category) {
        this.name = name;
        this.id = name.toLowerCase().replace(' ', '_');
        this.description = description;
        this.category = category;

        EventDispatcher.subscribe(this);
    }

    public final void setEnabled(final boolean enabled) {
        if (enabled && !VerifyManager.getInstance().isAuthenticated()) {
            return;
        }
        if (this.enabled == enabled) {
            return;
        }
        final ModuleToggleEvent event = new ModuleToggleEvent(this, enabled);
        EventDispatcher.dispatch(event);
        if (event.isCancelled()) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }

        wtf.uitems.utility.data.SaveUtility.saveLocalConfigDebounced("default");
    }

    public final void toggle() {
        this.setEnabled(!this.isEnabled());

        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        if (overlayModule.isEnabled()) {
            final NotificationSettings notificationSettings = overlayModule.getNotifications().getSettings();
            if (notificationSettings.isEnabled() && notificationSettings.isModuleToggleNotifications()) {
                OpalClient.getInstance().getNotificationManager()
                        .builder(this.enabled ? NotificationType.INFO : NotificationType.ERROR)
                        .duration(1000)
                        .title(this.name)
                        .description("Module " + (this.enabled ? "enabled." : "disabled."))
                        .buildAndPublish();

                if (notificationSettings.isModuleToggleSound() && mc.player != null) {
                    final float pitch = this.enabled ? 1.0F : 0.82F;
                    mc.player.playSound(SoundEvents.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON, 0.7F, pitch);
                }
            }
        }
    }

    protected void onEnable() {
        if (getActiveMode() != null)
            getActiveMode().onEnable();
    }

    protected void onDisable() {
        if (getActiveMode() != null)
            getActiveMode().onDisable();
    }

    public final String getName() {
        return name;
    }

    public final String getId() {
        return id;
    }

    public final String getDescription() {
        return description;
    }

    public final ModuleCategory getCategory() {
        return category;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final boolean isVisible() {
        return visible;
    }

    public final void setVisible(boolean visible) {
        this.visible = visible;
    }

    public final void addProperties(Property<?>... properties) {
        for (final Property<?> property : properties) {
            if (property == null) continue;
            propertyList.add(property);
            if (property instanceof ScreenPositionProperty screenPositionProperty) {
                ScreenPositionManager.getInstance().register(this, screenPositionProperty);
            } else if (property instanceof GroupProperty group) {
                for (final Property<?> groupProperty : group.getPropertyList()) {
                    if (groupProperty instanceof ScreenPositionProperty screenPositionProperty) {
                        ScreenPositionManager.getInstance().register(this, screenPositionProperty);
                    }
                }
            }
        }
    }

    @SafeVarargs
    public final <T extends Module> void addModuleModes(final ModeProperty<?> modeProperty, final ModuleMode<T>... modes) {
        this.modeProperty = modeProperty;
        Collections.addAll(moduleModeList, modes);
    }

    public final List<ModuleMode<?>> getModuleModes() {
        return moduleModeList;
    }

    public final ModuleMode<?> getActiveMode() {
        return moduleModeList.stream().filter(m -> m.getEnumValue().equals(modeProperty.getValue())).findFirst().orElse(null);
    }

    public final ModeProperty<?> getModeProperty() {
        return modeProperty;
    }

    public final void setModeProperty(ModeProperty<?> modeProperty) {
        this.modeProperty = modeProperty;
    }

    public String getSuffix() {
        return null;
    }

    public final boolean isExpanded() {
        return expanded;
    }

    public final int getPropertyIndex() {
        return propertyIndex;
    }

    public final void setPropertyIndex(final int propertyIndex) {
        this.propertyIndex = propertyIndex;
    }

    public final void setExpanded(final boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public final List<Property<?>> getPropertyList() {
        return propertyList;
    }

    @Override
    public final void onBindingInteraction() {
        toggle();
    }

    @Override
    public final boolean isHandlingEvents() {
        return enabled;
    }

}

