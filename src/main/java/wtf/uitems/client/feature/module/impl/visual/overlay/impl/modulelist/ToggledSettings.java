package wtf.uitems.client.feature.module.impl.visual.overlay.impl.modulelist;

import wtf.uitems.client.feature.helper.impl.render.ScaleProperty;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.bool.MultipleBooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.client.feature.module.property.impl.ScreenPositionProperty;

import java.util.stream.Stream;

public final class ToggledSettings {

    private final ScaleProperty scale;
    private final BooleanProperty enabled;
    private final BooleanProperty lowercase;
    private final BooleanProperty showSuffix;
    private final BooleanProperty offsetScoreboard;
    private final MultipleBooleanProperty visibleCategories;
    private final ModeProperty<BarMode> barMode;
    private final ModeProperty<FontMode> fontMode;
    private final ModeProperty<BackgroundMode> backgroundMode;
    private final NumberProperty debugLineSpacing;
    private final ScreenPositionProperty screenPosition;

    ToggledSettings(HUDModule module) {
        this.scale = ScaleProperty.newNVGElement();
        this.barMode = new ModeProperty<>("Bar mode", BarMode.LEFT);
        this.fontMode = new ModeProperty<>("Font", FontMode.MIFONT);
        this.backgroundMode = new ModeProperty<>("Background", BackgroundMode.BLUR);
        this.debugLineSpacing = new NumberProperty("Spacing", 12.0, 8.0, 20.0, 0.5)
                .hideIf(() -> this.backgroundMode.getValue() != BackgroundMode.NONE);
        this.screenPosition = new ScreenPositionProperty("Screen Position", 0.98F, 0.12F);

        this.enabled = new BooleanProperty("Enabled", true);
        this.lowercase = new BooleanProperty("Lowercase", true);
        this.showSuffix = new BooleanProperty("Show suffix", true);
        this.offsetScoreboard = new BooleanProperty("Offset scoreboard", true);

        this.visibleCategories = new MultipleBooleanProperty("Visible categories",
                Stream.of(ModuleCategory.VALUES)
                        .map(c -> new BooleanProperty(c.getName(), true))
                        .toArray(BooleanProperty[]::new)
        );

        module.addProperties(
                new GroupProperty(
                        "Toggled modules",
                        this.scale.get(), this.barMode, this.fontMode, this.backgroundMode, this.debugLineSpacing,
                        this.enabled, this.lowercase, this.showSuffix, this.offsetScoreboard, this.visibleCategories, this.screenPosition
                )
        );
    }

    public float getScale() {
        return this.scale.getScale();
    }

    public boolean isEnabled() {
        return this.enabled.getValue();
    }

    public boolean isLowercase() {
        return this.lowercase.getValue();
    }

    public boolean isShowSuffix() {
        return this.showSuffix.getValue();
    }

    public boolean isOffsetScoreboard() {
        return this.offsetScoreboard.getValue();
    }

    public MultipleBooleanProperty getVisibleCategories() {
        return this.visibleCategories;
    }

    public ModeProperty<BarMode> getBarMode() {
        return barMode;
    }

    public ModeProperty<FontMode> getFontMode() {
        return fontMode;
    }

    public BackgroundMode getBackgroundMode() {
        return backgroundMode.getValue();
    }

    public float getLineSpacing() {
        return debugLineSpacing.getValue().floatValue();
    }

    public ScreenPositionProperty getScreenPosition() {
        return screenPosition;
    }

    public enum BackgroundMode {
        NONE("None"),
        BLUR("Blur"),
        THEME("Theme");

        private final String name;

        BackgroundMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum BarMode {
        LEFT("Left"),
        RIGHT("Right"),
        OUTLINE("Outline"),
        NONE("None");

        private final String name;

        BarMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum FontMode {
        VANILLA("Vanilla"),
        DEFAULT("Default"),
        MIFONT("Mifont"),
        TENACITY_BOLD("Tenacity Bold");

        private final String name;

        FontMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
