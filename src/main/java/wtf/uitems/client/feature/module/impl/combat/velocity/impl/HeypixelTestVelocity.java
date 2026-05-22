package wtf.uitems.client.feature.module.impl.combat.velocity.impl;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
import wtf.uitems.client.feature.module.impl.combat.AntiBotModule;
import wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.uitems.client.feature.module.impl.combat.velocity.VelocityMode;
import wtf.uitems.client.feature.module.impl.combat.velocity.VelocityModule;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.utility.player.RotationUtility;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static wtf.uitems.client.Constants.mc;

public final class HeypixelTestVelocity extends VelocityMode {

    private enum AttackMode {
        ONE_TIME("OneTime"),
        PER_TICK("PerTick");

        private final String name;

        AttackMode(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final BooleanProperty autoAttackCount = new BooleanProperty("AutoAttackCount", true)
            .hideIf(() -> this.module.getActiveMode() != this);
    private final NumberProperty attackCount = new NumberProperty("AttackCount", 4.0F, 0.0F, 20.0F, 1.0F)
            .hideIf(() -> this.module.getActiveMode() != this || autoAttackCount.getValue());
    private final ModeProperty<AttackMode> attackMode = new ModeProperty<>("AttackMode", AttackMode.PER_TICK)
            .hideIf(() -> this.module.getActiveMode() != this);
    private final NumberProperty alinkTargetRange = new NumberProperty("AlinkTargetRange", 10.0F, 0.0F, 20.0F, 0.1F)
            .hideIf(() -> this.module.getActiveMode() != this);
    private final NumberProperty alinkMaxDelay = new NumberProperty("AlinkMaxDelay", 60.0F, 0.0F, 200.0F, 1.0F)
            .hideIf(() -> this.module.getActiveMode() != this);
    private final BooleanProperty requireKillAura = new BooleanProperty("RequireKillAura", false)
            .hideIf(() -> this.module.getActiveMode() != this);
    private final BooleanProperty debug = new BooleanProperty("Debug", false)
            .hideIf(() -> this.module.getActiveMode() != this);

    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();

    private Entity target;
    private Entity renderTarget;
    private Vec3d renderTargetPos;
    private int attackQueue;
    private boolean receiveDamage;
    private int alinkTicks = -1;
    private String releaseReason;
    private double velocityStrength;
    private boolean attacking;
    private int hitSelectSkips;

    public HeypixelTestVelocity(final VelocityModule module) {
        super(module);
        module.addProperties(attackCount, autoAttackCount, attackMode, alinkTargetRange, alinkMaxDelay, requireKillAura, debug);
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (mc.player == null || mc.world == null || this.module.isInvalid()) {
            return;
        }

        final Packet<?> packet = event.getPacket();

        if (alinkTicks >= 0) {
            if (packet instanceof ChatMessageS2CPacket || packet instanceof GameMessageS2CPacket) {
                return;
            }

            if (packet instanceof PlayerRespawnS2CPacket || packet instanceof GameJoinS2CPacket) {
                releaseReason = "disconnect";
                return;
            }

            if (packet instanceof PlayerPositionLookS2CPacket) {
                releaseReason = "flag";
                return;
            }

            if (packet instanceof CommonPingS2CPacket || packet instanceof EntityS2CPacket || packet instanceof EntityPositionS2CPacket || packet instanceof EntityPositionSyncS2CPacket) {
                updateRenderTargetPosition(packet);
                event.setCancelled();
                packets.add(packet);
                return;
            }
        }

        if (packet instanceof EntityDamageS2CPacket damagePacket && damagePacket.entityId() == mc.player.getId()) {
            receiveDamage = true;
            return;
        }

        if (packet instanceof EntityVelocityUpdateS2CPacket velocityPacket
                && velocityPacket.getEntityId() == mc.player.getId()
                && receiveDamage) {
            receiveDamage = false;

            if (mc.player.isUsingItem()) {
                return;
            }

            if (requireKillAura.getValue()) {
                final KillAuraModule killAura = OpalClient.getInstance().getModuleRepository().getModule(KillAuraModule.class);
                if (!killAura.isEnabled() || !killAura.getTargeting().isTargetSelected()) {
                    return;
                }
            }

            findTarget();
            if (renderTarget == null) {
                return;
            }

            final Vec3d vel = velocityPacket.getVelocity();
            velocityStrength = Math.sqrt((vel.x * 8000.0D) * (vel.x * 8000.0D) + (vel.y * 8000.0D) * (vel.y * 8000.0D));
            final int currentAttackCount = getCurrentAttackCount();
            if (currentAttackCount <= 0) {
                return;
            }

            hitSelectSkips = currentAttackCount;

            if (target == null || !mc.player.isSprinting()) {
                if (debug.getValue()) {
                    ChatUtility.print(!mc.player.isSprinting() ? "Alink... (not sprinting)" : "Alink...");
                }
                renderTargetPos = getEntityPos(renderTarget);
                alinkTicks = alinkMaxDelay.getValue().intValue();
                releaseReason = null;
                event.setCancelled();
                packets.add(packet);
            } else {
                attackQueue = currentAttackCount;
                if (debug.getValue()) {
                    ChatUtility.print("Attack count: " + attackQueue);
                }
            }
        }
    }

    @Subscribe
    public void onMoveInput(final MoveInputEvent event) {
        if (mc.player == null || alinkTicks < 0 || releaseReason != null) {
            return;
        }

        if (alinkTicks > 0) {
            alinkTicks--;
        }

        findTarget();

        if (alinkTicks == 0) {
            releaseReason = "max delay";
            return;
        }

        if (mc.player.getAbilities().flying || mc.player.isSpectator()) {
            releaseReason = "spectator";
            return;
        }

        if (renderTargetPos == null || getPlayerPos().distanceTo(renderTargetPos) > alinkTargetRange.getValue().doubleValue()) {
            releaseReason = "out of range";
            return;
        }

        if (target != null) {
            event.setForward(1.0F);
            event.setSideways(0.0F);
            releaseReason = "";
        }
    }

    @Subscribe
    public void onPreTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }

        attacking = false;

        if (releaseReason != null) {
            flushQueuedPackets();
            alinkTicks = -1;
            renderTarget = null;
            renderTargetPos = null;

            if (releaseReason.isEmpty()) {
                attackQueue = getCurrentAttackCount();
                if (debug.getValue()) {
                    ChatUtility.print("Finish alink");
                    ChatUtility.print("Attack count: " + attackQueue);
                }
            } else if (debug.getValue()) {
                ChatUtility.print("Finish alink (" + releaseReason + ")");
            }

            releaseReason = null;
        }

        if (attackQueue <= 0) {
            return;
        }

        if (target == null || target.isRemoved()) {
            attackQueue = 0;
            return;
        }

        if (!rotateToTarget(target)) {
            return;
        }

        attacking = true;
        if (attackMode.getValue() == AttackMode.ONE_TIME) {
            while (attackQueue > 0) {
                attackOnce();
                attackQueue--;
            }
        } else {
            attackOnce();
            attackQueue--;
        }

        if (attackQueue <= 0) {
            target = null;
            velocityStrength = 0.0D;
        }
    }

    private void attackOnce() {
        if (target == null || mc.player == null || mc.interactionManager == null) {
            return;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.setVelocity(mc.player.getVelocity().multiply(0.6D, 1.0D, 0.6D));
    }

    private boolean rotateToTarget(final Entity entity) {
        if (mc.player == null || entity == null) {
            return false;
        }

        final Vec2f rot = RotationUtility.getRotationFromPosition(entity.getEyePos());
        RotationHelper.getHandler().rotate(rot, InstantRotationModel.INSTANCE);
        return true;
    }

    private void findTarget() {
        if (mc.player == null || mc.world == null) {
            target = null;
            return;
        }

        Entity localTarget = null;
        if (mc.crosshairTarget instanceof EntityHitResult ehr) {
            final Entity entity = ehr.getEntity();
            if (entity instanceof LivingEntity && entity != mc.player && !entity.isRemoved() && !AntiBotModule.isBot(entity)) {
                localTarget = entity;
            }
        }

        if (alinkTicks == -1) {
            renderTarget = localTarget;
        }

        if (localTarget != null) {
            target = localTarget;
            return;
        }

        Entity targetAround = null;
        Vec3d targetPos = null;
        double bestDistance = alinkTargetRange.getValue().doubleValue();
        for (final Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity) || entity == mc.player || entity.isRemoved() || AntiBotModule.isBot(entity)) {
                continue;
            }

            final double distance = mc.player.distanceTo(entity);
            if (distance > bestDistance) {
                continue;
            }

            if (renderTarget != null && entity.getId() == renderTarget.getId()) {
                continue;
            }

            bestDistance = distance;
            targetAround = entity;
            targetPos = getEntityPos(entity);
        }

        if (renderTarget != null && renderTargetPos != null && targetPos != null) {
            if (renderTargetPos.distanceTo(getPlayerPos()) <= targetPos.distanceTo(getPlayerPos())) {
                targetAround = renderTarget;
                targetPos = renderTargetPos;
            }
        }

        if (targetAround == null || targetPos == null) {
            target = null;
            return;
        }

        if (targetPos.distanceTo(getPlayerPos()) <= 3.0D) {
            target = targetAround;
        }

        if (alinkTicks == -1) {
            renderTarget = targetAround;
        }
    }

    private void updateRenderTargetPosition(final Packet<?> packet) {
        if (renderTarget == null || renderTargetPos == null) {
            return;
        }

        if (packet instanceof EntityS2CPacket entityPacket) {
            final Entity entity = entityPacket.getEntity(mc.world);
            if (entity != null && entity.getId() == renderTarget.getId()) {
                renderTargetPos = renderTargetPos.add(entityPacket.getDeltaX(), entityPacket.getDeltaY(), entityPacket.getDeltaZ());
            }
            return;
        }

        if (packet instanceof EntityPositionS2CPacket positionPacket) {
            final Integer entityId = readTrackedEntityId(positionPacket);
            final Vec3d position = readTrackedPosition(positionPacket);
            if (entityId != null && position != null && entityId == renderTarget.getId()) {
                renderTargetPos = position;
                return;
            }
        }

        if (packet instanceof EntityPositionSyncS2CPacket syncPacket && syncPacket.id() == renderTarget.getId()) {
            renderTargetPos = syncPacket.values().position();
        }
    }

    private int getCurrentAttackCount() {
        if (!autoAttackCount.getValue()) {
            return attackCount.getValue().intValue();
        }

        if (velocityStrength < 1000.0D) {
            return 0;
        }
        if (velocityStrength < 2000.0D) {
            return 3;
        }
        if (velocityStrength < 10000.0D) {
            return 4;
        }
        return 5;
    }

    private void flushQueuedPackets() {
        final ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler == null) {
            packets.clear();
            return;
        }

        while (!packets.isEmpty()) {
            final Packet<?> packet = packets.poll();
            if (packet != null) {
                try {
                    ((Packet) packet).apply(networkHandler);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void reset() {
        target = null;
        renderTarget = null;
        renderTargetPos = null;
        attackQueue = 0;
        receiveDamage = false;
        alinkTicks = -1;
        releaseReason = null;
        velocityStrength = 0.0D;
        attacking = false;
        hitSelectSkips = 0;
        packets.clear();
    }

    private Vec3d getPlayerPos() {
        return new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private Vec3d getEntityPos(final Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    private Integer readTrackedEntityId(final EntityPositionS2CPacket packet) {
        try {
            final var method = packet.getClass().getMethod("getEntityId");
            return (Integer) method.invoke(packet);
        } catch (Exception ignored) {
        }
        try {
            final var method = packet.getClass().getMethod("comp_3237");
            return (Integer) method.invoke(packet);
        } catch (Exception ignored) {
        }
        return null;
    }

    private Vec3d readTrackedPosition(final EntityPositionS2CPacket packet) {
        try {
            final var changeMethod = packet.getClass().getMethod("change");
            final Object change = changeMethod.invoke(packet);
            final var positionMethod = change.getClass().getMethod("position");
            return (Vec3d) positionMethod.invoke(change);
        } catch (Exception ignored) {
        }
        try {
            final var changeMethod = packet.getClass().getMethod("comp_3238");
            final Object change = changeMethod.invoke(packet);
            final var positionMethod = change.getClass().getMethod("comp_3148");
            return (Vec3d) positionMethod.invoke(change);
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean isAttacking() {
        return attacking;
    }

    public int getHitSelectSkips() {
        return hitSelectSkips;
    }

    public boolean consumeHitSelectSkip() {
        if (hitSelectSkips > 0) {
            hitSelectSkips--;
            return true;
        }
        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        flushQueuedPackets();
        reset();
    }

    @Override
    public Enum<?> getEnumValue() {
        return VelocityModule.Mode.HEYPIXEL_TEST;
    }

    @Override
    public String getSuffix() {
        return alinkTicks >= 0 ? "HeypixelTest " + (alinkMaxDelay.getValue().intValue() - alinkTicks) + "Ticks" : "HeypixelTest";
    }

}
