package wtf.uitems.client.screen.auth;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nanovg.NanoVG;
import wtf.uitems.client.ReleaseInfo;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.client.verify.VerifyManager;
import wtf.uitems.utility.data.SaveUtility;
import wtf.uitems.utility.misc.math.MathUtility;
import wtf.uitems.utility.render.ColorUtility;

import static wtf.uitems.client.Constants.mc;

public final class OpalVerifyScreen extends Screen {
    private static final NVGTextRenderer FONT = FontRepository.getFont("inter-regular");
    private static final NVGTextRenderer BOLD_FONT = FontRepository.getFont("inter-bold");
    private static final NVGTextRenderer MI_FONT = FontRepository.getFont("Mifont");
    
    private static final int BG_COLOR = 0xEE111214;
    private static final int FIELD_BG = 0xFF1E1F22;
    
    private String username = "";
    private String password = "";
    private String cardKey = "";
    private String statusMessage = "Welcome to " + ReleaseInfo.NAME + " Client";
    private int statusColor = ColorUtility.MUTED_COLOR;
    
    private boolean isRegisterMode = false;
    private boolean rememberMe = true;
    private int activeField = 0; // 0: username, 1: password, 2: cardKey
    
    private long lastTime = System.currentTimeMillis();
    private float animationProgress = 0.0f;
    private float panelHeightProgress = 0.0f;
    private final float[] fieldHoverProgress = new float[3];
    private float buttonHoverProgress = 0.0f;
    private float checkboxHoverProgress = 0.0f;
    private boolean connecting = false;
    private long connectStartAt = 0L;
    
    public OpalVerifyScreen() {
        super(Text.of(ReleaseInfo.NAME + " Verification"));
    }

    @Override
    protected void init() {
        activeField = 0;
        animationProgress = 0.0f;
        panelHeightProgress = isRegisterMode ? 1.0f : 0.0f;
        lastTime = System.currentTimeMillis();
        connecting = false;
        connectStartAt = 0L;

        final var creds = SaveUtility.loadCredentials();
        if (creds != null) {
            username = creds.username();
            password = creds.password();
        }

        VerifyManager.getInstance().setCallback(result -> {
            mc.execute(() -> {
                statusMessage = result.message();
                statusColor = result.success() ? 0xFF50FF50 : 0xFFFF5050;
                connecting = false;
                
                if (result.success()) {
                    if (result.type() == VerifyManager.VerifyResult.Type.LOGIN) {
                        if (rememberMe) {
                            SaveUtility.saveCredentials(username, password);
                        } else {
                            SaveUtility.deleteCredentials();
                        }
                        // Avoid leaving title state with a null screen (can appear as black screen on some clients).
                        if (mc.world != null) {
                            mc.setScreen(null);
                        } else {
                            mc.setScreen(new TitleScreen());
                        }
                    } else if (result.type() == VerifyManager.VerifyResult.Type.REGISTER) {
                        cardKey = "";
                        isRegisterMode = false;
                        activeField = 0;

                        VerifyManager.getInstance().reconnect();
                        
                        statusMessage = "Registration successful! Please login.";
                    } else if (result.type() == VerifyManager.VerifyResult.Type.RECHARGE) {
                        cardKey = "";
                        isRegisterMode = true;
                        activeField = 0;
                        statusMessage = "Recharge successful!";
                    }
                }
            });
        });
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        final int windowWidth = context.getScaledWindowWidth();
        final int windowHeight = context.getScaledWindowHeight();
        final float targetScale = (mc.getWindow().getWidth() == 3840 && mc.getWindow().getHeight() == 2160) ? 4.0f : 2.0f;
        final float currentScale = (float) mc.getWindow().getScaleFactor();
        final float uiScale = targetScale / currentScale;
        final float cx = windowWidth / 2f;
        final float cy = windowHeight / 2f;
        final int scaledMouseX = (int) (cx + (mouseX - cx) / uiScale);
        final int scaledMouseY = (int) (cy + (mouseY - cy) / uiScale);

        final boolean frameStarted = NVGRenderer.beginFrame();
        if (!frameStarted) {
            NVGRenderer.endFrameAndReset(true);
            if (!NVGRenderer.beginFrame()) return;
        }

        org.lwjgl.opengl.GL11.glDepthMask(false);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;

        if (connecting && now - connectStartAt > 10000L) {
            statusMessage = "连接超时，正在重新连接...";
            statusColor = ColorUtility.getClientTheme().first;
            VerifyManager.getInstance().reconnect();
                if (isRegisterMode) {
                    if (!username.isEmpty() && !password.isEmpty() && !cardKey.isEmpty()) {
                        VerifyManager.getInstance().registerOrRecharge(username, password, cardKey);
                    }
                } else {
                if (!username.isEmpty() && !password.isEmpty()) {
                    VerifyManager.getInstance().login(username, password);
                }
            }
            connectStartAt = now;
        }

        if (animationProgress < 1.0f) {
            animationProgress = Math.min(1.0f, animationProgress + dt * 2.5f);
        }

        float targetHeightProgress = isRegisterMode ? 1.0f : 0.0f;
        if (Math.abs(panelHeightProgress - targetHeightProgress) > 0.001f) {
            panelHeightProgress = MathUtility.interpolate(panelHeightProgress, targetHeightProgress, dt * 8.0f);
        } else {
            panelHeightProgress = targetHeightProgress;
        }

        try {
            if (mc.world == null) {
                NVGRenderer.rect(0, 0, windowWidth, windowHeight, 0xFF000000);
                NVGRenderer.rect(0, 0, windowWidth, windowHeight, 0xFF101114);
            }

            NVGRenderer.scale(uiScale, 0, 0, windowWidth, windowHeight, () -> {
                float baseWidth = 280;
                float baseHeight = 350;
                float extraHeight = 60 * panelHeightProgress;
                float width = baseWidth;
                float height = baseHeight + extraHeight;
                
                float x = (windowWidth - width) / 2f;
                float y = (windowHeight - height) / 2f;

                float alpha = animationProgress;
                float animatedY = y + (1.0f - alpha) * 20;

                final int themeColor = ColorUtility.getClientTheme().first;

                NVGRenderer.roundedRect(x - 3, animatedY - 3, width + 6, height + 6, 14, ColorUtility.applyOpacity(0x000000, 0.35f * alpha));

                NVGRenderer.roundedRect(x, animatedY, width, height, 10, ColorUtility.applyOpacity(BG_COLOR, alpha));
                NVGRenderer.roundedRectOutline(x, animatedY, width, height, 10, 1.0f, ColorUtility.applyOpacity(themeColor, 0.2f * alpha));

                String headerText = panelHeightProgress > 0.5f ? "Create Account" : "Welcome Back";
                NVGTextRenderer headerFont = containsChinese(headerText) ? MI_FONT : BOLD_FONT;
                headerFont.drawGradientString(headerText, 
                        x + width / 2 - headerFont.getStringWidth(headerText, 20) / 2, 
                        animatedY + 40, 20, ColorUtility.applyOpacity(themeColor, alpha), ColorUtility.applyOpacity(0xFFFFFFFF, alpha), true);
                
                drawStringWithAutoFont(panelHeightProgress > 0.5f ? "Join Uitems community" : "Login to your account", 
                        x + width / 2, animatedY + 70, 12, ColorUtility.applyOpacity(0xFF909090, alpha), false, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

                float fieldX = x + 30;
                float fieldWidth = width - 60;
                
                renderInputField(fieldX, animatedY + 100, fieldWidth, 42, "Username", username, 0, scaledMouseX, scaledMouseY, dt, alpha);
                renderInputField(fieldX, animatedY + 160, fieldWidth, 42, "Password", password.replaceAll(".", "•"), 1, scaledMouseX, scaledMouseY, dt, alpha);
                
                float cardKeyAlpha = MathUtility.interpolate(0.0f, 1.0f, Math.max(0, (panelHeightProgress - 0.5f) / 0.5f)) * alpha;
                if (cardKeyAlpha > 0.01f) {
                    renderInputField(fieldX, animatedY + 220, fieldWidth, 42, "Card Key", cardKey, 2, scaledMouseX, scaledMouseY, dt, cardKeyAlpha);
                }

                float buttonY = animatedY + 235 + 55 * panelHeightProgress;

                float rememberMeAlpha = MathUtility.interpolate(0.0f, 1.0f, Math.max(0, (0.4f - panelHeightProgress) / 0.4f)) * alpha;
                if (rememberMeAlpha > 0.01f) {
                    renderCheckbox(fieldX, buttonY - 22, "Remember Me", rememberMe, scaledMouseX, scaledMouseY, dt, rememberMeAlpha);
                }
                
                renderButton(fieldX, buttonY, fieldWidth, 42, panelHeightProgress > 0.5f ? "REGISTER" : "LOGIN", scaledMouseX, scaledMouseY, dt, alpha);

                float switchY = buttonY + 65;
                boolean switchHovered = isHovered(scaledMouseX, scaledMouseY, fieldX, switchY, fieldWidth, 18);
                drawStringWithAutoFont(panelHeightProgress > 0.5f ? "Already have an account? Login" : "No account? Register", 
                        x + width / 2, switchY + 8, 12, ColorUtility.applyOpacity(switchHovered ? 0xFFFFFFFF : 0xFFBBBBBB, alpha), false, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

                if (!statusMessage.isEmpty()) {
                    drawStringWithAutoFont(statusMessage, x + width / 2, animatedY + height - 20, 11, ColorUtility.applyOpacity(statusColor, alpha), false, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (frameStarted) {
                NVGRenderer.endFrameAndReset(true);
            }
        }
    }
    private void renderCheckbox(float x, float y, String label, boolean checked, int mouseX, int mouseY, float dt, float alpha) {
        final int themeColor = ColorUtility.getClientTheme().first;
        boolean hovered = isHovered(mouseX, mouseY, x, y, 100, 14);
        checkboxHoverProgress = MathUtility.interpolate(checkboxHoverProgress, hovered ? 1.0f : 0.0f, dt * 10.0f);
        
        float size = 12;
        int boxColor = checked ? themeColor : 0xFF313338;
        int borderColor = checked ? themeColor : ColorUtility.interpolateColors(0xFF4E5058, 0xFF707070, checkboxHoverProgress);
        
        NVGRenderer.roundedRect(x, y + 1, size, size, 3, ColorUtility.applyOpacity(boxColor, alpha));
        NVGRenderer.roundedRectOutline(x, y + 1, size, size, 3, 1.0f, ColorUtility.applyOpacity(borderColor, alpha));
        
        if (checked) {
            long vg = NVGRenderer.getContext();
            NVGRenderer.applyColor(ColorUtility.applyOpacity(0xFFFFFFFF, alpha), NVGRenderer.NVG_COLOR_1);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, x + 3, y + 7);
            NanoVG.nvgLineTo(vg, x + 5.5f, y + 9.5f);
            NanoVG.nvgLineTo(vg, x + 9.5f, y + 4.5f);
            NanoVG.nvgStrokeWidth(vg, 1.5f);
            NanoVG.nvgStrokeColor(vg, NVGRenderer.NVG_COLOR_1);
            NanoVG.nvgStroke(vg);
        }
        
        drawStringWithAutoFont(label, x + size + 6, y + size / 2 + 1, 10.5f, ColorUtility.applyOpacity(hovered ? 0xFFFFFFFF : 0xFFBBBBBB, alpha), false, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
    }

    private void drawStringWithAutoFont(String text, float x, float y, float size, int color, boolean bold, int align) {
        NVGTextRenderer renderer = containsChinese(text) ? MI_FONT : (bold ? BOLD_FONT : FONT);
        renderer.drawString(text, x, y, size, color, false, align);
    }

    private boolean containsChinese(String s) {
        if (s == null) return false;
        for (char c : s.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FA5) {
                return true;
            }
        }
        return false;
    }

    private void renderInputField(float x, float y, float width, float height, String label, String value, int index, int mouseX, int mouseY, float dt, float alpha) {
        final int themeColor = ColorUtility.getClientTheme().first;
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);
        boolean active = activeField == index;

        NVGRenderer.roundedRect(x, y, width, height, 8, ColorUtility.applyOpacity(0x000000, 0.2f * alpha));
        NVGRenderer.roundedRectOutline(x, y, width, height, 8, 1.0f, ColorUtility.applyOpacity(active || hovered ? themeColor : 0xFF353535, 0.5f * alpha));

        boolean isFloating = active || !value.isEmpty();
        float labelY = y + (isFloating ? 8 : 22);
        float labelSize = isFloating ? 10 : 13;
        
        drawStringWithAutoFont(label, x + 12, labelY, labelSize, ColorUtility.applyOpacity(isFloating ? themeColor : 0xFF909090, alpha), false, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        if (!value.isEmpty() || active) {
            NVGTextRenderer renderer = containsChinese(value) ? MI_FONT : FONT;
            float textWidth = value.isEmpty() ? 0 : renderer.getStringWidth(value, 13);

            float maxTextWidth = width - 24;
            float textXOffset = 0;
            if (textWidth > maxTextWidth) {
                textXOffset = textWidth - maxTextWidth;
            }

            final float finalX = x + 12 - textXOffset;
            final float finalTextWidth = textWidth;
            
            NVGRenderer.scissor(x + 12, y, width - 24, height, () -> {
                if (!value.isEmpty()) {
                    drawStringWithAutoFont(value, finalX, y + 28, 13, ColorUtility.applyOpacity(0xFFFFFFFF, alpha), false, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
                }

                if (active) {
                    if ((System.currentTimeMillis() / 500) % 2 == 0) {
                        float cursorX = finalX + finalTextWidth + 1;
                        float cursorY = y + 28 - 6;
                        NVGRenderer.rect(cursorX, cursorY, 1.0f, 12, ColorUtility.applyOpacity(themeColor, alpha));
                    }
                }
            });
        }
    }

    private void renderButton(float x, float y, float width, float height, String text, int mouseX, int mouseY, float dt, float alpha) {
        final int themeColor = ColorUtility.getClientTheme().first;
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);

        NVGRenderer.roundedRect(x - 2, y - 2, width + 4, height + 4, 10, ColorUtility.applyOpacity(themeColor, 0.15f * alpha));

        NVGRenderer.roundedRect(x, y, width, height, 8, ColorUtility.applyOpacity(hovered ? ColorUtility.brighter(themeColor, 0.1f) : themeColor, alpha));
        
        drawStringWithAutoFont(text, x + width / 2, y + height / 2 + 1, 15, ColorUtility.applyOpacity(0xFFFFFFFF, alpha), true, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        
        if (keyCode == GLFW.GLFW_KEY_V && (keyInput.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clipboard = mc.keyboard.getClipboard();
            if (activeField == 0) username += clipboard;
            else if (activeField == 1) password += clipboard;
            else if (activeField == 2) cardKey += clipboard;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            activeField = (activeField + 1) % (isRegisterMode ? 3 : 2);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            handleVerify();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (activeField == 0 && !username.isEmpty()) username = username.substring(0, username.length() - 1);
            else if (activeField == 1 && !password.isEmpty()) password = password.substring(0, password.length() - 1);
            else if (activeField == 2 && !cardKey.isEmpty()) cardKey = cardKey.substring(0, cardKey.length() - 1);
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        char chr = (char) charInput.codepoint();
        if (activeField == 0) username += chr;
        else if (activeField == 1) password += chr;
        else if (activeField == 2) cardKey += chr;
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (handleMouseClickAt(click.x(), click.y())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    public void simulateRecoveryClicks(final int count) {
        final int safeCount = Math.max(0, count);
        for (int i = 0; i < safeCount; i++) {
            final double baseX = 4 + (i % 5) * 3;
            final double baseY = 4 + ((i / 5) % 4) * 3;
            handleMouseClickAt(baseX, baseY);
        }
    }

    private boolean handleMouseClickAt(double mouseX, double mouseY) {
        final float targetScale = (mc.getWindow().getWidth() == 3840 && mc.getWindow().getHeight() == 2160) ? 4.0f : 2.0f;
        final float currentScale = (float) mc.getWindow().getScaleFactor();
        final float uiScale = targetScale / currentScale;
        final float cx = mc.getWindow().getScaledWidth() / 2f;
        final float cy = mc.getWindow().getScaledHeight() / 2f;
        mouseX = cx + (mouseX - cx) / uiScale;
        mouseY = cy + (mouseY - cy) / uiScale;
        
        float width = 280;
        float baseHeight = 350;
        float height = baseHeight + 60 * panelHeightProgress;
        float x = (mc.getWindow().getScaledWidth() - width) / 2f;
        float y = (mc.getWindow().getScaledHeight() - height) / 2f;
        float animatedY = y + (1.0f - animationProgress) * 20;

        float fieldX = x + 30;
        float fieldWidth = width - 60;

        if (isHovered(mouseX, mouseY, fieldX, animatedY + 100, fieldWidth, 42)) activeField = 0;
        else if (isHovered(mouseX, mouseY, fieldX, animatedY + 160, fieldWidth, 42)) activeField = 1;
        else if (panelHeightProgress > 0.5f && isHovered(mouseX, mouseY, fieldX, animatedY + 220, fieldWidth, 42)) activeField = 2;

        float buttonY = animatedY + 235 + 55 * panelHeightProgress;
        if (panelHeightProgress < 0.4f && isHovered(mouseX, mouseY, fieldX, buttonY - 22, 100, 14)) {
            rememberMe = !rememberMe;
        }

        if (isHovered(mouseX, mouseY, fieldX, buttonY, fieldWidth, 42)) {
            handleVerify();
            return true;
        }

        float switchY = buttonY + 65;
        if (isHovered(mouseX, mouseY, fieldX, switchY, fieldWidth, 18)) {
            isRegisterMode = !isRegisterMode;
            activeField = 0;
            statusMessage = isRegisterMode ? "Please fill registration details" : "Please login";
            statusColor = ColorUtility.MUTED_COLOR;
            return true;
        }

        return false;
    }

    private void handleVerify() {
        if (username.isEmpty() || password.isEmpty()) {
            statusMessage = "Username and password cannot be empty";
            statusColor = 0xFFFF5050;
            return;
        }
        statusMessage = "Connecting to server...";
        statusColor = ColorUtility.getClientTheme().first;
        connecting = true;
        connectStartAt = System.currentTimeMillis();
        VerifyManager.getInstance().reconnect();
        if (isRegisterMode) {
            if (cardKey.isEmpty()) {
                statusMessage = "Card Key is required for registration";
                statusColor = 0xFFFF5050;
                return;
            }
            VerifyManager.getInstance().registerOrRecharge(username, password, cardKey);
        } else {
            VerifyManager.getInstance().login(username, password);
        }
    }

    private boolean isHovered(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
