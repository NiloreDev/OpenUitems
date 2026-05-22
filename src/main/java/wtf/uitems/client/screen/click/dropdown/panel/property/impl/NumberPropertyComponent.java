package wtf.uitems.client.screen.click.dropdown.panel.property.impl;

import net.minecraft.client.gui.DrawContext;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.client.screen.click.dropdown.panel.property.PropertyPanel;
import wtf.uitems.utility.misc.HoverUtility;
import wtf.uitems.utility.misc.math.MathUtility;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

public final class NumberPropertyComponent extends PropertyPanel<NumberProperty> {

    private static final float JELLO_SLIDER_WIDTH = 126F;
    private static final float JELLO_ROW_HEIGHT = 24F;

    private boolean dragging;

    private Animation dragAnimation;

    public NumberPropertyComponent(final NumberProperty property) {
        super(property);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        setHeight(PropertyPanel.isJelloStyle() ? JELLO_ROW_HEIGHT : 26);

        super.render(context, mouseX, mouseY, delta);

        final NumberProperty property = getProperty();
        final NVGTextRenderer font = FontRepository.getFont("productsans-medium");

        font.drawString(property.getName(), x + (PropertyPanel.isJelloStyle() ? 0F : 5F), y + (PropertyPanel.isJelloStyle() ? 12.5F : 8.5F), PropertyPanel.isJelloStyle() ? 9F : 7F, getTitleColor());

        final float sliderWidth = PropertyPanel.isJelloStyle() ? JELLO_SLIDER_WIDTH : width - 12;
        final float sliderHeight = PropertyPanel.isJelloStyle() ? 4F : 2.5F;

        final float sliderX = PropertyPanel.isJelloStyle() ? x + width - sliderWidth : x + 6;
        final float sliderY = PropertyPanel.isJelloStyle() ? y + height / 2F - sliderHeight / 2F : y + 13F;

        if (dragging && mouseX != -1) {
            final float percent = Math.min(1, Math.max(0, (mouseX - (sliderX)) / sliderWidth));
            property.setValue(MathUtility.interpolate(property.getMinValue(), property.getMaxValue(), percent));
        }

        final double widthPercent = ((property.getValue()) - property.getMinValue()) / (property.getMaxValue() - property.getMinValue());

        final double destination = sliderWidth * widthPercent;
        if (this.dragAnimation == null) {
            this.dragAnimation = new Animation(Easing.LINEAR, 50);
            this.dragAnimation.setValue((float) destination);
        } else if (dragging) {
            this.dragAnimation.setValue((float) destination);
            this.dragAnimation.setFinished(true);
        } else {
            this.dragAnimation.run((float) destination);
        }

        NVGRenderer.roundedRect(sliderX, sliderY, sliderWidth, sliderHeight, sliderHeight / 2f, PropertyPanel.isJelloStyle() ? 0xFFD8E7FB : getTrackColor());

        final float dragAnim = dragAnimation.getValue();
        if (dragAnim > 1) {
            if (PropertyPanel.isJelloStyle()) {
                NVGRenderer.roundedRect(sliderX, sliderY, dragAnim, sliderHeight, sliderHeight / 2f, 0xFF4DA1FF);
            } else {
                final int color = getAccentColor();
                NVGRenderer.roundedRectGradient(sliderX, sliderY, dragAnim, sliderHeight, sliderHeight / 2f, color, ColorUtility.darker(color, 0.5F), 90);
            }
        }

        if (PropertyPanel.isJelloStyle()) {
            NVGRenderer.dropShadow(sliderX + dragAnim - 6F, sliderY - 4F, 12F, 12F, 6F, 10F, 0F, ColorUtility.applyOpacity(0x000000, 0.12F));
            NVGRenderer.circle(sliderX + dragAnim, sliderY + sliderHeight / 2F, 6F, 0xFFFFFFFF);
        } else {
            final int handleColor = -1;
            NVGRenderer.roundedRectGradient(sliderX + dragAnim - 1, sliderY - 1.3f, 2, 5, 1, handleColor, ColorUtility.darker(handleColor, 0.1F), 90);
        }

        final Number value = getProperty().getValue();

        String valueString;
        if (value.doubleValue() == value.intValue()) {
            valueString = String.valueOf(value.intValue());
        } else {
            valueString = String.format("%.3f", value.doubleValue()).replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        if (getProperty().getSuffix() != null) {
            valueString += getProperty().getSuffix();
        }

        if (PropertyPanel.isJelloStyle()) {
            font.drawString(valueString, sliderX - 10F - font.getStringWidth(valueString, 7F), y + 12.5F, 7F, 0xFF9A9A9A);
        } else {
            font.drawString(valueString, sliderX + dragAnim - (font.getStringWidth(valueString, 5.5F) / 2), y + 22f, 5.5F, getValueColor());
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (HoverUtility.isHovering(x, y, width, height, mouseX, mouseY) && button == 0)
            dragging = true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0)
            dragging = false;
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (HoverUtility.isHovering(x, y, width, height, mouseX, mouseY)) {
            if (horizontalAmount != 0) {
                NumberProperty property = getProperty();
                property.setValue(property.getValue() + horizontalAmount * property.getIncrement());
            }
        }
    }
}
