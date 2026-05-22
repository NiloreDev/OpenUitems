package wtf.uitems.client.screen.click.dropdown.panel.property.impl;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.client.screen.click.dropdown.panel.property.PropertyPanel;
import wtf.uitems.utility.misc.HoverUtility;
import wtf.uitems.utility.render.ClientTheme;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;

public final class ModePropertyComponent extends PropertyPanel<ModeProperty<?>> {

    private static final float JELLO_CONTROL_WIDTH = 123F;
    private static final float JELLO_ROW_HEIGHT = 27F;

    private final Animation expandAnimation = new Animation(Easing.DECELERATE, 125);
    private boolean expanded;

    public ModePropertyComponent(ModeProperty<?> property) {
        super(property);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        expandAnimation.run(expanded ? 1 : 0);

        final NVGTextRenderer font = FontRepository.getFont("productsans-medium");
        final NVGTextRenderer fontBold = FontRepository.getFont("productsans-bold");

        if (PropertyPanel.isJelloStyle()) {
            final int visibleModes = getVisibleModeCount();
            final float expandedHeight = visibleModes * JELLO_ROW_HEIGHT * expandAnimation.getValue();
            setHeight(JELLO_ROW_HEIGHT + expandedHeight);

            final NVGTextRenderer labelFont = FontRepository.getFont("productsans-regular");
            final float controlX = x + width - JELLO_CONTROL_WIDTH;
            final float controlY = y;
            final float totalHeight = JELLO_ROW_HEIGHT + expandedHeight;

            labelFont.drawString(getProperty().getName(), x, y + 14F, 9F, 0xFF3F3F3F);

            if (expandAnimation.getValue() > 0.001F) {
                NVGRenderer.dropShadow(controlX, controlY, JELLO_CONTROL_WIDTH, totalHeight, 6F, 14F, 0F, ColorUtility.applyOpacity(0x000000, 0.08F * expandAnimation.getValue()));
                NVGRenderer.roundedRect(controlX, controlY, JELLO_CONTROL_WIDTH, totalHeight, 6F, ColorUtility.applyOpacity(0xFFFFFF, 0.5F));
                NVGRenderer.roundedRectOutline(controlX, controlY, JELLO_CONTROL_WIDTH, totalHeight, 6F, 0.6F, ColorUtility.applyOpacity(0x000000, 0.06F));
            }

            final String valueText = getProperty().getValue().toString();
            labelFont.drawString(valueText, controlX + 10F, y + 14F, 8F, 0xFF8F8F8F);
            if (getProperty().isTheme()) {
                final ClientTheme selectedTheme = ClientTheme.valueOf(getProperty().getValue().name());
                final Pair<Integer, Integer> colors = selectedTheme.getColors();
                final float valueWidth = labelFont.getStringWidth(valueText, 8F);
                final float colorX = Math.min(controlX + 10F + valueWidth + 8F, controlX + JELLO_CONTROL_WIDTH - 34F);
                NVGRenderer.roundedRect(colorX, y + 8.7F, 7F, 7F, 2.5F, colors.first);
                NVGRenderer.roundedRect(colorX + 9F, y + 8.7F, 7F, 7F, 2.5F, colors.second);
            }
            FontRepository.getFont("productsans-regular").drawString(">", controlX + JELLO_CONTROL_WIDTH - 12F, y + 11.9F, 10F, 0xFFC3C3C3, false, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);

            if (visibleModes > 0) {
                NVGRenderer.scissor(controlX, controlY, JELLO_CONTROL_WIDTH, totalHeight, () -> {
                    float optionY = y + JELLO_ROW_HEIGHT;
                    for (final Enum<?> mode : getProperty().getValues()) {
                        if (mode == null || mode == getProperty().getValue()) {
                            continue;
                        }

                        final float alpha = expandAnimation.getValue();
                        if (HoverUtility.isHovering(controlX, optionY, JELLO_CONTROL_WIDTH, JELLO_ROW_HEIGHT, mouseX, mouseY)) {
                            NVGRenderer.roundedRect(controlX + 3F, optionY + 2F, JELLO_CONTROL_WIDTH - 6F, JELLO_ROW_HEIGHT - 4F, 4F, ColorUtility.applyOpacity(0x000000, 0.035F * alpha));
                        }

                        labelFont.drawString(mode.toString(), controlX + 10F, optionY + 14F, 8F, ColorUtility.applyOpacity(0xFF9A9A9A, alpha));
                        if (getProperty().isTheme()) {
                            final ClientTheme selectedTheme = ClientTheme.valueOf(mode.name());
                            final Pair<Integer, Integer> colors = selectedTheme.getColors();
                            final float valueWidth = labelFont.getStringWidth(mode.toString(), 8F);
                            final float colorX = Math.min(controlX + 10F + valueWidth + 8F, controlX + JELLO_CONTROL_WIDTH - 34F);
                            NVGRenderer.roundedRect(colorX, optionY + 8.7F, 7F, 7F, 2.5F, ColorUtility.applyOpacity(colors.first, alpha));
                            NVGRenderer.roundedRect(colorX + 9F, optionY + 8.7F, 7F, 7F, 2.5F, ColorUtility.applyOpacity(colors.second, alpha));
                        }
                        optionY += JELLO_ROW_HEIGHT;
                    }
                });
            }
            return;
        }

        font.drawString(getProperty().getName(), x + 5, y + 9.5F, 7, getTitleColor());

        final float padding = 2;

        final float rectX = x + 3;
        final float rectY = y + padding + 11.5F;
        final float rectWidth = width - 5 - padding;
        if (PropertyPanel.modernStyle) {
        } else if (PropertyPanel.isJelloStyle()) {
            NVGRenderer.roundedRect(rectX, rectY, rectWidth, height - padding - (32 - DEFAULT_HEIGHT), 4, getFieldBackgroundColor());
            NVGRenderer.roundedRectOutline(rectX, rectY, rectWidth, height - padding - (32 - DEFAULT_HEIGHT), 4, 0.8F, getFieldOutlineColor());
        } else {
            NVGRenderer.roundedRect(rectX, rectY, rectWidth, height - padding - (32 - DEFAULT_HEIGHT), 4, ColorUtility.applyOpacity(0xff000000, 0.25F));
        }
        fontBold.drawString(getProperty().getValue().toString(), rectX + 4, rectY + 10, 7, PropertyPanel.isJelloStyle() ? getValueColor() : -1);

        if (getProperty().isTheme()) {
            final ClientTheme selectedTheme = ClientTheme.valueOf(getProperty().getValue().name());

            final Pair<Integer, Integer> colors = selectedTheme.getColors();
            final float valueWidth = fontBold.getStringWidth(getProperty().getValue().toString(), 7);

            NVGRenderer.roundedRect(rectX + valueWidth + 7, rectY + 4F, 7, 7, 2, colors.first);
            NVGRenderer.roundedRect(rectX + valueWidth + 16, rectY + 4F, 7, 7, 2, colors.second);
        }

        final String expandIcon = "\ue5cf";
        final NVGTextRenderer iconFont = FontRepository.getFont("materialicons-regular");
        final float iconSize = 9;
        final float iconWidth = iconFont.getStringWidth(expandIcon, iconSize);
        NVGRenderer.rotate(
                expandAnimation.getValue() * 180,
                rectX + rectWidth - 12,
                rectY + 2.5F,
                iconWidth,
                iconSize,
                () -> iconFont.drawString("\ue5cf", 0, 0, iconSize, -1, false, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE)
        );

        NVGRenderer.scissor(rectX, rectY, rectWidth, height - padding - (32 - DEFAULT_HEIGHT), () -> {
            int addedHeight = 0;
            if (expandAnimation.getValue() > 0) {
                for (final Enum<?> mode : getProperty().getValues()) {
                    if (mode == null || mode == getProperty().getValue()) continue;

                        font.drawString(mode.toString(), rectX + 4, rectY + 9.5F + 13 + addedHeight, 7, PropertyPanel.isJelloStyle() ? getValueColor() : -1);

                    if (getProperty().isTheme()) {
                        final ClientTheme selectedTheme = ClientTheme.valueOf(mode.name());

                        final Pair<Integer, Integer> colors = selectedTheme.getColors();

                        NVGRenderer.roundedRect(rectX + width - 5 - padding - 20.5F, rectY + 3.5F + 13 + addedHeight, 7, 7, 2.5F, colors.first);
                        NVGRenderer.roundedRect(rectX + width - 5 - padding - 12, rectY + 3.5F + 13 + addedHeight, 7, 7, 2.5F, colors.second);
                    }

                    addedHeight += 13;
                }
            }

            setHeight(32 + addedHeight * expandAnimation.getValue());
        });
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (PropertyPanel.isJelloStyle()) {
            final float controlX = x + width - JELLO_CONTROL_WIDTH;

            if (HoverUtility.isHovering(controlX, y, JELLO_CONTROL_WIDTH, JELLO_ROW_HEIGHT, mouseX, mouseY) && button == 1) {
                expanded = !expanded;
                return;
            }

            if (expanded) {
                float optionY = y + JELLO_ROW_HEIGHT;
                for (final Enum<?> mode : getProperty().getValues()) {
                    if (mode == null || mode == getProperty().getValue()) {
                        continue;
                    }

                    if (HoverUtility.isHovering(controlX, optionY, JELLO_CONTROL_WIDTH, JELLO_ROW_HEIGHT, mouseX, mouseY) && button == 0) {
                        getProperty().setValueOrdinal(mode.ordinal());
                        expanded = false;
                        return;
                    }
                    optionY += JELLO_ROW_HEIGHT;
                }
            }
            return;
        }

        if (HoverUtility.isHovering(x, y, width, 32, mouseX, mouseY) && button == 1) {
            expanded = !expanded;
            return;
        }

        if (expanded) {
            final float padding = 2;

            final float rectX = x + 3;
            final float rectY = y + padding + 11.5F;
            final float rectWidth = width - 8 - padding - (6 / 2F);

            int addedHeight = 0;
            for (final Enum<?> mode : getProperty().getValues()) {
                if (mode == null || mode.ordinal() == getProperty().getValue().ordinal()) continue;

                if (HoverUtility.isHovering(rectX, rectY + 13 + addedHeight, rectWidth, 13, mouseX, mouseY)) {
                    getProperty().setValueOrdinal(mode.ordinal());
                    expanded = false;
                }

                addedHeight += 13;
            }
        }
    }

    private int getVisibleModeCount() {
        int count = 0;
        for (final Enum<?> mode : getProperty().getValues()) {
            if (mode != null && mode != getProperty().getValue()) {
                count++;
            }
        }
        return count;
    }
}
