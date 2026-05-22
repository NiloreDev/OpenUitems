package wtf.uitems.client.screen.click;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class AbstractClickGui extends Screen {
    @Getter
    public static boolean selectingBind, typingString;

    protected AbstractClickGui() {
        super(Text.empty());
    }

    @Override
    public final void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {}
    @Override
    public final void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {}

    public abstract void doRender(DrawContext context, int mouseX, int mouseY, float delta);
}
