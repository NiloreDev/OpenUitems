package wtf.uitems.client.screen.click.dropdown;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.screen.click.AbstractClickGui;
import wtf.uitems.client.screen.click.dropdown.panel.CategoryPanel;
import wtf.uitems.utility.player.PlayerUtility;

import java.util.ArrayList;
import java.util.List;

import static wtf.uitems.client.Constants.mc;

public final class DropdownClickGUI extends AbstractClickGui {

    private final List<CategoryPanel> categoryPanelList = new ArrayList<>();
    public static boolean displayingBinds;

    public DropdownClickGUI() {
        int index = 0;
        for (ModuleCategory category : ModuleCategory.VALUES) {
            categoryPanelList.add(new CategoryPanel(category, index));

            index++;
        }
    }

    @Override
    public void doRender(DrawContext context, int mouseX, int mouseY, float delta) {
        final boolean frameStarted = NVGRenderer.beginFrame();

        displayingBinds = PlayerUtility.isKeyPressed(GLFW.GLFW_KEY_TAB);

        final int categoryAmount = categoryPanelList.size();
        for (int i = 0; i < categoryAmount; i++) {
            final CategoryPanel panel = categoryPanelList.get(i);

            final float y = 25;
            final float width = 110;
            final float spacing = 10;
            final float height = 20;

            final float totalWidth = categoryAmount * width + (categoryAmount - 1) * spacing;

            final float startX = (mc.getWindow().getScaledWidth() - totalWidth) / 2;
            final float x = startX + i * (width + spacing);

            panel.setDimensions(x, y, width, height);
            panel.render(context, mouseX, mouseY, delta);
        }

        if (frameStarted) {
            NVGRenderer.endFrameAndReset(true);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        categoryPanelList.forEach(categoryPanel -> categoryPanel.mouseClicked(click.x(), click.y(), click.button()));

        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        categoryPanelList.forEach(categoryPanel -> categoryPanel.mouseReleased(click.x(), click.y(), click.button()));

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (horizontalAmount == 0 && verticalAmount != 0) {
            if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
                horizontalAmount = verticalAmount;
                verticalAmount = 0;
            }
        }

        double finalHorizontalAmount = horizontalAmount;
        double finalVerticalAmount = verticalAmount;

        categoryPanelList.forEach(categoryPanel -> categoryPanel.mouseScrolled(mouseX, mouseY, finalHorizontalAmount, finalVerticalAmount));

        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        wtf.uitems.utility.KeyInput myKeyInput = new wtf.uitems.utility.KeyInput(keyInput.key(), keyInput.modifiers());

        categoryPanelList.forEach(categoryPanel -> categoryPanel.keyPressed(myKeyInput));

        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        categoryPanelList.forEach(categoryPanel -> categoryPanel.charTyped((char) charInput.codepoint(), charInput.modifiers()));
        return true;
    }

    @Override
    protected void init() {
        categoryPanelList.forEach(CategoryPanel::init);
    }

    @Override
    public void close() {
        if (selectingBind) {
            return;
        }

        categoryPanelList.forEach(CategoryPanel::close);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

}
