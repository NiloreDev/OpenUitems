package wtf.uitems.client.feature.module.impl.visual.overlay.impl.chathud;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.IOverlayElement;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.mixin.ChatHudAccessor;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.misc.chat.ChatUsernameDecorator;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

import java.util.ArrayList;
import java.util.List;

import static wtf.uitems.client.Constants.mc;

public final class ChatHudElement implements IOverlayElement {

    private static final NVGTextRenderer FONT = FontRepository.getFont("Mifont");
    private static final float FONT_SIZE = 8F;
    private static final float LINE_HEIGHT = 11.2F;
    private static final float WIDTH_SAFETY_PADDING = 4F;
    private static final int FADE_IN_TICKS = 8;
    private static final int HOLD_TICKS = 150;
    private static final int FADE_OUT_TICKS = 18;
    private static final int MAX_VISIBLE_AGE = HOLD_TICKS + FADE_OUT_TICKS;

    private final ChatHudSettings settings;
    private final HUDModule overlay;
    private final Animation panelAlphaAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180);
    private final Animation panelWidthAnimation = new Animation(Easing.EASE_OUT_CUBIC, 220);
    private final Animation panelHeightAnimation = new Animation(Easing.EASE_OUT_CUBIC, 220);
    private final Animation panelYAnimation = new Animation(Easing.EASE_OUT_CUBIC, 220);
    private float lastPanelW;
    private float lastPanelH;
    private float lastPanelY;

    public ChatHudElement(final HUDModule module) {
        this.overlay = module;
        this.settings = new ChatHudSettings(module);
    }

    @Override
    public void render(final DrawContext context, final float delta, final boolean isBloom) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (mc.currentScreen instanceof ChatScreen) {
            return;
        }

        final var chatHud = mc.inGameHud.getChatHud();
        final ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        final List<ChatHudLine.Visible> visibleMessages = accessor.getVisibleMessages();
        if (visibleMessages.isEmpty()) {
            return;
        }

        final Pair<Integer, Integer> theme = ColorUtility.getClientTheme();
        final boolean focused = mc.currentScreen instanceof ChatScreen;
        final int currentTick = mc.inGameHud.getTicks();
        final int from = Math.min(accessor.getScrolledLines(), Math.max(0, visibleMessages.size() - 1));
        final int limit = focused ? 16 : 10;

        final float x = 3.5F;
        final float bottomY = mc.getWindow().getScaledHeight() - 24F;

        final List<LineData> rows = new ArrayList<>(limit);
        float panelAlpha = 0F;

        int rendered = 0;
        for (int i = from; i < visibleMessages.size() && rendered < limit; i++) {
            final ChatHudLine.Visible line = visibleMessages.get(i);
            final int age = currentTick - line.addedTime();
            if (!focused && age > MAX_VISIBLE_AGE) {
                continue;
            }

            final float alpha;
            if (focused) {
                alpha = 1F;
            } else if (age < FADE_IN_TICKS) {
                alpha = Math.max(0F, Math.min(1F, age / (float) FADE_IN_TICKS));
            } else if (age <= HOLD_TICKS) {
                alpha = 1F;
            } else {
                alpha = Math.max(0F, Math.min(1F, 1F - (age - HOLD_TICKS) / (float) FADE_OUT_TICKS));
            }

            if (alpha <= 0.005F) {
                continue;
            }

            final var decorated = ChatUsernameDecorator.appendSocketUsernames(line.content(), alpha);
            final List<TextSegment> segments = this.toSegments(decorated, alpha);
            if (segments.isEmpty()) {
                continue;
            }

            float rowWidth = 0F;
            for (final TextSegment segment : segments) {
                rowWidth += FONT.getStringWidth(segment.text, FONT_SIZE);
            }

            rows.add(new LineData(segments, alpha, rowWidth));
            panelAlpha = Math.max(panelAlpha, alpha);

            rendered++;
        }

        final float panelPaddingX = 4.5F;
        final float panelPaddingTop = 3.5F;
        final float panelPaddingBottom = 3.0F;
        final float radius = 4.2F;

        if (!rows.isEmpty()) {
            float maxTextWidth = 0F;
            for (final LineData row : rows) {
                maxTextWidth = Math.max(maxTextWidth, row.width);
            }

            lastPanelW = maxTextWidth + panelPaddingX * 2F + WIDTH_SAFETY_PADDING;
            lastPanelH = rows.size() * LINE_HEIGHT + panelPaddingTop + panelPaddingBottom;
            lastPanelY = bottomY - (rows.size() - 1) * LINE_HEIGHT - panelPaddingTop - 7.6F;
        }

        panelAlphaAnimation.run(rows.isEmpty() ? 0F : panelAlpha);
        if (lastPanelW > panelWidthAnimation.getValue()) {
            panelWidthAnimation.setValue(lastPanelW);
            panelWidthAnimation.setStartValue(lastPanelW);
        }
        panelWidthAnimation.run(lastPanelW);
        panelHeightAnimation.run(lastPanelH);
        panelYAnimation.run(lastPanelY);

        final float animatedPanelAlpha = Math.max(0F, Math.min(1F, panelAlphaAnimation.getValue()));
        if (animatedPanelAlpha <= 0.01F) {
            return;
        }

        final float smoothPanelW = Math.max(0F, panelWidthAnimation.getValue());
        final float smoothPanelH = Math.max(0F, panelHeightAnimation.getValue());
        final float smoothPanelY = panelYAnimation.getValue();

        final float drawW = smoothPanelW;
        final float drawH = smoothPanelH;
        final float drawX = x;
        final float drawY = smoothPanelY;

        NVGRenderer.globalAlpha(animatedPanelAlpha);

        if (!isBloom && overlay.isBlur()) {
            NVGRenderer.roundedRect(drawX, drawY, drawW, drawH, radius, NVGRenderer.BLUR_PAINT);
        }

        if (!isBloom && settings.isGlow()) {
            final long now = System.currentTimeMillis();
            final float t = (float) ((now % 4000L) / 4000.0);
            final float angle = t * 360F;
            final int a1 = (int) (90 + 50 * Math.sin(t * Math.PI * 2));
            final int a2 = (int) (70 + 60 * Math.sin((t + 0.25F) * Math.PI * 2));
            final int c1 = ColorUtility.applyOpacity(theme.first, Math.max(0, Math.min(255, (int) (a1 * animatedPanelAlpha))));
            final int c2 = ColorUtility.applyOpacity(theme.second, Math.max(0, Math.min(255, (int) (a2 * animatedPanelAlpha))));
            NVGRenderer.roundedRectGradient(drawX, drawY, drawW, drawH, radius, c1, c2, angle);
            NVGRenderer.roundedRectGradient(drawX, drawY, drawW, drawH, radius, c2, c1, angle + 110F);
        }

        if (isBloom) {
            NVGRenderer.roundedRect(drawX, drawY, drawW, drawH, radius, ColorUtility.applyOpacity(0xFFFFFFFF, 0.28F * animatedPanelAlpha));
        } else {
            NVGRenderer.roundedRect(drawX, drawY, drawW, drawH, radius, ColorUtility.applyOpacity(0x70090909, 0.62F * animatedPanelAlpha));
            overlay.renderBlurTextureOverlay(drawX, drawY, drawW, drawH, radius);
        }

        NVGRenderer.globalAlpha(1F);

        NVGRenderer.scissor(drawX, drawY, drawW, drawH, () -> {
            for (int i = 0; i < rows.size(); i++) {
                final LineData row = rows.get(i);
                final float y = bottomY - i * LINE_HEIGHT;

                float cursorX = x + panelPaddingX;
                for (final TextSegment segment : row.segments) {
                    if (isBloom) {
                        FONT.drawString(segment.text, cursorX, y, FONT_SIZE, segment.color);
                    } else {
                        FONT.drawStringWithShadow(segment.text, cursorX, y, FONT_SIZE, segment.color);
                    }
                    cursorX += FONT.getStringWidth(segment.text, FONT_SIZE);
                }
            }
        });
    }

    private List<TextSegment> toSegments(final net.minecraft.text.OrderedText text, final float alpha) {
        final List<TextSegment> segments = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        final int[] currentColor = {ColorUtility.applyOpacity(-1, alpha)};

        text.accept((index, style, codePoint) -> {
            int rgb = 0xFFFFFF;
            if (style != null && style.getColor() != null) {
                rgb = style.getColor().getRgb() & 0xFFFFFF;
            }
            final int color = ColorUtility.applyOpacity(0xFF000000 | rgb, alpha);

            if (color != currentColor[0] && builder.length() > 0) {
                segments.add(new TextSegment(builder.toString(), currentColor[0]));
                builder.setLength(0);
            }

            currentColor[0] = color;
            builder.appendCodePoint(codePoint);
            return true;
        });

        if (builder.length() > 0) {
            segments.add(new TextSegment(builder.toString(), currentColor[0]));
        }

        return segments;
    }

    private static final class LineData {
        private final List<TextSegment> segments;
        private final float alpha;
        private final float width;

        private LineData(final List<TextSegment> segments, final float alpha, final float width) {
            this.segments = segments;
            this.alpha = alpha;
            this.width = width;
        }
    }

    private static final class TextSegment {
        private final String text;
        private final int color;

        private TextSegment(final String text, final int color) {
            this.text = text;
            this.color = color;
        }
    }

    @Override
    public boolean isActive() {
        return settings.isEnabled();
    }

    @Override
    public boolean isBloom() {
        return overlay.isBloom();
    }
}
