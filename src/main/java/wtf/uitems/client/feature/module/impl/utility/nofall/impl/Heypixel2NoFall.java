package wtf.uitems.client.feature.module.impl.utility.nofall.impl;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import wtf.uitems.client.feature.module.impl.utility.nofall.NoFallModule;
import wtf.uitems.client.feature.module.property.impl.mode.ModuleMode;
import wtf.uitems.duck.ClientConnectionAccess;
import wtf.uitems.event.impl.game.PostGameTickEvent;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.impl.game.player.movement.PreMovementPacketEvent;
import wtf.uitems.event.subscriber.Subscribe;

import static wtf.uitems.client.Constants.mc;

public final class Heypixel2NoFall extends ModuleMode<NoFallModule> {

    private double lastFallDistance;
    private boolean queuedJump;
    private PlayerMoveC2SPacket queuedPacket;

    public Heypixel2NoFall(final NoFallModule module) {
        super(module);
    }

    @Subscribe(priority = -5)
    public void onPreMovementPacket(final PreMovementPacketEvent event) {
        if (mc.player == null || mc.world == null || mc.player.getAbilities().allowFlying || mc.player.isSpectator() || mc.player.isDead()) {
            resetState();
            return;
        }

        final double currentFallDistance = mc.player.fallDistance;

        if (lastFallDistance >= 3.0F && event.isOnGround()) {
            sendPacketSilent(new PlayerMoveC2SPacket.OnGroundOnly(true, event.isHorizontalCollision()));
            queuedJump = true;
            queuedPacket = new PlayerMoveC2SPacket.PositionAndOnGround(event.getX(), event.getY(), event.getZ(), false, event.isHorizontalCollision());
            event.setCancelled();
        }

        lastFallDistance = currentFallDistance;
    }

    @Subscribe
    public void onPostGameTick(final PostGameTickEvent event) {
        if (queuedPacket != null) {
            sendPacketSilent(queuedPacket);
            queuedPacket = null;
        }
    }

    @Subscribe(priority = 1)
    public void onMoveInput(final MoveInputEvent event) {
        if (mc.player == null) {
            resetState();
            return;
        }

        if (lastFallDistance >= 3.0F && mc.player.isOnGround()) {
            event.setSneak(false);
        }

        if (queuedJump) {
            event.setJump(true);
            queuedJump = false;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    @Override
    public Enum<?> getEnumValue() {
        return NoFallModule.Mode.HEYPIXEL2;
    }

    private void resetState() {
        lastFallDistance = 0D;
        queuedJump = false;
        queuedPacket = null;
    }

    private void sendPacketSilent(final Packet<?> packet) {
        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() instanceof ClientConnectionAccess access) {
            access.opal$sendPacketSilent(packet);
        }
    }
}
