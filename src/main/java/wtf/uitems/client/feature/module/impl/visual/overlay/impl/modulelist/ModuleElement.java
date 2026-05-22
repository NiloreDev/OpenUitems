package wtf.uitems.client.feature.module.impl.visual.overlay.impl.modulelist;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

import static wtf.uitems.client.Constants.mc;

public final class ModuleElement implements Comparable<ModuleElement> {
    private Animation xAnimation, yAnimation, heightAnimation;

    private final ToggledSettings settings;
    private final Module module;
    private float outlinePosY;
    private boolean outlineVisible, displayVisible, pendingDisplaySync;

    public ModuleElement(ToggledSettings settings, Module module) {
        this.settings = settings;
        this.module = module;
    }

    public void render(final DrawContext context, final int index, final boolean isBloom, final float previousWidth, final float nextWidth) {
        if (this.xAnimation == null || this.yAnimation == null || this.heightAnimation == null) {
            return;
        }

        this.xAnimation.run(this.posX);
        this.yAnimation.run(this.posY);
        this.heightAnimation.run(this.module.isEnabled() ? 1 : 0);

        final float scale = this.settings.getScale();
        final float anchorLeft = this.settings.getScreenPosition().getScaledX();
        final float anchorY = this.settings.getScreenPosition().getScaledY();
        final float anchorRight = anchorLeft + this.settings.getScreenPosition().getWidth();
        final float posX = this.xAnimation.getValue() + anchorRight;
        final float posY = this.yAnimation.getValue() + anchorY;


        final float radius = 1.F;

        final Pair<Integer, Integer> colors = ColorUtility.getClientTheme();
        final int color = ColorUtility.interpolateColorsBackAndForth(
                6,
                index * 20,
                colors.first, colors.second
        );

        final float lineHeight = this.getOffset();
        final ToggledSettings.BackgroundMode backgroundMode = this.settings.getBackgroundMode();

        if (backgroundMode == ToggledSettings.BackgroundMode.BLUR) {
            final HUDModule overlay = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
            NVGRenderer.rect(
                    (posX - anchorRight - 6.5F) * scale + anchorRight, posY * scale,
                    (this.width + 6.5F) * scale, lineHeight * scale, NVGRenderer.BLUR_PAINT, overlay.getBlurSampleOpacity()
            );
        }

        NVGRenderer.scale(
                scale,
                anchorRight,
                anchorY,
                0,
                0,
                () -> {
                    if (backgroundMode == ToggledSettings.BackgroundMode.BLUR) {
                        final HUDModule overlay = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
                        NVGRenderer.rect(posX - 6.5F, posY, this.width + 6.5F, lineHeight, overlay.getBlurBackgroundColor());
                        overlay.renderBlurTextureOverlay(posX - 6.5F, posY, this.width + 6.5F, lineHeight, 0F);
                    } else if (backgroundMode == ToggledSettings.BackgroundMode.THEME) {
                        final int startColor = ColorUtility.applyOpacity(colors.first, 0.22F);
                        final int endColor = ColorUtility.applyOpacity(colors.second, 0.18F);
                        NVGRenderer.rectGradient(posX - 6.5F, posY, this.width + 6.5F, lineHeight, startColor, endColor, 0F);
                    }

                    final ToggledSettings.BarMode barMode = settings.getBarMode().getValue();
                    if (barMode != ToggledSettings.BarMode.NONE && barMode != ToggledSettings.BarMode.OUTLINE) {
                        final float xOffset = barMode == ToggledSettings.BarMode.LEFT ? -4.5F : width - 2.5F;

                        NVGRenderer.roundedRect(posX + xOffset + 0.5F, posY + 2.5F, 1.F, 8.F, radius, ColorUtility.getShadowColor(color));
                        NVGRenderer.roundedRect(posX + xOffset, posY + 2.F, 1.F, 8.F, radius, color);
                    }

                    final float textOffset = getTextOffset(barMode);
                    if (this.settings.getFontMode().getValue() != ToggledSettings.FontMode.VANILLA) {
                        getFont().drawStringWithShadow(this.text, posX - textOffset, posY + 9.F, 8.F, color);
                    }
                }
        );

        if (!isBloom && this.settings.getFontMode().getValue() == ToggledSettings.FontMode.VANILLA) {
            final ToggledSettings.BarMode barMode = settings.getBarMode().getValue();
            final float textOffset = getTextOffset(barMode);
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(anchorRight, anchorY);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(posX - textOffset - anchorRight, posY + 2.0F);
            context.drawText(mc.textRenderer, Text.of(this.text).asOrderedText(), 0, 0, color, true);
            context.getMatrices().popMatrix();
        }
    }

    private float getTextOffset(final ToggledSettings.BarMode barMode) {
        return switch (barMode) {
            case LEFT -> 2F;
            case RIGHT -> 4.25F;
            case OUTLINE, NONE -> 3.5F;
        };
    }

    private static final NVGTextRenderer DEFAULT_FONT = FontRepository.getFont("productsans-medium");
    private static final NVGTextRenderer MIFONT = FontRepository.getFont("Mifont");
    private static final NVGTextRenderer TENACITY_BOLD_FONT = FontRepository.getFont("tenacity-bold");

    private NVGTextRenderer getFont() {
        return switch (this.settings.getFontMode().getValue()) {
            case MIFONT -> MIFONT;
            case TENACITY_BOLD -> TENACITY_BOLD_FONT;
            default -> DEFAULT_FONT;
        };
    }

    public static final float OFFSET = 12.F;

    public float getOffset() {
        return this.settings.getBackgroundMode() == ToggledSettings.BackgroundMode.NONE ? this.settings.getLineSpacing() : OFFSET;
    }

    private String text;
    private float width;
    private float posX, posY;
    private boolean visible, disabled;

    public boolean tick(int index, boolean visible) {
        final boolean textChanged = this.updateText();
        this.updateState(false, true);
        this.updatePosition(index, index, false, true, visible, visible);
        return textChanged;
    }

    public boolean tick(final int displayIndex, final int outlineIndex, final boolean outlineMode, final boolean allowDisplaySync) {
        final boolean textChanged = this.updateText();
        this.updateState(outlineMode, allowDisplaySync);
        this.updatePosition(displayIndex, outlineIndex, outlineMode, allowDisplaySync, this.displayVisible, this.outlineVisible);
        return textChanged;
    }

    private void updateState(final boolean outlineMode, final boolean allowDisplaySync) {
        final boolean actualVisible = this.isModuleVisible();

        if (outlineMode) {
            if (!this.outlineVisible && actualVisible) {
                this.outlineVisible = true;
                this.pendingDisplaySync = true;
            } else if (this.outlineVisible && !actualVisible) {
                this.outlineVisible = false;
                this.displayVisible = false;
                this.pendingDisplaySync = false;
            }

            if (this.pendingDisplaySync && allowDisplaySync) {
                this.displayVisible = this.outlineVisible;
                this.pendingDisplaySync = false;
                if (this.displayVisible) {
                    this.visible = true;
                }
            }
        } else {
            this.outlineVisible = actualVisible;
            this.displayVisible = actualVisible;
            this.pendingDisplaySync = false;
            if (this.displayVisible) {
                this.visible = true;
            }
        }

        this.updateVisibility();
    }

    private boolean updateText() {
        final String previousText = this.text;
        final float previousWidth = this.width;

        final String name = this.module.getName();
        final String suffix = this.module.getSuffix();
        if (suffix == null || !this.settings.isShowSuffix()) {
            this.text = name;
        } else {
            this.text = name + " " + Formatting.GRAY + suffix; // TODO color suffix gray
        }
        if (this.settings.isLowercase()) {
            this.text = this.text.toLowerCase();
        }
        if (this.settings.getFontMode().getValue() == ToggledSettings.FontMode.VANILLA) {
            this.width = mc.textRenderer.getWidth(this.text);
        } else {
            this.width = getFont().getStringWidth(this.text, 8.F);
        }

        return !this.text.equals(previousText) || this.width != previousWidth;
    }

    private void updateVisibility() {
        if (!this.displayVisible) {
            if (this.visible) {
                if (this.xAnimation != null && this.xAnimation.isFinished() && this.disabled) {
                    this.xAnimation = null;
                    this.yAnimation = null;
                    this.heightAnimation = null;
                    this.visible = false;
                    return;
                }
                this.disabled = true;
            }
        } else {
            this.disabled = false;
        }
    }

    private void updatePosition(int displayIndex, int outlineIndex, boolean outlineMode, boolean allowDisplaySync, boolean displayVisible, boolean outlineVisible) {
        if (this.disabled) {
            this.posX = 8.F;
        } else {
            this.posX = -this.width;
        }

        if (outlineVisible) {
            this.outlinePosY = outlineIndex * this.getOffset();
        }

        if (displayVisible) {
            this.visible = true;

            final boolean followOutlineLayout = outlineMode && !allowDisplaySync;
            this.posY = (followOutlineLayout ? outlineIndex : displayIndex) * this.getOffset();

            if (this.xAnimation == null) {
                this.xAnimation = new Animation(Easing.EASE_OUT_EXPO, 400);
                this.xAnimation.setValue(8.F);
            }
            if (this.yAnimation == null) {
                this.yAnimation = new Animation(Easing.EASE_OUT_EXPO, 600);
                this.yAnimation.setValue(this.posY);
            }
            if (this.heightAnimation == null) {
                this.heightAnimation = new Animation(Easing.EASE_IN_OUT_CUBIC, 200);
            }
        }
    }

    public boolean isModuleVisible() {
        return this.module.isVisible() && this.module.isEnabled() && this.settings.getVisibleCategories().getProperty(this.module.getCategory().getName()).getValue();
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isOutlineVisible() {
        return this.outlineVisible;
    }

    public boolean isDisplayVisible() {
        return this.displayVisible;
    }

    public boolean hasOutlineStateChange() {
        return this.isModuleVisible() != this.outlineVisible;
    }

    public boolean hasPendingDisplaySync() {
        return this.pendingDisplaySync;
    }

    public Animation getHeightAnimation() {
        return heightAnimation;
    }

    public boolean isDisplayReady() {
        return this.visible
                && this.xAnimation != null
                && this.yAnimation != null
                && this.heightAnimation != null;
    }

    @Override
    public int compareTo(@NotNull ModuleElement o) {
        return Float.compare(o.width, this.width);
    }

    public Module getModule() {
        return this.module;
    }

    public float getWidth() {
        return this.width;
    }

    public float getCurrentRenderX() {
        final float anchorLeft = this.settings.getScreenPosition().getScaledX();
        final float anchorRight = anchorLeft + this.settings.getScreenPosition().getWidth();
        return anchorRight - this.width - 6.5F;
    }

    public float getCurrentRenderY() {
        return this.settings.getScreenPosition().getScaledY() + this.outlinePosY;
    }

    public float getCurrentRenderWidth() {
        return this.width + 6.5F;
    }

    public float getCurrentRenderHeight() {
        return this.getOffset();
    }

}
