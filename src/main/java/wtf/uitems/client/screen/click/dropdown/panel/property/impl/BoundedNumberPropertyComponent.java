package wtf.uitems.client.screen.click.dropdown.panel.property.impl;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import wtf.uitems.client.feature.module.property.impl.number.BoundedNumberProperty;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.client.screen.click.dropdown.panel.property.PropertyPanel;
import wtf.uitems.utility.misc.HoverUtility;
import wtf.uitems.utility.misc.math.MathUtility;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.animation.Animation;
import wtf.uitems.utility.render.animation.Easing;

public final class BoundedNumberPropertyComponent extends PropertyPanel<BoundedNumberProperty> {

    private static final float JELLO_SLIDER_WIDTH = 126F;
    private static final float JELLO_ROW_HEIGHT = 24F;

    private boolean draggingLow, draggingHigh;
    private Animation draggingLowAnimation;
    private Animation draggingHighAnimation;

    public BoundedNumberPropertyComponent(final BoundedNumberProperty property) {
        super(property);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        setHeight(PropertyPanel.isJelloStyle() ? JELLO_ROW_HEIGHT : 26);

        super.render(context, mouseX, mouseY, delta);

        final BoundedNumberProperty property = getProperty();
        final NVGTextRenderer font = FontRepository.getFont("productsans-medium");

        font.drawString(property.getName(), x + (PropertyPanel.isJelloStyle() ? 0F : 5F), y + (PropertyPanel.isJelloStyle() ? 12.5F : 8.5F), PropertyPanel.isJelloStyle() ? 9F : 7F, getTitleColor());

        final Pair<Double, Double> values = property.getValue();
        final Double lowValue = values.first;
        final Double highValue = values.second;

        final float sliderWidth = PropertyPanel.isJelloStyle() ? JELLO_SLIDER_WIDTH : width - 12;
        final float sliderHeight = PropertyPanel.isJelloStyle() ? 4F : 2.5F;

        final float sliderX = PropertyPanel.isJelloStyle() ? x + width - sliderWidth : x + 6;
        final float sliderY = PropertyPanel.isJelloStyle() ? y + height / 2F - sliderHeight / 2F : y + 13F;

        final float percent = Math.min(1, Math.max(0, (mouseX - sliderX) / sliderWidth));

        if (mouseX != -1) {
            final double newValue = MathUtility.interpolate(property.getMinValue(), property.getMaxValue(), percent);
            if (draggingLow) {
                if (newValue <= highValue) {
                    property.setValue(Pair.of(newValue, highValue));
                }
            }
            if (draggingHigh) {
                if (newValue >= lowValue) {
                    property.setValue(Pair.of(lowValue, newValue));
                }
            }
        }

        final double lowPercent = (lowValue - property.getMinValue()) / (property.getMaxValue() - property.getMinValue());
        final double highPercent = (highValue - property.getMinValue()) / (property.getMaxValue() - property.getMinValue());

        final double lowDestination = sliderWidth * lowPercent;
        if (this.draggingLowAnimation == null) {
            this.draggingLowAnimation = new Animation(Easing.LINEAR, 50);
            this.draggingLowAnimation.setValue((float) lowDestination);
        } else if (draggingLow) {
            this.draggingLowAnimation.setValue((float) lowDestination);
            this.draggingLowAnimation.setFinished(true);
        } else {
            this.draggingLowAnimation.run((float) lowDestination);
        }

        final double highDestination = sliderWidth * highPercent;
        if (this.draggingHighAnimation == null) {
            this.draggingHighAnimation = new Animation(Easing.LINEAR, 50);
            this.draggingHighAnimation.setValue((float) highDestination);
        } else if (draggingHigh) {
            this.draggingHighAnimation.setValue((float) highDestination);
            this.draggingHighAnimation.setFinished(true);
        } else {
            this.draggingHighAnimation.run((float) highDestination);
        }

        NVGRenderer.roundedRect(sliderX, sliderY, sliderWidth, sliderHeight, sliderHeight / 2f, PropertyPanel.isJelloStyle() ? 0xFFD8E7FB : getTrackColor());

        final float lowAnim = draggingLowAnimation.getValue();
        final float highAnim = draggingHighAnimation.getValue();
        if (highAnim > lowAnim) {
            if (PropertyPanel.isJelloStyle()) {
                NVGRenderer.roundedRect(sliderX + lowAnim, sliderY, highAnim - lowAnim, sliderHeight, sliderHeight / 2f, 0xFF4DA1FF);
            } else {
                final int color = getAccentColor();
                NVGRenderer.roundedRectGradient(sliderX + lowAnim, sliderY, highAnim - lowAnim, sliderHeight, sliderHeight / 2f, color, ColorUtility.darker(color, 0.5F), 90);
            }
        }

        if (PropertyPanel.isJelloStyle()) {
            NVGRenderer.dropShadow(sliderX + lowAnim - 6F, sliderY - 4F, 12F, 12F, 6F, 10F, 0F, ColorUtility.applyOpacity(0x000000, 0.12F));
            NVGRenderer.dropShadow(sliderX + highAnim - 6F, sliderY - 4F, 12F, 12F, 6F, 10F, 0F, ColorUtility.applyOpacity(0x000000, 0.12F));
            NVGRenderer.circle(sliderX + lowAnim, sliderY + sliderHeight / 2F, 6F, 0xFFFFFFFF);
            NVGRenderer.circle(sliderX + highAnim, sliderY + sliderHeight / 2F, 6F, 0xFFFFFFFF);
        } else {
            final int handleColor = -1;
            NVGRenderer.roundedRectGradient(sliderX + lowAnim - 1, sliderY - 1.3f, 2, 5, 1, handleColor, ColorUtility.darker(handleColor, 0.1F), 90);
            NVGRenderer.roundedRectGradient(sliderX + highAnim - 1, sliderY - 1.3f, 2, 5, 1, handleColor, ColorUtility.darker(handleColor, 0.1F), 90);
        }

        String lowValueString;
        if (lowValue == lowValue.intValue()) {
            lowValueString = String.valueOf(lowValue.intValue());
        } else {
            lowValueString = String.format("%.3f", lowValue).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        String highValueString;
        if (highValue == highValue.intValue()) {
            highValueString = String.valueOf(highValue.intValue());
        } else {
            highValueString = String.format("%.3f", highValue).replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        if (getProperty().getSuffix() != null) {
            lowValueString += getProperty().getSuffix();
            highValueString += getProperty().getSuffix();
        }

        if (PropertyPanel.isJelloStyle()) {
            final String valueText = lowValueString + " - " + highValueString;
            font.drawString(valueText, sliderX - 10F - font.getStringWidth(valueText, 7F), y + 12.5F, 7F, 0xFF9A9A9A);
        } else {
            font.drawString(lowValueString, sliderX + lowAnim - (font.getStringWidth(lowValueString, 5.5F) / 2), y + 22f, 5.5F, getValueColor());
            font.drawString(highValueString, sliderX + highAnim - (font.getStringWidth(highValueString, 5.5F) / 2), y + 22f, 5.5F, getValueColor());
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (HoverUtility.isHovering(x, y, width, height, mouseX, mouseY) && button == 0) {
            final float sliderWidth = PropertyPanel.isJelloStyle() ? JELLO_SLIDER_WIDTH : width - 12;
            final float sliderX = PropertyPanel.isJelloStyle() ? x + width - sliderWidth : x + 6;

            final BoundedNumberProperty property = getProperty();

            final Pair<Double, Double> values = property.getValue();
            final double lowPercent = (values.first - property.getMinValue()) /
                    (property.getMaxValue() - property.getMinValue());
            final double highPercent = (values.second - property.getMinValue()) /
                    (property.getMaxValue() - property.getMinValue());

            final float lowSliderX = sliderX + (float) (sliderWidth * lowPercent);
            final float highSliderX = sliderX + (float) (sliderWidth * highPercent);

            final double lowSliderDiff = Math.abs(mouseX - lowSliderX);
            final double highSliderDiff = Math.abs(mouseX - highSliderX);
            if (lowSliderDiff == highSliderDiff) {
                if (mouseX < lowSliderX) {
                    draggingLow = true;
                } else {
                    draggingHigh = true;
                }
            } else if (lowSliderDiff < highSliderDiff) {
                draggingLow = true;
            } else {
                draggingHigh = true;
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0)
            draggingHigh = draggingLow = false;
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (HoverUtility.isHovering(x, y, width, height, mouseX, mouseY)) {
            if (horizontalAmount != 0) {
                BoundedNumberProperty property = getProperty();
                final float sliderWidth = PropertyPanel.isJelloStyle() ? JELLO_SLIDER_WIDTH : width - 12;
                final float sliderX = PropertyPanel.isJelloStyle() ? x + width - sliderWidth : x + 6;
                final float midPointX = (float) (sliderX + sliderWidth * property.getMidpoint() / (property.getMaxValue()) - property.getMinValue());

                double inc = horizontalAmount * property.getIncrement();
                Pair<Double, Double> value = property.getValue();
                final Double lowValue = value.first;
                final Double highValue = value.second;
                property.setValue(
                        Pair.of(
                                mouseX <= midPointX? Math.min(lowValue + inc, highValue) : lowValue,
                                mouseX >= midPointX? Math.max(highValue + inc, lowValue) : highValue
                        )
                );
            }
        }
    }
}
