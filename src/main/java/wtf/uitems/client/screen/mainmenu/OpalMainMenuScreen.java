package wtf.uitems.client.screen.mainmenu;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.nanovg.NanoVG;
import wtf.uitems.client.ReleaseInfo;
import wtf.uitems.client.screen.auth.OpalMicrosoftTokenScreen;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.background.BackgroundRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.client.verify.VerifyManager;
import wtf.uitems.utility.misc.math.MathUtility;
import wtf.uitems.utility.render.ColorUtility;

import java.util.ArrayList;
import java.util.List;

import static wtf.uitems.client.Constants.mc;
import wtf.uitems.client.OpalClient;

public final class OpalMainMenuScreen extends Screen {
    private static final float PANEL_WIDTH = 240.0f;
    private static final float PANEL_TOP_PADDING = 74.0f;
    private static final float PANEL_BOTTOM_PADDING = 56.0f;
    private static final float BUTTON_WIDTH = 200.0f;
    private static final float BUTTON_HEIGHT = 32.0f;
    private static final float BUTTON_SPACING = 6.0f;

    private static final NVGTextRenderer FONT = FontRepository.getFont("inter-regular");
    private static final NVGTextRenderer BOLD_FONT = FontRepository.getFont("inter-bold");
    
    private static final int BG_COLOR = 0xEE111214;
    private static final int BUTTON_BG = 0xFF1E1F22;
    
    private final List<MenuButton> buttons = new ArrayList<>();
    private float animationProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();

    public OpalMainMenuScreen() {
        super(Text.of(ReleaseInfo.NAME + " Main Menu"));
    }

    @Override
    protected void init() {
        buttons.clear();

        buttons.add(new MenuButton("Singleplayer", () -> mc.setScreen(new SelectWorldScreen(this))));
        buttons.add(new MenuButton("Multiplayer", () -> mc.setScreen(new MultiplayerScreen(this))));
        buttons.add(new MenuButton("Options", () -> mc.setScreen(new OptionsScreen(this, mc.options))));
        buttons.add(new MenuButton("Microsoft Login", () -> mc.setScreen(new OpalMicrosoftTokenScreen(this))));
        buttons.add(new MenuButton("Exit", () -> mc.stop()));
        
        animationProgress = 0.0f;
        lastTime = System.currentTimeMillis();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Do nothing
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float dt = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;

        animationProgress = MathUtility.interpolate(animationProgress, 1.0f, dt * 4.0f);

        final int windowWidth = context.getScaledWindowWidth();
        final int windowHeight = context.getScaledWindowHeight();

        // Start NanoVG for background and panel
        BackgroundRenderer.render(context, mouseX, mouseY, delta);
        
        if (!NVGRenderer.beginFrame()) return;

        float alpha = animationProgress;

        // Center Panel
        float panelWidth = PANEL_WIDTH;
        float panelHeight = getPanelHeight();
        float panelX = (windowWidth - panelWidth) / 2.0f;
        float panelY = (windowHeight - panelHeight) / 2.0f + (1.0f - animationProgress) * 20;

        final com.ibm.icu.impl.Pair<Integer, Integer> themeColors = ColorUtility.getClientTheme();
        final int themeColor = themeColors.first;

        // Draw Panel Background
        NVGRenderer.roundedRect(panelX, panelY, panelWidth, panelHeight, 12, ColorUtility.applyOpacity(BG_COLOR, alpha));
        NVGRenderer.roundedRectOutline(panelX, panelY, panelWidth, panelHeight, 12, 1.0f, ColorUtility.applyOpacity(themeColor, 0.2f * alpha));

        // Draw Logo / Title
        float centerX = panelX + panelWidth / 2.0f;
        float titleWidth = BOLD_FONT.getStringWidth(ReleaseInfo.NAME, 28);
        BOLD_FONT.drawGradientStringWithShadow(ReleaseInfo.NAME, centerX - titleWidth / 2.0f, panelY + 36, 28, ColorUtility.applyOpacity(themeColors.first, alpha), ColorUtility.applyOpacity(themeColors.second, alpha));
        FONT.drawString(ReleaseInfo.CHANNEL.toString().toUpperCase(), centerX, panelY + 54, 12, ColorUtility.applyOpacity(ColorUtility.MUTED_COLOR, alpha), false, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        // Draw Buttons
        float startY = panelY + PANEL_TOP_PADDING;
        float buttonWidth = BUTTON_WIDTH;
        float buttonHeight = BUTTON_HEIGHT;
        float buttonSpacing = BUTTON_SPACING;

        for (int i = 0; i < buttons.size(); i++) {
            MenuButton btn = buttons.get(i);
            float btnY = startY + i * (buttonHeight + buttonSpacing);
            renderButton(btn, panelX + (panelWidth - buttonWidth) / 2.0f, btnY, buttonWidth, buttonHeight, mouseX, mouseY, dt, alpha);
        }

        // Draw Version / Footer
        FONT.drawString("User: " + getDisplayUsername(), centerX, panelY + panelHeight - 30, 10, ColorUtility.applyOpacity(ColorUtility.MUTED_COLOR, alpha), false, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        NVGRenderer.endFrameAndReset(true);

        int x = 6;
        int y = 6;
        int w = 110;
        int h = 18;
        int bg = 0x99111114;
        int fg = 0xFFFFFFFF;
        context.fill(x, y, x + w, y + h, bg);
        context.drawText(mc.textRenderer, Text.of("切换到原版菜单"), x + 6, y + 5, fg, false);
    }

    private void renderButton(MenuButton btn, float x, float y, float width, float height, int mouseX, int mouseY, float dt, float alpha) {
        final com.ibm.icu.impl.Pair<Integer, Integer> themeColors = ColorUtility.getClientTheme();
        final int themeColor = themeColors.first;
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        btn.hoverProgress = MathUtility.interpolate(btn.hoverProgress, hovered ? 1.0f : 0.0f, dt * 8.0f);

        int bgColor = ColorUtility.interpolateColors(BUTTON_BG, themeColor, btn.hoverProgress); 
        
        // Button Shadow/Glow
        if (btn.hoverProgress > 0) {
            NVGRenderer.roundedRect(x - 1, y - 1, width + 2, height + 2, 8, ColorUtility.applyOpacity(themeColor, 0.2f * btn.hoverProgress * alpha));
        }

        // Base fill
        NVGRenderer.roundedRect(x, y, width, height, 8, ColorUtility.applyOpacity(bgColor, alpha));
        // Gradient overlay according to theme
        float gradientAlpha = Math.min(1.0f, 0.6f * btn.hoverProgress) * alpha;
        NVGRenderer.roundedRectGradient(x, y, width, height, 8,
                ColorUtility.applyOpacity(themeColors.first, gradientAlpha),
                ColorUtility.applyOpacity(themeColors.second, gradientAlpha),
                0);
        NVGRenderer.roundedRectOutline(x, y, width, height, 8, 1.0f, ColorUtility.applyOpacity(0x10FFFFFF, alpha));

        BOLD_FONT.drawString(btn.text, x + width / 2.0f, y + height / 2.0f + 1, 13, ColorUtility.applyOpacity(0xFFFFFFFF, alpha), false, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double mouseX = click.x();
            double mouseY = click.y();
            int x = 6;
            int y = 6;
            int w = 110;
            int h = 18;
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                if (OpalClient.getInstance() != null) {
                    OpalClient.getInstance().setUseVanillaMainMenu(true);
                }
                mc.setScreen(new TitleScreen());
                return true;
            }
            
            float panelWidth = PANEL_WIDTH;
            float panelHeight = getPanelHeight();
            float panelX = (mc.getWindow().getScaledWidth() - panelWidth) / 2.0f;
            float panelY = (mc.getWindow().getScaledHeight() - panelHeight) / 2.0f;
            
            float startY = panelY + PANEL_TOP_PADDING;
            float buttonWidth = BUTTON_WIDTH;
            float buttonHeight = BUTTON_HEIGHT;
            float buttonSpacing = BUTTON_SPACING;

            for (int i = 0; i < buttons.size(); i++) {
                float btnX = panelX + (panelWidth - buttonWidth) / 2.0f;
                float btnY = startY + i * (buttonHeight + buttonSpacing);
                if (mouseX >= btnX && mouseX <= btnX + buttonWidth && mouseY >= btnY && mouseY <= btnY + buttonHeight) {
                    buttons.get(i).action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static class MenuButton {
        String text;
        Runnable action;
        float hoverProgress = 0.0f;

        MenuButton(String text, Runnable action) {
            this.text = text;
            this.action = action;
        }
    }

    private float getPanelHeight() {
        final float buttonBlockHeight = buttons.size() * BUTTON_HEIGHT + Math.max(0, buttons.size() - 1) * BUTTON_SPACING;
        return PANEL_TOP_PADDING + buttonBlockHeight + PANEL_BOTTOM_PADDING;
    }

    private String getDisplayUsername() {
        if (mc.getSession() != null && mc.getSession().getUsername() != null && !mc.getSession().getUsername().isBlank()) {
            return mc.getSession().getUsername();
        }

        final String verifiedUsername = VerifyManager.getInstance().getUsername();
        if (verifiedUsername != null && !verifiedUsername.isBlank()) {
            return verifiedUsername;
        }

        return "Unknown";
    }
}
