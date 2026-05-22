package wtf.uitems.client.feature.module.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.uitems.client.feature.module.impl.combat.killaura.target.CurrentTarget;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.player.InventoryUtility;
import wtf.uitems.utility.player.RotationUtility;

import java.util.HashSet;
import java.util.Set;

import static wtf.uitems.client.Constants.mc;

public final class AutoPearlModule extends Module {

    private static final double PEARL_SPEED = 1.5D;
    private static final double PEARL_DRAG = 0.99D;
    private static final double PEARL_GRAVITY = 0.03D;
    private static final int MAX_SIMULATION_TICKS = 120;

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.TRIGGER);
    private final NumberProperty angle = new NumberProperty("Angle", 180.0, 0.0, 180.0, 1.0);
    private final NumberProperty minDistance = new NumberProperty("Min Distance", 8.0, 0.0, 24.0, 0.5);
    private final NumberProperty destinationError = new NumberProperty("Destination Error", 8.0, 0.5, 30.0, 0.5);
    private final NumberProperty cooldown = new NumberProperty("Cooldown", "ms", 500.0, 0.0, 3000.0, 50.0);
    private final BooleanProperty rotate = new BooleanProperty("Rotate", true);

    private final Set<Integer> trackedPearls = new HashSet<>();

    private PendingThrow pendingThrow;
    private boolean restoreSlot;
    private long lastThrowTime;

    public AutoPearlModule() {
        super("Auto Pearl", "Automatically mirrors pearl landings, including KillAura target follow throws.", ModuleCategory.COMBAT);
        addProperties(mode, angle, minDistance, destinationError, cooldown, rotate);
    }

    @Subscribe(priority = -10)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            resetRuntimeState();
            return;
        }

        if (restoreSlot) {
            final SlotHelper slotHelper = SlotHelper.getInstance();
            slotHelper.stop();
            slotHelper.sync(true, true);
            restoreSlot = false;
        }

        if (pendingThrow != null) {
            handlePendingThrow();
            return;
        }

        trackedPearls.removeIf(id -> mc.world.getEntityById(id) == null);

        for (final Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity pearl) || !trackedPearls.add(pearl.getId())) {
                continue;
            }

            processNewPearl(pearl);
            if (pendingThrow != null) {
                break;
            }
        }
    }

    private void handlePendingThrow() {
        if (pendingThrow == null || mc.player == null || mc.interactionManager == null) {
            pendingThrow = null;
            return;
        }

        if (pendingThrow.rotateFirst) {
            RotationHelper.getHandler().rotate(pendingThrow.rotation, InstantRotationModel.INSTANCE);
            pendingThrow.rotateFirst = false;
            return;
        }

        if (pendingThrow.useOffhand) {
            if (!mc.player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
                pendingThrow = null;
                return;
            }

            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
        } else {
            if (pendingThrow.hotbarSlot < 0 || pendingThrow.hotbarSlot > 8 || !mc.player.getInventory().getStack(pendingThrow.hotbarSlot).isOf(Items.ENDER_PEARL)) {
                pendingThrow = null;
                return;
            }

            SlotHelper.getInstance().setTargetItem(pendingThrow.hotbarSlot).silence(SlotHelper.Silence.DEFAULT);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            restoreSlot = true;
        }

        lastThrowTime = System.currentTimeMillis();
        pendingThrow = null;
    }

    private void processNewPearl(final EnderPearlEntity pearl) {
        if (System.currentTimeMillis() - lastThrowTime < cooldown.getValue().longValue()) {
            return;
        }

        final Entity owner = resolveOwner(pearl);
        if (!canTrigger(owner, pearl)) {
            return;
        }

        final Vec3d pearlPosition = new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ());
        final Vec3d destination = simulateLanding(pearlPosition, pearl.getVelocity());
        if (destination == null || destination.distanceTo(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ())) < minDistance.getValue()) {
            return;
        }

        final Vec2f rotation = solvePearlRotation(destination);
        if (rotation == null) {
            return;
        }

        final Vec3d simulatedDestination = simulateLanding(getPlayerPearlStart(rotation), getPlayerPearlVelocity(rotation));
        if (simulatedDestination == null || simulatedDestination.distanceTo(destination) > destinationError.getValue()) {
            return;
        }

        final int hotbarSlot = InventoryUtility.findItemInHotbar(Items.ENDER_PEARL);
        final boolean useOffhand = mc.player.getOffHandStack().isOf(Items.ENDER_PEARL);
        if (!useOffhand && hotbarSlot == -1) {
            return;
        }

        pendingThrow = new PendingThrow(rotation, hotbarSlot, useOffhand, rotate.getValue());
    }

    private boolean canTrigger(final Entity owner, final EnderPearlEntity pearl) {
        if (owner == mc.player) {
            return false;
        }

        final Vec2f currentRotation = RotationUtility.getRotation();
        final Vec2f pearlRotation = RotationUtility.getRotationFromPosition(new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ()));
        if (RotationUtility.getRotationDifference(currentRotation, pearlRotation) > angle.getValue()) {
            return false;
        }

        if (!(owner instanceof LivingEntity livingEntity) || livingEntity == mc.player || livingEntity.isRemoved() || !livingEntity.isAlive()) {
            return mode.getValue() == Mode.TRIGGER;
        }

        if (owner instanceof PlayerEntity playerEntity && (AntiBotModule.isBot(playerEntity) || TeamsModule.isTeammate(playerEntity))) {
            return false;
        }

        if (mode.getValue() == Mode.TARGET) {
            final KillAuraModule killAura = OpalClient.getInstance().getModuleRepository().getModule(KillAuraModule.class);
            if (killAura == null || !killAura.isEnabled()) {
                return false;
            }

            final CurrentTarget currentTarget = killAura.getTargeting().getTarget();
            return currentTarget != null && currentTarget.getEntity() == livingEntity && owner != null;
        }

        return true;
    }

    private Entity resolveOwner(final EnderPearlEntity pearl) {
        if (pearl.getOwner() != null) {
            return pearl.getOwner();
        }

        PlayerEntity closest = null;
        double closestDistance = 3.5D;
        for (final PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isRemoved() || !player.isAlive()) {
                continue;
            }

            final double distance = player.getEyePos().distanceTo(new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ()));
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = player;
            }
        }

        return closest;
    }

    private Vec2f solvePearlRotation(final Vec3d target) {
        final Vec3d start = mc.player.getEyePos();
        final Vec3d diff = target.subtract(start);
        final double horizontalDistance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (horizontalDistance < 1.0E-4) {
            return null;
        }

        final double speedSq = PEARL_SPEED * PEARL_SPEED;
        final double root = speedSq * speedSq - PEARL_GRAVITY * (PEARL_GRAVITY * horizontalDistance * horizontalDistance + 2.0D * diff.y * speedSq);
        if (root < 0.0D) {
            return null;
        }

        final double sqrt = Math.sqrt(root);
        final double tan = (speedSq - sqrt) / (PEARL_GRAVITY * horizontalDistance);
        final float yaw = (float) Math.toDegrees(-Math.atan2(diff.x, diff.z));
        final float pitch = (float) -Math.toDegrees(Math.atan(tan));

        if (Float.isNaN(yaw) || Float.isNaN(pitch) || pitch < -90.0F || pitch > 90.0F) {
            return null;
        }

        return new Vec2f(yaw, pitch);
    }

    private Vec3d getPlayerPearlStart(final Vec2f rotation) {
        final Vec3d eyePos = mc.player.getEyePos();
        final Vec3d direction = RotationUtility.getRotationVector(rotation.y, rotation.x);
        return eyePos.add(direction.multiply(0.16D));
    }

    private Vec3d getPlayerPearlVelocity(final Vec2f rotation) {
        final Vec3d direction = RotationUtility.getRotationVector(rotation.y, rotation.x).normalize().multiply(PEARL_SPEED);
        return direction.add(mc.player.getVelocity().multiply(1.0D, mc.player.isOnGround() ? 0.0D : 1.0D, 1.0D));
    }

    private Vec3d simulateLanding(final Vec3d start, final Vec3d startVelocity) {
        Vec3d position = start;
        Vec3d velocity = startVelocity;

        for (int tick = 0; tick < MAX_SIMULATION_TICKS; tick++) {
            final Vec3d nextPosition = position.add(velocity);
            final BlockHitResult hitResult = mc.world.raycast(new RaycastContext(
                    position,
                    nextPosition,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (hitResult.getType() != HitResult.Type.MISS) {
                return hitResult.getPos();
            }

            position = nextPosition;
            velocity = velocity.multiply(PEARL_DRAG).subtract(0.0D, PEARL_GRAVITY, 0.0D);

            if (position.y < mc.world.getBottomY() - 4) {
                return position;
            }
        }

        return position;
    }

    private void resetRuntimeState() {
        pendingThrow = null;
        restoreSlot = false;
        trackedPearls.clear();
    }

    @Override
    protected void onEnable() {
        resetRuntimeState();

        if (mc.world != null) {
            for (final Entity entity : mc.world.getEntities()) {
                if (entity instanceof EnderPearlEntity pearl) {
                    trackedPearls.add(pearl.getId());
                }
            }
        }

        super.onEnable();
    }

    @Override
    protected void onDisable() {
        if (restoreSlot) {
            final SlotHelper slotHelper = SlotHelper.getInstance();
            slotHelper.stop();
            slotHelper.sync(true, true);
        }

        resetRuntimeState();
        super.onDisable();
    }

    private enum Mode {
        TRIGGER("Trigger"),
        TARGET("Follow Target");

        private final String name;

        Mode(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class PendingThrow {
        private final Vec2f rotation;
        private final int hotbarSlot;
        private final boolean useOffhand;
        private boolean rotateFirst;

        private PendingThrow(final Vec2f rotation, final int hotbarSlot, final boolean useOffhand, final boolean rotateFirst) {
            this.rotation = rotation;
            this.hotbarSlot = hotbarSlot;
            this.useOffhand = useOffhand;
            this.rotateFirst = rotateFirst;
        }
    }
}
