package wtf.uitems.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.screen.mainmenu.OpalMainMenuScreen;
import wtf.uitems.client.screen.auth.OpalVerifyScreen;
import wtf.uitems.client.verify.VerifyManager;

import static wtf.uitems.client.Constants.mc;

@Mixin(TitleScreen.class)
public final class TitleScreenMixin {

    private TitleScreenMixin() {
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void checkAuthInit(CallbackInfo ci) {
        if (!VerifyManager.getInstance().isAuthenticated()) {
            System.out.println("[UITEMS-DBG][TitleMixin] unauthenticated in init, currentScreen=" + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getName()));
            try {
                if (!OpalVerifyScreen.class.isInstance(mc.currentScreen)) {
                    mc.setScreen((net.minecraft.client.gui.screen.Screen) OpalVerifyScreen.class.getDeclaredConstructor().newInstance());
                    System.out.println("[UITEMS-DBG][TitleMixin] setScreen -> OpalVerifyScreen (init)");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (OpalClient.getInstance() != null && !OpalClient.getInstance().isUseVanillaMainMenu()) {
            try {
                if (!OpalMainMenuScreen.class.isInstance(mc.currentScreen)) {
                    mc.setScreen((net.minecraft.client.gui.screen.Screen) OpalMainMenuScreen.class.getDeclaredConstructor().newInstance());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void checkAuthRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!VerifyManager.getInstance().isAuthenticated()) {
            System.out.println("[UITEMS-DBG][TitleMixin] unauthenticated in render, currentScreen=" + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getName()));
            try {
                if (!OpalVerifyScreen.class.isInstance(mc.currentScreen)) {
                    mc.setScreen(OpalVerifyScreen.class.getDeclaredConstructor().newInstance());
                    System.out.println("[UITEMS-DBG][TitleMixin] setScreen -> OpalVerifyScreen (render)");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ci.cancel();
        } else if (OpalClient.getInstance() != null && !OpalClient.getInstance().isUseVanillaMainMenu()) {
            try {
                if (!OpalMainMenuScreen.class.isInstance(mc.currentScreen)) {
                    mc.setScreen(OpalMainMenuScreen.class.getDeclaredConstructor().newInstance());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ci.cancel();
        }
    }

    // toggle button rendering and click handling moved to ScreenMixin to avoid method signature mismatches

}
