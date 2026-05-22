package wtf.uitems.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.game.player.interaction.block.BlockBreakCanHarvestEvent;

@Mixin(AbstractBlock.class)
public final class AbstractBlockMixin {
    private AbstractBlockMixin() {
    }

    @Inject(method = "getOutlineShape", at = @At("HEAD"), cancellable = true)
    private void hookGetOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
    }

    @ModifyExpressionValue(
            method = "calcBlockBreakingDelta",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;canHarvest(Lnet/minecraft/block/BlockState;)Z")
    )
    private boolean redirectCanHarvest(boolean original, @Local(argsOnly = true) BlockState state) {
        final BlockBreakCanHarvestEvent event = new BlockBreakCanHarvestEvent(state, original);
        EventDispatcher.dispatch(event);
        return event.isCanHarvest();
    }
}
