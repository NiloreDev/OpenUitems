package wtf.uitems.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.utility.misc.chat.ChatUsernameDecorator;

@Mixin(ChatHud.class)
public final class ChatHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void opal$cancelVanillaChatRender(final DrawContext context, final int currentTick, final int mouseX, final int mouseY, final boolean focused, final CallbackInfo ci) {
        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        final boolean inChatScreen = net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
        if (overlayModule != null && overlayModule.isCustomChatHudEnabled() && !inChatScreen) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "method_71991",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;content()Lnet/minecraft/text/OrderedText;")
    )
    private OrderedText appendSocketUsernames(final ChatHudLine.Visible instance) {
        return ChatUsernameDecorator.appendSocketUsernames(instance.content());
    }
}
