package wtf.uitems.mixin;

import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.press.KeyPressEvent;

@Mixin(Keyboard.class)
public final class KeyboardMixin {

    private KeyboardMixin() {
    }

    @Inject(
            at = @At("HEAD"),
            method = "onKey(JILnet/minecraft/client/input/KeyInput;)V"
    )
    private void onKey(long window, int action, net.minecraft.client.input.KeyInput keyInput, CallbackInfo ci) {
        if (action == 1) {
            if (keyInput.key() == -1) {
                return;
            }

            wtf.uitems.utility.KeyInput input = new wtf.uitems.utility.KeyInput(keyInput.key(), keyInput.modifiers());
            EventDispatcher.dispatch(new KeyPressEvent(input));
        }
    }
}
