package wtf.uitems.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.world.scaffold.ScaffoldModule;

import static wtf.uitems.client.Constants.mc;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @ModifyReturnValue(method = "getFovMultiplier", at = @At("RETURN"))
    private float hookFovMultiplier(float original) {
        final ScaffoldModule scaffoldModule = OpalClient.getInstance().getModuleRepository().getModule(ScaffoldModule.class);
        if (mc.player != null && (Object) this == mc.player && scaffoldModule.isEnabled() && scaffoldModule.getSettings().isKeepFov()) {
            // Lock FOV to sprint value to avoid landing/jump oscillation.
            return 1.1F;
        }
        return original;
    }
}
