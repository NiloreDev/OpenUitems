package wtf.uitems.client.screen.auth;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nanovg.NanoVG;
import wtf.uitems.client.ReleaseInfo;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.background.BackgroundRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.utility.misc.Multithreading;
import wtf.uitems.utility.misc.math.MathUtility;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.security.SessionUtility;

import java.util.function.Supplier;

import static wtf.uitems.client.Constants.mc;

public final class OpalMicrosoftTokenScreen extends Screen {
    private static final NVGTextRenderer TITLE_FONT = FontRepository.getFont("productsans-bold");
    private static final NVGTextRenderer TEXT_FONT = FontRepository.getFont("productsans-medium");
    private static final NVGTextRenderer SMALL_FONT = FontRepository.getFont("productsans-regular");

    private static final int BG_COLOR = 0xEE111214;
    private static final int FIELD_BG = 0xFF1E1F22;
    private static final int BUTTON_BG = 0xFF222327;

    private final Screen parentScreen;

    private boolean refreshMode;
    private boolean connecting;
    private boolean queuedReturn;
    private int activeField;
    private float animationProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();
    private long successAt;

    private String accessToken = "";
    private String clientId = "";
    private String refreshToken = "";
    private String statusMessage = "Paste a token.";
    private int statusColor = ColorUtility.MUTED_COLOR;

    public OpalMicrosoftTokenScreen(final Screen parentScreen) {
        super(Text.of(ReleaseInfo.NAME + " Microsoft Login"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        this.activeField = 0;
        this.connecting = false;
        this.queuedReturn = false;
        this.successAt = 0L;
        this.animationProgress = 0.0f;
        this.lastTime = System.currentTimeMillis();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Use our own background pass.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        final long now = System.currentTimeMillis();
        final float dt = (now - lastTime) / 1000.0f;
        lastTime = now;

        animationProgress = MathUtility.interpolate(animationProgress, 1.0f, dt * 4.0f);

        BackgroundRenderer.render(context, mouseX, mouseY, delta);
        if (!NVGRenderer.beginFrame()) {
            NVGRenderer.endFrameAndReset(true);
            if (!NVGRenderer.beginFrame()) {
                return;
            }
        }

        try {
            if (queuedReturn && now - successAt > 700L) {
                mc.setScreen(parentScreen);
                return;
            }

            final int windowWidth = context.getScaledWindowWidth();
            final int windowHeight = context.getScaledWindowHeight();
            final float panelWidth = 420.0f;
            final float panelHeight = 320.0f;
            final float panelX = (windowWidth - panelWidth) / 2.0f;
            final float panelY = (windowHeight - panelHeight) / 2.0f + (1.0f - animationProgress) * 18.0f;
            final float alpha = animationProgress;

            final com.ibm.icu.impl.Pair<Integer, Integer> themeColors = ColorUtility.getClientTheme();
            final int themeColor = themeColors.first;

            NVGRenderer.roundedRect(panelX - 3.0f, panelY - 3.0f, panelWidth + 6.0f, panelHeight + 6.0f, 14.0f,
                    ColorUtility.applyOpacity(0x000000, 0.35f * alpha));
            NVGRenderer.roundedRect(panelX, panelY, panelWidth, panelHeight, 12.0f, ColorUtility.applyOpacity(BG_COLOR, alpha));
            NVGRenderer.roundedRectOutline(panelX, panelY, panelWidth, panelHeight, 12.0f, 1.0f,
                    ColorUtility.applyOpacity(themeColor, 0.2f * alpha));

            final float centerX = panelX + panelWidth / 2.0f;
            final String title = "Microsoft Token Login";
            final float titleWidth = TITLE_FONT.getStringWidth(title, 20);
            TITLE_FONT.drawGradientStringWithShadow(
                    title,
                    centerX - titleWidth / 2.0f,
                    panelY + 30.0f,
                    20.0f,
                    ColorUtility.applyOpacity(themeColors.first, alpha),
                    ColorUtility.applyOpacity(themeColors.second, alpha)
            );

            SMALL_FONT.drawString(
                    "Paste a token to switch the current session.",
                    centerX,
                    panelY + 54.0f,
                    11.0f,
                    ColorUtility.applyOpacity(0xFF9A9A9A, alpha),
                    false,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE
            );

            renderModeButton(
                    "Token",
                    !refreshMode,
                    panelX + 24.0f,
                    panelY + 80.0f,
                    174.0f,
                    28.0f,
                    mouseX,
                    mouseY,
                    dt,
                    alpha
            );
            renderModeButton(
                    "Refresh Token",
                    refreshMode,
                    panelX + 222.0f,
                    panelY + 80.0f,
                    174.0f,
                    28.0f,
                    mouseX,
                    mouseY,
                    dt,
                    alpha
            );

            if (!refreshMode) {
                renderInputField(
                        "Token",
                        accessToken,
                        "Paste token",
                        panelX + 24.0f,
                        panelY + 122.0f,
                        panelWidth - 48.0f,
                        44.0f,
                        activeField == 0,
                        mouseX,
                        mouseY,
                        dt,
                        alpha
                );

                SMALL_FONT.drawString(
                        "Use Ctrl+V to paste. The client id is not required here.",
                        panelX + 24.0f,
                        panelY + 178.0f,
                        10.0f,
                        ColorUtility.applyOpacity(0xFF8F939A, alpha),
                        false,
                        NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE
                );
            } else {
                renderInputField(
                        "Client ID (optional)",
                        clientId,
                        "00000000402b5328",
                        panelX + 24.0f,
                        panelY + 122.0f,
                        panelWidth - 48.0f,
                        44.0f,
                        activeField == 0,
                        mouseX,
                        mouseY,
                        dt,
                        alpha
                );

                renderInputField(
                        "Refresh Token",
                        refreshToken,
                        "Paste refresh token",
                        panelX + 24.0f,
                        panelY + 176.0f,
                        panelWidth - 48.0f,
                        44.0f,
                        activeField == 1,
                        mouseX,
                        mouseY,
                        dt,
                        alpha
                );

                SMALL_FONT.drawString(
                        "Leave client id blank to use the default app id.",
                        panelX + 24.0f,
                        panelY + 232.0f,
                        10.0f,
                        ColorUtility.applyOpacity(0xFF8F939A, alpha),
                        false,
                        NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE
                );
            }

            renderActionButton(
                    "Login",
                    panelX + 24.0f,
                    panelY + 260.0f,
                    178.0f,
                    34.0f,
                    mouseX,
                    mouseY,
                    dt,
                    alpha,
                    connecting
            );
            renderActionButton(
                    "Back",
                    panelX + 218.0f,
                    panelY + 260.0f,
                    178.0f,
                    34.0f,
                    mouseX,
                    mouseY,
                    dt,
                    alpha,
                    false
            );

            TEXT_FONT.drawString(
                    statusMessage,
                    centerX,
                    panelY + 304.0f,
                    11.0f,
                    ColorUtility.applyOpacity(statusColor, alpha),
                    false,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE
            );
        } finally {
            NVGRenderer.endFrameAndReset(true);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        final double mouseX = click.x();
        final double mouseY = click.y();
        final int button = click.button();

        if (button == 0) {
            final float panelWidth = 420.0f;
            final float panelHeight = 320.0f;
            final float panelX = (mc.getWindow().getScaledWidth() - panelWidth) / 2.0f;
            final float panelY = (mc.getWindow().getScaledHeight() - panelHeight) / 2.0f + (1.0f - animationProgress) * 18.0f;

            if (isHovered(mouseX, mouseY, panelX + 24.0f, panelY + 80.0f, 174.0f, 28.0f)) {
                refreshMode = false;
                activeField = 0;
                statusMessage = "Paste a Microsoft access token.";
                statusColor = ColorUtility.MUTED_COLOR;
                return true;
            }

            if (isHovered(mouseX, mouseY, panelX + 222.0f, panelY + 80.0f, 174.0f, 28.0f)) {
                refreshMode = true;
                activeField = 1;
                statusMessage = "Paste your refresh token.";
                statusColor = ColorUtility.MUTED_COLOR;
                return true;
            }

            if (!refreshMode) {
                if (isHovered(mouseX, mouseY, panelX + 24.0f, panelY + 122.0f, panelWidth - 48.0f, 44.0f)) {
                    activeField = 0;
                    return true;
                }
            } else {
                if (isHovered(mouseX, mouseY, panelX + 24.0f, panelY + 122.0f, panelWidth - 48.0f, 44.0f)) {
                    activeField = 0;
                    return true;
                }

                if (isHovered(mouseX, mouseY, panelX + 24.0f, panelY + 176.0f, panelWidth - 48.0f, 44.0f)) {
                    activeField = 1;
                    return true;
                }
            }

            if (isHovered(mouseX, mouseY, panelX + 24.0f, panelY + 260.0f, 178.0f, 34.0f)) {
                handleLogin();
                return true;
            }

            if (isHovered(mouseX, mouseY, panelX + 218.0f, panelY + 260.0f, 178.0f, 34.0f)) {
                goBack();
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        final int keyCode = keyInput.key();
        final int modifiers = keyInput.modifiers();

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            goBack();
            return true;
        }

        if (connecting) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            insertClipboard();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            cycleField();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            handleLogin();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            deleteFromActiveField();
            return true;
        }

        return true;
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (connecting) {
            return true;
        }

        final char chr = (char) charInput.codepoint();
        if (Character.isISOControl(chr)) {
            return true;
        }

        appendToActiveField(chr);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void handleLogin() {
        if (connecting) {
            return;
        }

        final Supplier<SessionUtility.MicrosoftLoginResult> task;
        final String modeText;

        if (!refreshMode) {
            final String token = accessToken.trim();
            if (token.isEmpty()) {
                statusMessage = "Token is empty.";
                statusColor = 0xFFFF5050;
                return;
            }

            modeText = "token";
            statusMessage = "Logging in with token...";
            statusColor = ColorUtility.getClientTheme().first;
            task = () -> SessionUtility.loginWithToken(token);
        } else {
            final String token = refreshToken.trim();
            if (token.isEmpty()) {
                statusMessage = "Refresh token is empty.";
                statusColor = 0xFFFF5050;
                return;
            }

            final String clientIdValue = clientId.trim();
            modeText = "refresh token";
            statusMessage = "Logging in with refresh token...";
            statusColor = ColorUtility.getClientTheme().first;
            task = () -> SessionUtility.loginWithMicrosoftRefreshToken(clientIdValue, token);
        }

        connecting = true;
        final String finalModeText = modeText;
        Multithreading.runAsync(() -> {
            try {
                final SessionUtility.MicrosoftLoginResult result = task.get();
                mc.execute(() -> {
                    connecting = false;
                    if (result.success()) {
                        SessionUtility.applyMinecraftSession(result);
                        statusMessage = result.message();
                        statusColor = 0xFF50FF50;
                        if (mc.currentScreen == this && parentScreen != null) {
                            queuedReturn = true;
                            successAt = System.currentTimeMillis();
                        }
                    } else {
                        statusMessage = finalModeText.substring(0, 1).toUpperCase() + finalModeText.substring(1) + " login failed: " + result.message();
                        statusColor = 0xFFFF5050;
                    }
                });
            } catch (Exception exception) {
                final String message = exception.getMessage() == null || exception.getMessage().isBlank()
                        ? "Unknown error."
                        : exception.getMessage();
                mc.execute(() -> {
                    connecting = false;
                    statusMessage = finalModeText.substring(0, 1).toUpperCase() + finalModeText.substring(1) + " login failed: " + message;
                    statusColor = 0xFFFF5050;
                });
            }
        });
    }

    private void renderModeButton(final String text, final boolean active, final float x, final float y, final float width, final float height,
                                  final int mouseX, final int mouseY, final float dt, final float alpha) {
        final int themeColor = ColorUtility.getClientTheme().first;
        final boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);

        NVGRenderer.roundedRect(x, y, width, height, 8.0f, ColorUtility.applyOpacity(active ? themeColor : BUTTON_BG, alpha));
        final float accentAlpha = Math.min(1.0f, hovered ? 0.35f : 0.18f) * alpha;
        NVGRenderer.roundedRectGradient(
                x,
                y,
                width,
                height,
                8.0f,
                ColorUtility.applyOpacity(themeColor, accentAlpha),
                ColorUtility.applyOpacity(ColorUtility.brighter(themeColor, 0.08f), accentAlpha),
                0
        );
        NVGRenderer.roundedRectOutline(x, y, width, height, 8.0f, 1.0f, ColorUtility.applyOpacity(0x15FFFFFF, alpha));

        TEXT_FONT.drawString(
                text,
                x + width / 2.0f,
                y + height / 2.0f + 1.0f,
                12.5f,
                ColorUtility.applyOpacity(0xFFFFFFFF, alpha),
                false,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE
        );
    }

    private void renderActionButton(final String text, final float x, final float y, final float width, final float height,
                                    final int mouseX, final int mouseY, final float dt, final float alpha, final boolean disabled) {
        final int themeColor = ColorUtility.getClientTheme().first;
        final boolean hovered = !disabled && isHovered(mouseX, mouseY, x, y, width, height);
        final int fillColor = disabled ? 0xFF1A1B1E : ColorUtility.interpolateColors(BUTTON_BG, themeColor, hovered ? 0.45f : 0.0f);

        NVGRenderer.roundedRect(x, y, width, height, 8.0f, ColorUtility.applyOpacity(fillColor, alpha));
        NVGRenderer.roundedRectOutline(
                x,
                y,
                width,
                height,
                8.0f,
                1.0f,
                ColorUtility.applyOpacity(disabled ? 0x20FFFFFF : 0x15FFFFFF, alpha)
        );

        TEXT_FONT.drawString(
                disabled ? "Logging in..." : text,
                x + width / 2.0f,
                y + height / 2.0f + 1.0f,
                12.5f,
                ColorUtility.applyOpacity(disabled ? 0xFFB0B0B0 : 0xFFFFFFFF, alpha),
                false,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE
        );
    }

    private void renderInputField(final String label, final String value, final String placeholder, final float x, final float y,
                                  final float width, final float height, final boolean active, final int mouseX, final int mouseY,
                                  final float dt, final float alpha) {
        final int themeColor = ColorUtility.getClientTheme().first;
        final boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);

        NVGRenderer.roundedRect(x, y, width, height, 8.0f, ColorUtility.applyOpacity(FIELD_BG, alpha));
        NVGRenderer.roundedRectOutline(
                x,
                y,
                width,
                height,
                8.0f,
                1.0f,
                ColorUtility.applyOpacity(active || hovered ? themeColor : 0xFF353535, 0.55f * alpha)
        );

        TEXT_FONT.drawString(
                label,
                x + 12.0f,
                y + 11.0f,
                10.0f,
                ColorUtility.applyOpacity(active ? themeColor : 0xFF9A9A9A, alpha),
                false,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE
        );

        final String drawValue = value == null ? "" : value;
        final String drawText = drawValue.isEmpty() && !active ? placeholder : drawValue;
        final int drawColor = drawValue.isEmpty() ? 0xFF70757C : 0xFFFFFFFF;
        final float textSize = drawValue.isEmpty() ? 11.0f : 12.0f;

        final NVGTextRenderer renderer = TEXT_FONT;
        final float textWidth = renderer.getStringWidth(drawText, textSize);
        final float availableWidth = width - 24.0f;
        final float offsetX = Math.max(0.0f, textWidth - availableWidth);

        NVGRenderer.scissor(x + 12.0f, y + 18.0f, availableWidth, height - 22.0f, () -> {
            if (!drawText.isEmpty()) {
                renderer.drawString(
                        drawText,
                        x + 12.0f - offsetX,
                        y + 30.0f,
                        textSize,
                        ColorUtility.applyOpacity(drawColor, alpha),
                        false,
                        NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE
                );
            }

            if (!connecting && active) {
                if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
                    final float cursorX = x + 12.0f - offsetX + (drawValue.isEmpty() ? 0.0f : textWidth) + 1.0f;
                    NVGRenderer.rect(cursorX, y + 23.0f, 1.0f, 12.0f, ColorUtility.applyOpacity(themeColor, alpha));
                }
            }
        });
    }

    private void appendToActiveField(final char chr) {
        if (!refreshMode) {
            accessToken += chr;
            activeField = 0;
            return;
        }

        if (activeField == 0) {
            clientId += chr;
        } else {
            refreshToken += chr;
        }
    }

    private void deleteFromActiveField() {
        if (!refreshMode) {
            if (!accessToken.isEmpty()) {
                accessToken = accessToken.substring(0, accessToken.length() - 1);
            }
            return;
        }

        if (activeField == 0) {
            if (!clientId.isEmpty()) {
                clientId = clientId.substring(0, clientId.length() - 1);
            }
        } else if (!refreshToken.isEmpty()) {
            refreshToken = refreshToken.substring(0, refreshToken.length() - 1);
        }
    }

    private void cycleField() {
        if (!refreshMode) {
            activeField = 0;
            return;
        }

        activeField = (activeField + 1) % 2;
    }

    private void insertClipboard() {
        final String clipboard = mc.keyboard.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            return;
        }

        final String cleaned = clipboard.replace("\r", "").replace("\n", "").trim();
        if (cleaned.isEmpty()) {
            return;
        }

        if (!refreshMode) {
            accessToken += cleaned;
            activeField = 0;
            return;
        }

        if (activeField == 0) {
            clientId += cleaned;
        } else {
            refreshToken += cleaned;
        }
    }

    private void goBack() {
        if (parentScreen != null) {
            mc.setScreen(parentScreen);
        } else {
            mc.setScreen(null);
        }
    }

    private boolean isHovered(final double mouseX, final double mouseY, final float x, final float y, final float width, final float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
