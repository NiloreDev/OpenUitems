package wtf.uitems.client.feature.module.impl.combat.velocity.impl;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.client.feature.module.impl.combat.velocity.VelocityMode;
import wtf.uitems.client.feature.module.impl.combat.velocity.VelocityModule;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.subscriber.Subscribe;

import java.util.ArrayDeque;
import java.util.Queue;

import static wtf.uitems.client.Constants.mc;

public final class Heypixel3Velocity extends VelocityMode {

    private final NumberProperty maxDelayTicks = new NumberProperty("Delay SPacket Ticks", 5, 1, 5, 1)
            .hideIf(() -> this.module.getActiveMode() != this);

    private final Queue<Packet<?>> delayedPackets = new ArrayDeque<>();
    private int delayTicks;
    private int attackCount;
    private boolean shouldFlush;
    private Vec3d pendingVelocity;
    private int hurtWindowTicks;

    public Heypixel3Velocity(final VelocityModule module) {
        super(module);
        module.addProperties(maxDelayTicks);
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        final Packet<?> packet = event.getPacket();

        if (packet instanceof EntityDamageS2CPacket damagePacket && damagePacket.entityId() == mc.player.getId()) {
            hurtWindowTicks = 3;
            return;
        }

        if (packet instanceof EntityVelocityUpdateS2CPacket velocityPacket && velocityPacket.getEntityId() == mc.player.getId()) {
            // Ignore unrelated velocity changes (pistons/etc.) unless they follow a real damage packet.
            if (hurtWindowTicks <= 0) {
                return;
            }

            // While delaying: try to trigger one AttackCount on this new hit,
            // if triggered successfully, end delay immediately.
            if (delayTicks > 0 || shouldFlush) {
                if (delayTicks > 0) {
                    if (canTriggerAttackCountNow()) {
                        attackCount = 1;
                        attackCount--;
                        delayTicks = 0;
                        pendingVelocity = null;
                        shouldFlush = true;
                    }
                }
                hurtWindowTicks = 0;
                event.setCancelled();
                return;
            }

            if (attackCount > 0 && canTriggerAttackCountNow()) {
                attackCount--;
                hurtWindowTicks = 0;
                event.setCancelled();
                return;
            }

            delayTicks = maxDelayTicks.getValue().intValue();
            shouldFlush = false;
            delayedPackets.clear();
            pendingVelocity = velocityPacket.getVelocity();
            hurtWindowTicks = 0;
            event.setCancelled();
            return;
        }

        if (delayTicks > 0) {
            if (packet instanceof PlayerPositionLookS2CPacket) {
                delayTicks = 0;
                shouldFlush = true;
                return;
            }

            delayedPackets.add(packet);
            event.setCancelled();
        }
    }

    @Subscribe
    public void onPreTick(final PreGameTickEvent event) {
        if (mc.player == null) {
            return;
        }

        if (delayTicks > 0) {
            delayTicks--;
            if (delayTicks == 0) {
                // Timeout without successful AttackCount trigger: just end delay.
                shouldFlush = true;
            }
        }

        if (hurtWindowTicks > 0) {
            hurtWindowTicks--;
        }

        if (shouldFlush) {
            flushDelayedPackets();
            shouldFlush = false;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        delayedPackets.clear();
        delayTicks = 0;
        shouldFlush = false;
        pendingVelocity = null;
        hurtWindowTicks = 0;
        attackCount = 1;
    }

    private void flushDelayedPackets() {
        if (mc.getNetworkHandler() == null) {
            delayedPackets.clear();
            delayTicks = 0;
            shouldFlush = false;
            pendingVelocity = null;
            hurtWindowTicks = 0;
            return;
        }

        if (pendingVelocity != null && mc.player != null) {
            mc.player.setVelocityClient(pendingVelocity);
            pendingVelocity = null;
        }

        while (!delayedPackets.isEmpty()) {
            final Packet<?> p = delayedPackets.poll();
            if (p != null) {
                ((Packet) p).apply(mc.getNetworkHandler());
            }
        }

        delayTicks = 0;
    }

    private boolean canTriggerAttackCountNow() {
        return mc.player != null
                && mc.player.isSprinting()
                && !mc.player.isSneaking()
                && mc.player.forwardSpeed > 0.0F
                && !mc.player.isUsingItem();
    }

    @Override
    public void onDisable() {
        flushDelayedPackets();
        shouldFlush = false;
        attackCount = 1;
        pendingVelocity = null;
        hurtWindowTicks = 0;
        super.onDisable();
    }

    @Override
    public Enum<?> getEnumValue() {
        return VelocityModule.Mode.HEYPIXEL;
    }

    @Override
    public String getSuffix() {
        if (delayTicks > 0) {
            return "Heypixel3 " + (maxDelayTicks.getValue().intValue() - delayTicks) + "Ticks";
        }
        return "Heypixel";
    }

    public boolean isDelaying() {
        return delayTicks > 0;
    }

    public boolean hasQueuedPackets() {
        return !delayedPackets.isEmpty() || shouldFlush;
    }
}
