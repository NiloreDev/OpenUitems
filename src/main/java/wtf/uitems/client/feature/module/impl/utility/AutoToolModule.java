package wtf.uitems.client.feature.module.impl.utility;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.GameMode;
import wtf.uitems.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.event.impl.game.input.MouseHandleInputEvent;
import wtf.uitems.event.subscriber.Subscribe;

import java.util.stream.IntStream;

import static wtf.uitems.client.Constants.mc;

public final class AutoToolModule extends Module {
    public final BooleanProperty silent = new BooleanProperty("Silent", true);

    public AutoToolModule() {
        super("Auto Tool", "Automatically switches to the best tool in your hotbar.", ModuleCategory.UTILITY);
        addProperties(silent);
    }

    @Subscribe
    public void onMouseHandleInput(final MouseHandleInputEvent event) {
        if (!(mc.crosshairTarget instanceof BlockHitResult blockHitResult) || !MouseHelper.getLeftButton().isPressed() || MouseHelper.getRightButton().isPressed() || mc.player.isUsingItem()) {
            return;
        }

        final PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.getSession().getUuidOrNull());
        if (playerListEntry != null && (playerListEntry.getGameMode() == GameMode.CREATIVE || playerListEntry.getGameMode() == GameMode.SPECTATOR)) {
            return;
        }

        final BlockState blockState = mc.world.getBlockState(blockHitResult.getBlockPos());
        final float hardness = blockState.getHardness(mc.world, blockHitResult.getBlockPos());
        if (hardness == 0) {
            return;
        }

        final int slot = IntStream.range(0, 9)
                .filter(i -> {
                    final ItemStack itemStack = mc.player.getInventory().getMainStacks().get(i);
                    final BlockState modifiedBlockState = blockState;
                    return itemStack.getMiningSpeedMultiplier(modifiedBlockState) > 1;
                })
                .findFirst()
                .orElse(-1);
        if (slot == -1) {
            return;
        }

        SlotHelper.setCurrentItem(slot).silence(silent.getValue()? SlotHelper.Silence.FULL : SlotHelper.Silence.NONE);
    }

}
