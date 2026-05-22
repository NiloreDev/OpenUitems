package wtf.uitems.client.feature.module.impl.combat;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EggItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.helper.impl.LocalDataWatch;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.helper.impl.player.swing.CPSProperty;
import wtf.uitems.client.feature.helper.impl.player.swing.SwingDelay;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.uitems.client.feature.module.impl.combat.killaura.target.CurrentTarget;
import wtf.uitems.client.feature.module.impl.world.scaffold.ScaffoldModule;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.input.MouseHandleInputEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.player.PlayerUtility;
import wtf.uitems.utility.player.RotationUtility;

import static wtf.uitems.client.Constants.mc;

public final class AutoShootModule extends Module {

    private static final double GENERIC_PROJECTILE_SPEED = 1.5D;
    private static final double GENERIC_PROJECTILE_GRAVITY = 0.03D;

    private final ModeProperty<ThrowableType> throwableType = new ModeProperty<>("Throwable Type", ThrowableType.EGG_AND_SNOWBALL);
    private final ModeProperty<GravityType> gravityType = new ModeProperty<>("Gravity Type", GravityType.AUTO);
    private final NumberProperty range = new NumberProperty("Range", 6.0, 1.0, 32.0, 0.5);
    private final NumberProperty aimOffThreshold = new NumberProperty("Aim Off Threshold", 2.0, 0.5, 10.0, 0.5);
    private final BooleanProperty selectSlotAutomatically = new BooleanProperty("Select Slot Automatically", true);
    private final NumberProperty ticksUntilReset = new NumberProperty("Ticks Until Slot Reset", 1.0, 0.0, 20.0, 1.0);
    private final BooleanProperty requiresKillAura = new BooleanProperty("Requires KillAura", false);
    private final BooleanProperty notDuringCombat = new BooleanProperty("Not During Combat", false);
    private final CPSProperty throwCps = new CPSProperty(this, "Throw CPS", false);

    private AimContext preparedAim;
    private int restoreTicks = -1;

    public AutoShootModule() {
        super("Auto Shoot", "Automatically throws eggs and snowballs at nearby players.", ModuleCategory.COMBAT);
        addProperties(throwableType, gravityType, range, aimOffThreshold, selectSlotAutomatically, ticksUntilReset, requiresKillAura, notDuringCombat);
    }

    @Subscribe(priority = -10)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            preparedAim = null;
            restoreTicks = -1;
            return;
        }

        if (restoreTicks >= 0) {
            if (restoreTicks == 0) {
                final SlotHelper slotHelper = SlotHelper.getInstance();
                slotHelper.stop();
                slotHelper.sync(true, true);
                restoreTicks = -1;
            } else {
                restoreTicks--;
            }
        }

        if (shouldPauseAutoShoot()) {
            preparedAim = null;
            return;
        }

        final LivingEntity target = selectTarget();
        if (target == null) {
            preparedAim = null;
            return;
        }

        final ThrowableSlot slot = findThrowableSlot();
        if (slot == null) {
            preparedAim = null;
            return;
        }

        final Vec2f rotation = getRotation(target, slot.stack().getItem());
        if (rotation == null) {
            preparedAim = null;
            return;
        }

        RotationHelper.getHandler().rotate(rotation, InstantRotationModel.INSTANCE);
        preparedAim = new AimContext(target.getId(), rotation, slot);
    }

    @Subscribe(priority = -10)
    public void onMouseHandleInput(final MouseHandleInputEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null || mc.getOverlay() != null) {
            return;
        }

        if (shouldPauseAutoShoot()) {
            preparedAim = null;
            return;
        }

        if (preparedAim == null || !SwingDelay.isSwingAvailable(throwCps, false)) {
            return;
        }

        final Entity entity = mc.world.getEntityById(preparedAim.targetId());
        if (!(entity instanceof LivingEntity target) || !isValidTarget(target)) {
            preparedAim = null;
            return;
        }

        final ThrowableSlot slot = refreshSlot(preparedAim.slot());
        if (slot == null) {
            preparedAim = null;
            return;
        }

        final Vec2f rotation = getRotation(target, slot.stack().getItem());
        if (rotation == null) {
            preparedAim = null;
            return;
        }

        final double rotationDifference = RotationUtility.getRotationDifference(RotationUtility.getRotation(), rotation);
        if (rotationDifference > aimOffThreshold.getValue()) {
            preparedAim = new AimContext(target.getId(), rotation, slot);
            return;
        }

        if (!slot.offhand() && selectSlotAutomatically.getValue()) {
            SlotHelper.getInstance().setTargetItem(slot.slot()).silence(SlotHelper.Silence.DEFAULT);
            restoreTicks = ticksUntilReset.getValue().intValue();
        } else if (!slot.offhand()) {
            preparedAim = null;
            return;
        }

        mc.interactionManager.interactItem(mc.player, slot.offhand() ? Hand.OFF_HAND : Hand.MAIN_HAND);
        throwCps.resetClick();
        SwingDelay.reset();
        preparedAim = null;
    }

    private LivingEntity selectTarget() {
        if (requiresKillAura.getValue()) {
            final KillAuraModule killAura = OpalClient.getInstance().getModuleRepository().getModule(KillAuraModule.class);
            if (killAura == null || !killAura.isEnabled() || !killAura.getTargeting().isTargetSelected()) {
                return null;
            }

            final CurrentTarget currentTarget = killAura.getTargeting().getTarget();
            final LivingEntity target = currentTarget == null ? null : currentTarget.getEntity();
            return isValidTarget(target) ? target : null;
        }

        if (notDuringCombat.getValue() && LocalDataWatch.get().lastEntityAttack.getRight() != null) {
            return null;
        }

        LivingEntity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        for (final Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity playerEntity) || !isValidTarget(playerEntity)) {
                continue;
            }

            final double distance = PlayerUtility.getDistanceToEntity(playerEntity);
            if (distance > range.getValue()) {
                continue;
            }

            if (!hasLineOfSight(playerEntity)) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                bestTarget = playerEntity;
            }
        }

        return bestTarget;
    }

    private boolean shouldPauseAutoShoot() {
        if (mc.player == null) {
            return true;
        }

        if (isConsumingFoodOrPotion()) {
            return true;
        }

        final ScaffoldModule scaffoldModule = OpalClient.getInstance().getModuleRepository().getModule(ScaffoldModule.class);
        return scaffoldModule != null && scaffoldModule.isEnabled();
    }

    private boolean isConsumingFoodOrPotion() {
        if (!mc.player.isUsingItem()) {
            return false;
        }

        final ItemStack stack = mc.player.getActiveItem();
        return !stack.isEmpty() && (stack.contains(DataComponentTypes.FOOD) || stack.getItem() instanceof PotionItem);
    }

    private boolean isValidTarget(final LivingEntity entity) {
        if (!(entity instanceof PlayerEntity playerEntity) || entity == mc.player || entity.isRemoved() || !entity.isAlive()) {
            return false;
        }

        if (entity.isInvisible()) {
            return false;
        }

        if (AntiBotModule.isBot(playerEntity) || AntiBotModule.isBedWarsBot(playerEntity) || TeamsModule.isTeammate(playerEntity)) {
            return false;
        }

        return !LocalDataWatch.getFriendList().contains(playerEntity.getName().getString().toUpperCase());
    }

    private boolean hasLineOfSight(final LivingEntity entity) {
        final Vec3d eyePos = mc.player.getEyePos();
        final Vec3d targetPos = PlayerUtility.getClosestVectorToBoundingBox(eyePos, entity);
        final HitResult hitResult = mc.world.raycast(new RaycastContext(
                eyePos,
                targetPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hitResult.getType() == HitResult.Type.MISS;
    }

    private ThrowableSlot findThrowableSlot() {
        if (throwableType.getValue() == ThrowableType.EGG_AND_SNOWBALL) {
            if (isEggOrSnowball(mc.player.getOffHandStack())) {
                return new ThrowableSlot(true, -1, mc.player.getOffHandStack());
            }

            for (int i = 0; i < 9; i++) {
                final ItemStack stack = mc.player.getInventory().getStack(i);
                if (isEggOrSnowball(stack)) {
                    return new ThrowableSlot(false, i, stack);
                }
            }

            return null;
        }

        if (!mc.player.getMainHandStack().isEmpty()) {
            return new ThrowableSlot(false, mc.player.getInventory().getSelectedSlot(), mc.player.getMainHandStack());
        }

        if (!mc.player.getOffHandStack().isEmpty()) {
            return new ThrowableSlot(true, -1, mc.player.getOffHandStack());
        }

        return null;
    }

    private ThrowableSlot refreshSlot(final ThrowableSlot slot) {
        if (slot.offhand()) {
            return !mc.player.getOffHandStack().isEmpty() && slot.stack().getItem() == mc.player.getOffHandStack().getItem()
                    ? new ThrowableSlot(true, -1, mc.player.getOffHandStack())
                    : null;
        }

        if (slot.slot() < 0 || slot.slot() > 8) {
            return null;
        }

        final ItemStack stack = mc.player.getInventory().getStack(slot.slot());
        return stack.isEmpty() ? null : new ThrowableSlot(false, slot.slot(), stack);
    }

    private boolean isEggOrSnowball(final ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof EggItem || stack.getItem() instanceof SnowballItem);
    }

    private Vec2f getRotation(final LivingEntity target, final Item item) {
        final GravityType resolvedGravity = gravityType.getValue() == GravityType.AUTO
                ? resolveAutoGravity(item)
                : gravityType.getValue();

        if (resolvedGravity == GravityType.LINEAR) {
            final Vec3d eyePos = mc.player.getEyePos();
            final Vec3d point = PlayerUtility.getClosestVectorToBoundingBox(eyePos, target);
            return RotationUtility.getRotationFromPosition(point);
        }

        return solveProjectileRotation(target);
    }

    private GravityType resolveAutoGravity(final Item item) {
        if (item instanceof EggItem || item instanceof SnowballItem) {
            return GravityType.PROJECTILE;
        }

        return GravityType.LINEAR;
    }

    private Vec2f solveProjectileRotation(final LivingEntity target) {
        final Vec3d start = mc.player.getEyePos();
        final Vec3d end = PlayerUtility.getClosestVectorToBoundingBox(start, target);
        final Vec3d diff = end.subtract(start);
        final double horizontalDistance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (horizontalDistance < 1.0E-4) {
            return null;
        }

        final double speedSq = GENERIC_PROJECTILE_SPEED * GENERIC_PROJECTILE_SPEED;
        final double discriminant = speedSq * speedSq
                - GENERIC_PROJECTILE_GRAVITY * (GENERIC_PROJECTILE_GRAVITY * horizontalDistance * horizontalDistance + 2.0D * diff.y * speedSq);
        if (discriminant < 0.0D) {
            return null;
        }

        final double sqrt = Math.sqrt(discriminant);
        final double tan = (speedSq - sqrt) / (GENERIC_PROJECTILE_GRAVITY * horizontalDistance);
        final float yaw = (float) Math.toDegrees(-Math.atan2(diff.x, diff.z));
        final float pitch = (float) -Math.toDegrees(Math.atan(tan));

        if (Float.isNaN(yaw) || Float.isNaN(pitch) || pitch < -90.0F || pitch > 90.0F) {
            return null;
        }

        return new Vec2f(yaw, pitch);
    }

    @Override
    protected void onDisable() {
        final SlotHelper slotHelper = SlotHelper.getInstance();
        slotHelper.stop();
        slotHelper.sync(true, true);
        preparedAim = null;
        restoreTicks = -1;
        super.onDisable();
    }

    private enum ThrowableType {
        EGG_AND_SNOWBALL("EggAndSnowball"),
        ANYTHING("Anything");

        private final String name;

        ThrowableType(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum GravityType {
        AUTO("Auto"),
        LINEAR("Linear"),
        PROJECTILE("Projectile");

        private final String name;

        GravityType(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private record ThrowableSlot(boolean offhand, int slot, ItemStack stack) {
    }

    private record AimContext(int targetId, Vec2f rotation, ThrowableSlot slot) {
    }
}
