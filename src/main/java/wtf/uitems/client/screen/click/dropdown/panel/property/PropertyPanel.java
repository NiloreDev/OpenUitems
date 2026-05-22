package wtf.uitems.client.screen.click.dropdown.panel.property;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import wtf.uitems.client.feature.module.property.Property;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.screen.click.dropdown.component.OpalPanelComponent;
import wtf.uitems.utility.render.ColorUtility;
import static wtf.uitems.client.Constants.mc;

public abstract class PropertyPanel<T extends Property<?>> extends OpalPanelComponent {

    private final T property;

    protected final static int DEFAULT_HEIGHT = 17;
    public static boolean modernStyle = false;
    public static Style style = Style.DEFAULT;

    protected boolean lastProperty;

    public PropertyPanel(final T property) {
        this.property = property;
        setHeight(DEFAULT_HEIGHT);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        final boolean isBloom = mouseX == -1 && mouseY == -1;
        if (isBloom) return;
        if (!lastProperty && style != Style.JELLO) {
            final Pair<Integer, Integer> themeColors = ColorUtility.getClientTheme();
            final long tod = mc.world != null ? mc.world.getTimeOfDay() : 0L;
            final boolean isNight = mc.world != null && ((tod % 24000L) >= 12000L && (tod % 24000L) <= 24000L);
            final float alpha = isNight ? 0.12F : 0.24F;
            final int c1 = ColorUtility.applyOpacity(themeColors.first, alpha);
            final int c2 = ColorUtility.applyOpacity(themeColors.second, alpha);
            NVGRenderer.rectGradient(x + 2, y + height - 1, width - 4, 1, c1, c2, 0);
        }
    }

    public T getProperty() {
        return property;
    }

    public boolean isHidden() {
        return property.isHidden();
    }

    public void setLastProperty(final boolean lastProperty) {
        this.lastProperty = lastProperty;
    }

    public static boolean isJelloStyle() {
        return style == Style.JELLO;
    }

    public static int getTitleColor() {
        return isJelloStyle() ? 0xFF3A3A3A : -1;
    }

    public static int getValueColor() {
        return isJelloStyle() ? 0xFF8E8E8E : ColorUtility.applyOpacity(-1, 0.8F);
    }

    public static int getAccentColor() {
        return ColorUtility.getClientTheme().first;
    }

    public static int getTrackColor() {
        return isJelloStyle() ? 0x1F000000 : ColorUtility.applyOpacity(ColorUtility.getClientTheme().second, 0.35F);
    }

    public static int getFieldBackgroundColor() {
        return isJelloStyle() ? 0x0E000000 : 0xFF191919;
    }

    public static int getFieldOutlineColor() {
        return isJelloStyle() ? 0x16000000 : 0xFF505050;
    }

    public enum Style {
        DEFAULT,
        MODERN,
        JELLO
    }

}
