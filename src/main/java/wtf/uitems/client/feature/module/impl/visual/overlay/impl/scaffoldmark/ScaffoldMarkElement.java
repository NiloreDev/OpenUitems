package wtf.uitems.client.feature.module.impl.visual.overlay.impl.scaffoldmark;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.visual.overlay.IOverlayElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.impl.world.scaffold.ScaffoldModule;
import wtf.uitems.client.renderer.MinecraftRenderer;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;
import wtf.uitems.utility.player.MoveUtility;

import static wtf.uitems.client.Constants.mc;

public final class ScaffoldMarkElement implements IOverlayElement {

    private final HUDModule module;
    private final Animation progressAnimation = new Animation(Easing.EASE_OUT_EXPO, 250);
    private final Animation openAnimation = new Animation(Easing.EASE_IN_OUT_QUART, 260);

    public ScaffoldMarkElement(HUDModule module) {
        this.module = module;
    }

    @Override
    public void render(DrawContext context, float delta, boolean isBloom) {
        final ScaffoldModule scaffold = OpalClient.getInstance().getModuleRepository().getModule(ScaffoldModule.class);
        final boolean scaffoldOn = scaffold != null && scaffold.isEnabled();
        final boolean islandOff = !module.isDynamicIslandEnabled();

        final ItemStack main = mc.player.getMainHandStack();
        final ItemStack off = mc.player.getOffHandStack();
        final boolean mainIsBlock = main.getItem() instanceof BlockItem;
        final boolean offIsBlock = off.getItem() instanceof BlockItem;
        final boolean hasBlock = mainIsBlock || offIsBlock;

        final boolean isCardMode = module.getScaffoldMarkStyle().getValue() == HUDModule.ScaffoldMarkStyle.CARD;
        final boolean shouldShow = scaffoldOn && islandOff && hasBlock && isCardMode;

        if (shouldShow) {
            openAnimation.run(1f);
            final int stackSize = (mainIsBlock ? main.getCount() : 0) + (offIsBlock ? off.getCount() : 0);
            renderCard(context, isBloom, mainIsBlock ? main : off, stackSize);
            return;
        }

        // When not show conditions, drive closing animation and keep rendering while it shrinks
        openAnimation.run(0f);
        if (isCardMode) {
            if (openAnimation.getValue() > 0.001f) {
                final int stackSize = (mainIsBlock ? main.getCount() : 0) + (offIsBlock ? off.getCount() : 0);
                renderCard(context, isBloom, mainIsBlock ? main : off, stackSize);
            }
            return; // 在卡片模式下不渲染传统描边样式，避免非方块时崩溃
        }

        if (!hasBlock) {
            return;
        }

        // Default outline style (legacy)
        final ItemStack handStack = mainIsBlock ? main : off;
        final String stackText = String.valueOf(handStack.getCount());
        final String suffixText = " blocks";
        final NVGTextRenderer font = FontRepository.getFont("productsans-medium");
        final float textSize = 8.5f;
        final float stackWidth = font.getStringWidth(stackText, textSize);
        final float suffixWidth = font.getStringWidth(suffixText, textSize);
        final float totalTextWidth = stackWidth + suffixWidth;

        final float height = 22;
        final float width = 32 + totalTextWidth + 12;
        final float x = (mc.getWindow().getScaledWidth() - width) / 2f;
        final float y = (mc.getWindow().getScaledHeight() / 2f) + 25;

        final int blockColor = ColorUtility.applyOpacity(((BlockItem) handStack.getItem()).getBlock().getDefaultMapColor().color, 255);
        
        // Progress animation calculation
        final float progress = Math.min(handStack.getCount(), 64) / 64f;
        progressAnimation.run(progress);

        final float borderThickness = 1.8f;
        // Adjust x and y by half thickness to make the outline center on the background edge
        final float outlineX = x + borderThickness / 2f;
        final float outlineY = y + borderThickness / 2f;
        final float outlineWidth = width - borderThickness;
        final float outlineHeight = height - borderThickness;
        final float outlineRadius = (height - borderThickness) / 2f;

        if (isBloom) {
            // Bloom progress bar - perfectly overlapping the background edge
            if (progressAnimation.getValue() > 0) {
                final float p = progressAnimation.getValue();
                final float progressX = x + width * p;
                final long vg = NVGRenderer.getContext();
                
                NVGRenderer.applyColor(blockColor, NVGRenderer.NVG_COLOR_1);
                NVGRenderer.applyColor(0x00000000, NVGRenderer.NVG_COLOR_2);
                
                // Use a sharp linear gradient instead of scissoring to achieve anti-aliased shortening
                org.lwjgl.nanovg.NanoVG.nvgLinearGradient(vg, progressX - 0.5f, y, progressX + 0.5f, y, NVGRenderer.NVG_COLOR_1, NVGRenderer.NVG_COLOR_2, NVGRenderer.NVG_PAINT);
                NVGRenderer.roundedRectOutline(outlineX, outlineY, outlineWidth, outlineHeight, outlineRadius, borderThickness, NVGRenderer.NVG_PAINT);
            }
        } else {
            // Normal background and progress outline
            // Oval Background (Capsule)
            NVGRenderer.roundedRect(x, y, width, height, height / 2f, 0x90000000);
            
            // Background outline (static gray capsule, perfectly overlapping)
            NVGRenderer.roundedRectOutline(outlineX, outlineY, outlineWidth, outlineHeight, outlineRadius, borderThickness, 0x30FFFFFF);
            
            // Active progress outline (perfectly overlapping and shortening)
            if (progressAnimation.getValue() > 0) {
                final float p = progressAnimation.getValue();
                final float progressX = x + width * p;
                final long vg = NVGRenderer.getContext();
                
                NVGRenderer.applyColor(blockColor, NVGRenderer.NVG_COLOR_1);
                NVGRenderer.applyColor(0x00000000, NVGRenderer.NVG_COLOR_2);
                
                // Use a sharp linear gradient instead of scissoring to achieve anti-aliased shortening
                org.lwjgl.nanovg.NanoVG.nvgLinearGradient(vg, progressX - 0.5f, y, progressX + 0.5f, y, NVGRenderer.NVG_COLOR_1, NVGRenderer.NVG_COLOR_2, NVGRenderer.NVG_PAINT);
                NVGRenderer.roundedRectOutline(outlineX, outlineY, outlineWidth, outlineHeight, outlineRadius, borderThickness, NVGRenderer.NVG_PAINT);
            }
        }

        // Icon position (left side of the capsule)
        final float iconPadding = 5;
        final float circleX = x + height / 2f + 1;
        final float circleY = y + height / 2f;

        // Draw icon in the center of the circle
        if (!isBloom) {
            final float iconSize = 12;
            final float iconX = circleX - iconSize / 2f;
            final float iconY = circleY - iconSize / 2f;
            
            MinecraftRenderer.addToQueue(() -> {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(iconX, iconY);
                context.getMatrices().scale(iconSize / 16f, iconSize / 16f); // Scale 16x16 icon to desired size
                context.drawItem(mc.player, handStack, 0, 0, 0);
                context.getMatrices().popMatrix();
            });
        }

        // Draw text
        final float textX = x + height + 2;
        final float textY = y + height / 2f + textSize / 2f - 1.5f;
        
        if (isBloom) {
            font.drawString(stackText, textX, textY, textSize, blockColor);
        } else {
            font.drawString(stackText, textX, textY, textSize, blockColor);
            font.drawString(suffixText, textX + stackWidth, textY, textSize, -1);
        }
    }

    private void renderCard(DrawContext context, boolean isBloom, ItemStack iconStack, int totalBlocks) {
        final NVGTextRenderer titleFont = FontRepository.getFont("productsans-bold");
        final NVGTextRenderer subFont = FontRepository.getFont("productsans-medium");

        final String title = "scaffold";
        final String sub = totalBlocks + " blocks left | " + MoveUtility.getBlocksPerSecond() + "m/s";
        final float titleSize = 8.0f;
        final float subSize = 6.0f;

        final float paddingX = 8f;
        final float paddingY = 7f;
        final float barHeight = 3.2f;
        final float spacing = 2.5f;

        final float open = openAnimation.getValue();
        if (open <= 0.001f) {
            return;
        }

        final float textWidth = Math.max(titleFont.getStringWidth(title, titleSize), subFont.getStringWidth(sub, subSize));
        final float width = Math.max(98, paddingX * 2 + textWidth);
        final float height = paddingY * 2 + titleSize + spacing + subSize + spacing + barHeight;
        final float x = (mc.getWindow().getScaledWidth() - width) / 2f;
        final float y = (mc.getWindow().getScaledHeight() / 2f) + 25;
        final float radius = 6.0f;

        // Progress
        final float progress = Math.min(totalBlocks, 64) / 64f;
        progressAnimation.run(progress);

        // Dynamic theme gradient（沿用灵动岛的 back-and-forth 逻辑）
        final var theme = ColorUtility.getClientTheme();
        final int c1 = ColorUtility.interpolateColorsBackAndForth(10, 0, theme.first, theme.second);
        final int c2 = ColorUtility.interpolateColorsBackAndForth(10, 120, theme.first, theme.second);

        final float titleX = x + paddingX;
        final float titleY = y + paddingY + titleSize;
        final float subX = titleX;
        final float subY = titleY + spacing + subSize;
        final float barX = x + paddingX;
        final float barY = subY + spacing + 1;
        final float barWidth = width - paddingX * 2;
        final float titleWidth = titleFont.getStringWidth(title, titleSize);
        final float titleHeight = titleFont.getStringHeight(title, titleSize);
        final long shimmerNow = System.currentTimeMillis();
        final float shimmerDuration = 1800f;
        final float shimmerGap = 700f;       // 消失间歇
        final float shimmerCycle = shimmerDuration + shimmerGap;
        final float shimmerPhase = (shimmerNow % (long) shimmerCycle) / shimmerCycle; // 0..1
        final boolean shimmerVisible = shimmerPhase < (shimmerDuration / shimmerCycle);
        final float shimmerProgress = shimmerVisible ? shimmerPhase / (shimmerDuration / shimmerCycle) : 0f; // 0..1
        final float stripeWidth = Math.max(10f, titleWidth * 0.25f);
        final float stripeCenter = (titleX - stripeWidth * 0.5f) + (titleWidth + stripeWidth) * shimmerProgress;
        final int shine1 = ColorUtility.applyOpacity(ColorUtility.brighter(c1, 0.35f), 0.95f);
        final int shine2 = ColorUtility.applyOpacity(ColorUtility.brighter(c2, 0.35f), 0.95f);

        final float scissorW = width * Math.min(1f, Math.max(0f, open));
        final float scissorX = x + width * 0.5f - scissorW * 0.5f;
        final boolean fullyOpen = open >= 0.999f;

        if (isBloom) {
            final float spread = 3.5f;
            final float bloomAlpha = 0.2f;
            if (fullyOpen) {
                NVGRenderer.roundedRectGradient(
                        x - spread, y - spread,
                        width + spread * 2, height + spread * 2,
                        radius + spread,
                        ColorUtility.applyOpacity(c1, bloomAlpha * 0.7f * open),
                        ColorUtility.applyOpacity(c2, bloomAlpha * 0.7f * open),
                        0f
                );

                titleFont.drawGradientString(title, titleX, titleY, titleSize, c1, c2);
                if (shimmerVisible) {
                    float accX = 0f;
                    for (int i = 0; i < title.length(); i++) {
                        String ch = title.substring(i, i + 1);
                        float cw = titleFont.getStringWidth(ch, titleSize);
                        float cx = titleX + accX;
                        float ccx = cx + cw * 0.5f;
                        float dist = Math.abs(ccx - stripeCenter);
                        float weight = 1f - Math.min(1f, dist / (stripeWidth * 0.5f));
                        if (weight > 0f) {
                            int cc1 = ColorUtility.applyOpacity(shine1, weight);
                            int cc2 = ColorUtility.applyOpacity(shine2, weight);
                            titleFont.drawGradientString(ch, cx, titleY, titleSize, cc1, cc2);
                        }
                        accX += cw;
                    }
                }
                if (progressAnimation.getValue() > 0) {
                    final float w = barWidth * progressAnimation.getValue();
                    NVGRenderer.roundedRectGradient(barX, barY, w, barHeight, barHeight / 2f, c1, c2, 0f);
                }
            } else {
                NVGRenderer.scissor(scissorX, y, scissorW, height, () -> {
                    NVGRenderer.roundedRectGradient(
                            x - spread, y - spread,
                            width + spread * 2, height + spread * 2,
                            radius + spread,
                            ColorUtility.applyOpacity(c1, bloomAlpha * 0.7f * open),
                            ColorUtility.applyOpacity(c2, bloomAlpha * 0.7f * open),
                            0f
                    );

                    titleFont.drawGradientString(title, titleX, titleY, titleSize, c1, c2);
                    if (shimmerVisible) {
                        float accX = 0f;
                        for (int i = 0; i < title.length(); i++) {
                            String ch = title.substring(i, i + 1);
                            float cw = titleFont.getStringWidth(ch, titleSize);
                            float cx = titleX + accX;
                            float ccx = cx + cw * 0.5f;
                            float dist = Math.abs(ccx - stripeCenter);
                            float weight = 1f - Math.min(1f, dist / (stripeWidth * 0.5f));
                            if (weight > 0f) {
                                int cc1 = ColorUtility.applyOpacity(shine1, weight);
                                int cc2 = ColorUtility.applyOpacity(shine2, weight);
                                titleFont.drawGradientString(ch, cx, titleY, titleSize, cc1, cc2);
                            }
                            accX += cw;
                        }
                    }
                    if (progressAnimation.getValue() > 0) {
                        final float w = barWidth * progressAnimation.getValue();
                        NVGRenderer.roundedRectGradient(barX, barY, w, barHeight, barHeight / 2f, c1, c2, 0f);
                    }
                });
            }
        } else {
            // Use exact geometry at full open to match legacy appearance
            NVGRenderer.roundedRect(x, y, width, height, radius, NVGRenderer.BLUR_PAINT, module.getBlurSampleOpacity());
            if (fullyOpen) {
                NVGRenderer.roundedRect(x, y, width, height, radius, module.getBlurBackgroundColor());
                module.renderBlurTextureOverlay(x, y, width, height, radius);

                titleFont.drawString(title, titleX, titleY, titleSize, -1);
                if (shimmerVisible) {
                    float accX = 0f;
                    for (int i = 0; i < title.length(); i++) {
                        String ch = title.substring(i, i + 1);
                        float cw = titleFont.getStringWidth(ch, titleSize);
                        float cx = titleX + accX;
                        float ccx = cx + cw * 0.5f;
                        float dist = Math.abs(ccx - stripeCenter);
                        float weight = 1f - Math.min(1f, dist / (stripeWidth * 0.5f));
                        if (weight > 0f) {
                            int cc1 = ColorUtility.applyOpacity(shine1, weight);
                            int cc2 = ColorUtility.applyOpacity(shine2, weight);
                            titleFont.drawGradientString(ch, cx, titleY, titleSize, cc1, cc2);
                        }
                        accX += cw;
                    }
                }
                subFont.drawString(sub, subX, subY, subSize, ColorUtility.applyOpacity(-1, 0.8f));

                NVGRenderer.roundedRect(barX, barY, barWidth, barHeight, barHeight / 2f, ColorUtility.applyOpacity(-1, 0.15f * open));
                if (progressAnimation.getValue() > 0) {
                    final float w = barWidth * progressAnimation.getValue();
                    NVGRenderer.roundedRectGradient(barX, barY, w, barHeight, barHeight / 2f, c1, c2, 0f);
                }
            } else {
                NVGRenderer.scissor(scissorX, y, scissorW, height, () -> {
                    NVGRenderer.roundedRect(x, y, width, height, radius, module.getBlurBackgroundColor());
                    module.renderBlurTextureOverlay(x, y, width, height, radius);

                    titleFont.drawString(title, titleX, titleY, titleSize, -1);
                    if (shimmerVisible) {
                        float accX = 0f;
                        for (int i = 0; i < title.length(); i++) {
                            String ch = title.substring(i, i + 1);
                            float cw = titleFont.getStringWidth(ch, titleSize);
                            float cx = titleX + accX;
                            float ccx = cx + cw * 0.5f;
                            float dist = Math.abs(ccx - stripeCenter);
                            float weight = 1f - Math.min(1f, dist / (stripeWidth * 0.5f));
                            if (weight > 0f) {
                                int cc1 = ColorUtility.applyOpacity(shine1, weight);
                                int cc2 = ColorUtility.applyOpacity(shine2, weight);
                                titleFont.drawGradientString(ch, cx, titleY, titleSize, cc1, cc2);
                            }
                            accX += cw;
                        }
                    }
                    subFont.drawString(sub, subX, subY, subSize, ColorUtility.applyOpacity(-1, 0.8f));

                    NVGRenderer.roundedRect(barX, barY, barWidth, barHeight, barHeight / 2f, ColorUtility.applyOpacity(-1, 0.15f * open));
                    if (progressAnimation.getValue() > 0) {
                        final float w = barWidth * progressAnimation.getValue();
                        NVGRenderer.roundedRectGradient(barX, barY, w, barHeight, barHeight / 2f, c1, c2, 0f);
                    }
                });
            }
        }
    }

    @Override
    public void onDisable() {
        progressAnimation.setValue(0);
        openAnimation.setValue(0);
    }

    @Override
    public boolean isActive() {
        if (mc.player == null || mc.world == null) return false;
        final ScaffoldModule scaffold = OpalClient.getInstance().getModuleRepository().getModule(ScaffoldModule.class);
        final boolean show = scaffold != null && scaffold.isEnabled() && !module.isDynamicIslandEnabled();
        if (!show) {
            openAnimation.run(0f);
            return openAnimation.getValue() > 0.001f;
        }
        return true;
    }

    @Override
    public boolean isBloom() {
        return true;
    }
}
