package wtf.uitems.client.feature.module.impl.utility.nofall.impl;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.module.impl.utility.nofall.NoFallModule;
import wtf.uitems.client.feature.module.property.impl.mode.ModuleMode;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.mixin.PlayerInventoryAccessor;
import wtf.uitems.utility.player.RaycastUtility;
import wtf.uitems.utility.player.RotationUtility;

import static wtf.uitems.client.Constants.mc;

public final class AutoMLGNoFall extends ModuleMode<NoFallModule> {

    private static final float FALL_DISTANCE = 3.5F;

    private int restoreSlot = -1;
    private boolean attemptedThisFall;
    private boolean retrievePending;
    private int retrieveTriesLeft;
    private int retrieveSlot = -1;
    private BlockPos placedWaterPos;
    private int postRetrievePauseTicks;
    private int retryCooldownTicks;

    public AutoMLGNoFall(final NoFallModule module) {
        super(module);
    }

    @Override
    public Enum<?> getEnumValue() {
        return NoFallModule.Mode.AUTO_MLG;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    @Subscribe
    public void onPreTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.world == null) {
            resetState();
            return;
        }

        if (mc.player.getAbilities().allowFlying || mc.player.isSpectator()) {
            resetState();
            return;
        }

        if (postRetrievePauseTicks > 0) postRetrievePauseTicks--;
        if (retryCooldownTicks > 0) retryCooldownTicks--;

        if (restoreSlot != -1) {
            SlotHelper.setCurrentItem(restoreSlot);
            restoreSlot = -1;
        }

        if (mc.player.isOnGround() || mc.player.fallDistance <= 0.0F) {
            attemptedThisFall = false;
        }

        if (retrievePending) {
            handleRetrieve();
            return;
        }

        if (mc.player.fallDistance < FALL_DISTANCE) return;

        final double remaining = getRemainingFallDistance(8.0D);

        if (attemptedThisFall) {
            if (placedWaterPos == null && retryCooldownTicks == 0 && remaining > 0.0D && remaining <= 1.35D) {
                int retrySlot = findWaterBucketSlot();
                if (retrySlot != -1) {
                    placeWaterWithRetrieve(retrySlot, false);
                    retryCooldownTicks = 2;
                }
            }
            return;
        }

        if (!(remaining > 0.0D && remaining <= 2.25D)) {
            return;
        }

        int waterSlot = findWaterBucketSlot();
        if (waterSlot == -1) return;

        placeWaterWithRetrieve(waterSlot, true);
    }

    @Subscribe
    public void onMoveInput(final MoveInputEvent event) {
        if (postRetrievePauseTicks > 0) {
            event.setSneak(false);
        }
    }

    private void handleRetrieve() {
        if (mc.player == null || mc.world == null) {
            retrievePending = false;
            return;
        }

        if (retrieveTriesLeft-- <= 0) {
            retrievePending = false;
            return;
        }

        if (retrieveSlot == -1) {
            retrieveSlot = findEmptyBucketSlot();
            if (retrieveSlot == -1) {
                retrievePending = false;
                return;
            }
        }

        final ItemStack stack = mc.player.getInventory().getStack(retrieveSlot);
        if (stack.isOf(Items.WATER_BUCKET)) {
            retrievePending = false;
            retrieveSlot = -1;
            placedWaterPos = null;
            postRetrievePauseTicks = Math.max(postRetrievePauseTicks, 1);
            return;
        }

        if (placedWaterPos == null || !isWaterSource(placedWaterPos)) {
            retrievePending = false;
            retrieveSlot = -1;
            placedWaterPos = null;
            return;
        }

        final Vec2f rot = getLookDownRotationTo(placedWaterPos);
        final HitResult hit = RaycastUtility.raycastBlock(4.5, 1.0F, true, rot.x, rot.y);
        if (!(hit instanceof BlockHitResult bhr) || !bhr.getBlockPos().equals(placedWaterPos)) {
            retrievePending = false;
            retrieveSlot = -1;
            placedWaterPos = null;
            return;
        }

        requestRotation(rot);
        saveAndSwitch(retrieveSlot);
        useItem();
    }

    private void placeWaterWithRetrieve(final int waterSlot, final boolean markAttempted) {
        final Vec2f rot = new Vec2f(mc.player.getYaw(), 90.0F);
        requestRotation(rot);
        saveAndSwitch(waterSlot);
        useItem();

        if (markAttempted) {
            attemptedThisFall = true;
        }

        placedWaterPos = computePlacedWaterPos(rot);
        if (placedWaterPos != null) {
            retrievePending = true;
            retrieveTriesLeft = 2;
            retrieveSlot = -1;
        } else {
            retrievePending = false;
        }
    }

    private BlockPos computePlacedWaterPos(final Vec2f rot) {
        final HitResult hit = RaycastUtility.raycastBlock(4.5, 1.0F, false, rot.x, rot.y);
        if (!(hit instanceof BlockHitResult bhr)) {
            return null;
        }
        return bhr.getBlockPos().offset(bhr.getSide());
    }

    private boolean isWaterSource(final BlockPos pos) {
        if (mc.world == null) return false;
        return mc.world.getFluidState(pos).isStill() && !mc.world.getFluidState(pos).isEmpty();
    }

    private Vec2f getLookDownRotationTo(final BlockPos pos) {
        final Vec3d target = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        final Vec2f raw = RotationUtility.getRotationFromPosition(target);
        return new Vec2f(raw.x, 90.0F);
    }

    private void requestRotation(final Vec2f rotation) {
        RotationHelper.getHandler().rotate(rotation, InstantRotationModel.INSTANCE);
    }

    private void saveAndSwitch(final int targetSlot) {
        if (restoreSlot == -1 && mc.player != null) {
            restoreSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
        }
        SlotHelper.setCurrentItem(targetSlot);
    }

    private void useItem() {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private int findWaterBucketSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WATER_BUCKET)) {
                return i;
            }
        }
        return -1;
    }

    private int findEmptyBucketSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.BUCKET)) {
                return i;
            }
        }
        return -1;
    }

    private void resetState() {
        restoreSlot = -1;
        attemptedThisFall = false;
        retrievePending = false;
        retrieveTriesLeft = 0;
        retrieveSlot = -1;
        placedWaterPos = null;
        postRetrievePauseTicks = 0;
        retryCooldownTicks = 0;
    }

    private double getRemainingFallDistance(double maxRange) {
        if (mc.player == null || mc.world == null) {
            return Double.POSITIVE_INFINITY;
        }

        Vec3d start = new Vec3d(mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ());
        Vec3d end = start.add(0.0D, -maxRange, 0.0D);

        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            return Double.POSITIVE_INFINITY;
        }

        return start.y - hit.getPos().y;
    }
}
