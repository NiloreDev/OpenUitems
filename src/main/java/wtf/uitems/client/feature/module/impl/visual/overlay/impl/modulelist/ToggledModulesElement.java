package wtf.uitems.client.feature.module.impl.visual.overlay.impl.modulelist;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.impl.visual.overlay.IOverlayElement;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.*;
import static wtf.uitems.client.Constants.mc;

public final class ToggledModulesElement implements IOverlayElement {

    private final ToggledSettings settings;
    private final Animation outlineHeightAnimation = new Animation(Easing.EASE_OUT_EXPO, 240);
    private boolean outlineTransitionPending;

    public ToggledModulesElement(final HUDModule module) {
        this.settings = new ToggledSettings(module);
    }

    private List<ModuleElement> moduleList = new ArrayList<>(), visibleList = new ArrayList<>();

    public void initialize() {
        Collection<Module> moduleList = OpalClient.getInstance().getModuleRepository().getModules();
        this.moduleList = new ArrayList<>(moduleList.size());
        this.visibleList = new ArrayList<>(moduleList.size());
        moduleList.forEach(m -> this.moduleList.add(new ModuleElement(this.settings, m)));
        this.tick();
        this.sort();
        this.markSortingDirty();
    }

    public float getTotalHeight() {
        float height = 0;
        for (final ModuleElement element : this.visibleList) {
            final Animation heightAnimation = element.getHeightAnimation();
            if (heightAnimation != null) {
                height += element.getOffset() * heightAnimation.getValue();
            }
        }
        return height * this.settings.getScale();
    }

    public ToggledSettings getSettings() {
        return this.settings;
    }

    private boolean sortingDirty;

    public void markSortingDirty() {
        this.sortingDirty = true;
    }

    private void sort() {
        Collections.sort(this.moduleList);
        this.sortingDirty = false;
    }

    @Override
    public void render(DrawContext context, float delta, boolean isBloom) {
        this.renderPass(context, isBloom);
    }

    @Override
    public void renderBlur(DrawContext context, float delta) {
//        this.renderPass(true);
    }

    private void renderPass(final DrawContext context, final boolean isBloom) {
        if (this.sortingDirty) {
            this.sort();
        }

        final var screenPosition = this.settings.getScreenPosition();
        float maxWidth = 0;
        for (final ModuleElement element : this.visibleList) {
            maxWidth = Math.max(maxWidth, element.getWidth() + 6.5F);
        }
        screenPosition.setWidth(maxWidth * this.settings.getScale());
        screenPosition.setHeight(this.getTotalHeight());

        final int size = this.visibleList.size();
        for (int i = 0; i < size; i++) {
            final ModuleElement element = this.visibleList.get(i);
            final float previousWidth = i > 0 ? this.visibleList.get(i - 1).getWidth() + 6.5F : -1F;
            final float nextWidth = i + 1 < size ? this.visibleList.get(i + 1).getWidth() + 6.5F : 0F;
            element.render(context, i, isBloom, previousWidth, nextWidth);
        }

        if (!isBloom && this.settings.getBarMode().getValue() == ToggledSettings.BarMode.OUTLINE) {
            renderOutlinePath();
        }
    }

    private void renderOutlinePath() {
        final List<OutlineNode> outlineNodes = this.moduleList.stream()
                .filter(ModuleElement::isOutlineVisible)
                .map(element -> new OutlineNode(
                        element.getCurrentRenderX(),
                        element.getCurrentRenderY(),
                        element.getCurrentRenderWidth(),
                        element.getCurrentRenderHeight()
                ))
                .toList();

        if (outlineNodes.isEmpty()) {
            this.outlineHeightAnimation.run(0F);
            return;
        }

        final float horizontalInset = 1.0F;
        final float topInset = 0.0F;
        final float bottomInset = 1.0F;
        final Pair<Integer, Integer> colors = ColorUtility.getClientTheme();
        final OutlineNode first = outlineNodes.getFirst();
        final OutlineNode last = outlineNodes.getLast();
        final float totalWidth = this.settings.getScreenPosition().getWidth() / this.settings.getScale();
        final float listLeft = first.x - horizontalInset;
        final float targetOutlineHeight = (last.y + last.height + bottomInset) - (first.y - topInset);
        this.outlineHeightAnimation.run(Math.max(0F, targetOutlineHeight));
        final float visibleOutlineHeight = this.outlineHeightAnimation.getValue();

        if (visibleOutlineHeight <= 0.5F) {
            return;
        }

        final long vg = NVGRenderer.getContext();
        NVGRenderer.applyColor(ColorUtility.applyOpacity(colors.first, 0.85F), NVGRenderer.NVG_COLOR_1);
        NVGRenderer.applyColor(ColorUtility.applyOpacity(colors.second, 0.85F), NVGRenderer.NVG_COLOR_2);
        nvgLinearGradient(
                vg,
                listLeft,
                first.y,
                listLeft + totalWidth,
                first.y,
                NVGRenderer.NVG_COLOR_1,
                NVGRenderer.NVG_COLOR_2,
                NVGRenderer.NVG_PAINT
        );

        NVGRenderer.scissor(
                listLeft - 2F,
                first.y - topInset - 2F,
                totalWidth + 4F,
                visibleOutlineHeight + 4F,
                () -> {
                    nvgBeginPath(vg);
                    nvgStrokePaint(vg, NVGRenderer.NVG_PAINT);
                    nvgStrokeWidth(vg, 1F);
                    nvgLineCap(vg, NVG_BUTT);
                    nvgLineJoin(vg, NVG_ROUND);

                    boolean started = false;
                    for (int i = 0; i < outlineNodes.size(); i++) {
                        final OutlineNode current = outlineNodes.get(i);
                        final float x = current.x - horizontalInset + 0.5F;
                        final float y = current.y - topInset;
                        final float width = current.width + horizontalInset;
                        final float height = current.height + topInset + bottomInset;
                        final float bottomY = y + height - 1F;
                        final float nextWidth = i + 1 < outlineNodes.size() ? outlineNodes.get(i + 1).width : 0F;
                        final float exposedBottomWidth = i + 1 < outlineNodes.size()
                                ? Math.max(0F, width - (nextWidth + horizontalInset))
                                : width;

                        if (!started) {
                            nvgMoveTo(vg, x, y);
                            started = true;
                        }

                        nvgLineTo(vg, x, bottomY);

                        if (exposedBottomWidth > 0F) {
                            nvgLineTo(vg, x + exposedBottomWidth, bottomY);
                        }
                    }

                    nvgStroke(vg);
                    nvgClosePath(vg);
                }
        );
    }

    private record OutlineNode(float x, float y, float width, float height) {
    }

    @Override
    public void tick() {
        this.visibleList.clear();
        final boolean outlineMode = this.settings.getBarMode().getValue() == ToggledSettings.BarMode.OUTLINE;
        final List<ModuleElement> moduleSnapshot = new ArrayList<>(this.moduleList);

        if (!outlineMode) {
            int index = 0;
            boolean requiresResort = false;
            for (final ModuleElement element : moduleSnapshot) {
                final boolean visible = element.isModuleVisible();
                if (element.tick(index, visible)) {
                    requiresResort = true;
                }
                if (element.isVisible()) {
                    this.visibleList.add(element);
                    if (visible) {
                        index++;
                    }
                }
            }

            this.outlineTransitionPending = false;
            if (requiresResort) {
                this.markSortingDirty();
            }
            return;
        }

        final boolean outlineChanged = outlineMode && this.moduleList.stream().anyMatch(ModuleElement::hasOutlineStateChange);
        if (outlineChanged) {
            this.outlineTransitionPending = true;
        }
        final boolean allowDisplaySync = (!outlineChanged && this.outlineTransitionPending && this.outlineHeightAnimation.isFinished())
                || !this.outlineTransitionPending;

        int displayIndex = 0;
        int outlineIndex = 0;
        boolean requiresResort = false;
        for (final ModuleElement element : moduleSnapshot) {
            if (element.tick(displayIndex, outlineIndex, outlineMode, allowDisplaySync)) {
                requiresResort = true;
            }
            if (element.isDisplayReady()) {
                this.visibleList.add(element);
            }
            if (element.isDisplayVisible()) {
                displayIndex++;
            }
            if (element.isOutlineVisible()) {
                outlineIndex++;
            }
        }

        if (outlineMode && this.outlineTransitionPending && allowDisplaySync
                && this.moduleList.stream().noneMatch(ModuleElement::hasPendingDisplaySync)) {
            this.outlineTransitionPending = false;
        }

        if (requiresResort) {
            this.markSortingDirty();
        }
    }

    @Override
    public boolean isActive() {
        return !mc.getDebugHud().shouldShowDebugHud() && this.settings.isEnabled();
    }

    @Override
    public boolean isBloom() {
        return true;
    }
}
