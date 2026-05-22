package wtf.uitems.client.renderer.image;

import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.utility.misc.system.IOUtility;

import net.minecraft.client.texture.NativeImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.nanovg.NanoVG.*;
import static wtf.uitems.client.renderer.NVGRenderer.getContext;


public final class NVGImageRenderer {

    private final ByteBuffer imageData;
    private int imageHandle = -1;
    private final int flags;
    private final boolean valid;
    private final boolean useRgbaData;
    private final int width, height;
    private final ByteBuffer rgbaData;

    public NVGImageRenderer(final InputStream inputStream, final int flags) {
        this.imageData = IOUtility.ioResourceToByteBuffer(inputStream, 512 * 1024);
        this.flags = flags;
        this.valid = true;
        this.useRgbaData = false;
        this.width = 0;
        this.height = 0;
        this.rgbaData = null;
    }

    public NVGImageRenderer() {
        this.imageData = null;
        this.imageHandle = 0;
        this.flags = 0;
        this.valid = false;
        this.useRgbaData = false;
        this.width = 0;
        this.height = 0;
        this.rgbaData = null;
    }

    public NVGImageRenderer(final InputStream inputStream) {
        this(inputStream, 0);
    }

    public static NVGImageRenderer fromNativeImage(final NativeImage img, final boolean whiteMask) {
        if (img == null) {
            return new NVGImageRenderer();
        }
        final int w = img.getWidth();
        final int h = img.getHeight();
        final ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int argb = img.getColorArgb(x, y);
                final int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = (argb) & 0xFF;
                if (whiteMask) {
                    r = 255;
                    g = 255;
                    b = 255;
                }
                buffer.put((byte) r);
                buffer.put((byte) g);
                buffer.put((byte) b);
                buffer.put((byte) a);
            }
        }
        buffer.flip();
        return new NVGImageRenderer(buffer, w, h, NVG_IMAGE_GENERATE_MIPMAPS);
    }

    public static NVGImageRenderer fromRgbaData(final ByteBuffer rgbaData, final int width, final int height, final int flags) {
        if (rgbaData == null || width <= 0 || height <= 0) {
            return new NVGImageRenderer();
        }

        final ByteBuffer source = rgbaData.duplicate();
        source.rewind();

        final ByteBuffer copy = ByteBuffer.allocateDirect(source.remaining());
        copy.put(source);
        copy.flip();
        return new NVGImageRenderer(copy, width, height, flags);
    }

    private NVGImageRenderer(final ByteBuffer rgbaData, final int width, final int height, final int flags) {
        this.imageData = null;
        this.flags = flags;
        this.valid = true;
        this.useRgbaData = true;
        this.rgbaData = rgbaData;
        this.width = width;
        this.height = height;
    }

    private void ensureImageCreated() {
        if (valid && imageHandle == -1) {
            long vg = getContext();
            if (vg != 0) {
                if (useRgbaData) {
                    imageHandle = nvgCreateImageRGBA(vg, width, height, flags, rgbaData);
                } else {
                    imageHandle = nvgCreateImageMem(vg, flags, this.imageData);
                }
            }
        }
    }

    public void drawImage(final float x, final float y, final float width, final float height) {
        if (!valid) return;
        ensureImageCreated();
        long vg = getContext();
        nvgImagePattern(
                vg,
                x,
                y,
                width,
                height,
                0,
                imageHandle,
                1,
                NVGRenderer.NVG_PAINT
        );

        nvgBeginPath(vg);
        nvgRect(
                vg,
                x,
                y,
                width,
                height
        );
        nvgImagePattern(
                vg,
                x,
                y,
                width,
                height,
                0,
                imageHandle,
                1,
                NVGRenderer.NVG_PAINT
        );
        nvgFillPaint(vg, NVGRenderer.NVG_PAINT);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public void drawImage(final float x, final float y, final float width, final float height, final int colorOverlay) {
        if (!valid) return;
        ensureImageCreated();
        long vg = getContext();
        nvgImagePattern(
                vg,
                x,
                y,
                width,
                height,
                0,
                imageHandle,
                1,
                NVGRenderer.NVG_PAINT
        );

        nvgBeginPath(vg);
        nvgRect(
                vg,
                x,
                y,
                width,
                height
        );
        nvgImagePattern(
                vg,
                x,
                y,
                width,
                height,
                0,
                imageHandle,
                1,
                NVGRenderer.NVG_PAINT
        );
        NVGRenderer.applyColor(colorOverlay, NVGRenderer.NVG_COLOR_1);
        NVGRenderer.NVG_PAINT.innerColor(NVGRenderer.NVG_COLOR_1);

        nvgFillPaint(vg, NVGRenderer.NVG_PAINT);
        nvgFill(vg);
        nvgClosePath(vg);
    }

    public void drawRoundedRect(final float x, final float y, final float width, final float height, final float radius) {
        drawRoundedRect(x, y, width, height, radius, 1F);
    }

    public void drawRoundedRect(final float x, final float y, final float width, final float height, final float radius, final float alpha) {
        if (!valid) return;
        ensureImageCreated();
        long vg = getContext();
        nvgImagePattern(
                vg,
                x,
                y,
                width,
                height,
                0,
                imageHandle,
                Math.max(0F, Math.min(1F, alpha)),
                NVGRenderer.NVG_PAINT
        );

        nvgBeginPath(vg);
        nvgRoundedRect(
                vg,
                x,
                y,
                width,
                height,
                radius
        );
        nvgFillPaint(vg, NVGRenderer.NVG_PAINT);
        nvgFill(vg);
        nvgClosePath(vg);
    }

}
