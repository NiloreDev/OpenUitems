package wtf.uitems.client.feature.module.impl.visual.overlay.impl.targetinfo;

import wtf.uitems.client.feature.helper.impl.render.ScaleProperty;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.ScreenPositionProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;

public final class TargetInfoSettings {

    private final BooleanProperty enabled;
    private final ScreenPositionProperty screenPosition;
    private final ScaleProperty scale;
    private final ModeProperty<Mode> mode;
    private final BooleanProperty glow = new BooleanProperty("Background glow", false);

    TargetInfoSettings(final HUDModule module) {
        this.enabled = new BooleanProperty("Enabled", true);
        this.screenPosition = new ScreenPositionProperty("Screen Position", 0.43F, 0.65F);
        this.scale = ScaleProperty.newNVGElement();
        this.mode = new ModeProperty<>("Mode", Mode.DEFAULT);
        module.addProperties(new GroupProperty("Target information", this.enabled, this.screenPosition, this.scale.get(), this.mode, glow));
    }

    public boolean isEnabled() {
        return this.enabled.getValue();
    }

    public ScreenPositionProperty getScreenPosition() {
        return this.screenPosition;
    }

    public float getScale() {
        return scale.getScale();
    }

    public boolean isGlow() {
        return glow.getValue();
    }

    public Mode getMode() {
        return mode.getValue();
    }

    public enum Mode {
        DEFAULT("Default"),
        CARD("Card");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
