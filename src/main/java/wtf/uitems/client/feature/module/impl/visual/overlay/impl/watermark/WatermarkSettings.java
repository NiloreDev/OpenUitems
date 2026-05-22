package wtf.uitems.client.feature.module.impl.visual.overlay.impl.watermark;

import wtf.uitems.client.feature.helper.impl.render.ScaleProperty;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.feature.module.property.impl.ScreenPositionProperty;

public final class WatermarkSettings {

    private final BooleanProperty enabled = new BooleanProperty("Enabled", true);
    private final ModeProperty<WatermarkMode> mode = new ModeProperty<>("Mode", WatermarkMode.NEW);
    private final BooleanProperty blur = new BooleanProperty("Blur", true);
    private final BooleanProperty bloom = new BooleanProperty("Bloom", true);
    private final ScaleProperty scale = ScaleProperty.newNVGElement();
    private final ScreenPositionProperty screenPosition = new ScreenPositionProperty("Screen Position", 0.02F, 0.05F);
    private final BooleanProperty glow = new BooleanProperty("Background glow", false);

    public WatermarkSettings(final HUDModule module) {
        screenPosition.setSnapEnabled(false);
        module.addProperties(new GroupProperty("Watermark", enabled, mode, blur, bloom, glow, scale.get(), screenPosition));
    }

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public WatermarkMode getMode() {
        return mode.getValue();
    }

    public boolean isBlur() {
        return blur.getValue();
    }

    public boolean isBloom() {
        return bloom.getValue();
    }

    public boolean isGlow() {
        return glow.getValue();
    }

    public float getScale() {
        return scale.getScale();
    }

    public ScreenPositionProperty getScreenPosition() {
        return screenPosition;
    }

    public enum WatermarkMode {
        CLASSIC, NEW, NEW2, TENACITY
    }
}
