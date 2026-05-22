package wtf.uitems.client.renderer.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.util.Identifier;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.mixin.GameRendererAccessor;
import wtf.uitems.utility.render.FramebufferUtility;

import static wtf.uitems.client.Constants.mc;

public final class ShaderFramebuffer {

    private static Framebuffer blurFramebuffer, glowFramebuffer;
    // Bloom only needs a soft silhouette, so we keep its working buffer at half resolution.
    private static final float GLOW_RENDER_SCALE = 0.5F;

    private static final Identifier BLUR_IDENTIFIER = Identifier.ofVanilla("blur");
    public static final CustomUniform CUSTOM_UNIFORM = new CustomUniform();

    private static PostEffectProcessor postEffectProcessor;

    /** Frame counter for throttling blur shader invocations */
    private static int blurFrameCounter = 0;
    /** Forces blur to rebuild on the next frame (e.g. after resize or screen open) */
    private static boolean blurDirty = true;

    public static void markBlurDirty() {
        blurDirty = true;
    }

    public static void applyBlurToFullScreen() {
        if (blurFramebuffer == null) return;

        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);

        if (!overlayModule.isBlur()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(blurFramebuffer.getColorAttachment(), 0, blurFramebuffer.getDepthAttachment(), 1.0);
            blurDirty = true;
            return;
        }

        blurFrameCounter++;
        final int throttleFrames = getThrottleFrames();
        // Only re-run the blur shader every few frames, or when forced dirty
        if (!blurDirty && (blurFrameCounter % throttleFrames != 0)) {
            return; // Reuse cached blurFramebuffer from last computation
        }
        blurDirty = false;

        final Framebuffer mainBuffer = mc.getFramebuffer();
        FramebufferUtility.blit(mainBuffer, blurFramebuffer);
        renderBlurToFramebuffer(blurFramebuffer, overlayModule.getBlurRadius());
    }

    public static void applyGlowToNVGObjects() {
        if (glowFramebuffer == null) return;

        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);

        if (!overlayModule.isBloom()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(glowFramebuffer.getColorAttachment(), 0, glowFramebuffer.getDepthAttachment(), 1.0);
            return;
        }

        renderBlurToFramebuffer(glowFramebuffer, scaleGlowRadius(overlayModule.getBloomRadius()));
    }

    public static void applyFixedBlurToFullScreen(final int radius) {
        if (blurFramebuffer == null) return;
        final Framebuffer mainBuffer = mc.getFramebuffer();
        FramebufferUtility.blit(mainBuffer, blurFramebuffer);
        renderBlurToFramebuffer(blurFramebuffer, radius);
        blurDirty = true; // Force rebuild when returning from fixed-blur mode
    }

    public static void applyFixedGlowToNVGObjects(final int radius) {
        if (glowFramebuffer == null) return;
        renderBlurToFramebuffer(glowFramebuffer, scaleGlowRadius(radius));
    }

    /** Clears the blur buffer without running the shader — used when blur is disabled. */
    public static void clearBlurBuffer() {
        if (blurFramebuffer == null) return;
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                blurFramebuffer.getColorAttachment(), 0,
                blurFramebuffer.getDepthAttachment(), 1.0
        );
        blurDirty = true;
    }

    public static void resetProcessor() {
        postEffectProcessor = null;
    }

    private static void renderBlurToFramebuffer(final Framebuffer framebuffer, final int radius) {
        if(mc.getOverlay() instanceof SplashOverlay splashOverlay) {
            // TODO: hacky fix, but should work KEKW
            resetProcessor();
            return;
        }

        if (postEffectProcessor == null) {
            postEffectProcessor = mc.getShaderLoader().loadPostEffect(
                    BLUR_IDENTIFIER, DefaultFramebufferSet.MAIN_ONLY
            );
        } else {
            final FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
            final PostEffectProcessor.FramebufferSet framebufferSet = PostEffectProcessor.FramebufferSet.singleton(
                    Identifier.ofVanilla("main"), frameGraphBuilder.createObjectNode("main", framebuffer)
            );
            CUSTOM_UNIFORM.use(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), radius, () -> {
                try {
                    postEffectProcessor.render(frameGraphBuilder, framebuffer.textureWidth, framebuffer.textureHeight, framebufferSet);
                    frameGraphBuilder.run(((GameRendererAccessor) mc.gameRenderer).getPool());
                } catch (IllegalStateException ex) {
                    postEffectProcessor = null;
                }
            });
        }
    }

    public static void onResized(final int width, final int height) {
        if (blurFramebuffer != null)
            blurFramebuffer.delete();
        if (glowFramebuffer != null)
            glowFramebuffer.delete();

        blurFramebuffer = new WindowFramebuffer(width, height);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(blurFramebuffer.getColorAttachment(), 0, blurFramebuffer.getDepthAttachment(), 1.0);

        final int glowWidth = getGlowFramebufferWidth(width);
        final int glowHeight = getGlowFramebufferHeight(height);

        glowFramebuffer = new WindowFramebuffer(glowWidth, glowHeight);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(glowFramebuffer.getColorAttachment(), 0, glowFramebuffer.getDepthAttachment(), 1.0);

        NVGRenderer.createNVGPaintFromTex(width, height, Integer.parseInt(blurFramebuffer.getColorAttachment().getLabel()), NVGRenderer.BLUR_PAINT);
        NVGRenderer.createNVGPaintFromTex(glowWidth, glowHeight, Integer.parseInt(glowFramebuffer.getColorAttachment().getLabel()), NVGRenderer.GLOW_PAINT);

        blurDirty = true; // Force re-render after resize
        blurFrameCounter = 0;
    }

    public static Framebuffer getGlowFramebuffer() {
        return glowFramebuffer;
    }

    public static Framebuffer getBlurFramebuffer() {
        return blurFramebuffer;
    }

    public static float getGlowRenderScale() {
        return GLOW_RENDER_SCALE;
    }

    private static int getThrottleFrames() {
        final long pixels = (long) mc.getWindow().getFramebufferWidth() * mc.getWindow().getFramebufferHeight();
        if (pixels >= 7_000_000L) {
            return 4;
        }
        if (pixels >= 3_500_000L) {
            return 3;
        }
        return 2;
    }

    private static int getGlowFramebufferWidth(final int width) {
        return Math.max(1, Math.round(width * GLOW_RENDER_SCALE));
    }

    private static int getGlowFramebufferHeight(final int height) {
        return Math.max(1, Math.round(height * GLOW_RENDER_SCALE));
    }

    private static int scaleGlowRadius(final int radius) {
        return Math.max(1, Math.round(radius * GLOW_RENDER_SCALE));
    }
}

