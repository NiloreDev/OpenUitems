package wtf.uitems.mixin;

import net.minecraft.client.util.Icons;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Icons.class)
public final class IconsMixin {

    private IconsMixin() {
    }

    // @Inject(
    //         at = @At("HEAD"),
    //         method = "getIcon",
    //         cancellable = true
    // )
    // private void getIcon(final ResourcePack resourcePack, final String fileName, final CallbackInfoReturnable<InputSupplier<InputStream>> info) {
    //     info.setReturnValue(() -> Knot.getLauncher().getTargetClassLoader().getResourceAsStream("assets/uitems/window-icons/" + fileName));
    // }

}
