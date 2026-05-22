package wtf.uitems.client.feature.helper.impl.render;

import net.minecraft.client.util.Window;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;

import static wtf.uitems.client.Constants.mc;

public final class ScaleProperty {

    private static final ScaleMode[] MINECRAFT_VALUES = new ScaleMode[]{ScaleMode.AUTO, ScaleMode.SMALL, ScaleMode.NORMAL, null, ScaleMode.LARGE};
    private final ModeProperty<ScaleMode> modeProperty;

    private ScaleProperty(final ScaleMode[] values) {
        this.modeProperty = new ModeProperty<>("Scale", ScaleMode.AUTO, values);
    }

    public static ScaleProperty newMinecraftElement() {
        return new ScaleProperty(MINECRAFT_VALUES);
    }

    public static ScaleProperty newNVGElement() {
        return new ScaleProperty(ScaleMode.values());
    }

    public ModeProperty<ScaleMode> get() {
        return modeProperty;
    }

    public float getScale() {
        final float targetScale = switch (modeProperty.getValue()) {
            case SMALL -> 1F;
            case NORMAL -> 2F;
            case MEDIUM -> 8F / 3F;
            case LARGE -> 3F;
            default -> 1F;
        };

        if (modeProperty.getValue() == ScaleMode.AUTO) {
            return 1F;
        }

        return targetScale / getCurrentGuiScale();
    }

    private float getCurrentGuiScale() {
        final Window window = mc.getWindow();
        final int scaledWidth = window.getScaledWidth();

        if (scaledWidth <= 0) {
            return 1F;
        }

        final float scale = (float) window.getFramebufferWidth() / scaledWidth;
        return scale > 0F ? scale : 1F;
    }

    public enum ScaleMode {
        AUTO("Auto"),
        SMALL("Small (1x)"),
        NORMAL("Normal (2x)"),
        MEDIUM("Medium (2.67x)"),
        LARGE("Large (3x)");

        private final String name;

        ScaleMode(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
