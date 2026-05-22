package wtf.uitems.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(YggdrasilUserApiService.class)
public final class YggdrasilUserApiServiceMixin {
    private YggdrasilUserApiServiceMixin() {
    }

    @Inject(method = "fetchProperties", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelFetchProperties(CallbackInfoReturnable<UserApiService.UserProperties> cir) {
        // cir.setReturnValue(new UserApiService.UserProperties(Collections.emptySet(), Collections.emptyMap()));
    }
}
