package wtf.uitems.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
//import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.visual.CapeModule;

import static wtf.uitems.client.Constants.mc;

@Mixin(PlayerListEntry.class)
public final class PlayerListEntryMixin {

    @Final
    @Shadow
    private GameProfile profile;

    @Inject(method = "getSkinTextures", at = @At("TAIL"), cancellable = true)
    private void hookSkinTextures(final CallbackInfoReturnable<SkinTextures> cir) {
        if (mc.player == null) {
            return;
        }

        if (!profile.id().equals(mc.player.getUuid())) {
            return;
        }

        final var moduleRepository = OpalClient.getInstance().getModuleRepository();
        if (moduleRepository == null) {
            return;
        }

        final CapeModule capeModule = moduleRepository.getModule(CapeModule.class);
        if (capeModule == null || !capeModule.isEnabled()) {
            return;
        }

        final CapeModule.CapeType capeType = capeModule.getType();
        if (capeType == null) {
            return;
        }

        final SkinTextures oldTextures = cir.getReturnValue();
        cir.setReturnValue(
                new SkinTextures(
                        oldTextures.body(),
                        new AssetInfo.TextureAssetInfo(capeType.getIdentifier(), capeType.getIdentifier()),
                        oldTextures.elytra(),
                        oldTextures.model(),
                        oldTextures.secure()
                )
        );
    }

}
