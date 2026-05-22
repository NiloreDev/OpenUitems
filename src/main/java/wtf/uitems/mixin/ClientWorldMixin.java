package wtf.uitems.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.game.world.PlaySoundEvent;
import wtf.uitems.utility.player.SkipTickUtility;

import static wtf.uitems.client.Constants.mc;

@Mixin(ClientWorld.class)
public final class ClientWorldMixin {

    private ClientWorldMixin() {
    }

    @Inject(method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZJ)V", at = @At("HEAD"), cancellable = true)
    private void playSound(double x, double y, double z, SoundEvent event, SoundCategory category, float volume, float pitch, boolean useDistance, long seed, CallbackInfo ci) {
        final PlaySoundEvent playSoundEvent = new PlaySoundEvent(event, x, y, z);
        EventDispatcher.dispatch(playSoundEvent);
        if (playSoundEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "tickEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;tick()V")
    )
    private void opal$hookSkipTicks(final Entity instance) {
        if (mc.player != null && instance == mc.player && SkipTickUtility.consumeSkipTick()) {
            return;
        }
        instance.tick();
    }

}
