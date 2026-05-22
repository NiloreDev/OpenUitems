package wtf.uitems.client.feature.module.impl.visual.overlay.impl.watermark;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.IOverlayElement;
import wtf.uitems.client.ReleaseInfo;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.utility.render.ColorUtility;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.lwjgl.nanovg.NanoVG.*;
import static wtf.uitems.client.Constants.mc;

public final class WatermarkElement implements IOverlayElement {

    private static final NVGTextRenderer FONT = FontRepository.getFont("Mifont");
    private static final NVGTextRenderer TENACITY_FONT = FontRepository.getFont("tenacity-bold");
    private static final NVGTextRenderer TENACITY_SMALL_FONT = FontRepository.getFont("productsans-regular");
    private static final float FONT_SIZE = 9.5F;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final WatermarkSettings settings;
    private final HUDModule overlay;
    private float lastYaw = 0;
    private long cachedTime = -1;
    private float cachedOffsetX = 0;
    private float cachedGlitchX1 = 0;
    private float cachedGlitchX2 = 0;
    private boolean cachedIsGlitching = false;
    private boolean hasCachedThisFrame = false;
    private boolean firstRender = true;

    public WatermarkElement(final HUDModule module) {
        this.overlay = module;
        this.settings = new WatermarkSettings(module);
    }

    @Override
    public void render(final DrawContext context, final float delta, final boolean isBloom) {
        hasCachedThisFrame = false; // Reset cache flag at start of render
        final Pair<Integer, Integer> colors = ColorUtility.getClientTheme();
        final WatermarkSettings.WatermarkMode mode = this.settings.getMode();

        if (mode == WatermarkSettings.WatermarkMode.TENACITY) {
            renderTenacityWatermark(colors, isBloom);
        } else if (mode == WatermarkSettings.WatermarkMode.CLASSIC) {
            final String text = String.format("%s (%s) (%s)", ReleaseInfo.NAME, mc.getCurrentFps(), LocalTime.now().format(TIME_FORMATTER));
            final float scale = this.settings.getScale();
            final var screenPosition = this.settings.getScreenPosition();
            final float width = FONT.getStringWidth(text, FONT_SIZE);
            final float height = FONT_SIZE + 4;
            screenPosition.setWidth(width * scale);
            screenPosition.setHeight(height * scale);
            final float x = screenPosition.getScaledX();
            final float y = screenPosition.getScaledY();
            NVGRenderer.scale(scale, x, y, 0, 0, () -> {
                if (isBloom) {
                    FONT.drawGradientString(text, x, y, FONT_SIZE, colors.first, colors.second, false);
                } else {
                    FONT.drawGradientStringWithShadow(text, x, y, FONT_SIZE, colors.first, colors.second);
                }
            });
        } else {
            renderNewWatermark(colors, mode, isBloom);
        }
    }

    private void renderTenacityWatermark(final Pair<Integer, Integer> colors, final boolean isBloom) {
        final boolean lowercase = overlay.getToggledModules().getSettings().isLowercase();
        final String baseName = ReleaseInfo.NAME;
        final String name = lowercase
                ? Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1)
                : Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
        final String versionText = ReleaseInfo.VERSION + "(" + formatChannelName(ReleaseInfo.CHANNEL) + ")";

        final float scale = this.settings.getScale();
        final float nameSize = 21F;
        final float versionSize = 8.5F;
        final float paddingX = 8F;
        final float paddingTop = 4F;
        final float width = TENACITY_FONT.getStringWidth(name, nameSize) + 40F;
        final float height = nameSize + 8F;

        final var screenPosition = this.settings.getScreenPosition();
        screenPosition.setWidth(width * scale);
        screenPosition.setHeight(height * scale);

        final float x = screenPosition.getScaledX();
        final float y = screenPosition.getScaledY();

        NVGRenderer.scale(scale, x, y, width, height, () -> {
            if (isBloom) {
                TENACITY_FONT.drawGradientString(name, x + paddingX, y + height - 6F, nameSize, colors.first, colors.second);
                final int versionStartBloom = ColorUtility.applyOpacity(ColorUtility.darker(colors.first, 0.25F), 0.75F);
                final int versionEndBloom = ColorUtility.applyOpacity(ColorUtility.darker(colors.second, 0.25F), 0.75F);
                final float versionWidth = TENACITY_SMALL_FONT.getStringWidth(versionText, versionSize);
                TENACITY_SMALL_FONT.drawGradientString(
                        versionText,
                        x + width - versionWidth - 6F,
                        y + paddingTop + 3F,
                        versionSize,
                        versionStartBloom,
                        versionEndBloom
                );
                return;
            }

            final int shadowStart = ColorUtility.applyOpacity(ColorUtility.darker(colors.first, 0.45F), 0.9F);
            final int shadowEnd = ColorUtility.applyOpacity(ColorUtility.darker(colors.second, 0.45F), 0.9F);
            TENACITY_FONT.drawGradientString(name, x + paddingX + 0.8F, y + height - 5.2F, nameSize, shadowStart, shadowEnd);
            TENACITY_FONT.drawGradientString(name, x + paddingX, y + height - 6F, nameSize, colors.first, colors.second);
            final float versionWidth = TENACITY_SMALL_FONT.getStringWidth(versionText, versionSize);
            final int versionStart = ColorUtility.applyOpacity(ColorUtility.darker(colors.first, 0.35F), 0.82F);
            final int versionEnd = ColorUtility.applyOpacity(ColorUtility.darker(colors.second, 0.35F), 0.82F);
            final int versionShadowStart = ColorUtility.applyOpacity(ColorUtility.darker(colors.first, 0.55F), 0.8F);
            final int versionShadowEnd = ColorUtility.applyOpacity(ColorUtility.darker(colors.second, 0.55F), 0.8F);
            TENACITY_SMALL_FONT.drawGradientString(
                    versionText,
                    x + width - versionWidth - 5.2F,
                    y + paddingTop + 3.8F,
                    versionSize,
                    versionShadowStart,
                    versionShadowEnd
            );
            TENACITY_SMALL_FONT.drawGradientString(
                    versionText,
                    x + width - versionWidth - 6F,
                    y + paddingTop + 3F,
                    versionSize,
                    versionStart,
                    versionEnd
            );
        });
    }

    private String formatChannelName(final ReleaseInfo.ReleaseChannel channel) {
        final String raw = channel.toString();
        return raw.isEmpty() ? raw : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private void renderNewWatermark(Pair<Integer, Integer> colors, WatermarkSettings.WatermarkMode mode, boolean isBloom) {
        final float scale = this.settings.getScale();
        
        // Cache values once per frame (to avoid jitter between passes)
        if (!hasCachedThisFrame) {
            cachedTime = System.currentTimeMillis();
            
            // Glitch calculation
            final long glitchCycle = 3000; // 3 seconds per cycle
            final long glitchDuration = 120; // 120ms duration
            final long glitchTime = cachedTime % glitchCycle;
            cachedIsGlitching = glitchTime < glitchDuration;
             if (cachedIsGlitching) {
                 // Random offset between -2 and 2 during glitch
                 cachedGlitchX1 = (float) (Math.random() * 4 - 2);
                 cachedGlitchX2 = (float) (Math.random() * 4 - 2);
             } else {
                 cachedGlitchX1 = 0;
                 cachedGlitchX2 = 0;
             }

            if (mc.player != null) {
                float currentYaw = mc.player.getYaw();
                if (firstRender) {
                    lastYaw = currentYaw;
                    firstRender = false;
                }
                float yawDiff = currentYaw - lastYaw;
                cachedOffsetX = Math.max(-2.5f, Math.min(2.5f, yawDiff * 1.0f));
            }
            hasCachedThisFrame = true;
        }

        final long time = cachedTime;
        final float finalOffsetX = cachedOffsetX;
        
        final String name = ReleaseInfo.NAME;
        final String channel = ReleaseInfo.CHANNEL.toString();
        
        final float nameWidth = FONT.getStringWidth(name, FONT_SIZE);
        final float channelWidth = FONT.getStringWidth(channel, FONT_SIZE);
        final float maxTextWidth = Math.max(nameWidth, channelWidth);
        
        // Animation logic
        final float animationCycle = 10000; // 10 seconds per cycle
        final float progress = (time % (long)animationCycle) / animationCycle; // 0.0 to 1.0
        
        // Wipe logic: 
        // 0.0 - 0.45: Show Name
        // 0.45 - 0.50: Wipe R to L (Reveal Channel on Right)
        // 0.50 - 0.95: Show Channel
        // 0.95 - 1.0: Wipe L to R (Reveal Name on Left)
        
        float wipeProgress; // 0.0 (all Name) to 1.0 (all Channel)
        if (progress < 0.45f) {
            wipeProgress = 0;
        } else if (progress < 0.50f) {
            wipeProgress = (progress - 0.45f) / 0.05f; // 0 to 1
        } else if (progress < 0.95f) {
            wipeProgress = 1;
        } else {
            wipeProgress = 1.0f - (progress - 0.95f) / 0.05f; // 1 to 0
        }

        final float paddingH = 10;
        final float paddingV = 5;
        final float rectWidth = maxTextWidth + paddingH * 2;
        final float rectHeight = FONT_SIZE + paddingV * 2;
        final var screenPosition = this.settings.getScreenPosition();
        screenPosition.setWidth(rectWidth * scale);
        screenPosition.setHeight(rectHeight * scale);
        final float x = screenPosition.getScaledX();
        final float y = screenPosition.getScaledY();

        final float finalWipeProgress = wipeProgress;
        final float glitchX1 = cachedGlitchX1;
        final float glitchX2 = cachedGlitchX2;

        NVGRenderer.scale(scale, x, y, rectWidth, rectHeight, () -> {
            Runnable renderTask = () -> {
                if (!isBloom) {
                    // Draw background (Oval-like rounded rect)
                    if (this.settings.isBlur()) {
                        NVGRenderer.roundedRect(x, y, rectWidth, rectHeight, rectHeight / 2f, NVGRenderer.BLUR_PAINT, overlay.getBlurSampleOpacity());
                    }
                    if (this.settings.isGlow()) {
                        final long now = System.currentTimeMillis();
                        final float t = (float) ((now % 4000L) / 4000.0);
                        final float angle = t * 360F;
                        final int a1 = (int) (90 + 50 * Math.sin(t * Math.PI * 2));
                        final int a2 = (int) (70 + 60 * Math.sin((t + 0.25F) * Math.PI * 2));
                        final int c1 = ColorUtility.applyOpacity(colors.first, Math.max(0, Math.min(255, a1)));
                        final int c2 = ColorUtility.applyOpacity(colors.second, Math.max(0, Math.min(255, a2)));
                        NVGRenderer.roundedRectGradient(x, y, rectWidth, rectHeight, rectHeight / 2f, c1, c2, angle);
                        NVGRenderer.roundedRectGradient(x, y, rectWidth, rectHeight, rectHeight / 2f, c2, c1, angle + 110F);
                    }
                    NVGRenderer.roundedRect(x, y, rectWidth, rectHeight, rectHeight / 2f, overlay.getBlurBackgroundColor());
                    overlay.renderBlurTextureOverlay(x, y, rectWidth, rectHeight, rectHeight / 2f);
                }

                if (mode == WatermarkSettings.WatermarkMode.NEW2) {
                        // Main outline logic for NEW2: Continuous brush draw and erase animation
                        final float loadingCycle = 3000; // 3 seconds for a full cycle
                        float animationProgress = (time % (long) loadingCycle) / loadingCycle; // 0.0 to 1.0
                        
                        // Animation segments:
                        // 0.0 - 0.5: Brush draws the outline clockwise (0% to 100%)
                        // 0.5 - 1.0: Brush erases the outline clockwise (0% to 100%)
                        float startProgress = 0;
                        float endProgress = 0;
                        
                        if (animationProgress < 0.5f) {
                            // Drawing phase
                            startProgress = 0;
                            endProgress = animationProgress / 0.5f;
                        } else {
                            // Erasing phase
                            startProgress = (animationProgress - 0.5f) / 0.5f;
                            endProgress = 1.0f;
                        }
                        
                        // Ghosting effect for the border (matches text ghosting)
                        final int steps = 3;
                        for (int s = 1; s <= steps; s++) {
                            final float stepRatio = (float) s / steps;
                            final float currentOffsetX = finalOffsetX * stepRatio;
                            // Fade out the ghost layers
                            final int ghostColor = ColorUtility.applyOpacity(colors.first, (int) (160 * (1.0f - stepRatio * 0.7f)));
                            NVGRenderer.roundedRectOutlineSegmented(x + currentOffsetX, y, rectWidth, rectHeight, rectHeight / 2f, 1.0f, startProgress, endProgress, ghostColor);
                        }

                        // Draw the main outline
                        NVGRenderer.roundedRectOutlineSegmented(x, y, rectWidth, rectHeight, rectHeight / 2f, 1.0f, startProgress, endProgress, colors.first);
                    } else if (isBloom) {
                    // NEW mode bloom: render a subtle outline in bloom pass to create neon effect
                    NVGRenderer.roundedRectOutline(x, y, rectWidth, rectHeight, rectHeight / 2f, 1.0f, colors.first);
                }

                float wipeX = x + rectWidth * (1.0f - finalWipeProgress);

                // Left side of wipe: Name (Uitems)
                NVGRenderer.scissor(x, y, wipeX - x, rectHeight, () -> {
                    FONT.drawGradientOutlineString(name, x + rectWidth / 2f - nameWidth / 2f, y + paddingV + FONT_SIZE - 1, FONT_SIZE, colors.first, colors.second, -1, finalOffsetX, 0);
                });

                // Right side of wipe: Channel (Version)
                NVGRenderer.scissor(wipeX, y, x + rectWidth - wipeX, rectHeight, () -> {
                    FONT.drawGradientOutlineString(channel, x + rectWidth / 2f - channelWidth / 2f, y + paddingV + FONT_SIZE - 1, FONT_SIZE, colors.first, colors.second, -1, finalOffsetX, 0);
                });

                // Wipe line with trail
                if (finalWipeProgress > 0 && finalWipeProgress < 1) {
                    float linePadding = 3.5f;
                    NVGRenderer.rect(wipeX - 0.5f, y + linePadding, 1, rectHeight - linePadding * 2, colors.first);
                }
            };

            if (cachedIsGlitching) {
                 // Top half
                 NVGRenderer.scissor(x - 10, y, rectWidth + 20, rectHeight / 2f, () -> {
                     nvgSave(NVGRenderer.getContext());
                     nvgTranslate(NVGRenderer.getContext(), glitchX1, 0);
                     renderTask.run();
                     nvgRestore(NVGRenderer.getContext());
                 });
                 // Bottom half
                 NVGRenderer.scissor(x - 10, y + rectHeight / 2f, rectWidth + 20, rectHeight / 2f, () -> {
                     nvgSave(NVGRenderer.getContext());
                     nvgTranslate(NVGRenderer.getContext(), glitchX2, 0);
                     renderTask.run();
                     nvgRestore(NVGRenderer.getContext());
                 });
             } else {
                renderTask.run();
            }
        });

        if (!isBloom) {
            hasCachedThisFrame = false;
            if (mc.player != null) {
                lastYaw = mc.player.getYaw();
            }
        }
    }

    @Override
    public boolean isActive() {
        return !mc.getDebugHud().shouldShowDebugHud() && this.settings.isEnabled();
    }

    @Override
    public boolean isBloom() {
        return this.settings.isBloom();
    }
}
