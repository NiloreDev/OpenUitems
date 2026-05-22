package wtf.uitems.client.renderer.overlay;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.Colors;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.shader.ShaderFramebuffer;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.client.screen.click.AbstractClickGui;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.render.RenderBloomEvent;
import wtf.uitems.event.impl.render.RenderScreenEvent;
import wtf.uitems.utility.render.ColorUtility;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import static wtf.uitems.client.Constants.mc;

public class ClientInGameOverlay {
    @Setter @Getter
    private static float sbRectX, sbRectY, sbRectWidth, sbRectHeight;
    public static void resetScoreboardRectDimensions() {
        sbRectX = 0;
        sbRectY = 0;
        sbRectWidth = 0;
        sbRectHeight = 0;
    }

    public static void render(final float tickDelta, DrawContext context) {
        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        if (!overlayModule.hasOverlayContent()) {
            return;
        }
        final boolean clickGuiOpen = mc.currentScreen instanceof AbstractClickGui;
        final boolean shouldDrawGlow = overlayModule.hasBloomContent() || clickGuiOpen;

        final Window window = mc.getWindow();

        final int scaledWidth = window.getScaledWidth();
        final int scaledHeight = window.getScaledHeight();

        final double mouseX = mc.mouse.getX() * ((double) scaledWidth / window.getWidth());
        final double mouseY = mc.mouse.getY() * ((double) scaledHeight / window.getHeight());

        NVGRenderer.beginFrame();
        if (shouldDrawGlow) {
            NVGRenderer.rect(0, 0, scaledWidth, scaledHeight, NVGRenderer.GLOW_PAINT);
        }
        renderScoreboardRect(false);
        EventDispatcher.dispatch(new RenderScreenEvent(context, tickDelta, mouseX, mouseY));
        NVGRenderer.endFrameAndReset(true);
    }

    public static void renderClickGui(DrawContext context, float tickDelta) {
        Screen screen = mc.currentScreen;
        if (screen instanceof AbstractClickGui clickGui) {
            final Window window = mc.getWindow();
            final int scaledWidth = window.getScaledWidth();
            final int scaledHeight = window.getScaledHeight();
            final double mouseX = mc.mouse.getX() * ((double) scaledWidth / window.getWidth());
            final double mouseY = mc.mouse.getY() * ((double) scaledHeight / window.getHeight());
            clickGui.doRender(
                    context,
                    (int) mouseX,
                    (int) mouseY,
                    tickDelta
            );
        }
    }

    public static void applyPostProcessing(final DrawContext context, final float tickDelta) {
        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        final boolean clickGuiOpen = mc.currentScreen instanceof AbstractClickGui;
        final boolean hasOverlayContent = overlayModule.hasOverlayContent();
        final boolean hasBloomContent = overlayModule.hasBloomContent();

        // If nothing is visible and no click GUI is open, skip the entire post-processing pipeline
        if (!hasOverlayContent && !clickGuiOpen) {
            NVGTextRenderer.blockTextRendering = false;
            return;
        }

        NVGTextRenderer.blockTextRendering = true;

        if (clickGuiOpen) {
            ShaderFramebuffer.applyFixedBlurToFullScreen(24);
        } else if (overlayModule.isBlur()) {
            // Only run blur shader if blur is actually enabled
            ShaderFramebuffer.applyBlurToFullScreen();
        } else {
            // Blur disabled: just clear the buffer so no stale data shows
            ShaderFramebuffer.clearBlurBuffer();
        }

        if (hasBloomContent || clickGuiOpen) {
            NVGTextRenderer.blockTextRendering = false; // Allow text rendering for bloom pass
            final Window window = mc.getWindow();
            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "uitems/bloom", ShaderFramebuffer.getGlowFramebuffer().getColorAttachmentView(), OptionalInt.empty(), ShaderFramebuffer.getGlowFramebuffer().useDepthAttachment ? ShaderFramebuffer.getGlowFramebuffer().getDepthAttachmentView() : null, OptionalDouble.empty())) {
                renderPass.setPipeline(RenderPipelines.GUI);

                final Framebuffer glowFramebuffer = ShaderFramebuffer.getGlowFramebuffer();
                final float bloomScaleFactor = (float) window.getScaleFactor() * ShaderFramebuffer.getGlowRenderScale();
                if (glowFramebuffer != null) {
                    NVGRenderer.beginFrame(glowFramebuffer.textureWidth, glowFramebuffer.textureHeight, bloomScaleFactor);
                } else {
                    NVGRenderer.beginFrame();
                }
                renderScoreboardRect(true);
                EventDispatcher.dispatch(new RenderBloomEvent(context, tickDelta));
                NVGRenderer.endFrameAndReset(false);
            }
            NVGTextRenderer.blockTextRendering = true; // Block again for any remaining post-processing
            if (hasBloomContent) {
                ShaderFramebuffer.applyGlowToNVGObjects();
            } else if (clickGuiOpen) {
                ShaderFramebuffer.applyFixedGlowToNVGObjects(10);
            }
        }

        NVGTextRenderer.blockTextRendering = false;
    }

    public static void renderScoreboardRect(final boolean bloom) {
        if (sbRectWidth == 0 || sbRectHeight == 0) {
            return;
        }
        if (bloom) {
            NVGRenderer.roundedRect(sbRectX, sbRectY, sbRectWidth, sbRectHeight, 1.5F, ColorUtility.applyOpacity(Colors.BLACK, 0.75F));
        } else {
            NVGRenderer.roundedRect(sbRectX, sbRectY, sbRectWidth, sbRectHeight, 1.5F, NVGRenderer.BLUR_PAINT);
            NVGRenderer.roundedRect(sbRectX, sbRectY, sbRectWidth, sbRectHeight, 1.5F, mc.options.getTextBackgroundColor(0.4F));
        }
    }
}

