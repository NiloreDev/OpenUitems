package wtf.uitems.mixin;

import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.game.input.SlotChangeEvent;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow public int selectedSlot;

    @Inject(
            method = "setSelectedSlot",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I")
    )
    private void hookSlotChangeMethod(int slot, CallbackInfo ci) {
        EventDispatcher.dispatch(new SlotChangeEvent(slot));
    }

}
