package wtf.uitems.client.feature.module.impl.world.breaker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.AirBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import wtf.uitems.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationProperty;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.dynamicisland.DynamicIslandElement;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.duck.ClientPlayerInteractionManagerAccess;
import wtf.uitems.event.impl.game.PostGameTickEvent;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.player.interaction.CancelBlockBreakingEvent;
import wtf.uitems.event.impl.game.player.interaction.VisualSwingEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.player.PlayerUtility;
import wtf.uitems.utility.player.RaycastUtility;
import wtf.uitems.utility.player.RotationUtility;

import java.util.*;

import static wtf.uitems.client.Constants.mc;

public final class BreakerModule extends Module {

    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final NumberProperty range = new NumberProperty("Range", 4.5F, 0.5F, 6F, 0.5F);

    private final ModeProperty<SwingMode> swingMode = new ModeProperty<>("Swing mode", SwingMode.CLIENT);
    private final ModeProperty<BreakMode> breakMode = new ModeProperty<>("Break Mode", BreakMode.PACKET);
    private final BooleanProperty breakSurroundings = new BooleanProperty("Break surroundings", true);
    private final BooleanProperty max1Layer = new BooleanProperty("Max 1 layer", true).hideIf(() -> !shouldTargetSurrounding());
    private final NumberProperty minLayerBreak = new NumberProperty("Min Layer Break", 1, 1, 5, 1).hideIf(() -> !shouldTargetSurrounding() || max1Layer.getValue());
    private final BooleanProperty clickBeforeBreak =  new BooleanProperty("Click before breaking", true).hideIf(() -> breakMode.is(BreakMode.LEGIT));
    private final ModeProperty<ToolSwitchTiming> toolSwitchTiming = new ModeProperty<>("Tool Switch Timing", ToolSwitchTiming.ALWAYS);
    private final BooleanProperty interactAfterBreak = new BooleanProperty("Interact After Break", false);

    private final ModeProperty<RotateTiming> rotateTiming = new ModeProperty<>("Rotate Timing", RotateTiming.ALWAYS).hideIf(() -> breakMode.is(BreakMode.LEGIT));
    private final ModeProperty<RayCastMode> rayCastMode = new ModeProperty<>("RayCast Mode", RayCastMode.DIRECT);
    private final RotationProperty rotationProps = new RotationProperty(InstantRotationModel.INSTANCE, rotateTiming, rayCastMode);

    @Getter
    private BlockTarget currentTarget;
    @Getter
    private BlockPos lastBreaking;
    private Vec2f rotation;

    @Getter
    private boolean breaking, cancelVisualSwing;
    @Getter
    private int remainingTicks, slot;
    private long lastBedBreak;

//    private final List<BlockPos> debugRenderList = new ArrayList<>();
    private final BreakerIsland breakerIsland = new BreakerIsland(this);

    public BreakerModule() {
        super("Breaker", "Breaks relevant blocks for mini-games.", ModuleCategory.WORLD);
        addProperties(
                range,
                rotationProps.get(),
                new GroupProperty(
                        "Breaking",
                        swingMode, breakMode,
                        breakSurroundings,
                        max1Layer, minLayerBreak,
                        toolSwitchTiming, clickBeforeBreak,
                        interactAfterBreak
                )
        );
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        boolean runIsland = false;

        if (!shouldRun()) {
            this.breaking = false;
            return;
        }

        this.updateTargetBlock();

        if (this.currentTarget == null || mc.world.getBlockState(this.currentTarget.candidate.getPos()).getBlock() instanceof AirBlock) {
            this.breaking = false;
            return;
        }

        final BlockPos blockPos = this.currentTarget.candidate.pos;
        final ClientPlayerInteractionManagerAccess access = (ClientPlayerInteractionManagerAccess) mc.interactionManager;

        final float breakingDelta = mc.world.getBlockState(blockPos).calcBlockBreakingDelta(mc.player, mc.world, blockPos);
        final float breakingProgress = access.opal$currentBreakingProgress() + breakingDelta;

        this.rotation = RotationUtility.getRotationFromPosition(blockPos.toCenterPos());

        final double value = breakingProgress + breakingDelta;

        boolean startTick = !this.breaking, endTick = value >= 1;

        if (
                breakMode.is(BreakMode.LEGIT)
                        || rotateTiming.is(RotateTiming.ALWAYS)
                        || (rotateTiming.is(RotateTiming.START_AND_END) && (startTick || endTick))
                        || (rotateTiming.is(RotateTiming.START) && startTick)
                        ||  (rotateTiming.is(RotateTiming.END) && endTick)
        ) RotationHelper.getHandler().rotate(this.rotation, rotationProps.createModel());
        if (
                toolSwitchTiming.is(ToolSwitchTiming.ALWAYS)
                        || (toolSwitchTiming.is(ToolSwitchTiming.START_AND_END) && (startTick || endTick))
                        || (toolSwitchTiming.is(ToolSwitchTiming.START) && startTick)
        ) if (this.slot != -1) SlotHelper.setCurrentItem(this.slot).silence(SlotHelper.Silence.NONE);

        final BlockHitResult hitResult = this.getRaycastHitResult();
        if (hitResult == null) return;
        final Direction direction = hitResult.getSide();

        if (startTick) {
            boolean rayCastResult = rayCastMode.is(RayCastMode.DISABLED);
            if (!rayCastResult) {
                if (rayCastMode.is(RayCastMode.DIRECT)) {
                    Box blockHitbox = PlayerUtility.getBlockBox(blockPos);
                    RaycastUtility.SimpleRayCastResult result = RaycastUtility.rayCastBoxDirectly(
                            RaycastUtility.getCameraPosVec(1, mc.player),
                            mc.player.getYaw(), mc.player.getPitch(),
                            blockHitbox.getMinPos(), blockHitbox.getMaxPos()
                    );
                    rayCastResult = result.hit();
                }
            }

            this.remainingTicks = (int) (mc.world.getBlockState(blockPos).getHardness(mc.world, blockPos) * 20);

            final boolean success = rayCastResult && clickBlock(blockPos, direction);
            if (!success) {
                return;
            }

            this.breaking = true;
        }

        runIsland = updateBreakingProgress(blockPos, direction);
        switch (breakMode.getValue()) {
            case LEGIT -> {
                HitResult r = RaycastUtility.raycastBlock(
                        range.getValue(),
                        1,
                        false,
                        mc.player.getYaw(), mc.player.getPitch()
                );
                if (r instanceof BlockHitResult bhr) {
                    lastBreaking = bhr.getBlockPos();
                }
            }
            case PACKET -> {
                lastBreaking = this.currentTarget.candidate.getPos();
            }
        }

        if (runIsland) {
            DynamicIslandElement.addTrigger(breakerIsland);
        } else {
            DynamicIslandElement.removeTrigger(breakerIsland);
            breakerIsland.onDisable();
        }
    }

    @Subscribe
    public void onPostGameTick(final PostGameTickEvent event) {
        if (!this.breaking || mc.player == null) {
            return;
        }

        this.cancelVisualSwing = this.swingMode.is(SwingMode.SERVER);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (this.remainingTicks < 0) {
            this.breaking = false;

            if (this.currentTarget != null && mc.world.getBlockState(this.currentTarget.candidate.pos).getBlock() instanceof BedBlock) {
                this.lastBedBreak = System.currentTimeMillis();
            }
        }
    }

    @Subscribe
    public void onCancelBlockBreaking(final CancelBlockBreakingEvent event) {
        if (this.breaking) {
            event.setCancelled();
        }
    }

    @Subscribe
    public void onVisualSwing(final VisualSwingEvent event) {
        if (this.cancelVisualSwing) {
            this.cancelVisualSwing = false;
            event.setCancelled();
        }
    }

//    @Subscribe
//    public void onRender3D(RenderWorldEvent event) {
//        VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(new BufferAllocator(1024));
//        WorldRenderer rc = new WorldRenderer(vcp);
//        final Vec3d dimensions = new Vec3d(1, 1, 1);
//        for (BlockPos pos : debugRenderList) {
//            rc.drawFilledCube(
//                    event.matrixStack(),
//                    CustomRenderLayers.getPositionColorQuads(true),
//                    new Vec3d(pos), dimensions,
//                    0x30FFFFFF
//            );
//        }
//        vcp.draw();
//    }

    private boolean clickBlock(BlockPos blockPos, Direction direction) {
        if (breakMode.is(BreakMode.LEGIT)) {
            MouseHelper.getLeftButton().setPressed(true, remainingTicks);
            return true;
        }
        if (!clickBeforeBreak.getValue()) return true;
        return mc.interactionManager.attackBlock(blockPos, direction);
    }
    private boolean updateBreakingProgress(BlockPos blockPos, Direction direction) {
        switch (breakMode.getValue()) {
            case LEGIT -> {
                MouseHelper.getRightButton().setDisabled();
                this.remainingTicks--;
                return true;
            }
            case PACKET -> {
                if (mc.interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
                    MouseHelper.getRightButton().setDisabled();
                    MouseHelper.getLeftButton().setDisabled();
                    // TODO: add particles
                    //  mc.particleManager.addBlockBreakingParticles(blockPos, direction);
                    this.remainingTicks--;

                    this.cancelVisualSwing = this.swingMode.is(SwingMode.SERVER);
                    mc.player.swingHand(Hand.MAIN_HAND);

                    return true;
                }
            }
        }
        return false;
    }
    private void interactBlock(BlockPos pos, Vec3d hitPos, Direction direction) {
        mc.interactionManager.interactItem(
                mc.player, Hand.MAIN_HAND
        );
    }

    private void updateTargetBlock() {
        this.slot = -1;

        final Vec3d eyePos = mc.player.getEyePos();
        final float range = this.range.getValue().floatValue();

        final int fromX = (int) Math.floor(eyePos.x - range - 1);
        final int fromY = (int) Math.floor(eyePos.y - range - 1);
        final int fromZ = (int) Math.floor(eyePos.z - range - 1);

        final int toX = (int) Math.ceil(eyePos.x + range + 1);
        final int toY = (int) Math.ceil(eyePos.y + range + 1);
        final int toZ = (int) Math.ceil(eyePos.z + range + 1);

        final List<BlockCandidate> targetCandidates = new ArrayList<>();

        final Object ownBedColor = null;

        for (int x = fromX; x <= toX; x++) {
            for (int y = fromY; y <= toY; y++) {
                for (int z = fromZ; z <= toZ; z++) {
                    final BlockPos blockPos = new BlockPos(x, y, z);
                    final BlockState blockState = mc.world.getBlockState(blockPos);

                    // TODO: egg
                    if (!(blockState.getBlock() instanceof BedBlock bedBlock)) {
                        continue;
                    }

                    if (ownBedColor != null) {
                        continue;
                    }

                    final BlockCandidate candidate = new BlockCandidate(blockPos);
                    targetCandidates.add(candidate);

                    final BlockCandidate otherBedPartCandidate = candidate.offset(BedBlock.getOppositePartDirection(blockState));
                    targetCandidates.add(otherBedPartCandidate);
                }
            }
        }

        final BlockCandidate closestCandidate = targetCandidates.stream()
                .filter(c -> c.distance <= range)
                .min(Comparator.comparingDouble(c -> c.distance))
                .orElse(null);

        if (closestCandidate == null) {
            this.currentTarget = null;
            return;
        }

        if (!shouldTargetSurrounding()) {
            this.setTargetBlock(new BlockTarget(closestCandidate, 0.01));
            return;
        }

        List<List<BlockCandidate>> adjacentCandidates = new ArrayList<>();

        final BlockState bedState = mc.world.getBlockState(closestCandidate.pos);
        BlockCandidate otherBedPart = null;
        if (bedState.getBlock() instanceof BedBlock) {
            otherBedPart = closestCandidate.offset(BedBlock.getOppositePartDirection(bedState));
        }

        Set<BlockPos> currentLayer = new HashSet<>();

        currentLayer.add(closestCandidate.pos);
        if (otherBedPart != null) currentLayer.add(otherBedPart.pos);

        Set<BlockPos> visited = new HashSet<>(currentLayer);

        for (int i = 1; i <= minLayerBreak.getValue(); i++) {

            Set<BlockPos> nextLayer = new HashSet<>();
            List<BlockCandidate> layerList = new ArrayList<>();

            for (BlockPos pos : currentLayer) {
                for (Direction d : DIRECTIONS) {
                    BlockPos newPos = pos.offset(d);

                    if (visited.contains(newPos))
                        continue;

                    visited.add(newPos);

                    BlockCandidate candidate = new BlockCandidate(newPos);
                    layerList.add(candidate);
                    nextLayer.add(newPos);
                }
            }

            adjacentCandidates.add(layerList);
            currentLayer = nextLayer;
        }

        adjacentCandidates.replaceAll(blockCandidates -> blockCandidates.stream()
                .filter(c -> c.distance <= range)
                .sorted(Comparator.comparingDouble(c -> c.distance))
                .toList()
        );
        Collections.reverse(adjacentCandidates);

        layerProcess:
        for (int index = 0; index < adjacentCandidates.size(); index++) {
            boolean last = index == adjacentCandidates.size() - 1;
            List<BlockCandidate> layerCandidates = adjacentCandidates.get(index);

            for (final BlockCandidate adjacentCandidate : layerCandidates) {
                final BlockState blockState = mc.world.getBlockState(adjacentCandidate.pos);

                if (blockState.isAir() || !blockState.isFullCube(mc.world, adjacentCandidate.pos) || !blockState.getFluidState().isEmpty()) {
                    if (last) {
                        this.setTargetBlock(new BlockTarget(closestCandidate, 0.01));
                        return;
                    } else {
                        continue layerProcess;
                    }
                } else if (!last) {
                    if (max1Layer.getValue()) return;
                }
            }

            BlockCandidate weakestCandidate = null;
            double weakestCandidateResistance = Float.MAX_VALUE;
            int bestSlot = -1;

            for (final BlockCandidate adjacentCandidate : layerCandidates) {
                final BlockState blockState = mc.world.getBlockState(adjacentCandidate.pos);

                if (blockState.getBlock() instanceof BedBlock) {
                    continue;
                }

                double fastestMiningSpeed = SlotHelper.getInstance().getMainHandStack(mc.player).getMiningSpeedMultiplier(blockState);
                int bestSlotForCandidate = SlotHelper.getInstance().getSelectedSlot(mc.player.getInventory());

                for (int i = 0; i < 9; i++) {
                    if (i == SlotHelper.getInstance().getSelectedSlot(mc.player.getInventory())) {
                        continue;
                    }

                    float miningSpeed = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(blockState);
                    if (miningSpeed > fastestMiningSpeed) {
                        fastestMiningSpeed = miningSpeed;
                        bestSlotForCandidate = i;
                    }
                }

                double resistance = Math.max(0.01, blockState.getHardness(mc.world, adjacentCandidate.pos)) / fastestMiningSpeed;
                if (!breaking) {
                    final ClientPlayerInteractionManagerAccess access = (ClientPlayerInteractionManagerAccess) mc.interactionManager;
                    final BlockPos currentBreakingPos = access.opal$getCurrentBreakingPos();

                    if (currentBreakingPos != null && currentBreakingPos.equals(adjacentCandidate.pos)) {
                        resistance *= 1 - access.opal$currentBreakingProgress();
                    }
                }

                if (weakestCandidate == null || resistance < weakestCandidateResistance) {
                    weakestCandidate = adjacentCandidate;
                    weakestCandidateResistance = resistance;
                    bestSlot = bestSlotForCandidate;
                }
            }

            if (weakestCandidate == null) {
                return;
            }

            if (System.currentTimeMillis() - this.lastBedBreak < 500) {
                this.currentTarget = null;
                return;
            }

            this.slot = bestSlot;
            this.setTargetBlock(new BlockTarget(weakestCandidate, weakestCandidateResistance));
            return;
        }
    }
    private void setTargetBlock(final BlockTarget newTarget) {
        if (this.shouldUpdateTarget(newTarget)) {
            this.currentTarget = newTarget;
        }
    }
    private boolean shouldUpdateTarget(final BlockTarget newTarget) {
        if (this.currentTarget == null) {
            return true;
        }

        final BlockPos currentBlockPos = this.currentTarget.candidate.pos;
        final BlockState currentBlockState = mc.world.getBlockState(currentBlockPos);
        if (currentBlockState.isAir() || !currentBlockState.getFluidState().isEmpty()) {
            if (interactAfterBreak.getValue()) {
                Vec3d vector = RotationUtility.getRotationVector(mc.player.getPitch(), mc.player.getYaw());
                interactBlock(
                        currentBlockPos.offset(Direction.DOWN), vector, Direction.UP
                );
            }
            final BlockState newBlockState = mc.world.getBlockState(currentBlockPos);
            return newBlockState.isAir() || !newBlockState.getFluidState().isEmpty() || newBlockState.equals(currentBlockState);
        }

        // bed no longer exposed, update target to surrounding block
        if (shouldTargetSurrounding()
                && currentBlockState.getBlock() instanceof BedBlock
                && !(mc.world.getBlockState(newTarget.candidate.pos).getBlock() instanceof BedBlock)) {
            return true;
        }

        this.currentTarget.candidate.updateDistance();

        if (this.currentTarget.candidate.distance > this.range.getValue().floatValue()) {
            return true;
        }

        final float breakingProgress = ((ClientPlayerInteractionManagerAccess) mc.interactionManager).opal$currentBreakingProgress();
        final double remainingResistance = this.currentTarget.resistance * (1 - breakingProgress);
        if (remainingResistance < newTarget.resistance) {
            return false;
        }

        return true;
    }
    private BlockHitResult getRaycastHitResult() {
        if (this.rotation == null) {
            return null;
        }

        final HitResult hitResult = RaycastUtility.raycastBlock(this.range.getValue(), 1, false, this.rotation.x, this.rotation.y);
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return null;
        }

        return blockHitResult;
    }
    private boolean shouldRun() {
        return mc.player != null;
    }

    private boolean shouldTargetSurrounding() {
        if (breakMode.is(BreakMode.LEGIT)) return true;
        return breakSurroundings.getValue();
    }

    public static class BlockCandidate {
        private final BlockPos pos;
        private double distance;

        private BlockCandidate(final BlockPos pos) {
            this.pos = pos;
            this.updateDistance();
        }

        private BlockCandidate offset(final Direction direction) {
            return offset(direction, 1);
        }
        private BlockCandidate offset(final Direction direction, final int value) {
            return new BlockCandidate(pos.offset(direction, value));
        }

        private void updateDistance() {
            this.distance = PlayerUtility.getDistanceToBlock(pos);
        }

        public BlockPos getPos() {
            return pos;
        }
    }
    public record BlockTarget(BlockCandidate candidate, double resistance) {}

    @RequiredArgsConstructor
    private enum SwingMode {
        CLIENT("Client"),
        SERVER("Server");

        private final String name;
        @Override public String toString() {
            return name;
        }
    }
    @RequiredArgsConstructor
    private enum RotateTiming {
        START("Start"),
        END("End"),
        START_AND_END("Start & End"),
        ALWAYS("Always");

        private final String name;
        @Override public String toString() {
            return name;
        }
    }
    @RequiredArgsConstructor
    private enum ToolSwitchTiming {
        START("Start"),
        START_AND_END("Start & End"),
        ALWAYS("Always");

        private final String name;
        @Override public String toString() {
            return name;
        }
    }
    @RequiredArgsConstructor
    private enum RayCastMode {
        DISABLED("Disabled"),
        DIRECT("Direct");
        private final String name;
        @Override public String toString() {
            return name;
        }
    }
    @RequiredArgsConstructor
    private enum BreakMode {
        LEGIT("Legit"),
        PACKET("Packet");
        private final String name;
        @Override public String toString() {
            return name;
        }
    }
}
