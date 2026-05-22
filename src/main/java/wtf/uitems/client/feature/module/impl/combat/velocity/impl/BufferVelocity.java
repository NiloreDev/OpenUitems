package wtf.uitems.client.feature.module.impl.combat.velocity.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import wtf.uitems.client.feature.module.impl.combat.AntiBotModule;
import wtf.uitems.client.feature.module.impl.combat.velocity.VelocityMode;
import wtf.uitems.client.feature.module.impl.combat.velocity.VelocityModule;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.subscriber.Subscribe;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static wtf.uitems.client.Constants.mc;

public final class BufferVelocity extends VelocityMode {

    private final NumberProperty attacks = new NumberProperty("AttackCounts", 3.0F, 1.0F, 5.0F, 1.0F)
            .hideIf(() -> this.module.getActiveMode() != this);
    private final BooleanProperty delayTillGround = new BooleanProperty("Delay Till Ground", false)
            .hideIf(() -> this.module.getActiveMode() != this);
    private final NumberProperty maxDelayMs = new NumberProperty("MaxDelay (ms)", 5000.0F, 50.0F, 10000.0F, 50.0F)
            .hideIf(() -> this.module.getActiveMode() != this || delayTillGround.getValue());
    private final BooleanProperty debug = new BooleanProperty("Debug", false)
            .hideIf(() -> this.module.getActiveMode() != this);

    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private long lastLag;
    private long velocityTime;
    private Entity target;
    private Stage stage = Stage.NONE;
    private boolean canAttack;
    private boolean receiveDamage;
    private int attacksRemaining;
    private int hitSelectSkips;

    public BufferVelocity(final VelocityModule module) {
        super(module);
        module.addProperties(attacks, delayTillGround, maxDelayMs, debug);
    }

    private void debugLog(final String msg) {
        if (!debug.getValue()) return;
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("\u00a77[\u00a7bVelocity\u00a77] \u00a7f" + msg), false);
        }
        System.out.println("[Velocity] " + msg);
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (mc.player == null) {
            return;
        }

        final Packet<?> packet = event.getPacket();
        final int selfId = mc.player.getId();

        if (packet instanceof EntityDamageS2CPacket damagePacket && damagePacket.entityId() == selfId) {
            receiveDamage = true;
            return;
        }

        if (packet instanceof PlayerPositionLookS2CPacket) {
            debugLog("Received PlayerPositionLookS2CPacket, clearing state.");
            lastLag = System.currentTimeMillis();
            packets.add(packet);
            event.setCancelled();
            clear(true);
            return;
        }

        if (packet instanceof EntityVelocityUpdateS2CPacket v && v.getEntityId() == selfId) {
            if (!receiveDamage) {
                return;
            }
            receiveDamage = false;

            if (mc.player.isUsingItem()) {
                clear(true);
                return;
            }

            if (stage == Stage.NONE) {
                if (System.currentTimeMillis() - lastLag >= 500L) {
                    stage = Stage.DELAY;
                    velocityTime = System.currentTimeMillis();
                    packets.add(packet);
                    event.setCancelled();
                    debugLog("Buffered velocity packet (Stage.NONE -> DELAY).");
                }
                return;
            }

            if (stage == Stage.DELAY) {
                packets.add(packet);
                event.setCancelled();
                debugLog("Buffered velocity packet (Stage -> " + stage + ").");
                return;
            }
        }

        if (stage == Stage.DELAY) {
            if (packet instanceof CommonPingS2CPacket) {
                packets.add(packet);
                event.setCancelled();
            }
        }
    }

    @Subscribe
    public void onMoveInput(final MoveInputEvent event) {
        if (mc.player == null || stage != Stage.DELAY) {
            return;
        }

        Entity targetEntity = null;
        wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule killAura = wtf.uitems.client.OpalClient.getInstance().getModuleRepository().getModule(wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTargeting().getTarget() != null) {
            targetEntity = killAura.getTargeting().getTarget().getEntity();
        } else if (mc.crosshairTarget instanceof EntityHitResult ehr) {
            targetEntity = ehr.getEntity();
        }

        if (targetEntity instanceof PlayerEntity p && !AntiBotModule.isBot(p)) {
            event.setForward(1F);
            event.setSideways(0F);
            canAttack = true;
            target = p;
            if (!mc.player.isSprinting()) {
                mc.player.setSprinting(true);
            }
        }
    }

    @Subscribe
    public void onPreTick(final PreGameTickEvent event) {
        if (mc.player == null) {
            return;
        }

        if (stage == Stage.DELAY && mc.player.isUsingItem()) {
            clear(true);
            return;
        }

        if (stage == Stage.DELAY && !canAttack) {
            Entity targetEntity = null;
            wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule killAura = wtf.uitems.client.OpalClient.getInstance().getModuleRepository().getModule(wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule.class);
            if (killAura != null && killAura.isEnabled() && killAura.getTargeting().getTarget() != null) {
                targetEntity = killAura.getTargeting().getTarget().getEntity();
            } else if (mc.crosshairTarget instanceof EntityHitResult ehr) {
                targetEntity = ehr.getEntity();
            }

            if (targetEntity instanceof PlayerEntity p && !AntiBotModule.isBot(p)) {
                canAttack = true;
                target = p;
                if (!mc.player.isSprinting()) {
                    mc.player.setSprinting(true);
                }
            }
        }

        if (canAttack) {
            canAttack = false;
            stage = Stage.ATTACK;
            attacksRemaining = attacks.getValue().intValue();
            hitSelectSkips = attacksRemaining;
            debugLog("Entering ATTACK stage, flushing " + packets.size() + " packets.");
            flushQueuedPackets();
        }

        final boolean timedOut = stage == Stage.DELAY
                && !delayTillGround.getValue()
                && System.currentTimeMillis() - velocityTime >= maxDelayMs.getValue().longValue();
        final boolean reachedGround = stage == Stage.DELAY
                && delayTillGround.getValue()
                && mc.player.isOnGround();

        if (timedOut || reachedGround || (System.currentTimeMillis() - lastLag < 500L && stage == Stage.DELAY)) {
            if (stage != Stage.NONE) {
                debugLog("Clearing state due to " + (timedOut ? "timeout" : (reachedGround ? "reached ground" : "recent lag")));
            }
            clear(true);
        }

        if (stage == Stage.ATTACK) {
            if (attacksRemaining > 0 && target != null && !target.isRemoved()) {
                debugLog("Attacking target (" + attacksRemaining + " remaining)");
                mc.player.setSprinting(false);
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.setVelocity(mc.player.getVelocity().multiply(0.6D, 1.0D, 0.6D));
                attacksRemaining--;

                if (attacksRemaining > 0) {
                    mc.player.setSprinting(true);
                } else {
                    stage = Stage.NONE;
                }
            } else {
                stage = Stage.NONE;
                attacksRemaining = 0;
            }
        }
    }

    private void clear(final boolean flush) {
        if (stage != Stage.NONE && debug.getValue()) {
            debugLog("Clearing state. Flush: " + flush + " (Packets: " + packets.size() + ")");
        }
        stage = Stage.NONE;
        target = null;
        canAttack = false;
        attacksRemaining = 0;
        if (flush) {
            flushQueuedPackets();
        } else {
            packets.clear();
        }
    }

    private void flushQueuedPackets() {
        if (mc.getNetworkHandler() == null) {
            packets.clear();
            return;
        }
        while (!packets.isEmpty()) {
            final Packet<?> p = packets.poll();
            if (p != null) {
                try {
                    ((Packet) p).apply(mc.getNetworkHandler());
                } catch (Exception ignored) {
                }
            }
        }
    }

    public boolean isAttacking() {
        return stage == Stage.ATTACK;
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
        lastLag = 0L;
        velocityTime = 0L;
        target = null;
        stage = Stage.NONE;
        canAttack = false;
        receiveDamage = false;
        attacksRemaining = 0;
        hitSelectSkips = 0;
        packets.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clear(true);
        receiveDamage = false;
        hitSelectSkips = 0;
    }

    @Override
    public Enum<?> getEnumValue() {
        return VelocityModule.Mode.BUFFER;
    }

    @Override
    public String getSuffix() {
        return stage == Stage.DELAY ? "Buffer " + (System.currentTimeMillis() - velocityTime) / 50 + "Ticks" : "Buffer";
    }

    private enum Stage {
        NONE, DELAY, ATTACK
    }
}
