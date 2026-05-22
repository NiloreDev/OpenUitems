package wtf.uitems.client.feature.module.impl.visual.overlay.impl.potions;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.IOverlayElement;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static wtf.uitems.client.Constants.mc;

public final class PotionsElement implements IOverlayElement {

    private static final NVGTextRenderer FONT = FontRepository.getFont("Mifont");
    private static final float TITLE_SIZE = 8.6F;
    private static final float TEXT_SIZE = 8.0F;

    private final PotionsSettings settings;
    private final HUDModule overlay;
    private final Animation visibilityAnimation = new Animation(Easing.EASE_OUT_EXPO, 260);
    private final Animation rowsAnimation = new Animation(Easing.EASE_IN_OUT_CUBIC, 320);

    public PotionsElement(final HUDModule module) {
        this.overlay = module;
        this.settings = new PotionsSettings(module);
    }

    @Override
    public void render(final DrawContext context, final float delta, final boolean isBloom) {
        if (mc.player == null) {
            return;
        }

        final List<String> rows = new ArrayList<>();
        final List<RegistryEntry<StatusEffect>> effects = new ArrayList<>(mc.player.getActiveStatusEffects().keySet());
        effects.sort(Comparator.comparingDouble((RegistryEntry<StatusEffect> entry) -> {
            final StatusEffectInstance instance = mc.player.getActiveStatusEffects().get(entry);
            return -FONT.getStringWidth(formatEffect(instance), TEXT_SIZE);
        }));

        for (RegistryEntry<StatusEffect> entry : effects) {
            final StatusEffectInstance instance = mc.player.getActiveStatusEffects().get(entry);
            if (instance != null) {
                rows.add(formatEffect(instance));
            }
        }

        final Pair<Integer, Integer> colors = ColorUtility.getClientTheme();
        final float x = settings.getScreenPosition().getScaledX();
        final float y = settings.getScreenPosition().getScaledY();

        final String title = convertCase("Potions");
        final float padding = 5F;
        final float lineHeight = 11.0F;
        final float headerHeight = 13F;

        float maxTextWidth = FONT.getStringWidth(title, TITLE_SIZE);
        for (final String row : rows) {
            maxTextWidth = Math.max(maxTextWidth, FONT.getStringWidth(row, TEXT_SIZE));
        }

        final float width = Math.max(90F, maxTextWidth + padding * 2F + 2F);
        final float targetRows = rows.size();

        visibilityAnimation.run(targetRows > 0 ? 1F : 0F);
        rowsAnimation.run(targetRows);

        final float visibility = MathHelper.clamp(visibilityAnimation.getValue(), 0F, 1F);
        final float animatedRows = Math.max(0F, rowsAnimation.getValue());
        if (visibility <= 0.01F && targetRows == 0F) {
            settings.getScreenPosition().setWidth(width);
            settings.getScreenPosition().setHeight(0F);
            return;
        }

        final float height = (padding * 2F + headerHeight + animatedRows * lineHeight) * visibility;

        settings.getScreenPosition().setWidth(width);
        settings.getScreenPosition().setHeight(height);

        final float drawX = x + (1F - visibility) * 10F;
        final float drawY = y;
        final float radius = 5F;

        NVGRenderer.globalAlpha(visibility);

        if (!isBloom && overlay.isBlur()) {
            NVGRenderer.roundedRect(drawX, drawY, width, height, radius, NVGRenderer.BLUR_PAINT, overlay.getBlurSampleOpacity());
        }

        if (!isBloom && settings.isGlow()) {
            final long now = System.currentTimeMillis();
            final float t = (float) ((now % 4000L) / 4000.0);
            final float angle = t * 360F;
            final int a1 = (int) (90 + 50 * Math.sin(t * Math.PI * 2));
            final int a2 = (int) (70 + 60 * Math.sin((t + 0.25F) * Math.PI * 2));
            final int c1 = ColorUtility.applyOpacity(colors.first, Math.max(0, Math.min(255, a1)));
            final int c2 = ColorUtility.applyOpacity(colors.second, Math.max(0, Math.min(255, a2)));
            NVGRenderer.roundedRectGradient(drawX, drawY, width, height, radius, c1, c2, angle);
            NVGRenderer.roundedRectGradient(drawX, drawY, width, height, radius, c2, c1, angle + 110F);
        }

        if (isBloom) {
            final int bloomBase = settings.isGlow() ? 0x55FFFFFF : 0x35FFFFFF;
            NVGRenderer.roundedRect(drawX, drawY, width, height, radius, bloomBase);
            NVGRenderer.roundedRectOutline(drawX, drawY, width, height, radius, 1.15F, ColorUtility.applyOpacity(colors.first, 190));
            NVGRenderer.roundedRectOutline(drawX + 0.35F, drawY + 0.35F, width - 0.7F, height - 0.7F, radius, 1.0F, ColorUtility.applyOpacity(colors.second, 170));
        } else {
            NVGRenderer.roundedRect(drawX, drawY, width, height, radius, overlay.getBlurBackgroundColor());
            overlay.renderBlurTextureOverlay(drawX, drawY, width, height, radius);
        }

        if (isBloom) {
            FONT.drawString(title, drawX + padding, drawY + padding + 6.5F, TITLE_SIZE, -1);
        } else {
            FONT.drawGradientString(title, drawX + padding, drawY + padding + 6.5F, TITLE_SIZE, colors.first, colors.second, false);
        }

        final float rowStartY = drawY + padding + headerHeight + 6F;
        final float revealHeight = animatedRows * lineHeight + 1F;
        NVGRenderer.scissor(drawX, rowStartY - 8F, width, Math.max(0F, revealHeight), () -> {
            float rowY = rowStartY;
            for (final String row : rows) {
                if (isBloom) {
                    FONT.drawString(row, drawX + padding, rowY, TEXT_SIZE, -1);
                } else {
                    FONT.drawStringWithShadow(row, drawX + padding, rowY, TEXT_SIZE, -1);
                }
                rowY += lineHeight;
            }
        });

        NVGRenderer.globalAlpha(1F);
    }

    private String formatEffect(final StatusEffectInstance instance) {
        final String duration = instance.isInfinite() ? "**:**" : formatTicks(instance.getDuration());
        final String name = convertCase(I18n.translate(instance.getTranslationKey()));
        final String amp = instance.getAmplifier() > 0 ? " " + (instance.getAmplifier() + 1) : "";
        return name + amp + " §7" + duration;
    }

    private String formatTicks(final int ticks) {
        int i = MathHelper.floor((float) ticks / 20);
        int j = i / 60;
        i %= 60;
        int h = j / 60;
        j %= 60;
        return h > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", h, j, i)
                : String.format(Locale.ROOT, "%d:%02d", j, i);
    }

    private String convertCase(final String text) {
        return settings.isLowercase() ? text.toLowerCase(Locale.ROOT) : text;
    }

    @Override
    public boolean isActive() {
        return !mc.getDebugHud().shouldShowDebugHud() && settings.isEnabled();
    }

    @Override
    public boolean isBloom() {
        return overlay.isBloom();
    }
}
