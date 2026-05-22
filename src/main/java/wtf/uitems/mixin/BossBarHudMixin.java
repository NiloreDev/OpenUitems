package wtf.uitems.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;

@Mixin(BossBarHud.class)
public final class BossBarHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(final DrawContext context, final CallbackInfo ci) {
        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        if (overlayModule.isEnabled() && !overlayModule.isBossbarEnabled()) {
            ci.cancel();
        }
    }

}
