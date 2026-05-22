package wtf.uitems.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Style;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.renderer.background.BackgroundRenderer;
import wtf.uitems.utility.misc.RunnableClickEvent;

import static wtf.uitems.client.Constants.mc;

@Mixin(Screen.class)
public final class ScreenMixin {

    private ScreenMixin() {
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Object screen = (Object) this;
        final String className = screen.getClass().getName();
        final boolean isOptionsSubpage = className.startsWith("net.minecraft.client.gui.screen.option");
        if (screen instanceof SelectWorldScreen || screen instanceof MultiplayerScreen || screen instanceof OptionsScreen || isOptionsSubpage) {
            BackgroundRenderer.render(context, mouseX, mouseY, delta);
            ci.cancel();
        }
    }

    @Inject(method = "handleTextClick", at = @At(value = "HEAD"), cancellable = true)
    private void onInvalidClickEvent(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style.getClickEvent() instanceof RunnableClickEvent runnableClickEvent) {
            runnableClickEvent.getRunnable().run();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Object screen = (Object) this;
        if (screen instanceof TitleScreen && OpalClient.getInstance() != null && OpalClient.getInstance().isUseVanillaMainMenu()) {
            int x = 6;
            int y = 6;
            int w = 110;
            int h = 18;
            int bg = 0x99111114;
            int fg = 0xFFFFFFFF;
            context.fill(x, y, x + w, y + h, bg);
            context.drawText(mc.textRenderer, Text.of("切换到新版菜单"), x + 6, y + 5, fg, false);
        }
    }

    // click handling moved to MouseMixin to avoid injection signature mismatches

}
