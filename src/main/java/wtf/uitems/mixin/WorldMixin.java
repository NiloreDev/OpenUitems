package wtf.uitems.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.duck.WorldAccessor;

import java.util.ArrayList;
import java.util.List;

@Mixin(World.class)
public abstract class WorldMixin implements WorldAccessor {

    @Unique
    private final List<BlockEntity> opal$blockEntities = new ArrayList<>();

    @Override
    public List<BlockEntity> getBlockEntities() {
        return opal$blockEntities;
    }

    @Inject(method = "addBlockEntity", at = @At("TAIL"))
    private void onAddBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        if (blockEntity != null && !opal$blockEntities.contains(blockEntity)) {
            opal$blockEntities.add(blockEntity);
        }
    }

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    private void onRemoveBlockEntity(net.minecraft.util.math.BlockPos pos, CallbackInfo ci) {
        BlockEntity blockEntity = ((World) (Object) this).getBlockEntity(pos);
        if (blockEntity != null) {
            opal$blockEntities.remove(blockEntity);
        }
    }

}
