package wtf.uitems.client.feature.module.impl.combat;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.TrackedPosition;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.client.feature.helper.impl.player.packet.blockage.block.holder.BlockHolder;
import wtf.uitems.client.feature.helper.impl.player.packet.blockage.impl.InboundNetworkBlockage;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.client.renderer.world.WorldRenderer;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.packet.InstantaneousReceivePacketEvent;
import wtf.uitems.event.impl.game.player.interaction.AttackEvent;
import wtf.uitems.event.impl.render.RenderWorldEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.mixin.EntityAccessor;
import wtf.uitems.mixin.EntityS2CPacketAccessor;
import wtf.uitems.utility.player.PlayerUtility;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.CustomRenderLayers;

import static wtf.uitems.client.Constants.mc;

public final class BacktrackModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 6.0, 2.0, 8.0, 0.1);
    private final NumberProperty delay = new NumberProperty("Delay", "ms", 150.0, 0.0, 750.0, 10.0);
    private final NumberProperty chance = new NumberProperty("Chance", "%", 100.0, 0.0, 100.0, 1.0);
    private final NumberProperty targetTimeout = new NumberProperty("Target Timeout", "ms", 1000.0, 100.0, 4000.0, 50.0);
    private final BooleanProperty resetOnAttack = new BooleanProperty("Reset On Attack", false);
    private final BooleanProperty render = new BooleanProperty("Render", true);

    private final BlockHolder inboundBlock = new BlockHolder(InboundNetworkBlockage.get());

    private TrackedTarget trackedTarget;
    private long blockStartTime;

    public BacktrackModule() {
        super("Backtrack", "Queues target updates to keep a hittable ghost position.", ModuleCategory.COMBAT);
        addProperties(range, delay, chance, targetTimeout, resetOnAttack, render);
    }

    @Subscribe
    public void onAttack(final AttackEvent event) {
        if (mc.player == null || mc.world == null || !(event.getTarget() instanceof LivingEntity livingEntity) || livingEntity == mc.player) {
            return;
        }

        if (livingEntity instanceof PlayerEntity playerEntity && (AntiBotModule.isBot(playerEntity) || TeamsModule.isTeammate(playerEntity))) {
            clearTarget(true);
            return;
        }

        if (Math.random() * 100.0 > chance.getValue()) {
            clearTarget(true);
            return;
        }

        if (trackedTarget == null || trackedTarget.entity.getId() != livingEntity.getId() || resetOnAttack.getValue()) {
            trackedTarget = new TrackedTarget(livingEntity);
            releasePackets();
        } else {
            trackedTarget.lastAttackTime = System.currentTimeMillis();
        }
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.world == null) {
            clearTarget(true);
            return;
        }

        if (trackedTarget == null) {
            releasePackets();
            return;
        }

        if (!isTargetValid(trackedTarget.entity)) {
            clearTarget(true);
            return;
        }

        final long now = System.currentTimeMillis();
        if (now - trackedTarget.lastAttackTime > targetTimeout.getValue().longValue()) {
            clearTarget(true);
            return;
        }

        if (inboundBlock.isBlocking() && now - blockStartTime >= delay.getValue().longValue()) {
            releasePackets();
        }

        updateBlockingState();
    }

    @Subscribe
    public void onInstantaneousReceivePacket(final InstantaneousReceivePacketEvent event) {
        if (mc.player == null || mc.world == null || trackedTarget == null) {
            return;
        }

        final Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerPositionLookS2CPacket) {
            clearTarget(true);
            return;
        }

        if (packet instanceof EntitiesDestroyS2CPacket destroyPacket) {
            if (destroyPacket.getEntityIds().contains(trackedTarget.entity.getId())) {
                clearTarget(true);
            }
            return;
        }

        if (packet instanceof EntityS2CPacket movePacket) {
            final EntityS2CPacketAccessor accessor = (EntityS2CPacketAccessor) movePacket;
            if (accessor.getId() == trackedTarget.entity.getId()) {
                trackedTarget.serverPosition.setPos(trackedTarget.serverPosition.withDelta(
                        accessor.getDeltaX(),
                        accessor.getDeltaY(),
                        accessor.getDeltaZ()
                ));
                trackedTarget.lastServerSyncTime = System.currentTimeMillis();
                updateBlockingState();
            }
            return;
        }

        if (packet instanceof EntityPositionS2CPacket positionPacket && positionPacket.entityId() == trackedTarget.entity.getId()) {
            trackedTarget.serverPosition.setPos(positionPacket.change().position());
            trackedTarget.lastServerSyncTime = System.currentTimeMillis();
            updateBlockingState();
            return;
        }

        if (packet instanceof EntityPositionSyncS2CPacket syncPacket && syncPacket.id() == trackedTarget.entity.getId()) {
            trackedTarget.serverPosition.setPos(syncPacket.values().position());
            trackedTarget.lastServerSyncTime = System.currentTimeMillis();
            updateBlockingState();
        }
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (!render.getValue() || trackedTarget == null || mc.player == null || mc.world == null || !isTargetValid(trackedTarget.entity)) {
            return;
        }

        final Box box = ((EntityAccessor) trackedTarget.entity).callCalculateDefaultBoundingBox(trackedTarget.serverPosition.getPos())
                .expand(trackedTarget.entity.getTargetingMargin());
        final Vec3d dimensions = new Vec3d(box.getLengthX(), box.getLengthY(), box.getLengthZ());
        final Vec3d position = new Vec3d(box.minX, box.minY, box.minZ);

        final VertexConsumerProvider.Immediate vertexConsumers = VertexConsumerProvider.immediate(new BufferAllocator(2048));
        final WorldRenderer renderer = new WorldRenderer(vertexConsumers);
        final int themeColor = ColorUtility.getClientTheme().first;

        renderer.drawFilledCube(
                event.matrixStack(),
                CustomRenderLayers.getPositionColorQuads(true),
                position,
                dimensions,
                ColorUtility.applyOpacity(themeColor, 0.18F)
        );
        drawBoxOutline(renderer, event, box, ColorUtility.applyOpacity(themeColor, 0.8F));
        vertexConsumers.draw();
    }

    private void updateBlockingState() {
        if (mc.player == null || trackedTarget == null || !isTargetValid(trackedTarget.entity)) {
            clearTarget(true);
            return;
        }

        final Vec3d eyePos = mc.player.getEyePos();
        final Box serverBox = ((EntityAccessor) trackedTarget.entity).callCalculateDefaultBoundingBox(trackedTarget.serverPosition.getPos())
                .expand(trackedTarget.entity.getTargetingMargin());
        final Vec3d closestServerPoint = PlayerUtility.getClosestVectorToBox(eyePos, serverBox);
        final Vec3d closestClientPoint = PlayerUtility.getClosestVectorToBoundingBox(eyePos, trackedTarget.entity);

        final double serverDistance = eyePos.distanceTo(closestServerPoint);
        final double clientDistance = eyePos.distanceTo(closestClientPoint);
        final double interactionRange = mc.player.getEntityInteractionRange();
        final boolean shouldBlock = clientDistance <= interactionRange
                && serverDistance <= range.getValue()
                && serverDistance + 0.05 > clientDistance
                && System.currentTimeMillis() - trackedTarget.lastServerSyncTime <= targetTimeout.getValue().longValue();

        if (shouldBlock) {
            if (!inboundBlock.isBlocking()) {
                inboundBlock.block(null, InboundNetworkBlockage.VISUAL_VALIDATOR);
                blockStartTime = System.currentTimeMillis();
            }
        } else {
            releasePackets();
        }
    }

    private boolean isTargetValid(final LivingEntity entity) {
        return entity != null && entity.isAlive() && !entity.isRemoved();
    }

    private void releasePackets() {
        if (inboundBlock.isBlocking()) {
            inboundBlock.release();
        }
    }

    private void clearTarget(final boolean flushPackets) {
        trackedTarget = null;
        if (flushPackets) {
            releasePackets();
        }
    }

    private void drawBoxOutline(final WorldRenderer renderer, final RenderWorldEvent event, final Box box, final int color) {
        final Vec3d min = new Vec3d(box.minX, box.minY, box.minZ);
        final Vec3d max = new Vec3d(box.maxX, box.maxY, box.maxZ);

        final Vec3d a = new Vec3d(min.x, min.y, min.z);
        final Vec3d b = new Vec3d(max.x, min.y, min.z);
        final Vec3d c = new Vec3d(max.x, min.y, max.z);
        final Vec3d d = new Vec3d(min.x, min.y, max.z);
        final Vec3d e = new Vec3d(min.x, max.y, min.z);
        final Vec3d f = new Vec3d(max.x, max.y, min.z);
        final Vec3d g = new Vec3d(max.x, max.y, max.z);
        final Vec3d h = new Vec3d(min.x, max.y, max.z);

        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), a, b, color);
        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), b, c, color);
        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), c, d, color);
        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), d, a, color);

        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), e, f, color);
        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), f, g, color);
        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), g, h, color);
        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), h, e, color);

        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), a, e, color);
        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), b, f, color);
        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), c, g, color);
        renderer.drawLine(event.matrixStack(), CustomRenderLayers.getLines(2.0F, true), d, h, color);
    }

    @Override
    protected void onEnable() {
        clearTarget(true);
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        clearTarget(true);
        super.onDisable();
    }

    private static final class TrackedTarget {
        private final LivingEntity entity;
        private final TrackedPosition serverPosition = new TrackedPosition();
        private long lastAttackTime;
        private long lastServerSyncTime;

        private TrackedTarget(final LivingEntity entity) {
            this.entity = entity;
            this.serverPosition.setPos(new Vec3d(entity.getX(), entity.getY(), entity.getZ()));
            this.lastAttackTime = System.currentTimeMillis();
            this.lastServerSyncTime = this.lastAttackTime;
        }
    }
}
