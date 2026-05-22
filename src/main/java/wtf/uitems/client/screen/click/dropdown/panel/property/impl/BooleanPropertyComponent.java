package wtf.uitems.client.screen.click.dropdown.panel.property.impl;

import com.ibm.icu.impl.Pair;
import net.minecraft.client.gui.DrawContext;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.component.ToggleSwitchComponent;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.client.screen.click.dropdown.panel.property.PropertyPanel;
import wtf.uitems.utility.render.ColorUtility;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;

public final class BooleanPropertyComponent extends PropertyPanel<BooleanProperty> {

    private final ToggleSwitchComponent toggleSwitch;

    public BooleanPropertyComponent(BooleanProperty property) {
        super(property);

        toggleSwitch = new ToggleSwitchComponent(property::toggle, property::getValue);
    }

    @Override
    public void init() {
        toggleSwitch.reset();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (PropertyPanel.isJelloStyle()) {
            setHeight(24);

            final NVGTextRenderer font = FontRepository.getFont("productsans-regular");
            font.drawString(getProperty().getName(), x, y + 12.5F, 9F, 0xFF3F3F3F);

            final float boxSize = 15F;
            final float boxX = x + width - boxSize - 2F;
            final float boxY = y + (height - boxSize) / 2F;
            final int fillColor = getProperty().getValue() ? 0xFF42A5FF : 0xFFE4E4E4;

            NVGRenderer.dropShadow(boxX, boxY, boxSize, boxSize, boxSize / 2F, 10F, 0F, ColorUtility.applyOpacity(0x000000, getProperty().getValue() ? 0.1F : 0.05F));
            NVGRenderer.roundedRect(boxX, boxY, boxSize, boxSize, boxSize / 2F, fillColor);

            if (getProperty().getValue()) {
                FontRepository.getFont("materialicons-regular")
                        .drawString("\ue5ca", boxX + boxSize / 2F, boxY + boxSize / 2F + 0.5F, 9F, 0xFFFFFFFF, false, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            }
            return;
        } else {
            FontRepository.getFont("productsans-medium").drawString(getProperty().getName(), x + 5, y + 10.5F, 7, getTitleColor());
            final Pair<Integer, Integer> themeColors = ColorUtility.getClientTheme();
            toggleSwitch.setBoxColors(Pair.of(themeColors.first, ColorUtility.applyOpacity(themeColors.second, 0.45F)));
            toggleSwitch.render(x + 88, y + 3.8F, 0.85F);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (PropertyPanel.isJelloStyle()) {
            if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                getProperty().toggle();
            }
            return;
        }

        if (button == 0) {
            toggleSwitch.mouseClicked(x, y, width, height, mouseX, mouseY);
        }
    }

}
