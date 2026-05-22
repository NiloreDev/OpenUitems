package wtf.uitems.client.screen.click.jello;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.visual.ClickGUIModule;
import wtf.uitems.client.feature.module.property.Property;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.client.screen.click.AbstractClickGui;
import wtf.uitems.client.screen.click.dropdown.panel.property.PropertyPanel;
import wtf.uitems.utility.misc.HoverUtility;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.Scroller;
import wtf.uitems.utility.render.ScreenPosition;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.nanovg.NanoVG.nvgRestore;
import static org.lwjgl.nanovg.NanoVG.nvgSave;
import static org.lwjgl.nanovg.NanoVG.nvgScale;
import static org.lwjgl.nanovg.NanoVG.nvgGlobalAlpha;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;
import static wtf.uitems.client.Constants.mc;

public final class JelloClickGUI extends AbstractClickGui {

    private static final float OUTER_MARGIN = 30F;
    private static final float CARD_GAP = 10F;
    private static final float CARD_HEADER_HEIGHT = 50F;
    private static final float MODULE_ROW_HEIGHT = 24F;
    private static final float SETTINGS_WIDTH = 500F;
    private static final float FORCED_GUI_SCALE = 2F;

    private final Animation openAnimation = new Animation(Easing.EASE_OUT_QUINT, 260);
    private final Animation settingsAnimation = new Animation(Easing.EASE_OUT_CUBIC, 220);
    private boolean closing;

    private final Map<ModuleCategory, Scroller> categoryScrollers = new EnumMap<>(ModuleCategory.class);
    private final Map<ModuleCategory, ScreenPosition> categoryPositions = new EnumMap<>(ModuleCategory.class);
    private final Scroller settingsScroller = new Scroller();
    private final Map<Module, List<PropertyPanel<?>>> propertyPanels = new HashMap<>();

    private Module selectedModule;
    private Module pendingSelectedModule;
    private boolean closingSettings;
    private ModuleCategory draggingCategory;
    private float dragOffsetX;
    private float dragOffsetY;

    public JelloClickGUI() {
        final float cardWidth = 200F;
        final float cardHeight = 320F;
        final int columns = 4;
        final float gridTop = 68F;
        int index = 0;
        for (final ModuleCategory category : ModuleCategory.VALUES) {
            this.categoryScrollers.put(category, new Scroller());
            final int row = index / columns;
            final int col = index % columns;
            final float cardX = OUTER_MARGIN + col * (cardWidth + CARD_GAP);
            final float cardY = gridTop + row * (cardHeight + CARD_GAP);
            this.categoryPositions.put(category, new ScreenPosition(cardX, cardY, cardWidth, cardHeight));
            index++;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.closing = false;
        this.closingSettings = false;
        this.pendingSelectedModule = null;
        this.openAnimation.reset();
    }

    @Override
    public void doRender(DrawContext context, int mouseX, int mouseY, float delta) {
        final boolean isBloom = mouseX == -1 && mouseY == -1;
        final boolean frameStarted = !isBloom && NVGRenderer.beginFrame();

        if (!isBloom) {
            this.openAnimation.run(this.closing ? 0F : 1F);
            this.settingsAnimation.run(this.selectedModule != null && !this.closingSettings ? 1F : 0F);

            if (this.closingSettings && this.settingsAnimation.isFinished()) {
                this.selectedModule = this.pendingSelectedModule;
                this.pendingSelectedModule = null;
                this.closingSettings = false;

                if (this.selectedModule != null) {
                    this.settingsScroller.getAnimation().reset();
                    this.settingsAnimation.setValue(0F);
                    this.settingsAnimation.reset();
                }
            }

            if (this.closing && this.openAnimation.isFinished()) {
                mc.setScreen(null);
                OpalClient.getInstance().getModuleRepository().getModule(ClickGUIModule.class).setEnabled(false);
                if (frameStarted) {
                    NVGRenderer.endFrameAndReset(true);
                }
                return;
            }
        }

        final float animation = isBloom ? 1F : this.openAnimation.getValue();
        final float surfaceAnimation = this.closing ? animation * animation * animation : animation;
        final float backgroundAnimation = this.closing ? animation * animation : animation;
        final float settingsVisibility = this.selectedModule != null ? this.settingsAnimation.getValue() * surfaceAnimation : 0F;
        final float cardInteraction = this.selectedModule != null ? this.settingsAnimation.getValue() : 0F;
        final boolean renderSurfaces = isBloom || !this.closing || surfaceAnimation > 0.12F;
        final float scaledWidth = mc.getWindow().getScaledWidth();
        final float scaledHeight = mc.getWindow().getScaledHeight();
        final float uiWidth = mc.getWindow().getFramebufferWidth() / FORCED_GUI_SCALE;
        final float uiHeight = mc.getWindow().getFramebufferHeight() / FORCED_GUI_SCALE;
        final float renderScale = getRenderScale();
        final double actualMouseX = isBloom ? -1 : mouseX / renderScale;
        final double actualMouseY = isBloom ? -1 : mouseY / renderScale;

        final float sigmaX = 10F;
        final float sigmaY = 14F;

        NVGRenderer.globalAlpha(animation);
        final long vg = NVGRenderer.getContext();

        if (!isBloom) {
            NVGRenderer.rect(0, 0, scaledWidth, scaledHeight, NVGRenderer.BLUR_PAINT, 0.48F * backgroundAnimation);
            NVGRenderer.rect(0, 0, scaledWidth, scaledHeight, ColorUtility.applyOpacity(0x1C2128, 0.22F * backgroundAnimation));
        }

        nvgSave(vg);
        nvgScale(vg, renderScale, renderScale);

        drawBranding(sigmaX, sigmaY, isBloom);

        final float cardWidth = 200F;
        final float cardHeight = 320F;
        final int columns = 4;
        final float gridTop = 68F;
        final float settingsX = (uiWidth - SETTINGS_WIDTH) / 2F;
        final float settingsHeight = Math.min(600F, uiHeight * 0.7F);
        final float settingsTop = (uiHeight - settingsHeight) / 2F + 20F;

        if (!isBloom && this.draggingCategory != null) {
            final ScreenPosition position = this.categoryPositions.get(this.draggingCategory);
            if (position != null) {
                position.setX(Math.max(0F, Math.min(uiWidth - cardWidth, (float) actualMouseX - this.dragOffsetX)));
                position.setY(Math.max(0F, Math.min(uiHeight - cardHeight, (float) actualMouseY - this.dragOffsetY)));
            }
        }

        if (renderSurfaces) {
            for (final ModuleCategory category : ModuleCategory.VALUES) {
                final ScreenPosition position = this.categoryPositions.get(category);
                final float cardBaseX = position != null ? position.getX() : OUTER_MARGIN;
                final float cardBaseY = position != null ? position.getY() : gridTop;
                final float cardX = cardBaseX - 18F * cardInteraction;
                final float cardY = cardBaseY + 4F * cardInteraction;
                final float cardAlpha = surfaceAnimation * (1F - 0.18F * cardInteraction);

                drawCategoryCard(context, category, cardX, cardY, cardWidth, cardHeight, actualMouseX, actualMouseY, delta, isBloom, cardAlpha);
            }
        }

        if (renderSurfaces && this.selectedModule != null) {
            if (!isBloom) {
                NVGRenderer.rect(0, 0, uiWidth, uiHeight, ColorUtility.applyOpacity(0x2C3038, 0.42F * backgroundAnimation * settingsVisibility));
            }
            drawSettingsPanel(context, settingsX, settingsTop, SETTINGS_WIDTH, settingsHeight, actualMouseX, actualMouseY, delta, isBloom, settingsVisibility);
        }

        nvgRestore(vg);
        NVGRenderer.globalAlpha(1F);

        if (frameStarted) {
            NVGRenderer.endFrameAndReset(true);
        }
    }

    private void drawBranding(final float x, final float y, final boolean isBloom) {
        final NVGTextRenderer sigmaFont = FontRepository.getFont("productsans-regular");
        final NVGTextRenderer jelloFont = FontRepository.getFont("productsans-regular");

        final int sigmaColor = isBloom ? ColorUtility.applyOpacity(0xFFFFFFFF, 0.15F) : ColorUtility.applyOpacity(0xFFFFFF, 0.38F);
        final int jelloColor = isBloom ? ColorUtility.applyOpacity(0xFFFFFFFF, 0.14F) : ColorUtility.applyOpacity(0xFFFFFF, 0.3F);

        sigmaFont.drawString("Sigma", x, y + 14F, 22F, sigmaColor, false, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        jelloFont.drawString("Jello", x + 4F, y + 33F, 10F, jelloColor, false, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
    }

    private void drawCategoryCard(
            final DrawContext context,
            final ModuleCategory category,
            final float x,
            final float y,
            final float width,
            final float height,
            final double mouseX,
            final double mouseY,
            final float delta,
            final boolean isBloom,
            final float animation
    ) {
        if (isBloom) {
            return;
        }

        NVGRenderer.roundedRect(x, y, width, height, 5F, ColorUtility.applyOpacity(0xF6F6F6, 0.9F * animation));
        NVGRenderer.roundedRect(x, y, width, CARD_HEADER_HEIGHT, 5F, ColorUtility.applyOpacity(0xEDEDED, 0.85F * animation));

        final NVGTextRenderer titleFont = FontRepository.getFont("productsans-regular");
        titleFont.drawString(category.getName(), x + 18F, y + 26F, 11F, 0xFF7D7D7D, false, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        final List<Module> modules = new ArrayList<>(OpalClient.getInstance().getModuleRepository().getModulesInCategory(category));
        final Scroller scroller = this.categoryScrollers.get(category);
        final float contentX = x + 18F;
        final float contentY = y + CARD_HEADER_HEIGHT + 10F;
        final float contentHeight = height - CARD_HEADER_HEIGHT - 12F;
        final float scrollValue = scroller.getAnimation().getValue();

        NVGRenderer.scissor(x + 1F, y + CARD_HEADER_HEIGHT, width - 2F, height - CARD_HEADER_HEIGHT - 2F, () -> {
            float moduleY = contentY + scrollValue;
            final NVGTextRenderer moduleFont = FontRepository.getFont("productsans-regular");

            for (final Module module : modules) {
                final boolean hovered = HoverUtility.isHovering(contentX - 6F, moduleY - 10F, width - 28F, MODULE_ROW_HEIGHT, mouseX, mouseY);
                if (hovered) {
                    NVGRenderer.roundedRect(contentX - 6F, moduleY - 10F, width - 28F, 20F, 4F, ColorUtility.applyOpacity(0x000000, 0.05F));
                }

                final int textColor = module.isEnabled() ? 0xFF1E7BFF : 0xFF2F2F2F;
                moduleFont.drawString(module.getName(), contentX, moduleY, 8.5F, textColor, false, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

                if (module == this.selectedModule) {
                    NVGRenderer.roundedRect(x + width - 13F, moduleY - 4F, 3F, 8F, 1.5F, 0xFF2E86FF);
                } else if (!module.getPropertyList().isEmpty()) {
                    FontRepository.getFont("materialicons-regular").drawString("\ue315", x + width - 14F, moduleY, 8F, 0x889B9B9B, false, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
                }

                moduleY += MODULE_ROW_HEIGHT;
            }
        });

        final float maxScroll = Math.max(0F, modules.size() * MODULE_ROW_HEIGHT - contentHeight + 8F);
        scroller.onScroll(maxScroll);
    }

    private void drawSettingsPanel(
            final DrawContext context,
            final float x,
            final float y,
            final float width,
            final float height,
            final double mouseX,
            final double mouseY,
            final float delta,
            final boolean isBloom,
            final float animation
    ) {
        if (isBloom) {
            return;
        }

        final float animatedY = y + (1F - animation) * 12F;
        NVGRenderer.dropShadow(x, y, width, height, 10F, 32F, 1F, ColorUtility.applyOpacity(0x000000, 0.12F * animation));
        NVGRenderer.roundedRect(x, animatedY, width, height, 10F, ColorUtility.applyOpacity(0xFAFAFA, 0.97F * animation));

        final NVGTextRenderer titleFont = FontRepository.getFont("productsans-bold");
        final NVGTextRenderer descFont = FontRepository.getFont("productsans-regular");
        titleFont.drawString(this.selectedModule.getName(), x, animatedY - 22F, 23F, ColorUtility.applyOpacity(0xFFF3F3F3, animation), false, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        descFont.drawString(this.selectedModule.getDescription(), x + 30F, animatedY + 31F, 10F, ColorUtility.applyOpacity(0x16364F, 0.56F * animation), false, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        if (animation <= 0.03F) {
            return;
        }

        final List<PropertyPanel<?>> panels = getPropertyPanels(this.selectedModule);
        final float contentX = x + 20F;
        final float contentY = animatedY + 59F;
        final float contentWidth = width - 40F;
        final float contentHeight = height - 69F;
        final float scrollValue = this.settingsScroller.getAnimation().getValue();

        PropertyPanel.style = PropertyPanel.Style.JELLO;
        PropertyPanel.modernStyle = false;

        final long vg = NVGRenderer.getContext();
        nvgSave(vg);
        nvgGlobalAlpha(vg, animation);
        NVGRenderer.scissor(x + 8F, contentY, width - 16F, contentHeight, () -> {
            float currentY = contentY + scrollValue;
            int lastVisibleIndex = -1;
            for (int i = panels.size() - 1; i >= 0; i--) {
                if (!panels.get(i).isHidden()) {
                    lastVisibleIndex = i;
                    break;
                }
            }

            for (int i = 0; i < panels.size(); i++) {
                final PropertyPanel<?> panel = panels.get(i);
                if (panel.isHidden()) {
                    continue;
                }
                panel.setX(contentX);
                panel.setY(currentY);
                panel.setWidth(contentWidth);
                panel.setLastProperty(i == lastVisibleIndex);
                panel.render(context, (int) mouseX, (int) mouseY, delta);
                currentY += panel.getHeight();
            }
        });
        nvgRestore(vg);

        PropertyPanel.style = PropertyPanel.Style.DEFAULT;

        final float totalHeight = getSettingsContentHeight(panels);
        this.settingsScroller.onScroll(Math.max(0F, totalHeight - contentHeight));
    }

    private float getRenderScale() {
        return (float) (FORCED_GUI_SCALE / mc.getWindow().getScaleFactor());
    }

    private List<PropertyPanel<?>> getPropertyPanels(final Module module) {
        return this.propertyPanels.computeIfAbsent(module, ignored -> {
            final List<PropertyPanel<?>> panels = new ArrayList<>();
            for (final Property<?> property : module.getPropertyList()) {
                final PropertyPanel<?> panel = property.createClickGUIComponent();
                if (panel != null) {
                    panel.init();
                    panels.add(panel);
                }
            }
            return panels;
        });
    }

    private float getSettingsContentHeight(final List<PropertyPanel<?>> panels) {
        float totalHeight = 0F;
        for (final PropertyPanel<?> panel : panels) {
            if (!panel.isHidden()) {
                totalHeight += panel.getHeight();
            }
        }
        return totalHeight;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        final float renderScale = getRenderScale();
        final double mouseX = click.x() / renderScale;
        final double mouseY = click.y() / renderScale;
        final int button = click.button();

        final float uiWidth = mc.getWindow().getFramebufferWidth() / FORCED_GUI_SCALE;
        final float uiHeight = mc.getWindow().getFramebufferHeight() / FORCED_GUI_SCALE;
        final float settingsX = (uiWidth - SETTINGS_WIDTH) / 2F;
        final float settingsHeight = Math.min(600F, uiHeight * 0.7F);
        final float settingsTop = (uiHeight - settingsHeight) / 2F + 20F;
        final float cardWidth = 200F;
        final float cardHeight = 320F;
        final int columns = 4;
        final float gridTop = 68F;

        for (final ModuleCategory category : ModuleCategory.VALUES) {
            final ScreenPosition position = this.categoryPositions.get(category);
            final float cardX = position != null ? position.getX() : OUTER_MARGIN;
            final float cardY = position != null ? position.getY() : gridTop;

            if (HoverUtility.isHovering(cardX, cardY, cardWidth, CARD_HEADER_HEIGHT, mouseX, mouseY) && button == 0) {
                this.draggingCategory = category;
                this.dragOffsetX = (float) mouseX - cardX;
                this.dragOffsetY = (float) mouseY - cardY;
                return true;
            }

            if (HoverUtility.isHovering(cardX, cardY, cardWidth, cardHeight, mouseX, mouseY)) {
                final List<Module> modules = new ArrayList<>(OpalClient.getInstance().getModuleRepository().getModulesInCategory(category));
                float moduleY = cardY + CARD_HEADER_HEIGHT + 10F + this.categoryScrollers.get(category).getAnimation().getValue();
                for (final Module module : modules) {
                    if (HoverUtility.isHovering(cardX + 12F, moduleY - 10F, cardWidth - 24F, MODULE_ROW_HEIGHT, mouseX, mouseY)) {
                        if (button == 0) {
                            module.toggle();
                        } else if (button == 1) {
                            openSettingsPanel(module);
                        }
                        return true;
                    }
                    moduleY += MODULE_ROW_HEIGHT;
                }
            }
        }

        if (this.selectedModule != null && HoverUtility.isHovering(settingsX, settingsTop, SETTINGS_WIDTH, settingsHeight, mouseX, mouseY)) {
            PropertyPanel.style = PropertyPanel.Style.JELLO;
            final List<PropertyPanel<?>> panels = getPropertyPanels(this.selectedModule);
            final float contentX = settingsX + 20F;
            float currentY = settingsTop + 59F + this.settingsScroller.getAnimation().getValue();
            for (final PropertyPanel<?> panel : panels) {
                if (panel.isHidden()) {
                    continue;
                }
                panel.setX(contentX);
                panel.setY(currentY);
                panel.setWidth(SETTINGS_WIDTH - 40F);
                panel.mouseClicked(mouseX, mouseY, button);
                currentY += panel.getHeight();
            }
            PropertyPanel.style = PropertyPanel.Style.DEFAULT;
            return true;
        }

        if (this.selectedModule != null && button == 0) {
            closeSettingsPanel();
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.draggingCategory = null;

        if (this.selectedModule != null) {
            final List<PropertyPanel<?>> panels = getPropertyPanels(this.selectedModule);
            for (final PropertyPanel<?> panel : panels) {
                final float renderScale = getRenderScale();
                panel.mouseReleased(click.x() / renderScale, click.y() / renderScale, click.button());
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        final float renderScale = getRenderScale();
        mouseX /= renderScale;
        mouseY /= renderScale;
        final float uiHeight = mc.getWindow().getFramebufferHeight() / FORCED_GUI_SCALE;
        final float uiWidth = mc.getWindow().getFramebufferWidth() / FORCED_GUI_SCALE;
        final float settingsX = (uiWidth - SETTINGS_WIDTH) / 2F;
        final float settingsHeight = Math.min(600F, uiHeight * 0.7F);
        final float settingsTop = (uiHeight - settingsHeight) / 2F + 20F;
        if (this.selectedModule != null && HoverUtility.isHovering(settingsX, settingsTop, SETTINGS_WIDTH, settingsHeight, mouseX, mouseY)) {
            final float totalHeight = getSettingsContentHeight(getPropertyPanels(this.selectedModule));
            this.settingsScroller.addScroll(verticalAmount, Math.max(0F, totalHeight - (settingsHeight - 69F)));
            return true;
        }

        final int columns = 4;
        final float cardWidth = 200F;
        final float cardHeight = 320F;
        final float gridTop = 68F;

        for (final ModuleCategory category : ModuleCategory.VALUES) {
            final ScreenPosition position = this.categoryPositions.get(category);
            final float cardX = position != null ? position.getX() : OUTER_MARGIN;
            final float cardY = position != null ? position.getY() : gridTop;
            if (HoverUtility.isHovering(cardX, cardY, cardWidth, cardHeight, mouseX, mouseY)) {
                final float maxScroll = Math.max(0F, OpalClient.getInstance().getModuleRepository().getModulesInCategory(category).size() * MODULE_ROW_HEIGHT - (cardHeight - CARD_HEADER_HEIGHT - 12F) + 8F);
                this.categoryScrollers.get(category).addScroll(verticalAmount, maxScroll);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (this.selectedModule != null) {
                closeSettingsPanel();
                return true;
            }
            close();
            return true;
        }

        if (this.selectedModule != null) {
            final wtf.uitems.utility.KeyInput input = new wtf.uitems.utility.KeyInput(keyInput.key(), keyInput.modifiers());
            for (final PropertyPanel<?> panel : getPropertyPanels(this.selectedModule)) {
                panel.keyPressed(input);
            }
        }

        return true;
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (this.selectedModule != null) {
            for (final PropertyPanel<?> panel : getPropertyPanels(this.selectedModule)) {
                panel.charTyped((char) charInput.codepoint(), charInput.modifiers());
            }
        }
        return true;
    }

    @Override
    public void close() {
        if (selectingBind) {
            return;
        }
        this.closing = true;
    }

    private void openSettingsPanel(final Module module) {
        if (this.selectedModule == null) {
            this.selectedModule = module;
            this.pendingSelectedModule = null;
            this.closingSettings = false;
            this.settingsScroller.getAnimation().reset();
            this.settingsAnimation.setValue(0F);
            this.settingsAnimation.reset();
            return;
        }

        if (this.selectedModule != module || this.closingSettings) {
            this.pendingSelectedModule = module;
            this.closingSettings = true;
        }
    }

    private void closeSettingsPanel() {
        this.pendingSelectedModule = null;
        this.closingSettings = true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
