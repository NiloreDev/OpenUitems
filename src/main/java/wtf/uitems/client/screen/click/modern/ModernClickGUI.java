package wtf.uitems.client.screen.click.modern;

import com.ibm.icu.impl.Pair;
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
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

import java.util.*;
import java.util.stream.Collectors;

import static wtf.uitems.client.Constants.mc;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;

public final class ModernClickGUI extends AbstractClickGui {
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private final Animation openAnimation = new Animation(Easing.EASE_OUT_QUINT, 250);
    private boolean closing;

    private float x, y, width, height;
    private final Scroller scroller = new Scroller();
    
    private final Map<Module, Boolean> expandedModules = new HashMap<>();
    private final Map<Module, List<PropertyPanel<?>>> propertyPanels = new HashMap<>();
    private final Map<Module, Animation> hoverAnimations = new HashMap<>();
    private final Map<Module, Animation> expandAnimations = new HashMap<>();
    private final Map<ModuleCategory, Animation> categoryHoverAnimations = new HashMap<>();
    private final Map<ModuleCategory, Animation> categorySelectionAnimations = new HashMap<>();
    private final Animation categorySelectorAnimation = new Animation(Easing.EASE_OUT_QUINT, 300);
    
    private final Animation logoSwitchAnimation = new Animation(Easing.EASE_IN_OUT_QUAD, 1200);
    private long lastLogoSwitchTime = System.currentTimeMillis();
    private boolean logoState = false; // false = Text, true = Logo

    @Override
    protected void init() {
        super.init();
        closing = false;
        openAnimation.reset();
        scroller.getAnimation().reset();
    }

    @Override
    public void doRender(DrawContext context, int mouseX, int mouseY, float delta) {

        final boolean isBloom = mouseX == -1 && mouseY == -1;
        final boolean frameStarted = !isBloom && NVGRenderer.beginFrame();

        if (!isBloom) {
            openAnimation.run(closing ? 0 : 1);

            if (closing && openAnimation.isFinished()) {
                mc.setScreen(null);
                OpalClient.getInstance().getModuleRepository().getModule(ClickGUIModule.class).setEnabled(false);

                if (frameStarted) NVGRenderer.endFrameAndReset(true);
                return;
            }
        }

        final float animationValue = openAnimation.getValue();

        this.width = 550;
        this.height = 380;
        this.x = (mc.getWindow().getScaledWidth() - width) / 2;
        this.y = (mc.getWindow().getScaledHeight() - height) / 2;

        NVGRenderer.globalAlpha(animationValue);

        final Pair<Integer, Integer> themeColors = ColorUtility.getClientTheme();
        final int themePrimary = ColorUtility.applyOpacity(themeColors.first, 1.0F);
        final int themeSecondary = ColorUtility.applyOpacity(themeColors.second, 1.0F);
        final long tod = mc.world != null ? mc.world.getTimeOfDay() : 0L;
        final boolean isNight = mc.world != null && ((tod % 24000L) >= 12000L && (tod % 24000L) <= 24000L);
        final int baseTextColor = isNight ? 0xFFFFFFFF : 0xFF000000;

        if (!isBloom) {
            NVGRenderer.dropShadow(x, y, width, height, 12, 35, 4, ColorUtility.applyOpacity(0x000000, 0.45F));
        }
        NVGRenderer.roundedRect(x, y, width, height, 12, NVGRenderer.BLUR_PAINT);
        final float containerAlpha = 0.18F;
        NVGRenderer.roundedRectGradient(x, y, width, height, 12, ColorUtility.applyOpacity(themeColors.first, containerAlpha), ColorUtility.applyOpacity(themeColors.second, containerAlpha), 0);

        // Sidebar
        float sidebarWidth = 130;
        final float sidebarAlpha = 0.12F;
        NVGRenderer.roundedRectVarying(x, y, sidebarWidth, height, 12, 0, 0, 12, NVGRenderer.BLUR_PAINT);
        NVGRenderer.roundedRectVaryingGradient(x, y, sidebarWidth, height, 12, 0, 0, 12, ColorUtility.applyOpacity(themeColors.first, sidebarAlpha), ColorUtility.applyOpacity(themeColors.second, sidebarAlpha), 90);

        // Logo
        if (!isBloom) {
            if (System.currentTimeMillis() - lastLogoSwitchTime > 4000) {
                logoState = !logoState;
                lastLogoSwitchTime = System.currentTimeMillis();
            }
            logoSwitchAnimation.run(logoState ? 1 : 0);
        }

        final float logoSwitchValue = logoSwitchAnimation.getValue();
        final float sidebarCenterX = x + sidebarWidth / 2f;
        final float logoY = y + 30;
        final float logoMaxRevealWidth = 70; // Slightly wider for better transition

        final float logoDrawSize = 24;

        // Use the center as the absolute reference point
        final float cursorX = sidebarCenterX - (logoMaxRevealWidth / 2f) + (logoMaxRevealWidth * logoSwitchValue);

        NVGRenderer.globalAlpha(animationValue);

        // Draw Uitems text (centered on sidebarCenterX)
        NVGRenderer.scissor(cursorX, logoY - 15, logoMaxRevealWidth - (logoMaxRevealWidth * logoSwitchValue), 30, () -> {
            drawUitemsText(sidebarCenterX, logoY, 15, isBloom, themeColors);
        });

        // Draw Logo (centered on sidebarCenterX)
        NVGRenderer.scissor(sidebarCenterX - (logoMaxRevealWidth / 2f), logoY - 15, logoMaxRevealWidth * logoSwitchValue, 30, () -> {
            drawUitemsLogo(sidebarCenterX - logoDrawSize / 2f, logoY - 12, logoDrawSize, themeColors.first, themeColors.second);
        });

        // Draw animated cursor line with smooth fade
        if (!isBloom) {
            float cursorAlpha = 0;
            if (logoSwitchValue > 0.0F && logoSwitchValue < 0.2F) {
                cursorAlpha = logoSwitchValue / 0.2F; // Fade in
            } else if (logoSwitchValue >= 0.2F && logoSwitchValue <= 0.8F) {
                cursorAlpha = 1.0F; // Fully visible
            } else if (logoSwitchValue > 0.8F && logoSwitchValue < 1.0F) {
                cursorAlpha = (1.0F - logoSwitchValue) / 0.2F; // Fade out
            }

            if (cursorAlpha > 0) {
                int cursorColor = ColorUtility.applyOpacity(themePrimary, cursorAlpha * animationValue);
                NVGRenderer.roundedRect(cursorX - 1.0F, logoY - 12, 2.0F, 24, 1.0F, cursorColor);
            }
        }

        NVGRenderer.globalAlpha(animationValue);

        // Categories
        float categoryY = y + 55;
        final float mX_cat = (float) (mc.mouse.getX() * (double) mc.getWindow().getScaledWidth() / (double) mc.getWindow().getFramebufferWidth());
        final float mY_cat = (float) (mc.mouse.getY() * (double) mc.getWindow().getScaledHeight() / (double) mc.getWindow().getFramebufferHeight());

        // Draw selection background with movement animation
        if (!isBloom) {
            categorySelectorAnimation.run(selectedCategory.ordinal());
        }
        final float selectorValue = categorySelectorAnimation.getValue();
        final float selectorY = y + 55 + selectorValue * 38;

        if (isBloom) {
            final float bloomAlpha = 0.22F;
            NVGRenderer.roundedRect(x + 10, selectorY, sidebarWidth - 20, 32, 8, NVGRenderer.BLUR_PAINT);
            NVGRenderer.roundedRectGradient(x + 8F, selectorY - 2.0F, sidebarWidth - 16, 36, 10,
                    ColorUtility.applyOpacity(themeColors.first, bloomAlpha * 0.7F),
                    ColorUtility.applyOpacity(themeColors.second, bloomAlpha * 0.7F), 0);
            NVGRenderer.roundedRectOutline(x + 10, selectorY, sidebarWidth - 20, 32, 8, 1.0F,
                    ColorUtility.applyOpacity(themeColors.first, bloomAlpha));
        } else {
            final float selAlpha = 0.22F;
            NVGRenderer.roundedRect(x + 10, selectorY, sidebarWidth - 20, 32, 8, NVGRenderer.BLUR_PAINT);
            NVGRenderer.roundedRect(x + 10, selectorY, sidebarWidth - 20, 32, 8, ColorUtility.applyOpacity(0xFFFFFFFF, 0.08F));
            NVGRenderer.roundedRectGradient(x + 10, selectorY, sidebarWidth - 20, 32, 8, ColorUtility.applyOpacity(themeColors.first, selAlpha), ColorUtility.applyOpacity(themeColors.second, selAlpha), 0);
            NVGRenderer.roundedRectOutline(x + 10, selectorY, sidebarWidth - 20, 32, 8, 0.8F, ColorUtility.applyOpacity(themeColors.first, 0.15F));
            NVGRenderer.roundedRectGradient(x + 14, selectorY + 8, 3, 16, 1.5F, themePrimary, themeSecondary, 90);
        }

        for (ModuleCategory category : ModuleCategory.VALUES) {
            boolean hovered = HoverUtility.isHovering(x + 10, categoryY, sidebarWidth - 20, 32, mX_cat, mY_cat);
            boolean selected = category == selectedCategory;

            final Animation catHoverAnim = categoryHoverAnimations.computeIfAbsent(category, c -> new Animation(Easing.EASE_OUT_QUAD, 180));
            final Animation catSelectAnim = categorySelectionAnimations.computeIfAbsent(category, c -> new Animation(Easing.EASE_OUT_QUAD, 250));

            if (!isBloom) {
                catHoverAnim.run(hovered ? 1 : 0);
                catSelectAnim.run(selected ? 1 : 0);
            }
            final float chv = catHoverAnim.getValue();
            final float csv = catSelectAnim.getValue();

            // Apply lift effect
            float lift = -1.5F * chv;
            float currentCategoryY = categoryY + lift;

            if (isBloom) {
                if (chv > 0.001F) {
                    final float bloomAlpha = 0.06F * chv;
                    NVGRenderer.roundedRectOutline(x + 10, currentCategoryY, sidebarWidth - 20, 32, 8, 0.8F,
                            ColorUtility.applyOpacity(themeColors.first, bloomAlpha));
                }
            } else {
                if (!selected && chv > 0.001F) {
                    // Hover state: Subtle white overlay like modules, with no extra blur to avoid "erasing" background
                    NVGRenderer.roundedRect(x + 10, currentCategoryY, sidebarWidth - 20, 32, 8, ColorUtility.applyOpacity(0xFFFFFFFF, 0.05F * chv));
                    NVGRenderer.roundedRectOutline(x + 10, currentCategoryY, sidebarWidth - 20, 32, 8, 0.5F, ColorUtility.applyOpacity(0xFFFFFFFF, 0.12F * chv));
                }
            }

            final NVGTextRenderer categoryFont = FontRepository.getFont(csv > 0.5F ? "productsans-bold" : "productsans-medium");
            NVGRenderer.globalAlpha(animationValue);

            if (isNight) {
                int glow = ColorUtility.applyOpacity(-1, (isBloom ? 0.35F : 0.28F) * (0.7F + 0.3F * csv));
                categoryFont.drawString(category.getName(), x + 44 - 0.5F, currentCategoryY + 17, 8.5F, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                categoryFont.drawString(category.getName(), x + 44 + 0.5F, currentCategoryY + 17, 8.5F, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                categoryFont.drawString(category.getName(), x + 44, currentCategoryY + 17 - 0.5F, 8.5F, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                categoryFont.drawString(category.getName(), x + 44, currentCategoryY + 17 + 0.5F, 8.5F, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                NVGTextRenderer iconFont = FontRepository.getFont("materialicons-outlined");
                iconFont.drawString(category.getIcon(), x + 22 - 0.5F, currentCategoryY + 16, 11, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                iconFont.drawString(category.getIcon(), x + 22 + 0.5F, currentCategoryY + 16, 11, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                iconFont.drawString(category.getIcon(), x + 22, currentCategoryY + 16 - 0.5F, 11, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                iconFont.drawString(category.getIcon(), x + 22, currentCategoryY + 16 + 0.5F, 11, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
            }

            if (isBloom) {
                float bloomAlpha = (0.15F * csv + 0.1F * chv);

                // Icon bloom
                int iconColor = ColorUtility.applyOpacity(ColorUtility.interpolateColorsBackAndForth(10, 0, themeColors.first, themeColors.second), bloomAlpha);
                FontRepository.getFont("materialicons-outlined").drawString(category.getIcon(), x + 22, currentCategoryY + 16, 11, iconColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);

                // Text bloom with gradient
                String name = category.getName();
                float offset = 0;
                for (int i = 0; i < name.length(); i++) {
                    String character = String.valueOf(name.charAt(i));
                    int charColor = ColorUtility.applyOpacity(ColorUtility.interpolateColorsBackAndForth(10, i * 15, themeColors.first, themeColors.second), bloomAlpha);
                    categoryFont.drawString(character, x + 44 + offset, currentCategoryY + 17, 8.5F, charColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    offset += categoryFont.getStringWidth(character, 8.5F);
                }
            } else {
                // Icon
                int iconTargetColor = ColorUtility.interpolateColorsBackAndForth(10, 0, themeColors.first, themeColors.second);
                int iconColor = ColorUtility.interpolateColors(baseTextColor, iconTargetColor, csv);
                FontRepository.getFont("materialicons-outlined").drawString(category.getIcon(), x + 22, currentCategoryY + 16, 11, iconColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);

                // Text with gradient
                String name = category.getName();
                float offset = 0;
                for (int i = 0; i < name.length(); i++) {
                    String character = String.valueOf(name.charAt(i));
                    int charTargetColor = ColorUtility.interpolateColorsBackAndForth(10, i * 15, themeColors.first, themeColors.second);
                    int charColor = ColorUtility.interpolateColors(baseTextColor, charTargetColor, csv);
                    categoryFont.drawString(character, x + 44 + offset, currentCategoryY + 17, 8.5F, charColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    offset += categoryFont.getStringWidth(character, 8.5F);
                }
            }
            NVGRenderer.globalAlpha(animationValue);

            categoryY += 38;
        }

        // Modules area
        float modulesX = x + sidebarWidth + 20;
        float modulesY = y + 20;
        float modulesWidth = width - sidebarWidth - 40;
        float modulesHeight = height - 40;

        NVGTextRenderer headerFont = FontRepository.getFont("productsans-bold");
        NVGRenderer.globalAlpha(animationValue);
        if (isNight) {
            int glow = ColorUtility.applyOpacity(-1, 0.32F);
            String headerName = selectedCategory.getName();
            float hOffset = 0;
            for (int i = 0; i < headerName.length(); i++) {
                String character = String.valueOf(headerName.charAt(i));
                headerFont.drawString(character, modulesX + hOffset - 0.6F, modulesY + 5, 12, glow, true, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                headerFont.drawString(character, modulesX + hOffset + 0.6F, modulesY + 5, 12, glow, true, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                headerFont.drawString(character, modulesX + hOffset, modulesY + 5 - 0.6F, 12, glow, true, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                headerFont.drawString(character, modulesX + hOffset, modulesY + 5 + 0.6F, 12, glow, true, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                hOffset += headerFont.getStringWidth(character, 12);
            }
        }

        String headerName = selectedCategory.getName();
        float hOffset = 0;
        for (int i = 0; i < headerName.length(); i++) {
            String character = String.valueOf(headerName.charAt(i));
            int charGradientColor = ColorUtility.interpolateColorsBackAndForth(10, i * 15, themeColors.first, themeColors.second);

            if (isBloom) {
                int bloomColor = ColorUtility.applyOpacity(charGradientColor, 0.45F);
                headerFont.drawString(character, modulesX + hOffset, modulesY + 5, 12, bloomColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
            } else {
                headerFont.drawString(character, modulesX + hOffset, modulesY + 5, 12, charGradientColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
            }
            hOffset += headerFont.getStringWidth(character, 12);
        }

        // Scissors for module list
        NVGRenderer.scissor(modulesX - 5, modulesY + 25, modulesWidth + 10, modulesHeight - 25, () -> {
            List<Module> modules = OpalClient.getInstance().getModuleRepository().getModulesInCategory(selectedCategory).stream().collect(Collectors.toList());
            float currentModuleY = modulesY + 35 + scroller.getAnimation().getValue();

            for (Module module : modules) {
                final float mX = (float) (mc.mouse.getX() * (double) mc.getWindow().getScaledWidth() / (double) mc.getWindow().getFramebufferWidth());
                final float mY = (float) (mc.mouse.getY() * (double) mc.getWindow().getScaledHeight() / (double) mc.getWindow().getFramebufferHeight());
                boolean hovered = HoverUtility.isHovering(modulesX + 5, currentModuleY, modulesWidth - 10, 40, mX, mY);
                boolean expanded = expandedModules.getOrDefault(module, false);

                final Animation expandAnim = expandAnimations.computeIfAbsent(module, m -> new Animation(Easing.EASE_OUT_QUINT, 350));
                if (!isBloom) {
                    expandAnim.run(expanded ? 1 : 0);
                }
                final float ev = expandAnim.getValue();

                float moduleHeight = 40 + (calculateModuleHeight(module) - 45) * ev;
                final Animation hoverAnim = hoverAnimations.computeIfAbsent(module, m -> new Animation(Easing.EASE_OUT_QUAD, 180));
                if (!isBloom) {
                    hoverAnim.run(hovered ? 1 : 0);
                }
                final float hv = hoverAnim.getValue();

                // Add card margin and lift effect
                final float cardX = modulesX + 5;
                final float cardWidth = modulesWidth - 10;
                final float lift = hv * -4.0F;
                final float cardY = currentModuleY + lift;

                if (isBloom) {
                    if (module.isEnabled() || hv > 0.01F) {
                        final float bloomAlpha = (module.isEnabled() ? 0.22F : 0.08F * hv);
                        final float spread = module.isEnabled() ? 1.5F : 1.0F;

                        // Render a slightly larger rect to create an independent "outer glow" effect
                        NVGRenderer.roundedRectGradient(cardX - spread, cardY - spread, cardWidth + spread * 2, moduleHeight + spread * 2, 8 + spread,
                                ColorUtility.applyOpacity(themeColors.first, bloomAlpha * 0.6F),
                                ColorUtility.applyOpacity(themeColors.second, bloomAlpha * 0.6F), 0);

                        // Also render the outline in bloom pass with thinner stroke
                        final int bloomOutlineColor = ColorUtility.applyOpacity(themeColors.first, bloomAlpha);
                        NVGRenderer.roundedRectOutline(cardX, cardY, cardWidth, moduleHeight, 8, 0.8F, bloomOutlineColor);
                    }
                } else {
                    if (module.isEnabled()) {
                        NVGRenderer.roundedRect(cardX, cardY, cardWidth, moduleHeight, 8, ColorUtility.applyOpacity(0xFF1A1A1A, 1.0F));
                        NVGRenderer.roundedRect(cardX, cardY, cardWidth, moduleHeight, 8, NVGRenderer.BLUR_PAINT);
                        final long now = System.currentTimeMillis();
                        final float t = (float) ((now % 4000L) / 4000.0);
                        final float angle = 0F;
                        final int a1 = (int) (90 + 50 * Math.sin(t * Math.PI * 2));
                        final int a2 = (int) (70 + 60 * Math.sin((t + 0.25F) * Math.PI * 2));
                        final int c1 = ColorUtility.applyOpacity(themeColors.first, Math.max(0, Math.min(255, a1)));
                        final int c2 = ColorUtility.applyOpacity(themeColors.second, Math.max(0, Math.min(255, a2)));

                        // Dynamic background with depth gradient
                        NVGRenderer.roundedRectGradient(cardX, cardY, cardWidth, moduleHeight, 8, c1, c2, angle);
                        NVGRenderer.roundedRectGradient(cardX, cardY, cardWidth, moduleHeight, 8, c2, c1, angle);

                        // Subtle depth overlay (lighter at top, darker at bottom)
                        NVGRenderer.roundedRectGradient(cardX, cardY, cardWidth, moduleHeight, 8,
                                ColorUtility.applyOpacity(0xFFFFFFFF, 0.08F),
                                ColorUtility.applyOpacity(0x00000000, 0.08F), 90);

                        final int outlineColor = ColorUtility.applyOpacity(themeColors.first, (0.3F + hv * 0.4F));
                        NVGRenderer.roundedRectOutline(cardX, cardY, cardWidth, moduleHeight, 8, 1.2F, outlineColor);

                        // Inner highlight for 3D effect
                        final int highlightColor = ColorUtility.applyOpacity(0xFFFFFFFF, (0.12F + 0.08F * hv));
                        NVGRenderer.roundedRectOutline(cardX + 0.5F, cardY + 0.5F, cardWidth - 1, moduleHeight - 1, 8, 0.5F, highlightColor);
                    } else {
                        NVGRenderer.roundedRect(cardX, cardY, cardWidth, moduleHeight, 8, NVGRenderer.BLUR_PAINT);
                        final float cardAlpha = (0.28F + hv * 0.15F);
                        NVGRenderer.roundedRectGradient(cardX, cardY, cardWidth, moduleHeight, 8,
                                ColorUtility.applyOpacity(themeColors.first, cardAlpha),
                                ColorUtility.applyOpacity(themeColors.second, cardAlpha), 0);

                        // Subtle depth overlay for disabled cards
                        NVGRenderer.roundedRectGradient(cardX, cardY, cardWidth, moduleHeight, 8,
                                ColorUtility.applyOpacity(0xFFFFFFFF, 0.05F),
                                ColorUtility.applyOpacity(0x00000000, 0.05F), 90);

                        final int outlineColor = ColorUtility.applyOpacity(themeColors.first, (0.15F + hv * 0.45F));
                        NVGRenderer.roundedRectOutline(cardX, cardY, cardWidth, moduleHeight, 8, 1.2F, outlineColor);

                        // Inner highlight for 3D effect
                        final int highlightColor = ColorUtility.applyOpacity(0xFFFFFFFF, (0.08F + 0.08F * hv));
                        NVGRenderer.roundedRectOutline(cardX + 0.5F, cardY + 0.5F, cardWidth - 1, moduleHeight - 1, 8, 0.5F, highlightColor);
                    }
                }

                // Module info
                final NVGTextRenderer nameFont = FontRepository.getFont(module.isEnabled() ? "productsans-bold" : "productsans-medium");
                final int nameColor = baseTextColor;
                final int descColor = baseTextColor;
                NVGRenderer.globalAlpha(animationValue);
                if (isNight) {
                    int glow = ColorUtility.applyOpacity(-1, 0.28F);
                    nameFont.drawString(module.getName(), cardX + 15 - 0.5F, cardY + 14, 9, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    nameFont.drawString(module.getName(), cardX + 15 + 0.5F, cardY + 14, 9, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    nameFont.drawString(module.getName(), cardX + 15, cardY + 14 - 0.5F, 9, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    nameFont.drawString(module.getName(), cardX + 15, cardY + 14 + 0.5F, 9, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    NVGTextRenderer descFont = FontRepository.getFont("productsans-regular");
                    descFont.drawString(module.getDescription(), cardX + 15 - 0.4F, cardY + 28, 7, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    descFont.drawString(module.getDescription(), cardX + 15 + 0.4F, cardY + 28, 7, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    descFont.drawString(module.getDescription(), cardX + 15, cardY + 28 - 0.4F, 7, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    descFont.drawString(module.getDescription(), cardX + 15, cardY + 28 + 0.4F, 7, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                }
                if (isBloom) {
                    final float moduleBloomAlpha = (module.isEnabled() ? 0.45F : 0.18F * hv);
                    final int bloomColor = ColorUtility.applyOpacity(themeColors.first, moduleBloomAlpha);
                    nameFont.drawString(module.getName(), cardX + 15, cardY + 14, 9, bloomColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    FontRepository.getFont("productsans-regular").drawString(module.getDescription(), cardX + 15, cardY + 28, 7, bloomColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                } else {
                    nameFont.drawString(module.getName(), cardX + 15, cardY + 14, 9, nameColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    FontRepository.getFont("productsans-regular").drawString(module.getDescription(), cardX + 15, cardY + 28, 7, descColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                }
                NVGRenderer.globalAlpha(animationValue);

                // Expand icon
                if (!module.getPropertyList().isEmpty()) {
                    final int iconColor = baseTextColor;
                    NVGRenderer.globalAlpha(animationValue);
                    if (isNight) {
                        int glow = ColorUtility.applyOpacity(-1, 0.28F);
                        NVGTextRenderer iconFont = FontRepository.getFont("materialicons-outlined");
                        float rotation = ev * 180;
                        // Use ev to interpolate icon or rotation if needed, but for simplicity we'll just use rotation if the engine supports it
                        // Since we don't have a direct rotation parameter in drawString, we use the icon change
                        iconFont.drawString(ev > 0.5F ? "\ue5ce" : "\ue5cf", cardX + cardWidth - 25 - 0.4F, cardY + 20, 10, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                        iconFont.drawString(ev > 0.5F ? "\ue5ce" : "\ue5cf", cardX + cardWidth - 25 + 0.4F, cardY + 20, 10, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                        iconFont.drawString(ev > 0.5F ? "\ue5ce" : "\ue5cf", cardX + cardWidth - 25, cardY + 20 - 0.4F, 10, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                        iconFont.drawString(ev > 0.5F ? "\ue5ce" : "\ue5cf", cardX + cardWidth - 25, cardY + 20 + 0.4F, 10, glow, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    }
                    if (isBloom) {
                        final float iconBloomAlpha = (module.isEnabled() || hv > 0.01F ? 0.45F : 0.15F);
                        final int bloomColor = ColorUtility.applyOpacity(themeColors.first, iconBloomAlpha);
                        FontRepository.getFont("materialicons-outlined").drawString(ev > 0.5F ? "\ue5ce" : "\ue5cf", cardX + cardWidth - 25, cardY + 20, 10, bloomColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    } else {
                        FontRepository.getFont("materialicons-outlined").drawString(ev > 0.5F ? "\ue5ce" : "\ue5cf", cardX + cardWidth - 25, cardY + 20, 10, iconColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
                    }
                    NVGRenderer.globalAlpha(animationValue);
                }

                // Render properties if expanded
                if (ev > 0.001F) {
                    NVGRenderer.globalAlpha(ev * animationValue);
                    renderProperties(context, module, cardX + 10, cardY + 45, cardWidth - 20, mouseX, mouseY, delta);
                    NVGRenderer.globalAlpha(animationValue);
                }

                currentModuleY += moduleHeight + 14;
            }

            float totalHeight = calculateTotalHeight(modules);
            scroller.onScroll(Math.max(0, totalHeight - (modulesHeight - 35)));
        });

        NVGRenderer.globalAlpha(1.0F);

        if (frameStarted) {
            NVGRenderer.endFrameAndReset(true);
        }
    }

    private float calculateModuleHeight(Module module) {
        float height = 45;
        List<PropertyPanel<?>> panels = getPropertyPanels(module);
        for (PropertyPanel<?> panel : panels) {
            if (!panel.isHidden()) {
                height += panel.getHeight();
            }
        }
        return height + 5;
    }

    private float calculateTotalHeight(List<Module> modules) {
        float total = 0;
        for (Module module : modules) {
            final Animation expandAnim = expandAnimations.get(module);
            final float ev = expandAnim != null ? expandAnim.getValue() : (expandedModules.getOrDefault(module, false) ? 1.0F : 0.0F);
            total += (40 + (calculateModuleHeight(module) - 45) * ev) + 14;
        }
        return total;
    }

    private List<PropertyPanel<?>> getPropertyPanels(Module module) {
        return propertyPanels.computeIfAbsent(module, m -> {
            List<PropertyPanel<?>> panels = new ArrayList<>();
            for (Property<?> property : m.getPropertyList()) {
                PropertyPanel<?> panel = property.createClickGUIComponent();
                if (panel != null) {
                    panels.add(panel);
                }
            }
            return panels;
        });
    }

    private void renderProperties(DrawContext context, Module module, float x, float y, float width, int mouseX, int mouseY, float delta) {
        final boolean isBloom = mouseX == -1 && mouseY == -1;
        if (isBloom) return;
        
        wtf.uitems.client.screen.click.dropdown.panel.property.PropertyPanel.modernStyle = true;
        List<PropertyPanel<?>> panels = getPropertyPanels(module);
        float currentY = y;
        for (PropertyPanel<?> panel : panels) {
            if (panel.isHidden()) continue;
            panel.setX(x);
            panel.setY(currentY);
            panel.setWidth(width);
            panel.render(context, mouseX, mouseY, delta);
            currentY += panel.getHeight();
        }
        wtf.uitems.client.screen.click.dropdown.panel.property.PropertyPanel.modernStyle = false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        float sidebarWidth = 130;
        float categoryY = y + 55;
        
        // Sidebar clicks
        for (ModuleCategory category : ModuleCategory.VALUES) {
            if (HoverUtility.isHovering(x + 10, categoryY, sidebarWidth - 20, 32, mouseX, mouseY)) {
                if (selectedCategory != category) {
                    selectedCategory = category;
                    scroller.getAnimation().reset();
                }
                return true;
            }
            categoryY += 38;
        }
        
        // Module area clicks
        float modulesX = x + sidebarWidth + 20;
        float modulesY = y + 20;
        float modulesWidth = width - sidebarWidth - 40;
        float modulesHeight = height - 40;
        
        // Ensure clicks only affect the visible scissored region
        if (!HoverUtility.isHovering(modulesX - 5, modulesY + 25, modulesWidth + 10, modulesHeight - 25, mouseX, mouseY)) {
            return super.mouseClicked(click, doubled);
        }
        
        List<Module> modules = OpalClient.getInstance().getModuleRepository().getModulesInCategory(selectedCategory).stream().collect(Collectors.toList());
        float currentModuleY = modulesY + 35 + scroller.getAnimation().getValue();
        
        for (Module module : modules) {
            boolean expanded = expandedModules.getOrDefault(module, false);
            float moduleHeight = expanded ? calculateModuleHeight(module) : 40;
            float headerHeight = 40;
            // Match hit-test to visual card position (including hover lift and margins)
            final Animation hoverAnim = hoverAnimations.computeIfAbsent(module, m -> new Animation(Easing.EASE_OUT_QUAD, 180));
            final float hv = hoverAnim.getValue();
            final float lift = hv * -4.0F;
            final float cardX = modulesX + 5;
            final float cardY = currentModuleY + lift;
            final float cardWidth = modulesWidth - 10;
            
            if (HoverUtility.isHovering(cardX, cardY, cardWidth, headerHeight, mouseX, mouseY)) {
                if (button == 0) {
                    module.toggle();
                } else if (button == 1 && !module.getPropertyList().isEmpty()) {
                    expandedModules.put(module, !expanded);
                }
                return true;
            }
            
            if (expanded && HoverUtility.isHovering(cardX + 10, cardY + 45, cardWidth - 20, moduleHeight - 45, mouseX, mouseY)) {
                getPropertyPanels(module).forEach(p -> p.mouseClicked(mouseX, mouseY, button));
                return true;
            }
            
            currentModuleY += moduleHeight + 14;
        }
        
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float sidebarWidth = 130;
        float modulesX = x + sidebarWidth + 20;
        float modulesY = y + 20;
        float modulesWidth = width - sidebarWidth - 40;
        float modulesHeight = height - 40;

        if (HoverUtility.isHovering(modulesX, modulesY, modulesWidth, modulesHeight, mouseX, mouseY)) {
            List<Module> modules = OpalClient.getInstance().getModuleRepository().getModulesInCategory(selectedCategory).stream().collect(Collectors.toList());
            float totalHeight = calculateTotalHeight(modules);
            scroller.addScroll(verticalAmount, Math.max(0, totalHeight - (modulesHeight - 35)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        int modifiers = keyInput.modifiers();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        
        wtf.uitems.utility.KeyInput myKeyInput = new wtf.uitems.utility.KeyInput(keyCode, modifiers);
        propertyPanels.values().forEach(panels -> panels.forEach(p -> p.keyPressed(myKeyInput)));
        
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        propertyPanels.values().forEach(panels -> panels.forEach(p -> p.charTyped((char) charInput.codepoint(), charInput.modifiers())));
        return super.charTyped(charInput);
    }

    @Override
    public void close() {
        if (selectingBind) return;
        
        closing = true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        propertyPanels.values().forEach(panels -> panels.forEach(p -> p.mouseReleased(click.x(), click.y(), click.button())));
        return super.mouseReleased(click);
    }

    private void drawUitemsLogo(float x, float y, float size, int color1, int color2) {
        float padding = size * 0.15f;
        float innerSize = size - padding * 2;
        float strokeWidth = size * 0.12f;

        // Draw outer U shape
        NVGRenderer.roundedRectVaryingGradient(
                x + padding, y + padding,
                innerSize, innerSize,
                0, 0, innerSize / 2f, innerSize / 2f,
                color1, color2, 0
        );

        // Mask the middle part of the U
        NVGRenderer.roundedRectVarying(
                x + padding + strokeWidth, y + padding,
                innerSize - strokeWidth * 2, innerSize - strokeWidth,
                0, 0, (innerSize - strokeWidth * 2) / 2.2f, (innerSize - strokeWidth * 2) / 2.2f,
                ColorUtility.applyOpacity(0xFF090909, 0.45F)
        );
    }

    private void drawUitemsText(float x, float y, float size, boolean isBloom, Pair<Integer, Integer> themeColors) {
        NVGTextRenderer logoFont = FontRepository.getFont("productsans-bold");
        String name = "Uitems";
        float totalWidth = logoFont.getStringWidth(name, size);
        float drawX = x - totalWidth / 2f; // Center it based on total width
        float offset = 0;
        for (int i = 0; i < name.length(); i++) {
            String character = String.valueOf(name.charAt(i));
            int charColor = ColorUtility.interpolateColorsBackAndForth(10, i * 20, themeColors.first, themeColors.second);
            if (isBloom) {
                charColor = ColorUtility.applyOpacity(charColor, 0.45F);
            }
            logoFont.drawString(character, drawX + offset, y, size, charColor, false, NVG_ALIGN_MIDDLE | NVG_ALIGN_LEFT);
            offset += logoFont.getStringWidth(character, size);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
