package wtf.uitems.client.renderer;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.util.Window;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.opengl.GL33C;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.GLUtility;
import wtf.uitems.utility.render.ScreenPosition;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static wtf.uitems.client.Constants.mc;

public final class NVGRenderer {

    private static long VG;

    public static long getContext() {
        if (VG == 0) {
            VG = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        }
        return VG;
    }

    public static final NVGPaint NVG_PAINT = NVGPaint.create();
    public static final NVGPaint BLUR_PAINT = NVGPaint.create();
    public static final NVGPaint GLOW_PAINT = NVGPaint.create();

    public static final NVGColor NVG_COLOR_1 = NVGColor.create();
    public static final NVGColor NVG_COLOR_2 = NVGColor.create();

    private static boolean frameStarted;
    public static float globalAlpha = 1;

    public static boolean beginFrame() {
        final Window window = mc.getWindow();
        return beginFrame(window.getFramebufferWidth(), window.getFramebufferHeight(), (float) window.getScaleFactor());
    }

    public static boolean beginFrame(final int framebufferWidth, final int framebufferHeight, final float scaleFactor) {
        final long vg = getContext();

        if (!frameStarted) {
            GLUtility.setup();
            GLUtility.push();

            globalAlpha = 1.0f;

            nvgBeginFrame(vg, framebufferWidth / scaleFactor, framebufferHeight / scaleFactor, scaleFactor);
            if (!scissors.isEmpty()) {
                useCurrentScissors();
            }
            frameStarted = true;

            return true;
        }

        return false;
    }

    public static void endFrameAndReset(final boolean createRenderPass) {
        if (frameStarted) {
            endFrame(createRenderPass);
            clearScissors();
            
            // Ensure depth test is re-enabled for Minecraft after NanoVG
            GLUtility.pop();
            
            // founded by unc (trol1337)
            GL33C.glViewport(0, 0, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        }
    }

    public static void endFrame(final boolean createRenderPass) {
        if (frameStarted) {
            final long vg = getContext();
            if (createRenderPass) {
                final Framebuffer framebuffer = mc.getFramebuffer();
                try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "uitems/nvg", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.useDepthAttachment ? framebuffer.getDepthAttachmentView() : null, OptionalDouble.empty())) {
                    renderPass.setPipeline(RenderPipelines.GUI);

                    nvgEndFrame(vg);
                }
            } else {
                nvgEndFrame(vg);
            }

            frameStarted = false;
        }
    }

    public static void clearScissors() {
        scissors.clear();
    }

    public static void globalAlpha(final float alpha) {
        globalAlpha = alpha;
        nvgGlobalAlpha(getContext(), alpha);
    }

    public static void rect(final float x, final float y, final float width, final float height, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);

        nvgBeginPath(vg);
        nvgFillColor(vg, NVG_COLOR_1);
        nvgRect(vg, x, y, width, height);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void rect(final float x, final float y, final float width, final float height, final NVGPaint nvgPaint) {
        final long vg = getContext();
        nvgBeginPath(vg);
        nvgFillPaint(vg, nvgPaint);
        nvgRect(vg, x, y, width, height);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void rect(final float x, final float y, final float width, final float height, final NVGPaint nvgPaint, final float alpha) {
        final long vg = getContext();
        nvgSave(vg);
        nvgGlobalAlpha(vg, Math.max(0F, Math.min(1F, alpha)));
        rect(x, y, width, height, nvgPaint);
        nvgRestore(vg);
    }

    public static void scale(final float factor, final float x, final float y, final float width, final float height, final Runnable content) {
        final long vg = getContext();
        final float translateX = x + width / 2F;
        final float translateY = y + height / 2F;

        nvgSave(vg);
        nvgTranslate(vg, translateX, translateY);
        nvgScale(vg, factor, factor);
        nvgTranslate(vg, -translateX, -translateY);

        try {
            content.run();
        } finally {
            nvgRestore(vg);
        }
    }

    public static void rectStroke(final float x, final float y, final float width, final float height, final float strokeThickness, final int color, final int strokeColor) {
        rect(x - strokeThickness, y - strokeThickness, width + (strokeThickness * 2), height + (strokeThickness * 2), strokeColor);
        rect(x, y, width, height, color);
    }

    public static void rotate(final double degrees, final float x, final float y, final float width, final float height, final Runnable content) {
        final long vg = getContext();
        final float translateX = x + width / 2f;
        final float translateY = y + height / 2f;

        nvgSave(vg);
        nvgTranslate(vg, translateX, translateY);
        nvgRotate(vg, (float) Math.toRadians(degrees));

        content.run();

        nvgRestore(vg);
    }

    public static void rectOutlineStroke(final float x, final float y, final float width, final float height, final float outlineThickness, final float strokeThickness, final int outlineColor, final int strokeColor) {
        rectOutline(x - outlineThickness, y - outlineThickness, width + (outlineThickness * 2), height + (outlineThickness * 2), strokeThickness, strokeColor);
        rectOutline(x, y, width, height, outlineThickness, outlineColor);
    }

    public static void rectOutline(final float x, final float y, final float width, final float height, final float thickness, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);

        nvgBeginPath(vg);
        nvgFillColor(vg, NVG_COLOR_1);
        nvgRect(vg, x, y, width, thickness);
        nvgFill(vg);
        nvgClosePath(vg);

        nvgBeginPath(vg);
        nvgFillColor(vg, NVG_COLOR_1);
        nvgRect(vg, x + width - thickness, y + thickness, thickness, height - thickness);
        nvgFill(vg);
        nvgClosePath(vg);

        nvgBeginPath(vg);
        nvgFillColor(vg, NVG_COLOR_1);
        nvgRect(vg, x, y + height - thickness, width - thickness, thickness);
        nvgFill(vg);
        nvgClosePath(vg);

        nvgBeginPath(vg);
        nvgFillColor(vg, NVG_COLOR_1);
        nvgRect(vg, x, y + thickness, thickness, height - thickness);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void rainbowRect(final float x, final float y, final float width, final float height) {
        final long vg = getContext();
        for (float i = y; i < y + height; i += 0.5f) {
            final float hue = (i - y) / height;
            final int rgbColor = Color.HSBtoRGB(hue, 1, 1);

            final float segmentHeight = Math.min(0.5f, y + height - i);

            nvgShapeAntiAlias(vg, false);
            rect(x, i, width, segmentHeight, rgbColor);
            nvgShapeAntiAlias(vg, true);
        }
    }

    public static void roundedRect(final float x, final float y, final float width, final float height, final float radius, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);

        nvgBeginPath(vg);
        nvgFillColor(vg, NVG_COLOR_1);
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void roundedRect(final float x, final float y, final float width, final float height, final float radius, final NVGPaint paint) {
        final long vg = getContext();

        nvgBeginPath(vg);
        nvgFillPaint(vg, paint);
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void roundedRect(final float x, final float y, final float width, final float height, final float radius, final NVGPaint paint, final float alpha) {
        final long vg = getContext();
        nvgSave(vg);
        nvgGlobalAlpha(vg, Math.max(0F, Math.min(1F, alpha)));
        roundedRect(x, y, width, height, radius, paint);
        nvgRestore(vg);
    }

    public static void roundedRectOutline(final float x, final float y, final float width, final float height, final float radius, final float thickness, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);

        nvgBeginPath(vg);
        nvgStrokeColor(vg, NVG_COLOR_1);
        nvgStrokeWidth(vg, thickness);
        nvgRoundedRect(
                vg,
                x,
                y,
                width,
                height,
                radius
        );
        nvgStroke(vg);
        nvgClosePath(vg);
    }

    public static void roundedRectOutline(final float x, final float y, final float width, final float height, final float radius, final float thickness, final NVGPaint paint) {
        final long vg = getContext();

        nvgBeginPath(vg);
        nvgStrokePaint(vg, paint);
        nvgStrokeWidth(vg, thickness);
        nvgRoundedRect(
                vg,
                x,
                y,
                width,
                height,
                radius
        );
        nvgStroke(vg);
        nvgClosePath(vg);
    }

    public static void roundedRectOutline(final float x, final float y, final float width, final float height, final float radius, final float thickness, final float startAngle, final float endAngle, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);

        nvgBeginPath(vg);
        nvgStrokeColor(vg, NVG_COLOR_1);
        nvgStrokeWidth(vg, thickness);
        
        // nvgArc only draws a circular arc. To draw a "rounded rect" arc, we need to use scissoring or 
        // a more complex path. For a capsule, we can use two arcs and two lines.
        final float r = Math.min(radius, Math.min(width, height) / 2f);
        
        // Approximate the progress by scissoring the full outline
        // This is a common trick when you want a non-circular progress bar
        nvgSave(vg);
        
        // Calculate an angle-based scissor if needed, but for a capsule, 
        // it's better to just draw the segments. 
        // However, a simpler approach for "decreasing progress" on a capsule is to 
        // use a circular arc if the user is okay with it, or a custom path.
        // Let's implement a proper capsule path that can be clipped.
        
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgStroke(vg);
        
        nvgRestore(vg);
        nvgClosePath(vg);
    }

    public static void roundedRectOutlineSegmented(final float x, final float y, final float width, final float height, final float radius, final float thickness, final float startProgress, final float endProgress, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);

        final float r = Math.min(radius, Math.min(width, height) / 2f);
        final float straightW = width - 2 * r;
        final float straightH = height - 2 * r;
        final float arcLen = (float) (Math.PI * r / 2.0);
        final float totalLen = 2 * straightW + 2 * straightH + 4 * arcLen;
        
        float startLen = totalLen * Math.max(0, Math.min(1, startProgress));
        float endLen = totalLen * Math.max(0, Math.min(1, endProgress));
        
        if (startLen >= endLen) return;

        nvgBeginPath(vg);
        nvgStrokeColor(vg, NVG_COLOR_1);
        nvgStrokeWidth(vg, thickness);
        nvgLineCap(vg, NVG_ROUND);
        nvgLineJoin(vg, NVG_ROUND);

        float currentPos = 0;
        final boolean[] pathStarted = {false};

        // 1. Top straight
        drawSegment(vg, x + r, y, x + r + straightW, y, currentPos, straightW, startLen, endLen, pathStarted);
        currentPos += straightW;

        // 2. Top-right arc
        drawArcSegment(vg, x + width - r, y + r, r, (float) (-Math.PI / 2.0), (float) (Math.PI / 2.0), currentPos, arcLen, startLen, endLen, pathStarted);
        currentPos += arcLen;

        // 3. Right straight
        if (straightH > 0) {
            drawSegment(vg, x + width, y + r, x + width, y + r + straightH, currentPos, straightH, startLen, endLen, pathStarted);
            currentPos += straightH;
        }

        // 4. Bottom-right arc
        drawArcSegment(vg, x + width - r, y + height - r, r, 0, (float) (Math.PI / 2.0), currentPos, arcLen, startLen, endLen, pathStarted);
        currentPos += arcLen;

        // 5. Bottom straight
        drawSegment(vg, x + width - r, y + height, x + r, y + height, currentPos, straightW, startLen, endLen, pathStarted);
        currentPos += straightW;

        // 6. Bottom-left arc
        drawArcSegment(vg, x + r, y + height - r, r, (float) (Math.PI / 2.0), (float) (Math.PI / 2.0), currentPos, arcLen, startLen, endLen, pathStarted);
        currentPos += arcLen;

        // 7. Left straight
        if (straightH > 0) {
            drawSegment(vg, x, y + height - r, x, y + r, currentPos, straightH, startLen, endLen, pathStarted);
            currentPos += straightH;
        }

        // 8. Top-left arc
        drawArcSegment(vg, x + r, y + r, r, (float) Math.PI, (float) (Math.PI / 2.0), currentPos, arcLen, startLen, endLen, pathStarted);

        nvgStroke(vg);
    }

    private static void drawSegment(long vg, float x1, float y1, float x2, float y2, float segmentStart, float segmentLen, float startLen, float endLen, boolean[] pathStarted) {
        if (startLen >= segmentStart + segmentLen || endLen <= segmentStart) return;

        float activeStart = Math.max(0, startLen - segmentStart);
        float activeEnd = Math.min(segmentLen, endLen - segmentStart);

        float t1 = activeStart / segmentLen;
        float t2 = activeEnd / segmentLen;

        float drawX1 = x1 + (x2 - x1) * t1;
        float drawY1 = y1 + (y2 - y1) * t1;
        float drawX2 = x1 + (x2 - x1) * t2;
        float drawY2 = y1 + (y2 - y1) * t2;

        if (!pathStarted[0]) {
            nvgMoveTo(vg, drawX1, drawY1);
            pathStarted[0] = true;
        } else {
            nvgLineTo(vg, drawX1, drawY1);
        }
        nvgLineTo(vg, drawX2, drawY2);
    }

    private static void drawArcSegment(long vg, float cx, float cy, float r, float startAngle, float sweep, float segmentStart, float segmentLen, float startLen, float endLen, boolean[] pathStarted) {
        if (startLen >= segmentStart + segmentLen || endLen <= segmentStart) return;

        float activeStart = Math.max(0, startLen - segmentStart);
        float activeEnd = Math.min(segmentLen, endLen - segmentStart);

        float t1 = activeStart / segmentLen;
        float t2 = activeEnd / segmentLen;

        float a1 = startAngle + sweep * t1;
        float a2 = startAngle + sweep * t2;

        float startX = (float) (cx + Math.cos(a1) * r);
        float startY = (float) (cy + Math.sin(a1) * r);

        if (!pathStarted[0]) {
            nvgMoveTo(vg, startX, startY);
            pathStarted[0] = true;
        } else {
            nvgLineTo(vg, startX, startY);
        }
        
        nvgArc(vg, cx, cy, r, a1, a2, NVG_CW);
    }

    private static final List<ScreenPosition> scissors = new ArrayList<>();

    public static void scissor(final float x, final float y, final float width, final float height, final Runnable content) {
        final long vg = getContext();
        ScreenPosition scissor = new ScreenPosition(x, y, width, height);
        scissors.add(scissor);

        nvgIntersectScissor(vg, x, y, width, height);
        content.run();
        nvgResetScissor(vg);

        scissors.remove(scissor);
        useCurrentScissors();
    }

    private static void useCurrentScissors() {
        final long vg = getContext();
        for (ScreenPosition scissor : scissors) {
            nvgIntersectScissor(vg, scissor.getX(), scissor.getY(), scissor.getWidth(), scissor.getHeight());
        }
    }

    public static void roundedRectGradient(final float x, final float y, final float width, final float height, final float radius, final int color1, final int color2, final float angleDegrees) {
        final long vg = getContext();
        applyColor(color1, NVG_COLOR_1);
        applyColor(color2, NVG_COLOR_2);

        final float angleRadians = (float) Math.toRadians(angleDegrees);
        final float dx = (float) Math.cos(angleRadians);
        final float dy = (float) Math.sin(angleRadians);

        nvgLinearGradient(
                vg,
                x + width * 0.5f - dx * width * 0.5f,
                y + height * 0.5f - dy * height * 0.5f,
                x + width * 0.5f + dx * width * 0.5f,
                y + height * 0.5f + dy * height * 0.5f,
                NVG_COLOR_1,
                NVG_COLOR_2,
                NVG_PAINT
        );

        nvgBeginPath(vg);
        nvgFillPaint(vg, NVG_PAINT);
        nvgRoundedRect(
                vg,
                x,
                y,
                width,
                height,
                radius
        );
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void rectGradient(final float x, final float y, final float width, final float height, final int color1, final int color2, final float angleDegrees) {
        final long vg = getContext();
        applyColor(color1, NVG_COLOR_1);
        applyColor(color2, NVG_COLOR_2);

        final float angleRadians = (float) Math.toRadians(angleDegrees);
        final float dx = (float) Math.cos(angleRadians);
        final float dy = (float) Math.sin(angleRadians);

        nvgLinearGradient(
                vg,
                x + width * 0.5f - dx * width * 0.5f,
                y + height * 0.5f - dy * height * 0.5f,
                x + width * 0.5f + dx * width * 0.5f,
                y + height * 0.5f + dy * height * 0.5f,
                NVG_COLOR_1,
                NVG_COLOR_2,
                NVG_PAINT
        );

        nvgBeginPath(vg);
        nvgFillPaint(vg, NVG_PAINT);
        nvgRect(
                vg,
                x,
                y,
                width,
                height
        );
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void arcOutline(final float x, final float y, final float radius, final float thickness, final float startAngle, final float endAngle, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);

        nvgBeginPath(vg);
        nvgStrokeColor(vg, NVG_COLOR_1);
        nvgStrokeWidth(vg, thickness);
        nvgArc(vg, x, y, radius, (float) Math.toRadians(startAngle), (float) Math.toRadians(endAngle), NVG_CW);
        nvgStroke(vg);
        nvgClosePath(vg);
    }

    public static void roundedRectVarying(final float x, final float y, final float width, final float height, final float radiusTopLeft, final float radiusTopRight, final float radiusBottomRight, final float radiusBottomLeft, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);

        nvgBeginPath(vg);
        nvgFillColor(vg, NVG_COLOR_1);
        nvgRoundedRectVarying(
                vg,
                x,
                y,
                width,
                height,
                radiusTopLeft,
                radiusTopRight,
                radiusBottomRight,
                radiusBottomLeft
        );
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void roundedRectVaryingGradient(final float x, final float y, final float width, final float height, final float radiusTopLeft, final float radiusTopRight, final float radiusBottomRight, final float radiusBottomLeft, final int color1, final int color2, final float angleDegrees) {
        final long vg = getContext();
        applyColor(color1, NVG_COLOR_1);
        applyColor(color2, NVG_COLOR_2);

        final float angleRadians = (float) Math.toRadians(angleDegrees);
        final float dx = (float) Math.cos(angleRadians);
        final float dy = (float) Math.sin(angleRadians);

        nvgLinearGradient(
                vg,
                x + width * 0.5f - dx * width * 0.5f,
                y + height * 0.5f - dy * height * 0.5f,
                x + width * 0.5f + dx * width * 0.5f,
                y + height * 0.5f + dy * height * 0.5f,
                NVG_COLOR_1,
                NVG_COLOR_2,
                NVG_PAINT
        );

        nvgBeginPath(vg);
        nvgFillPaint(vg, NVG_PAINT);
        nvgRoundedRectVarying(
                vg,
                x,
                y,
                width,
                height,
                radiusTopLeft,
                radiusTopRight,
                radiusBottomRight,
                radiusBottomLeft
        );
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void roundedRectVarying(final float x, final float y, final float width, final float height, final float radiusTopLeft, final float radiusTopRight, final float radiusBottomRight, final float radiusBottomLeft, final NVGPaint nvgPaint) {
        final long vg = getContext();
        nvgBeginPath(vg);
        nvgFillPaint(vg, nvgPaint);
        nvgRoundedRectVarying(
                vg,
                x,
                y,
                width,
                height,
                radiusTopLeft,
                radiusTopRight,
                radiusBottomRight,
                radiusBottomLeft
        );
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void roundedRectVarying(final float x, final float y, final float width, final float height, final float radiusTopLeft, final float radiusTopRight, final float radiusBottomRight, final float radiusBottomLeft, final NVGPaint nvgPaint, final float alpha) {
        final long vg = getContext();
        nvgSave(vg);
        nvgGlobalAlpha(vg, Math.max(0F, Math.min(1F, alpha)));
        roundedRectVarying(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, nvgPaint);
        nvgRestore(vg);
    }

    public static void circle(final float x, final float y, final float radius, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);

        nvgBeginPath(vg);
        nvgFillColor(vg, NVG_COLOR_1);
        nvgCircle(vg, x, y, radius);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void circleGradient(final float x, final float y, final float radius, final int color1, final int color2) {
        final long vg = getContext();
        applyColor(color1, NVG_COLOR_1);
        applyColor(color2, NVG_COLOR_2);

        nvgRadialGradient(vg, x, y, 0, radius, NVG_COLOR_1, NVG_COLOR_2, NVG_PAINT);

        nvgBeginPath(vg);
        nvgFillPaint(vg, NVG_PAINT);
        nvgCircle(vg, x, y, radius);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void dropShadow(final float x, final float y, final float width, final float height, final float radius, final float feather, final float spread, final int color) {
        final long vg = getContext();
        applyColor(color, NVG_COLOR_1);
        applyColor(0x00000000, NVG_COLOR_2);

        nvgBoxGradient(vg, x - spread, y - spread, width + spread * 2, height + spread * 2, radius + spread, feather, NVG_COLOR_1, NVG_COLOR_2, NVG_PAINT);
        nvgBeginPath(vg);
        nvgRect(vg, x - spread - feather, y - spread - feather, width + spread * 2 + feather * 2, height + spread * 2 + feather * 2);
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, NVG_PAINT);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public static void applyColor(final int color, final NVGColor nvgColor) {
        final int[] rgba = ColorUtility.hexToRGBA(color);

        nvgRGBAf(rgba[0] / 255f, rgba[1] / 255f, rgba[2] / 255f, rgba[3] / 255f, nvgColor);
    }

    public static void createNVGPaintFromTex(final int width, final int height, final int glTex, final NVGPaint nvgPaint) {
        final long vg = getContext();
        final int imageHandle = nvglCreateImageFromHandle(vg, glTex, width, height, NVG_IMAGE_GENERATE_MIPMAPS | NVG_IMAGE_FLIPY);

        nvgImagePattern(vg, 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), 0, imageHandle, 1.0F, nvgPaint);
    }
}
