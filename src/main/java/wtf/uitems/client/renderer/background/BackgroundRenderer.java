package wtf.uitems.client.renderer.background;

import net.minecraft.client.gui.DrawContext;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.utility.render.ColorUtility;

import java.util.ArrayList;
import java.util.List;

import static wtf.uitems.client.Constants.mc;

public final class BackgroundRenderer {
    private static final List<Particle> particles = new ArrayList<>();
    private static long lastTime = System.currentTimeMillis();
    private static boolean initialized = false;
    private static int lastThemeColor1 = 0;
    private static int lastThemeColor2 = 0;

    public static void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!initialized) {
            initParticles();
            initialized = true;
        }

        long currentTime = System.currentTimeMillis();
        float dt = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;

        final int windowWidth = context.getScaledWindowWidth();
        final int windowHeight = context.getScaledWindowHeight();

        // Update particle colors immediately if theme changed
        final com.ibm.icu.impl.Pair<Integer, Integer> themeColors = ColorUtility.getClientTheme();
        if (themeColors.first != lastThemeColor1 || themeColors.second != lastThemeColor2) {
            lastThemeColor1 = themeColors.first;
            lastThemeColor2 = themeColors.second;
            int[] colors = {lastThemeColor1, lastThemeColor2, 0xFFFFFFFF, lastThemeColor1, lastThemeColor2};
            for (int i = 0; i < particles.size(); i++) {
                Particle p = particles.get(i);
                p.color = colors[(int) (Math.random() * colors.length)];
            }
        }

        // Start NanoVG
        if (!NVGRenderer.beginFrame()) return;

        // Draw Background (Full Screen)
        NVGRenderer.rect(0, 0, windowWidth, windowHeight, 0xFF000000); // Base black
        
        // Draw Animated Particles
        for (Particle p : particles) {
            p.update(dt, windowWidth, windowHeight);
            p.render(1.0f);
        }

        // Draw overlay gradient to blend particles
        NVGRenderer.rectGradient(0, 0, windowWidth, windowHeight, 
            ColorUtility.applyOpacity(0x00000000, 0.0f), 
            ColorUtility.applyOpacity(0xFF000000, 0.3f), 0);

        NVGRenderer.endFrameAndReset(true);
    }

    private static void initParticles() {
        particles.clear();
        int particleCount = 20;
        com.ibm.icu.impl.Pair<Integer, Integer> themeColors = ColorUtility.getClientTheme();
        lastThemeColor1 = themeColors.first;
        lastThemeColor2 = themeColors.second;
        int[] colors = {themeColors.first, themeColors.second, 0xFFFFFFFF, themeColors.first, themeColors.second};
        for (int i = 0; i < particleCount; i++) {
            particles.add(new Particle(
                (float) (Math.random() * mc.getWindow().getScaledWidth()),
                (float) (Math.random() * mc.getWindow().getScaledHeight()),
                colors[(int) (Math.random() * colors.length)]
            ));
        }
    }

    private static class Particle {
        float x, y;
        float vx, vy;
        float radius;
        int color;
        float[] lastX = new float[10];
        float[] lastY = new float[10];
        int trailIndex = 0;

        Particle(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (float) (Math.random() * 40 - 20);
            this.vy = (float) (Math.random() * 40 - 20);
            this.radius = (float) (Math.random() * 20 + 10);
            for (int i = 0; i < lastX.length; i++) {
                lastX[i] = x;
                lastY[i] = y;
            }
        }

        void update(float dt, int width, int height) {
            lastX[trailIndex] = x;
            lastY[trailIndex] = y;
            trailIndex = (trailIndex + 1) % lastX.length;

            x += vx * dt;
            y += vy * dt;

            if (x < -radius) x = width + radius;
            if (x > width + radius) x = -radius;
            if (y < -radius) y = height + radius;
            if (y > height + radius) y = -radius;
        }

        void render(float alpha) {
            for (int i = 0; i < lastX.length; i++) {
                int idx = (trailIndex + i) % lastX.length;
                float trailAlpha = (i / (float) lastX.length) * 0.15f * alpha;
                NVGRenderer.circle(lastX[idx], lastY[idx], radius * (0.5f + 0.5f * i / lastX.length), ColorUtility.applyOpacity(color, trailAlpha));
            }
            NVGRenderer.circleGradient(x, y, radius * 2.5f, ColorUtility.applyOpacity(color, 0.15f * alpha), ColorUtility.applyOpacity(color, 0.0f));
            NVGRenderer.circleGradient(x, y, radius, ColorUtility.applyOpacity(color, 0.4f * alpha), ColorUtility.applyOpacity(color, 0.1f * alpha));
        }
    }
}
