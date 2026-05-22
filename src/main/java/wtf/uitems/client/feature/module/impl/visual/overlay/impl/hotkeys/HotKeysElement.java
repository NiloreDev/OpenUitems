package wtf.uitems.client.feature.module.impl.visual.overlay.impl.hotkeys;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.binding.IBindable;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.impl.visual.overlay.IOverlayElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.repository.ImageRepository;
import wtf.uitems.client.renderer.image.NVGImageRenderer;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.client.feature.module.property.impl.ScreenPositionProperty;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HotKeysElement implements IOverlayElement {

    private static final NVGTextRenderer FONT = FontRepository.getFont("productsans-medium");
    private static final float FONT_SIZE = 8.5F;
    private static final Identifier KEYBOARD_ICON = Identifier.of("uitems", "icons/keyboard_icon.png");
    private static final NVGImageRenderer KEYBOARD_IMAGE = ImageRepository.getImage("icons/keyboard_icon.png");
    private static final NVGImageRenderer KEYBOARD_WHITE_IMAGE = loadWhiteKeyboard();
    private final HotKeysSettings settings;
    private final Animation appearAnimation = new Animation(Easing.EASE_OUT_EXPO, 250);
    private final java.util.Map<Module, Animation> itemAnimations = new java.util.HashMap<>();
    private final java.util.Map<Module, Float> itemLastY = new java.util.HashMap<>();

    public HotKeysElement(final HUDModule module) {
        this.settings = new HotKeysSettings(module);
    }

    @Override
    public void render(DrawContext context, float delta, boolean isBloom) {
        if (!settings.isEnabled()) return;

        final List<Module> activeBinds = new ArrayList<>();
        OpalClient.getInstance().getBindRepository().getBindingService().getBindingMap().asMap().forEach((key, bindables) -> {
            for (IBindable bindable : bindables) {
                if (bindable instanceof Module module && module.isEnabled()) {
                    activeBinds.add(module);
                }
            }
        });

        final float padding = 5;
        final float itemHeight = 12;
        final float headerHeight = 14;
        final float headerTextSize = 8.5F;
        final float iconSize = 10;
        final float iconGap = 5;
        final float paddingTop = 4;
        final float paddingBottom = 6;
        final String headerText = "HotKeys";

        float contentWidth = 90;
        if (!activeBinds.isEmpty()) {
            // do not prune here; allow out animations for recently disabled modules
            for (Module module : activeBinds) {
                if (!itemAnimations.containsKey(module)) {
                    final Animation anim = new Animation(Easing.EASE_OUT_EXPO, 250);
                    anim.setValue(0);
                    itemAnimations.put(module, anim);
                }
                String keyName = OpalClient.getInstance().getBindRepository().getNameFromInteger(
                    OpalClient.getInstance().getBindRepository().getBindingService().getKeyFromBindable(module).get().first
                );
                float textWidth = FONT.getStringWidth(module.getName() + " [" + keyName + "]", FONT_SIZE);
                contentWidth = Math.max(contentWidth, textWidth + padding * 2);
            }
        }
        // Also include disappearing items to keep container width stable during out animation
        if (!itemAnimations.isEmpty()) {
            for (final Module module : itemAnimations.keySet()) {
                if (activeBinds.contains(module)) continue;
                final String keyName = OpalClient.getInstance().getBindRepository().getNameFromInteger(
                        OpalClient.getInstance().getBindRepository().getBindingService().getKeyFromBindable(module).get().first
                );
                final float textWidth = FONT.getStringWidth(module.getName() + " [" + keyName + "]", FONT_SIZE);
                contentWidth = Math.max(contentWidth, textWidth + padding * 2);
            }
        }
        float headerWidth = iconSize + iconGap + FONT.getStringWidth(headerText, headerTextSize) + padding * 2;
        float width = Math.max(contentWidth, headerWidth);

        // Drive out animations for items no longer active and include animated height contribution
        float removingHeightFactor = 0F;
        if (!itemAnimations.isEmpty()) {
            for (final Map.Entry<Module, Animation> entry : itemAnimations.entrySet()) {
                final Module module = entry.getKey();
                if (activeBinds.contains(module)) continue;
                final Animation anim = entry.getValue();
                anim.run(0F);
                removingHeightFactor += Math.max(0F, Math.min(1F, anim.getValue()));
            }
        }
        float totalHeight = paddingTop + headerHeight + (activeBinds.size() * itemHeight) + (removingHeightFactor * itemHeight) + paddingBottom;
        final ScreenPositionProperty screenPosition = this.settings.getScreenPosition();
        screenPosition.setWidth(width);
        screenPosition.setHeight(totalHeight);
        float x = screenPosition.getScaledX();
        float y = screenPosition.getScaledY();

        appearAnimation.run(1);
        final float progress = Math.max(0, Math.min(1, appearAnimation.getValue()));
        final float offsetX = (1 - progress) * 8;
        NVGRenderer.globalAlpha(progress);
        final float drawX = x + offsetX;

        final Pair<Integer, Integer> theme = ColorUtility.getClientTheme();

        final HUDModule overlay = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);

        final boolean allowBlur = overlay.isBlur();
        final boolean allowBloom = overlay.isBloom();

        if (!isBloom && allowBlur) {
            NVGRenderer.roundedRect(drawX, y, width, totalHeight, 6F, NVGRenderer.BLUR_PAINT, overlay.getBlurSampleOpacity());
        }

        if (!isBloom && allowBlur && settings.isGlow()) {
            final long now = System.currentTimeMillis();
            final float t = (float) ((now % 4000L) / 4000.0);
            final float angle = t * 360F;
            final int a1 = (int) (90 + 50 * Math.sin(t * Math.PI * 2));
            final int a2 = (int) (70 + 60 * Math.sin((t + 0.25F) * Math.PI * 2));
            final int c1 = ColorUtility.applyOpacity(theme.first, Math.max(0, Math.min(255, a1)));
            final int c2 = ColorUtility.applyOpacity(theme.second, Math.max(0, Math.min(255, a2)));
            final float radius = 6F;
            NVGRenderer.roundedRectGradient(drawX, y, width, totalHeight, radius, c1, c2, angle);
            NVGRenderer.roundedRectGradient(drawX, y, width, totalHeight, radius, c2, c1, angle + 110F);
        }

        if (isBloom) {
            if (allowBloom) {
                NVGRenderer.roundedRect(drawX, y, width, totalHeight, 6, 0x32FFFFFF);
            }
        } else {
            NVGRenderer.roundedRect(drawX, y, width, totalHeight, 6, overlay.getBlurBackgroundColor());
            overlay.renderBlurTextureOverlay(drawX, y, width, totalHeight, 6F);
        }

        float headerY = y + paddingTop;
        float iconX = drawX + padding;
        float iconY = headerY + (headerHeight - iconSize) / 2.0F;
        int snappedIconX = Math.round(iconX);
        int snappedIconY = Math.round(iconY);
        KEYBOARD_WHITE_IMAGE.drawImage(snappedIconX, snappedIconY, iconSize, iconSize);

        float headerTextX = iconX + iconSize + iconGap;
        float headerTextY = headerY + headerHeight / 2.0F + 3.5F;
        FONT.drawString(headerText, headerTextX, headerTextY, headerTextSize, -1);

        if (!activeBinds.isEmpty()) {
            for (int i = 0; i < activeBinds.size(); i++) {
                Module module = activeBinds.get(i);
                String keyName = OpalClient.getInstance().getBindRepository().getNameFromInteger(
                    OpalClient.getInstance().getBindRepository().getBindingService().getKeyFromBindable(module).get().first
                );
                
                float currentY = y + paddingTop + headerHeight + i * itemHeight;
                itemLastY.put(module, currentY);
                
                int color = ColorUtility.interpolateColorsBackAndForth(
                    15, i * 20, theme.first, theme.second
                );

                final Animation itemAnim = itemAnimations.get(module);
                float itemProgress = 1F;
                if (itemAnim != null) {
                    itemAnim.run(1F);
                    itemProgress = itemAnim.getValue();
                }
                itemProgress = Math.max(0F, Math.min(1F, itemProgress));
                final float itemOffsetX = (1F - itemProgress) * 10F;
                NVGRenderer.globalAlpha(itemProgress);
                FONT.drawString(module.getName(), drawX + padding + itemOffsetX, currentY + itemHeight / 2.0F + 3.5F, FONT_SIZE, -1);
                FONT.drawString("[" + keyName + "]", drawX + width - padding - FONT.getStringWidth("[" + keyName + "]", FONT_SIZE) + itemOffsetX, currentY + itemHeight / 2.0F + 3.5F, FONT_SIZE, color);
                NVGRenderer.globalAlpha(1F);
            }
        }
        // Render disappearing items with out animation based on last recorded Y
        if (!itemAnimations.isEmpty()) {
            final List<Module> disappearing = new ArrayList<>();
            for (final Module module : itemAnimations.keySet()) {
                if (!activeBinds.contains(module)) {
                    disappearing.add(module);
                }
            }
            for (int i = 0; i < disappearing.size(); i++) {
                final Module module = disappearing.get(i);
                final Animation anim = itemAnimations.get(module);
                final float itemProgress = Math.max(0F, Math.min(1F, anim.getValue()));
                if (itemProgress <= 0.01F && anim.isFinished()) {
                    itemAnimations.remove(module);
                    itemLastY.remove(module);
                    continue;
                }
                final String keyName = OpalClient.getInstance().getBindRepository().getNameFromInteger(
                        OpalClient.getInstance().getBindRepository().getBindingService().getKeyFromBindable(module).get().first
                );
                final float lastY = itemLastY.getOrDefault(module, y + paddingTop + headerHeight + i * itemHeight);
                final int color = ColorUtility.interpolateColorsBackAndForth(15, i * 20, theme.first, theme.second);
                final float itemOffsetX = (1F - itemProgress) * 10F;
                NVGRenderer.globalAlpha(itemProgress);
                FONT.drawString(module.getName(), drawX + padding + itemOffsetX, lastY + itemHeight / 2.0F + 3.5F, FONT_SIZE, -1);
                FONT.drawString("[" + keyName + "]", drawX + width - padding - FONT.getStringWidth("[" + keyName + "]", FONT_SIZE) + itemOffsetX, lastY + itemHeight / 2.0F + 3.5F, FONT_SIZE, color);
                NVGRenderer.globalAlpha(1F);
            }
        }

        NVGRenderer.globalAlpha(1);
    }

    private static NVGImageRenderer loadWhiteKeyboard() {
        final Path pathURL = FabricLoader.getInstance().getModContainer("uitems")
                .flatMap(c -> c.findPath("assets/uitems/icons/keyboard_icon.png"))
                .orElse(null);
        try {
            if (pathURL != null) {
                final var nativeImage = net.minecraft.client.texture.NativeImage.read(Files.newInputStream(pathURL));
                return NVGImageRenderer.fromNativeImage(nativeImage, true);
            }
        } catch (Exception ignored) {
        }
        return KEYBOARD_IMAGE;
    }

    @Override
    public void onDisable() {
        appearAnimation.setValue(0);
    }

    @Override
    public boolean isBloom() {
        final HUDModule overlay = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        return overlay.isBloom();
    }
}
