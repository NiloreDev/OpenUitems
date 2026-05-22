package wtf.uitems.client.feature.module.impl.combat.velocity.impl;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
import wtf.uitems.client.feature.module.impl.combat.AntiBotModule;
import wtf.uitems.client.feature.module.impl.combat.velocity.VelocityMode;
import wtf.uitems.client.feature.module.impl.combat.velocity.VelocityModule;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.utility.player.RotationUtility;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static wtf.uitems.client.Constants.mc;

public final class DelayVelocity extends VelocityMode {

    private static final int DEFAULT_ATTACK_COUNT = 4;
    private static final boolean DEFAULT_AUTO_ATTACK_COUNT = true;
    private static final double DEFAULT_ALINK_TARGET_RANGE = 10.0;
    private static final int DEFAULT_ALINK_MAX_DELAY = 60;
    private static final boolean DEFAULT_AUTO_ROTATE = true;
    private static final boolean DEFAULT_DEBUG = false;

    private Entity target;
    private int attackQueue;
    private boolean receiveDamage;
    private boolean isProjectileDamage;
    private boolean isExplosionOrFireDamage;
    private int alinkTicks;
    private String releaseReason;
    private Vec3d velocity;
    private boolean attacking;
    private boolean velocityApplied;
    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private int hitSelectSkipAttacks;

    public DelayVelocity(final VelocityModule module) {
        super(module);
    }

    private void findTarget() {
        if (mc.player == null || mc.world == null) return;

        if (mc.crosshairTarget instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();
            if (entity instanceof LivingEntity && !entity.isRemoved() && !AntiBotModule.isBot(entity)) {
                this.target = entity;
                return;
            }
        }

        Entity nearest = null;
        double bestDist = DEFAULT_ALINK_TARGET_RANGE;
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !(entity instanceof LivingEntity) || entity.isRemoved() || AntiBotModule.isBot(entity)) continue;
            double dist = mc.player.distanceTo(entity);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = entity;
            }
        }

        if (nearest != null) {
            if (nearest.distanceTo(mc.player) <= 3.0 && DEFAULT_AUTO_ROTATE) {
                Vec2f rotations = RotationUtility.getRotationFromPosition(nearest.getEyePos());
                RotationHelper.getHandler().rotate(rotations, InstantRotationModel.INSTANCE);
            }
            if (alinkTicks >= 0) {
                this.target = nearest;
            }
        }
    }

    private void handle() {
        if (mc.getNetworkHandler() == null) {
            packets.clear();
            return;
        }

        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        while (!packets.isEmpty()) {
            Packet<?> packet = packets.poll();
            if (packet != null) {
                try {
                    ((Packet) packet).apply(networkHandler);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private int getCurrentAttackCount() {
        if (!DEFAULT_AUTO_ATTACK_COUNT) return DEFAULT_ATTACK_COUNT;
        if (velocity == null) return 0;
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (speed < 0.1) return 0;
        if (speed < 0.3) return 3;
        if (speed < 1.0) return 4;
        return 5;
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.player == null || mc.world == null) return;

        Packet<?> packet = event.getPacket();

        if (alinkTicks >= 0) {
            if (packet instanceof ChatMessageS2CPacket || packet instanceof GameMessageS2CPacket) {
                return;
            }

            if (packet instanceof DisconnectS2CPacket || packet instanceof PlayerRespawnS2CPacket || packet instanceof GameJoinS2CPacket) {
                handle();
                alinkTicks = -1;
                return;
            }

            if (packet instanceof PlayerPositionLookS2CPacket) {
                releaseReason = "flag";
                return;
            }

            event.setCancelled();
            packets.add(packet);
            return;
        }

        if (packet instanceof EntityDamageS2CPacket damagePacket && damagePacket.entityId() == mc.player.getId()) {
            receiveDamage = true;
            isProjectileDamage = false;
            isExplosionOrFireDamage = false;

            final DamageSource source = damagePacket.createDamageSource(mc.world);
            final String name = source.getName();
            if (name != null) {
                final String n = name.toLowerCase();
                if (n.contains("explosion") || n.contains("fire") || n.contains("lava") || n.contains("hot_floor")) {
                    isExplosionOrFireDamage = true;
                }
            }

            if (damagePacket.sourceDirectId() != -1) {
                Entity direct = mc.world.getEntityById(damagePacket.sourceDirectId());
                if (direct instanceof ProjectileEntity) {
                    isProjectileDamage = true;
                }
            }
        }

        if (packet instanceof EntityVelocityUpdateS2CPacket velocityPacket && velocityPacket.getEntityId() == mc.player.getId() && receiveDamage) {
            receiveDamage = false;

            if (mc.player.isUsingItem()) return;
            if (isExplosionOrFireDamage) {
                if (DEFAULT_DEBUG) ChatUtility.print("DelayVelocity: Ignoring Explosion/Fire velocity");
                return;
            }

            findTarget();
            this.velocity = velocityPacket.getVelocity();

            int count = getCurrentAttackCount();
            if (count == 0) return;

            if (isProjectileDamage || this.target == null || !mc.player.isSprinting()) {
                if (DEFAULT_DEBUG) {
                    ChatUtility.print("DelayVelocity: Starting Alink delay" + (isProjectileDamage ? " (Projectile)" : "") + "...");
                }
                alinkTicks = DEFAULT_ALINK_MAX_DELAY;
                event.setCancelled();
                packets.add(packet);
            } else {
                attackQueue = count;
                velocityApplied = false;
                hitSelectSkipAttacks = count;
                event.setCancelled();
            }
        }
    }

    @Subscribe
    public void onPreTick(PreGameTickEvent event) {
        if (mc.player == null) return;

        attacking = false;

        if (releaseReason != null) {
            if (releaseReason.isEmpty() && !mc.player.isSprinting()) {
                releaseReason = null;
                return;
            }

            handle();
            alinkTicks = -1;

            if (releaseReason.isEmpty()) {
                if (DEFAULT_DEBUG) ChatUtility.print("DelayVelocity: Finish Alink");
                int c = getCurrentAttackCount();
                attackQueue = c;
                hitSelectSkipAttacks = c;
                velocityApplied = false;
            } else {
                if (DEFAULT_DEBUG) ChatUtility.print("DelayVelocity: Finish Alink (" + releaseReason + ")");
            }
            releaseReason = null;
        }

        if (attackQueue > 0) {
            attacking = true;
            if (this.target == null || this.target.isRemoved()) {
                attackQueue = 0;
                velocity = null;
                velocityApplied = false;
                return;
            }

            if (!velocityApplied && velocity != null) {
                mc.player.setVelocity(velocity.x, velocity.y, velocity.z);
                velocityApplied = true;
            }

            mc.player.setSprinting(true);
            mc.interactionManager.attackEntity(mc.player, this.target);
            mc.player.swingHand(Hand.MAIN_HAND);
            attackQueue--;

            if (attackQueue <= 0) {
                this.target = null;
                this.velocity = null;
                this.velocityApplied = false;
            }
        }
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null) return;

        if (alinkTicks >= 0 && releaseReason == null) {
            if (alinkTicks > 0) alinkTicks--;
            findTarget();

            if (alinkTicks == 0) {
                releaseReason = "max delay";
            } else if (mc.player.getAbilities().flying) {
                releaseReason = "flying";
            } else if (this.target != null && mc.player.distanceTo(this.target) > DEFAULT_ALINK_TARGET_RANGE) {
                releaseReason = "out of range";
            } else if (this.target != null) {
                event.setForward(1.0f);
                event.setSideways(0.0f);
                releaseReason = "";
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        handle();
        reset();
    }

    private void reset() {
        this.target = null;
        this.attackQueue = 0;
        this.receiveDamage = false;
        this.isProjectileDamage = false;
        this.isExplosionOrFireDamage = false;
        this.alinkTicks = -1;
        this.releaseReason = null;
        this.velocity = null;
        this.attacking = false;
        this.velocityApplied = false;
        this.packets.clear();
        this.hitSelectSkipAttacks = 0;
    }

    public boolean isAttacking() {
        return attacking;
    }

    public int getHitSelectSkips() {
        return hitSelectSkipAttacks;
    }

    public boolean consumeHitSelectSkip() {
        if (hitSelectSkipAttacks > 0) {
            hitSelectSkipAttacks--;
            return true;
        }
        return false;
    }

    @Override
    public Enum<?> getEnumValue() {
        return VelocityModule.Mode.DELAY;
    }

    @Override
    public String getSuffix() {
        return "Delay" + (alinkTicks >= 0 ? " " + (DEFAULT_ALINK_MAX_DELAY - alinkTicks) + "Ticks" : "");
    }
}
