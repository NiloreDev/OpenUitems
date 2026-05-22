package wtf.uitems.client.feature.module.impl.visual.overlay;

import net.minecraft.util.Colors;
import com.ibm.icu.impl.Pair;
import org.lwjgl.glfw.GLFW;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.helper.impl.render.ScaleProperty;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.client.ClientElements;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.chathud.ChatHudElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.dynamicisland.DynamicIslandElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.modulelist.ToggledModulesElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.notifications.NotificationsElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.potions.PotionsElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.scaffoldmark.ScaffoldMarkElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.targetinfo.TargetInfoElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.watermark.WatermarkElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.dynamicisland.preset.TabListIsland;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.hotkeys.HotKeysElement;
import wtf.uitems.client.feature.module.property.Property;
import wtf.uitems.client.feature.module.property.impl.ColorProperty;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.bool.MultipleBooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.image.NVGImageRenderer;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.event.impl.client.PostClientInitializationEvent;
import wtf.uitems.event.impl.client.PropertyUpdateEvent;
import wtf.uitems.event.impl.game.PostGameTickEvent;
import wtf.uitems.event.impl.press.KeyPressEvent;
import wtf.uitems.event.impl.render.RenderBloomEvent;
import wtf.uitems.event.impl.render.RenderScreenEvent;
import wtf.uitems.event.impl.render.ResolutionChangeEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.render.ClientTheme;
import wtf.uitems.utility.render.ColorUtility;

import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;

import static wtf.uitems.client.Constants.mc;
import static org.lwjgl.nanovg.NanoVG.NVG_IMAGE_GENERATE_MIPMAPS;
import static org.lwjgl.nanovg.NanoVG.NVG_IMAGE_REPEATX;
import static org.lwjgl.nanovg.NanoVG.NVG_IMAGE_REPEATY;

public final class HUDModule extends Module {

    // Theme
    private final ModeProperty<ClientTheme> themeMode = new ModeProperty<>("Theme", ClientTheme.DISCORD, true);
    public static final ColorProperty primaryColorProperty = new ColorProperty("Primary color", Colors.BLACK);
    public static final ColorProperty secondaryColorProperty = new ColorProperty("Secondary color", Colors.BLACK);

    // Minecraft elements
    private final BooleanProperty statusEffectOverlayEnabled = new BooleanProperty("Enabled", false);
    private final BooleanProperty scoreboardEnabled = new BooleanProperty("Enabled", true);
    private final BooleanProperty scoreboardTextShadow = new BooleanProperty("Text shadow", true).hideIf(() -> !scoreboardEnabled.getValue());
    private final ScaleProperty scoreboardScale = ScaleProperty.newMinecraftElement();
    private final BooleanProperty bossbarEnabled = new BooleanProperty("Enabled", false);
    private final BooleanProperty dynamicIslandEnabled = new BooleanProperty("Enabled", true);
    private final BooleanProperty dynamicIslandLeftAligned = new BooleanProperty("Left-aligned", false);
    private final BooleanProperty dynamicIslandHideServerIp = new BooleanProperty("Hide Server IP", false);
    private final BooleanProperty tabListIsland = new BooleanProperty("TabList Island", false);

    // Visual effects
    private final BooleanProperty blurEnabled = new BooleanProperty("Enabled", true).id("blurEnabled");
    private final BooleanProperty bloomEnabled = new BooleanProperty("Enabled", true).id("bloomEnabled");
    private final NumberProperty blurRadius = new NumberProperty("Radius", 7, 1, 20, 1).id("blurRadius");
    private final ModeProperty<BlurBackgroundMode> blurBackgroundMode = new ModeProperty<>("Background style", BlurBackgroundMode.BLACK);
    private final NumberProperty bloomRadius = new NumberProperty("Radius", 7, 1, 20, 1).id("bloomRadius");

    // Tab GUI
    private final BooleanProperty tabGuiEnabled = new BooleanProperty("Enabled", false);
    private final BooleanProperty tabGuiGlow = new BooleanProperty("Background glow", false);
    private int tabCategoryIndex;
    private boolean tabCategoryExpanded;

    private final List<IOverlayElement> elements = new ArrayList<>();
    private static final int WINDOWS_MATERIAL_TEXTURE_SIZE = 64;
    private static final NVGImageRenderer WINDOWS_MATERIAL_TEXTURE = createWindowsMaterialTexture();
    
    public enum ScaffoldMarkStyle {
        OUTLINE,
        CARD
    }

    public enum BlurBackgroundMode {
        BLACK("Black", 0.82F, 0x090909, 0.58F, 0F),
        WHITE("White", 0.56F, 0xFFFFFF, 0.035F, 0F),
        WINDOWS_10("Windows 10", 0.68F, 0x050609, 0.66F, 0.030F),
        MICA("Mica", 0.60F, 0x12171D, 0.72F, 0.018F),
        ACRYLIC("Acrylic", 0.78F, 0x101621, 0.38F, 0.048F);

        private final String name;
        private final float sampleOpacity;
        private final int backgroundColor;
        private final float textureOpacity;

        BlurBackgroundMode(final String name, final float sampleOpacity, final int backgroundColor, final float backgroundOpacity, final float textureOpacity) {
            this.name = name;
            this.sampleOpacity = sampleOpacity;
            this.backgroundColor = ColorUtility.applyOpacity(backgroundColor, backgroundOpacity);
            this.textureOpacity = textureOpacity;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public float getSampleOpacity() {
            return this.sampleOpacity;
        }

        public int getBackgroundColor() {
            return this.backgroundColor;
        }

        public float getTextureOpacity() {
            return this.textureOpacity;
        }
    }

    private final ModeProperty<ScaffoldMarkStyle> scaffoldMarkStyle = new ModeProperty<>("Scaffold mark style", ScaffoldMarkStyle.CARD);

    private final TargetInfoElement targetInfo;
    private final ToggledModulesElement toggledModules;
    private final NotificationsElement notifications;
    private final DynamicIslandElement dynamicIsland;
    private final WatermarkElement watermark;
    private final ChatHudElement chatHudElement;
    private final TabListIsland tabListTrigger = new TabListIsland();

    public HUDModule() {
        super("HUD", "Renders the clients display.", ModuleCategory.VISUAL);

        primaryColorProperty.hideIf(() -> !themeMode.is(ClientTheme.CUSTOM));
        secondaryColorProperty.hideIf(() -> !themeMode.is(ClientTheme.CUSTOM));

        this.setEnabled(true);
        this.addProperties(
                themeMode, primaryColorProperty, secondaryColorProperty,
                new GroupProperty("Minecraft elements",
                        new GroupProperty(
                                "Status effect overlay",
                                statusEffectOverlayEnabled
                        ),
                        new GroupProperty(
                                "Scoreboard",
                                scoreboardScale.get(), scoreboardEnabled, scoreboardTextShadow
                        ),
                        new GroupProperty(
                                "Bossbar",
                                bossbarEnabled
                        ),
                        new GroupProperty(
                                "Scaffold mark",
                                scaffoldMarkStyle
                        )
                )
        );

        this.targetInfo = this.register(new TargetInfoElement(this));
        this.toggledModules = this.register(new ToggledModulesElement(this));
        this.watermark = this.register(new WatermarkElement(this));
        this.register(new ClientElements(this));

        dynamicIslandLeftAligned.hideIf(() -> !dynamicIslandEnabled.getValue());
        tabListIsland.hideIf(() -> !dynamicIslandEnabled.getValue());

        this.addProperties(
                new GroupProperty(
                        "Dynamic island",
                        dynamicIslandEnabled, dynamicIslandLeftAligned, dynamicIslandHideServerIp, tabListIsland
                )
        );
        this.addProperties(
                new GroupProperty("Visual effects",
                        new GroupProperty("Blur", blurEnabled, blurRadius, blurBackgroundMode),
                        new GroupProperty("Bloom", bloomEnabled, bloomRadius).hideIf(() -> !blurEnabled.getValue())
                )
        );
        this.addProperties(
                new GroupProperty("Tab GUI", tabGuiEnabled, tabGuiGlow)
        );
        this.notifications = this.register(new NotificationsElement(this));

        this.dynamicIsland = this.register(new DynamicIslandElement(this));
        this.register(new ScaffoldMarkElement(this));
        this.register(new HotKeysElement(this));
        this.register(new PotionsElement(this));
        this.chatHudElement = this.register(new ChatHudElement(this));
    }

    private <T extends IOverlayElement> T register(T element) {
        this.elements.add(element);
        return element;
    }

    @Override
    protected void onDisable() {
        this.elements.forEach(IOverlayElement::onDisable);
    }

    @Override
    protected void onEnable() {
        if (OpalClient.getInstance().isPostInitialization()) {
            this.toggledModules.initialize();
            this.targetInfo.initialize();
        }
    }

    @Subscribe
    public void onPostClientInitialization(PostClientInitializationEvent event) {
        this.toggledModules.initialize();
        this.targetInfo.initialize();
    }

    @Subscribe
    public void onPropertyUpdate(PropertyUpdateEvent event) {
        if (event.property() == tabListIsland) {
            if (tabListIsland.getValue()) {
                DynamicIslandElement.addTrigger(tabListTrigger);
            } else {
                DynamicIslandElement.removeTrigger(tabListTrigger);
            }
        }

        if (this.toggledModules != null) {
            this.toggledModules.markSortingDirty();
        }
    }

    @Subscribe(priority = -20)
    public void onRenderScreen(RenderScreenEvent event) {
        for (IOverlayElement element : this.elements) {
            if (element.isActive()) {
                element.render(event.drawContext(), event.tickDelta(), false);
            }
        }
        if (tabGuiEnabled.getValue()) {
            renderTabGUI();
        }
    }

    @Subscribe(priority = -20)
    public void onBloomRender(RenderBloomEvent event) {
        for (IOverlayElement element : this.elements) {
            if (element.isActive() && element.isBloom()) {
                element.render(event.drawContext(), event.tickDelta(), true);
            }
        }
        if (tabGuiEnabled.getValue()) {
            renderTabGUI();
        }
    }

    @Subscribe
    public void onResize(ResolutionChangeEvent event) {
        this.elements.forEach(IOverlayElement::onResize);
    }

    @Subscribe
    public void onPostTick(PostGameTickEvent event) {
        for (IOverlayElement element : this.elements) {
            if (element.isActive()) {
                element.tick();
            }
        }
    }

    @Subscribe
    public void onKeyPress(final KeyPressEvent event) {
        if (!tabGuiEnabled.getValue()) return;
        if (mc.currentScreen != null) return;
        final ModuleCategory category = ModuleCategory.VALUES[tabCategoryIndex];
        final List<Module> moduleList = OpalClient.getInstance().getModuleRepository().getModulesInCategory(category).stream().toList();
        final Module module = moduleList.isEmpty() ? null : moduleList.get(category.getModuleIndex());
        final List<Property<?>> propertyList = module == null ? List.of() : module.getPropertyList();
        final int key = event.getInteractionCode();
        switch (key) {
            case GLFW.GLFW_KEY_UP -> handleUpKey(category, module, moduleList, propertyList);
            case GLFW.GLFW_KEY_DOWN -> handleDownKey(category, module, moduleList, propertyList);
            case GLFW.GLFW_KEY_RIGHT -> handleRightKey(module, propertyList);
            case GLFW.GLFW_KEY_LEFT -> handleLeftKey(module, propertyList);
            case GLFW.GLFW_KEY_ENTER -> handleEnterKey(module, propertyList);
            case GLFW.GLFW_KEY_TAB -> handleTabKey(module, propertyList);
        }
    }

    public ModeProperty<ClientTheme> getThemeMode() {
        return themeMode;
    }

    public ToggledModulesElement getToggledModules() {
        return toggledModules;
    }

    public NotificationsElement getNotifications() {
        return notifications;
    }

    public DynamicIslandElement getDynamicIsland() {
        return dynamicIsland;
    }

    public boolean isTargetInfoActive() {
        return this.isEnabled() && this.targetInfo != null && this.targetInfo.isActive();
    }

    public boolean hasOverlayContent() {
        if (!this.isEnabled()) {
            return false;
        }
        if (this.tabGuiEnabled.getValue()) {
            return true;
        }
        for (final IOverlayElement element : this.elements) {
            if (element.isActive()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasBloomContent() {
        if (!this.isEnabled()) {
            return false;
        }
        if (this.tabGuiEnabled.getValue()) {
            return true;
        }
        for (final IOverlayElement element : this.elements) {
            if (element.isActive() && element.isBloom()) {
                return true;
            }
        }
        return false;
    }

    public boolean isDynamicIslandEnabled() {
        return dynamicIslandEnabled.getValue();
    }

    public boolean isDynamicIslandLeftAligned() {
        return dynamicIslandLeftAligned.getValue();
    }

    public boolean isTabListIsland() {
        return tabListIsland.getValue();
    }

    public boolean isDynamicIslandHideServerIP() {
        return dynamicIslandHideServerIp.getValue();
    }

    public ModeProperty<ScaffoldMarkStyle> getScaffoldMarkStyle() {
        return scaffoldMarkStyle;
    }

    public boolean isScoreboardTextShadow() {
        return scoreboardEnabled.getValue() && scoreboardTextShadow.getValue();
    }

    public float getScoreboardScale() {
        return scoreboardEnabled.getValue() ? scoreboardScale.getScale() : 1;
    }

    public boolean isBossbarEnabled() {
        return bossbarEnabled.getValue();
    }

    public boolean isStatusEffectOverlayEnabled() {
        return statusEffectOverlayEnabled.getValue();
    }

    public boolean isBlur() {
        return blurEnabled.getValue();
    }

    public boolean isBloom() {
        return bloomEnabled.getValue() && isBlur();
    }

    public int getBlurRadius() {
        return blurRadius.getValue().intValue();
    }

    public float getBlurSampleOpacity() {
        return blurBackgroundMode.getValue().getSampleOpacity();
    }

    public int getBloomRadius() {
        return bloomRadius.getValue().intValue();
    }

    public int getBlurBackgroundColor() {
        return blurBackgroundMode.getValue().getBackgroundColor();
    }

    public void renderBlurTextureOverlay(final float x, final float y, final float width, final float height, final float radius) {
        final BlurBackgroundMode mode = blurBackgroundMode.getValue();
        if (WINDOWS_MATERIAL_TEXTURE == null || mode.getTextureOpacity() <= 0F) {
            return;
        }

        final float tintAlpha = switch (mode) {
            case WINDOWS_10 -> 0.04F;
            case MICA -> 0.03F;
            case ACRYLIC -> 0.02F;
            default -> 0F;
        };

        if (tintAlpha > 0F) {
            NVGRenderer.roundedRect(
                    x, y, width, height, radius,
                    ColorUtility.applyOpacity(0x05070A, tintAlpha)
            );
        }

        WINDOWS_MATERIAL_TEXTURE.drawRoundedRect(x, y, width, height, radius, mode.getTextureOpacity());
    }

    private static NVGImageRenderer createWindowsMaterialTexture() {
        final int size = WINDOWS_MATERIAL_TEXTURE_SIZE;
        final ByteBuffer buffer = ByteBuffer.allocateDirect(size * size * 4);

        for (int y = 0; y < size; y++) {
            final double v = (double) y / size;
            for (int x = 0; x < size; x++) {
                final double u = (double) x / size;
                double cloud = 0.5D;
                cloud += Math.sin((u * Math.PI * 2D * 1.8D) + (v * Math.PI * 2D * 1.1D)) * 0.18D;
                cloud += Math.cos((u * Math.PI * 2D * 2.7D) - (v * Math.PI * 2D * 2.0D)) * 0.14D;
                cloud += Math.sin(((u + v) * Math.PI * 2D * 3.4D)) * 0.08D;
                cloud += Math.cos(((u - v) * Math.PI * 2D * 4.1D)) * 0.06D;
                cloud = Math.max(0D, Math.min(1D, cloud));

                final int alpha = 1 + (int) Math.round(cloud * 7D);
                final int red = 225 + (int) Math.round(cloud * 12D);
                final int green = 228 + (int) Math.round(cloud * 10D);
                final int blue = 236 + (int) Math.round(cloud * 8D);

                buffer.put((byte) red);
                buffer.put((byte) green);
                buffer.put((byte) blue);
                buffer.put((byte) alpha);
            }
        }

        buffer.flip();
        return NVGImageRenderer.fromRgbaData(buffer, size, size, NVG_IMAGE_GENERATE_MIPMAPS | NVG_IMAGE_REPEATX | NVG_IMAGE_REPEATY);
    }

    public boolean isCustomChatHudEnabled() {
        return this.isEnabled() && this.chatHudElement != null && this.chatHudElement.isActive();
    }

    private void renderTabGUI() {
        NVGTextRenderer font = FontRepository.getFont("productsans-medium");
        Pair<Integer, Integer> colors = ColorUtility.getClientTheme();
        float x = 10, y = 40, width = 75, panelHeight = 16;
        renderPanel(x, y, width, panelHeight, ModuleCategory.VALUES.length, colors);
        for (int i = 0; i < ModuleCategory.VALUES.length; i++) {
            float yPos = y + (i * panelHeight);
            boolean isCurrent = (i == tabCategoryIndex);
            renderTab(x, yPos, width, panelHeight, i, ModuleCategory.VALUES.length, isCurrent, colors);
            font.drawString(ModuleCategory.VALUES[i].getName(), x + 5, yPos + 11, 8.25F,
                    isCurrent ? -1 : ColorUtility.brighter(ColorUtility.MUTED_COLOR, 0.3F));
        }
        if (tabCategoryExpanded) {
            renderModules(font, colors, x + width + 7, y, width, panelHeight);
        }
    }

    private void renderPanel(float x, float y, float width, float panelHeight, int itemCount, Pair<Integer, Integer> colors) {
        float height = panelHeight * itemCount;
        float border = 2;
        NVGRenderer.roundedRect(x - border, y - border, width + border * 2, height + border * 2, 5.5F, NVGRenderer.BLUR_PAINT);
        if (tabGuiGlow.getValue()) {
            final long now = System.currentTimeMillis();
            final float t = (float) ((now % 4000L) / 4000.0);
            final float angle = t * 360F;
            final int a1 = (int) (90 + 50 * Math.sin(t * Math.PI * 2));
            final int a2 = (int) (70 + 60 * Math.sin((t + 0.25F) * Math.PI * 2));
            final int c1 = ColorUtility.applyOpacity(colors.first, Math.max(0, Math.min(255, a1)));
            final int c2 = ColorUtility.applyOpacity(colors.second, Math.max(0, Math.min(255, a2)));
            NVGRenderer.roundedRectGradient(x - border, y - border, width + border * 2, height + border * 2, 5.5F, c1, c2, angle);
            NVGRenderer.roundedRectGradient(x - border, y - border, width + border * 2, height + border * 2, 5.5F, c2, c1, angle + 110F);
        }
        NVGRenderer.roundedRect(x - border, y - border, width + border * 2, height + border * 2, 5.5F, 0x80090909);
    }

    private void renderTab(float x, float y, float width, float height, int index, int total, boolean isCurrent, Pair<Integer, Integer> colors) {
        boolean isFirst = (index == 0), isLast = (index == total - 1);
        float radius = 4;
        if (isFirst || isLast) {
            NVGRenderer.roundedRectVarying(x, y, width, height, isFirst ? radius : 0, isFirst ? radius : 0, isLast ? radius : 0, isLast ? radius : 0, NVGRenderer.BLUR_PAINT);
            NVGRenderer.roundedRectVarying(x, y, width, height, isFirst ? radius : 0, isFirst ? radius : 0, isLast ? radius : 0, isLast ? radius : 0, 0x80090909);
        } else {
            NVGRenderer.rect(x, y, width, height, NVGRenderer.BLUR_PAINT);
            NVGRenderer.rect(x, y, width, height, 0x80090909);
        }
        if (isCurrent) {
            renderGradient(x, y, width, height, isFirst, isLast, colors);
        }
    }

    private void renderGradient(float x, float y, float width, float height, boolean isFirst, boolean isLast, Pair<Integer, Integer> colors) {
        int startColor = ColorUtility.applyOpacity(colors.first, 0.4F);
        int endColor = ColorUtility.applyOpacity(colors.second, 0.4F);
        if (isFirst || isLast) {
            NVGRenderer.roundedRectVaryingGradient(x, y, width, height, isFirst ? 4 : 0, isFirst ? 4 : 0, isLast ? 4 : 0, isLast ? 4 : 0, startColor, endColor, 0);
        } else {
            NVGRenderer.rectGradient(x, y, width, height, startColor, endColor, 0);
        }
    }

    private void renderModules(NVGTextRenderer font, Pair<Integer, Integer> colors, float x, float y, float width, float panelHeight) {
        ModuleCategory category = ModuleCategory.VALUES[tabCategoryIndex];
        List<Module> modules = OpalClient.getInstance().getModuleRepository().getModulesInCategory(category).stream().toList();
        if (modules.isEmpty()) return;
        renderPanel(x, y, width, panelHeight, modules.size(), colors);
        for (int i = 0; i < modules.size(); i++) {
            float yPos = y + (i * panelHeight);
            boolean isCurrent = (i == category.getModuleIndex());
            renderTab(x, yPos, width, panelHeight, i, modules.size(), isCurrent, colors);
            font.drawString(modules.get(i).getName(), x + 5, yPos + 11, 8.25F,
                    isCurrent ? -1 : ColorUtility.brighter(ColorUtility.MUTED_COLOR, 0.3F));
            if (isCurrent && modules.get(i).isExpanded()) {
                renderProperties(font, colors, x + width + 7, y, panelHeight, modules.get(i));
            }
        }
    }

    private void renderProperties(NVGTextRenderer font, Pair<Integer, Integer> colors, float x, float y, float panelHeight, Module module) {
        List<Property<?>> properties = module.getPropertyList();
        if (properties.isEmpty()) return;
        double maxLength = 0;
        for (final Property<?> p : properties) {
            final double l = font.getStringWidth(p.getName() + ": " + getPropertyValue(p), 8.25F);
            if (l > maxLength) maxLength = l;
        }
        renderPanel(x, y, (float) (maxLength + 12.5F), panelHeight, properties.size(), colors);
        for (int i = 0; i < properties.size(); i++) {
            float yPos = y + (i * panelHeight);
            boolean isCurrent = (i == module.getPropertyIndex());
            renderTab(x, yPos, (float) (maxLength + 12.5F), panelHeight, i, properties.size(), isCurrent, Pair.of(ColorUtility.darker(colors.first, properties.get(i).isFocused() ? 0.35F : 0), ColorUtility.darker(colors.second, properties.get(i).isFocused() ? 0.35F : 0)));
            String propertyName = properties.get(i).getName() + ": ";
            float textX = x + 5;
            font.drawString(propertyName, textX, yPos + 11, 8.25F, isCurrent ? -1 : ColorUtility.brighter(ColorUtility.MUTED_COLOR, 0.3F));
            font.drawString(getPropertyValue(properties.get(i)), textX + font.getStringWidth(propertyName, 8.25F), yPos + 11, 8.25F,
                    isCurrent ? -1 : ColorUtility.brighter(ColorUtility.MUTED_COLOR, 0.3F));
        }
    }

    private String getPropertyValue(Property<?> property) {
        if (property instanceof BooleanProperty booleanProperty) {
            return String.valueOf(booleanProperty.getValue());
        } else if (property instanceof NumberProperty numberProperty) {
            return String.format("%.3f", numberProperty.getValue()).replaceAll("0+$", "").replaceAll("\\.$", "");
        } else if (property instanceof ModeProperty<?> modeProperty) {
            return String.valueOf(modeProperty.getValue());
        } else if (property instanceof MultipleBooleanProperty multipleBooleanProperty) {
            List<BooleanProperty> subProperties = multipleBooleanProperty.getValue();
            int selectedIndex = multipleBooleanProperty.getSubPropertyIndex();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < subProperties.size(); i++) {
                BooleanProperty p = subProperties.get(i);
                if (i > 0) sb.append(", ");
                if (i == selectedIndex) sb.append("**");
                sb.append(p.getName()).append(": ").append(p.getValue());
                if (i == selectedIndex) sb.append("**");
            }
            sb.append("]");
            return sb.toString();
        }
        return "";
    }

    private void handleUpKey(ModuleCategory category, Module module, List<Module> moduleList, List<Property<?>> propertyList) {
        if (!tabCategoryExpanded) {
            tabCategoryIndex = (tabCategoryIndex - 1 + ModuleCategory.VALUES.length) % ModuleCategory.VALUES.length;
        } else if (module != null && module.isExpanded() && !propertyList.isEmpty()) {
            cyclePropertyIndex(module, propertyList, -1);
        } else {
            cycleModuleIndex(category, moduleList, -1);
        }
    }

    private void handleDownKey(ModuleCategory category, Module module, List<Module> moduleList, List<Property<?>> propertyList) {
        if (!tabCategoryExpanded) {
            tabCategoryIndex = (tabCategoryIndex + 1) % ModuleCategory.VALUES.length;
        } else if (module != null && module.isExpanded() && !propertyList.isEmpty()) {
            cyclePropertyIndex(module, propertyList, 1);
        } else {
            cycleModuleIndex(category, moduleList, 1);
        }
    }

    private void handleRightKey(Module module, List<Property<?>> propertyList) {
        if (!tabCategoryExpanded) {
            tabCategoryExpanded = true;
            return;
        }
        if (module != null && !propertyList.isEmpty()) {
            Property<?> property = propertyList.get(module.getPropertyIndex());
            if (!property.isFocused()) {
                module.setExpanded(true);
            } else {
                modifyProperty(property, true);
            }
        }
    }

    private void handleLeftKey(Module module, List<Property<?>> propertyList) {
        if (tabCategoryExpanded && module != null && module.isExpanded()) {
            if (!propertyList.isEmpty() && !propertyList.get(module.getPropertyIndex()).isFocused()) {
                module.setExpanded(false);
            } else if (!propertyList.isEmpty()) {
                modifyProperty(propertyList.get(module.getPropertyIndex()), false);
            }
        } else {
            tabCategoryExpanded = false;
        }
    }

    private void handleEnterKey(Module module, List<Property<?>> propertyList) {
        if (!tabCategoryExpanded || module == null) return;
        if (!module.isExpanded()) {
            module.toggle();
        } else {
            Property<?> property = propertyList.get(module.getPropertyIndex());
            property.setFocused(!property.isFocused());
        }
    }

    private void handleTabKey(Module module, List<Property<?>> propertyList) {
        if (propertyList.isEmpty() || !tabCategoryExpanded || module == null) return;
        Property<?> property = propertyList.get(module.getPropertyIndex());
        if (property instanceof MultipleBooleanProperty multipleBooleanProperty) {
            if (property.isFocused()) {
                multipleBooleanProperty.cycleSubPropertyIndex();
            }
        }
    }

    private void cycleModuleIndex(ModuleCategory category, List<Module> moduleList, int direction) {
        if (moduleList.isEmpty()) return;
        category.setModuleIndex((category.getModuleIndex() + direction + moduleList.size()) % moduleList.size());
    }

    private void cyclePropertyIndex(Module module, List<Property<?>> propertyList, int direction) {
        module.setPropertyIndex((module.getPropertyIndex() + direction + propertyList.size()) % propertyList.size());
        propertyList.get(module.getPropertyIndex()).setFocused(false);
    }

    private void modifyProperty(Property<?> property, boolean increase) {
        if (property instanceof BooleanProperty booleanProperty) {
            booleanProperty.toggle();
        } else if (property instanceof NumberProperty numberProperty) {
            numberProperty.setValue(numberProperty.getValue() + (increase ? numberProperty.getIncrement() : -numberProperty.getIncrement()));
        } else if (property instanceof ModeProperty<?> modeProperty) {
            modeProperty.cycle(increase);
        } else if (property instanceof MultipleBooleanProperty multipleBooleanProperty) {
            final BooleanProperty selected = multipleBooleanProperty.getSelectedSubProperty();
            if (selected != null) selected.toggle();
        }
    }

}
