package wtf.uitems.client.feature.module.impl.utility.nofall.impl;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import wtf.uitems.client.feature.module.impl.utility.nofall.NoFallModule;
import wtf.uitems.client.feature.module.property.impl.mode.ModuleMode;
import wtf.uitems.duck.ClientConnectionAccess;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.impl.game.player.movement.PreMovementPacketEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.player.PlayerUtility;

import static wtf.uitems.client.Constants.mc;

public final class HeypixelNoFall extends ModuleMode<NoFallModule> {

    private boolean queuedJump;
    private double lastFallDistance;

    public HeypixelNoFall(NoFallModule module) {
        super(module);
    }

    @Override
    public Enum<?> getEnumValue() {
        return NoFallModule.Mode.HEYPIXEL;
    }

    @Subscribe
    public void onPreMovementPacket(final PreMovementPacketEvent event) {
        if (mc.player == null || mc.world == null || mc.player.getAbilities().allowFlying || mc.player.isSpectator()) {
            queuedJump = false;
            lastFallDistance = 0D;
            return;
        }

        if (mc.player.isDead()) {
            queuedJump = false;
            lastFallDistance = 0D;
            return;
        }

        final double currentFall = mc.player.fallDistance;

        if (lastFallDistance >= PlayerUtility.getMaxFallDistance() && mc.player.isOnGround()) {
            event.setOnGround(true);
            sendPacketSilent(new PlayerMoveC2SPacket.OnGroundOnly(true, event.isHorizontalCollision()));
            if (mc.player.isUsingItem()) {
                mc.options.useKey.setPressed(false);
                mc.interactionManager.stopUsingItem(mc.player);
            }
            queuedJump = true;
        }

        lastFallDistance = currentFall;
    }

    @Subscribe
    public void onMoveInput(final MoveInputEvent event) {
        if (queuedJump) {
            event.setJump(true);
            if (mc.player != null && mc.world != null) {
                final boolean moving = Math.abs(event.getForward()) > 1.0E-3F || Math.abs(event.getSideways()) > 1.0E-3F;
                if (moving && isOnStairLikeLanding()) {
                    event.setForward(0.0F);
                    event.setSideways(0.0F);
                }
            }
            queuedJump = false;
        }
        if (lastFallDistance >= PlayerUtility.getMaxFallDistance() && mc.player != null && mc.player.isOnGround()) {
            event.setSneak(false);
        }
    }

    private boolean isOnStairLikeLanding() {
        final BlockPos feet = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        final BlockPos under = feet.down();
        final BlockPos frontFeet = feet.offset(mc.player.getHorizontalFacing());
        final BlockPos frontUnder = frontFeet.down();

        final BlockState feetState = mc.world.getBlockState(feet);
        final BlockState underState = mc.world.getBlockState(under);
        final BlockState frontFeetState = mc.world.getBlockState(frontFeet);
        final BlockState frontUnderState = mc.world.getBlockState(frontUnder);

        return isStairOrSlab(feetState)
                || isStairOrSlab(underState)
                || isStairOrSlab(frontFeetState)
                || isStairOrSlab(frontUnderState);
    }

    private boolean isStairOrSlab(final BlockState state) {
        return state.getBlock() instanceof StairsBlock || state.getBlock() instanceof SlabBlock;
    }

    private void sendPacketSilent(Packet<?> packet) {
        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() instanceof ClientConnectionAccess access) {
            access.opal$sendPacketSilent(packet);
        }
    }
}
