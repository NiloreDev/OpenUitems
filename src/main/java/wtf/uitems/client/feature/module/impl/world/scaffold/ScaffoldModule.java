package wtf.uitems.client.feature.module.impl.world.scaffold;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.helper.impl.LocalDataWatch;
import wtf.uitems.client.feature.helper.impl.player.mouse.MouseButton;
import wtf.uitems.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.uitems.client.feature.helper.impl.player.rotation.handler.RotationMouseHandler;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.EnumRotationModel;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.IRotationModel;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.impl.InstantRotationModel;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.helper.impl.player.swing.SwingDelay;
import wtf.uitems.client.feature.helper.impl.render.FadingBlockHelper;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.movement.StuckModule;
import wtf.uitems.client.feature.module.impl.movement.flight.FlightModule;
import wtf.uitems.client.feature.module.impl.movement.longjump.LongJumpModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.dynamicisland.IslandTrigger;
import wtf.uitems.client.feature.module.impl.world.scaffold.mode.HeypixelScaffold;
import wtf.uitems.client.feature.module.repository.ModuleRepository;
import wtf.uitems.client.feature.simulation.PlayerSimulation;
import wtf.uitems.client.renderer.world.WorldRenderer;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.input.MouseHandleInputEvent;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.impl.game.player.interaction.block.BlockPlacedEvent;
import wtf.uitems.event.impl.render.RenderWorldEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.mixin.LivingEntityAccessor;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.utility.player.*;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.CustomRenderLayers;

import java.awt.*;
import java.util.*;
import java.util.List;

import static wtf.uitems.client.Constants.mc;

public final class ScaffoldModule extends Module implements IslandTrigger {

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int[] SEARCH_SIGNS = {1, -1};
    private static final int DEFAULT_LOOKAHEAD_TICKS = 10;
    private static final int DEFAULT_BLOCK_SEARCH_RANGE = 2;
    private static final double SKIP_TICK_RECOVERY_MAX_DISTANCE = 4.85D;
    private static final float SKIP_TICK_RECOVERY_MAX_ROTATION_DIFFERENCE = 16.0F;

    private final ScaffoldIsland dynamicIsland = new ScaffoldIsland(this);
    private final ScaffoldSettings settings = new ScaffoldSettings(this);

    public BlockData blockCache;
    private int sameYPos;

    private Vec3d preExpandPos;
    private RaytracedRotation rotation;
    private boolean skipTickRecoveryActive;
    private boolean skipTickRecoveryFailed;
    private int skipTickRecoveryCandidateTicks;
    private BlockPos skipTickRecoveryBlockPos;
    private Direction skipTickRecoveryFace;
    private boolean upTellyBypassActive;
    private boolean upTellyBypassRecovering;
    private boolean upTellyBypassPendingGroundJump;

    private Map<Integer, Integer> realStackSizeMap;

    public ScaffoldModule() {
        super("Scaffold", "Automatically places blocks under you.", ModuleCategory.WORLD);
    }

    @Override
    protected void onDisable() {
        RotationHelper.getHandler().reset();
        SlotHelper.getInstance().stop();
        this.dynamicIsland.onDisable();
        this.realStackSizeMap = null;
        this.intelligentRotation = null;
        this.placeTick = 0;
        this.rotation = null;
        this.blockCache = null;
        this.skipTickRecoveryActive = false;
        this.skipTickRecoveryFailed = false;
        this.skipTickRecoveryCandidateTicks = 0;
        this.skipTickRecoveryBlockPos = null;
        this.skipTickRecoveryFace = null;
        this.upTellyBypassActive = false;
        this.upTellyBypassRecovering = false;
        this.upTellyBypassPendingGroundJump = false;
        SkipTickUtility.reset();

        super.onDisable();
    }

    @Override
    protected void onEnable() {
        super.onEnable();

        blockCache = null;
        rotation = null;

        this.realStackSizeMap = new HashMap<>();

        if (mc.player == null) return;
        sameYPos = MathHelper.floor(mc.player.getY());
        this.skipTickRecoveryFailed = false;
        this.skipTickRecoveryCandidateTicks = 0;
        this.skipTickRecoveryBlockPos = null;
        this.skipTickRecoveryFace = null;
        this.upTellyBypassActive = false;
        this.upTellyBypassRecovering = false;
        this.upTellyBypassPendingGroundJump = false;

        // 自救逻辑：如果开启 Scaffold 时处于虚空或正在坠落
        if (false && settings.getMode().is(ScaffoldSettings.Mode.HEYPIXEL) && settings.getSelfRescueMode() != ScaffoldSettings.SelfRescueMode.DISABLED) {
            if (shouldTriggerSkipTickRecovery() && getPlaceableBlock() != -1) {
                // 预测性查找：先尝试在当前位置下方或侧面寻找可放置点
                if (findRecoveryBlock() != null) {
                    if (settings.getSelfRescueMode() == ScaffoldSettings.SelfRescueMode.SKIP_TICK) {
                        this.skipTickRecoveryActive = true;
                        SkipTickUtility.reset();
                        SkipTickUtility.addSkipTicks(12);
                        final StuckModule stuckModule = OpalClient.getInstance().getModuleRepository().getModule(StuckModule.class);
                        if (stuckModule.isEnabled()) {
                            stuckModule.setEnabled(false);
                        }
                    } else {
                        final StuckModule stuckModule = OpalClient.getInstance().getModuleRepository().getModule(StuckModule.class);
                        if (!stuckModule.isEnabled()) {
                            stuckModule.setEnabled(true);
                        }
                    }
                }
            }
        }
        tryArmSkipTickRecovery();
    }

    /**
     * 预测性查找可用于自救的方块放置点
     */
    private BlockPos findRecoveryBlock() {
        if (mc.player == null || mc.world == null) return null;
        
        BlockPos playerPos = mc.player.getBlockPos();
        // 搜索范围：脚下 3 格，水平 2 格
        for (int y = 0; y >= -3; y--) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos target = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(target).isReplaceable()) {
                        // 检查四周是否有可支撑方块
                        for (Direction dir : DIRECTIONS) {
                            BlockPos neighbor = target.offset(dir);
                            if (!mc.world.getBlockState(neighbor).isAir() && !mc.world.getBlockState(neighbor).isReplaceable()) {
                                return target;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private SkipTickRecoveryTarget findReadySkipTickRecoveryTarget() {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        final Vec2f currentRotation = getCurrentClientRotation();
        if (currentRotation == null) {
            return null;
        }

        final Vec3d eyePos = mc.player.getEyePos();
        final BlockPos playerPos = mc.player.getBlockPos();
        SkipTickRecoveryTarget bestTarget = null;

        for (int y = 0; y >= -3; y--) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    final BlockPos targetPos = playerPos.add(x, y, z);
                    if (!mc.world.getBlockState(targetPos).isReplaceable()) {
                        continue;
                    }

                    for (Direction direction : DIRECTIONS) {
                        final BlockPos supportPos = targetPos.offset(direction);
                        if (!isSolidAndNonInteractive(mc.world.getBlockState(supportPos), supportPos)) {
                            continue;
                        }

                        final Direction face = direction.getOpposite();
                        final Vec2f rotation = RotationUtility.getRotationFromBlock(supportPos, face);
                        final float rotationDifference = RotationUtility.getRotationDifference(currentRotation, rotation);
                        if (rotationDifference > SKIP_TICK_RECOVERY_MAX_ROTATION_DIFFERENCE) {
                            continue;
                        }

                        final Vec3d supportCenter = new Vec3d(
                                supportPos.getX() + 0.5D,
                                supportPos.getY() + 0.5D,
                                supportPos.getZ() + 0.5D
                        );
                        final double dx = eyePos.getX() - supportCenter.getX();
                        final double dy = eyePos.getY() - supportCenter.getY();
                        final double dz = eyePos.getZ() - supportCenter.getZ();
                        final double distanceSq = dx * dx + dy * dy + dz * dz;
                        if (distanceSq > SKIP_TICK_RECOVERY_MAX_DISTANCE * SKIP_TICK_RECOVERY_MAX_DISTANCE) {
                            continue;
                        }

                        if (bestTarget == null
                                || distanceSq < bestTarget.distanceSq
                                || (Math.abs(distanceSq - bestTarget.distanceSq) < 1.0E-6D
                                && rotationDifference < bestTarget.rotationDifference)) {
                            bestTarget = new SkipTickRecoveryTarget(targetPos, supportPos, face, rotation, rotationDifference, distanceSq);
                        }
                    }
                }
            }
        }

        return bestTarget;
    }

    private Vec2f getCurrentClientRotation() {
        return RotationUtility.getRotation();
    }

    private boolean isSolidAndNonInteractive(BlockState state, BlockPos pos) {
        return !state.getCollisionShape(mc.world, pos).isEmpty() && state.createScreenHandlerFactory(mc.world, pos) == null;
    }

    private boolean isFallingIntoVoid() {
        if (mc.player == null || mc.world == null) return false;
        if (mc.player.getY() < -5) return true;
        
        // 检查下方是否有方块
        for (int y = (int) mc.player.getY(); y > -64; y--) {
            if (!mc.world.getBlockState(new BlockPos((int) mc.player.getX(), y, (int) mc.player.getZ())).isAir()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldTriggerSkipTickRecovery() {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        if (mc.player.isOnGround()) {
            return false;
        }

        if (this.settings.getMode().is(ScaffoldSettings.Mode.HEYPIXEL)) {
            final var activeMode = this.getActiveMode();
            if (activeMode instanceof HeypixelScaffold heypixelScaffold && heypixelScaffold.shouldSuppressSkipTickRecoveryTrigger()) {
                return false;
            }
        }

        final boolean overVoid = PlayerUtility.isOverVoid(mc.player.getBoundingBox());
        final double verticalSpeed = mc.player.getVelocity().y;
        final boolean fallingFast = verticalSpeed < -0.14D && mc.player.fallDistance > 0.75F;
        final boolean hardFall = mc.player.fallDistance > 1.6F && verticalSpeed < -0.06D;
        if (!overVoid && !fallingFast && !hardFall) {
            return false;
        }

        return true;
    }

    private void tryArmSkipTickRecovery() {
        if (mc.player == null || mc.world == null) {
            this.skipTickRecoveryCandidateTicks = 0;
            this.skipTickRecoveryBlockPos = null;
            this.skipTickRecoveryFace = null;
            return;
        }

        if (!this.settings.getMode().is(ScaffoldSettings.Mode.HEYPIXEL)
                || this.settings.getSelfRescueMode() != ScaffoldSettings.SelfRescueMode.SKIP_TICK
                || this.skipTickRecoveryActive
                || this.skipTickRecoveryFailed) {
            this.skipTickRecoveryCandidateTicks = 0;
            this.skipTickRecoveryBlockPos = null;
            this.skipTickRecoveryFace = null;
            return;
        }

        if (!shouldTriggerSkipTickRecovery()) {
            this.skipTickRecoveryCandidateTicks = 0;
            this.skipTickRecoveryBlockPos = null;
            this.skipTickRecoveryFace = null;
            return;
        }

        final SkipTickRecoveryTarget recoveryTarget = findReadySkipTickRecoveryTarget();
        if (recoveryTarget == null) {
            this.skipTickRecoveryCandidateTicks = 0;
            this.skipTickRecoveryBlockPos = null;
            this.skipTickRecoveryFace = null;
            return;
        }

        this.skipTickRecoveryBlockPos = recoveryTarget.supportPos();
        this.skipTickRecoveryFace = recoveryTarget.face();

        this.skipTickRecoveryCandidateTicks = Math.min(this.skipTickRecoveryCandidateTicks + 1, 2);
        if (this.skipTickRecoveryCandidateTicks < 2) {
            return;
        }

        if (getPlaceableBlock() == -1) {
            return;
        }

        this.skipTickRecoveryActive = true;
        this.skipTickRecoveryCandidateTicks = 0;
        SkipTickUtility.reset();
        SkipTickUtility.addSkipTicks(12);

        final StuckModule stuckModule = OpalClient.getInstance().getModuleRepository().getModule(StuckModule.class);
        if (stuckModule.isEnabled()) {
            stuckModule.setEnabled(false);
        }
    }

    public BlockPos getSkipTickRecoveryBlockPos() {
        return this.skipTickRecoveryBlockPos;
    }

    public Direction getSkipTickRecoveryFace() {
        return this.skipTickRecoveryFace;
    }

    @Subscribe
    public void onBlockPlaced(BlockPlacedEvent event) {
        if (!mc.interactionManager.getCurrentGameMode().isCreative()) {
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            this.realStackSizeMap.put(selectedSlot, this.realStackSizeMap.getOrDefault(selectedSlot, mc.player.getMainHandStack().getCount() + 1) - 1);
        }
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult && rotation != null && settings.isBlockOverlayEnabled() && !mc.world.getBlockState(blockHitResult.getBlockPos()).isAir()) {
            final Vec3d startVec = new Vec3d(blockHitResult.getBlockPos().getX(), blockHitResult.getBlockPos().getY(), blockHitResult.getBlockPos().getZ());
            final Vec3d dimensions = new Vec3d(1, 1, 1);

            VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(new BufferAllocator(1024));
            WorldRenderer rc = new WorldRenderer(vcp);

            rc.drawFilledCube(event.matrixStack(), CustomRenderLayers.getPositionColorQuads(true), startVec, dimensions, ColorUtility.applyOpacity(ColorUtility.getClientTheme().first, 0.25F));

            vcp.draw();
        }
    }

    @Subscribe(priority = 1)
    public void onMoveInput(final MoveInputEvent event) {
        if (this.upTellyBypassRecovering) {
            event.setForward(0.0F);
            event.setSideways(0.0F);

            if (this.upTellyBypassPendingGroundJump && mc.player.isOnGround()) {
                ((LivingEntityAccessor) mc.player).setJumpingCooldown(0);
                event.setJump(true);
                this.upTellyBypassPendingGroundJump = false;
            }
            return;
        }

        if (this.settings.isSameYEnabled() && this.settings.isAutoJump() && mc.player.isOnGround() && LocalDataWatch.get().groundTicks > 0) {
            final PlayerSimulation simulation = new PlayerSimulation(mc.player);
            final OtherClientPlayerEntity entity = simulation.getSimulatedEntity();
            boolean flag = false;
            for (int i = 0; i < 2; i++) {
                simulation.simulateTick();
                if(!entity.isOnGround()) {
                    flag = true;
                    break;
                }
            }
            if(flag) {
                ((LivingEntityAccessor) mc.player).setJumpingCooldown(0);
                event.setJump(true);
            }
        }
    }

    @Subscribe(priority = 1)
    public void onHandleInput(final MouseHandleInputEvent event) {
        final ScaffoldSettings.Mode effectiveMode = getEffectiveMode();
        if (effectiveMode == ScaffoldSettings.Mode.HEYPIXEL || effectiveMode == ScaffoldSettings.Mode.HYPIXEL) return;

        final boolean isBlock = mc.player.getMainHandStack().getItem() instanceof BlockItem || mc.player.getOffHandStack().getItem() instanceof BlockItem;
        if (this.blockCache != null) {
            if (rotation != null && settings.isOverrideRaycast()) {
                mc.crosshairTarget = this.rotation.hitResult();
            }

            final Block blockOver = PlayerUtility.getBlockOver();

            if (!InventoryUtility.isBlockInteractable(blockOver) && isBlock) {
                if (settings.isDuplicateRotPlace()) {
                    for (int i = 0; i < 3; i++) {
                        place();
                    }
                } else {
                    place();
                }

                this.placeTick = mc.player.age;

                if (settings.isBlockOverlayEnabled()) {
                    FadingBlockHelper.getInstance().addFadingBlock(
                            new FadingBlockHelper.FadingBlock(
                                    blockCache.blockWithDirection.blockPos,
                                    Color.BITMASK,
                                    ColorUtility.applyOpacity(ColorUtility.getClientTheme().first, 0.25F),
                                    300
                            )
                    );
                }
            }
        } else {
            if (!isBlock || !this.simulateClick()) {
                MouseHelper.getRightButton().setDisabled();
            }
        }

        if (preExpandPos != null) {
            mc.player.setPos(preExpandPos.x, preExpandPos.y, preExpandPos.z);
            preExpandPos = null;
        }
    }

    private void place() {
        final MouseButton rightButton = MouseHelper.getRightButton();
        rightButton.setPressed();
        if (this.settings.getSwingMode().getValue() == ScaffoldSettings.SwingMode.SERVER) {
            rightButton.setShowSwings(false);
        }
    }

    private boolean simulateClick() {
        if (!SwingDelay.isSwingAvailable(this.settings.getSimulationCps(), false)) {
            return false;
        }
        if (mc.crosshairTarget != null) {
            if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
//                if(!this.settings.isOverrideRaycast() && (this.blockCache.blockWithDirection.blockPos() != blockHitResult.getBlockPos() || this.blockCache.blockWithDirection.direction() != blockHitResult.getSide())) {
//                    return false;
//                }
                final BlockPos blockPos = blockHitResult.getBlockPos();
                final Block block = mc.world.getBlockState(blockPos).getBlock();
                if (InventoryUtility.isBlockInteractable(block)) {
                    return false;
                }
                final Hand hand;
                final BlockItem blockItem;
                if (mc.player.getMainHandStack().getItem() instanceof BlockItem item) {
                    hand = Hand.MAIN_HAND;
                    blockItem = item;
                } else if (mc.player.getOffHandStack().getItem() instanceof BlockItem item) {
                    hand = Hand.OFF_HAND;
                    blockItem = item;
                } else {
                    ChatUtility.debug("???");
                    return false;
                }
                final ItemUsageContext itemUsageContext = new ItemUsageContext(mc.player, hand, blockHitResult);
                final ItemPlacementContext placementContext = blockItem.getPlacementContext(new ItemPlacementContext(itemUsageContext));
                if (placementContext == null) {
                    return false;
                }
                final BlockPos offsetPos = blockPos.offset(blockHitResult.getSide());
                final BlockState placementState = block.getPlacementState(placementContext);
                final Block heldBlock = blockItem.getBlock();
                final VoxelShape collisionShape = heldBlock.getDefaultState().getCollisionShape(mc.world, offsetPos);
                if (collisionShape.isEmpty()) {
                    return false;
                }
                final Box blockBox = collisionShape.getBoundingBox().offset(offsetPos);
                if (placementState == null || placementState.canPlaceAt(mc.world, blockPos) && !mc.player.getBoundingBox().intersects(blockBox)) {
                    return false;
                }



                MouseHelper.getRightButton().setPressed();
                this.settings.getSimulationCps().resetClick();
                SwingDelay.reset();
                return true;
            }
        }
        return false;
    }

    private Vec2f intelligentRotation;

    @Subscribe(priority = 1)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null) {
            blockCache = null;
            return;
        }

        final boolean shouldBypass = shouldUseUpTellyBypass();
        if (shouldBypass) {
            this.upTellyBypassActive = true;
            this.upTellyBypassRecovering = false;
            this.upTellyBypassPendingGroundJump = false;
        } else if (this.upTellyBypassActive) {
            this.upTellyBypassActive = false;
            this.upTellyBypassRecovering = true;
            this.upTellyBypassPendingGroundJump = true;
        }

        if (this.upTellyBypassRecovering && !this.upTellyBypassPendingGroundJump && !mc.player.isOnGround()) {
            this.upTellyBypassRecovering = false;
        }

        tryArmSkipTickRecovery();

        final StuckModule stuckModule = OpalClient.getInstance().getModuleRepository().getModule(StuckModule.class);
        if (stuckModule.isEnabled() || this.skipTickRecoveryActive) {
            boolean groundBelow = mc.player.isOnGround();
            if (!groundBelow) {
                // 检查下方 1.5 格内是否有非空气方块
                for (double offset = 0.01; offset <= 1.5; offset += 0.5) {
                    if (!mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - offset, mc.player.getZ())).isAir()) {
                        groundBelow = true;
                        break;
                    }
                }
            }
            if (groundBelow) {
                if (stuckModule.isEnabled()) {
                    stuckModule.setEnabled(false);
                }
                this.skipTickRecoveryActive = false;
                SkipTickUtility.reset();
                this.skipTickRecoveryFailed = false;
            }
        }

        final ScaffoldSettings.Mode effectiveMode = getEffectiveMode();
        if (effectiveMode == ScaffoldSettings.Mode.HEYPIXEL || effectiveMode == ScaffoldSettings.Mode.HYPIXEL) return;

        if (this.settings.isSameYEnabled() && this.settings.isAutoJump() && mc.player.isOnGround()) {
            RotationMouseHandler handler = RotationHelper.getHandler();
            if(mc.player != null) {
                if(rotation != null)
                handler.rotate(new Vec2f(mc.gameRenderer.getCamera().getYaw() + (44f * Math.signum(rotation.rotation().x)), mc.gameRenderer.getCamera().getPitch()), InstantRotationModel.INSTANCE);
            }
            this.rotation = null;
            return;
        }
            // Expand
        Vec3d expandOffset = null;
        

        if (expandOffset != null) {
            preExpandPos = mc.player.getEntityPos();

            mc.player.setPos(mc.player.getX() + expandOffset.getX(), mc.player.getY() + expandOffset.getY(), mc.player.getZ() + expandOffset.getZ());
        }

        final int slot = getPlaceableBlock();
        if (slot == -1) {
            if (!(mc.player.getOffHandStack().getItem() instanceof BlockItem blockItem && InventoryUtility.isGoodBlock(blockItem.getBlock()))) {
                return;
            }
        }
        final SlotHelper.Silence silence;
        switch (settings.getSwitchMode().getValue()) {
            case NORMAL -> silence = SlotHelper.Silence.NONE;
            case FULL -> silence = SlotHelper.Silence.FULL;
            default -> silence = SlotHelper.Silence.DEFAULT;
        }
        SlotHelper.setCurrentItem(slot).silence(silence);

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();
        final boolean updateY = !settings.isSameYEnabled()
              //  || mc.options.useKey.isPressed()
                || (this.settings.isAutoJump() && PlayerUtility.isKeyPressed(mc.options.jumpKey))
                || mc.player.isOnGround()
                || Math.abs(Math.floor(mc.player.getY() - sameYPos)) > 3
                || moduleRepository.getModule(LongJumpModule.class).isEnabled()
                || moduleRepository.getModule(FlightModule.class).isEnabled();

        if (updateY) {
            sameYPos = MathHelper.floor(mc.player.getY());
        }

        this.intelligentRotation = null;

        if (!mc.player.input.playerInput.jump() ||
                !mc.player.isOnGround() && (mc.player.getVelocity().getY() >= 0.0D || PlayerUtility.isBoxEmpty(mc.player.getBoundingBox().offset(0.0D, mc.player.getVelocity().getY(), 0.0D)))) {
            this.updateMovementIntelligence();
            updateData();
            this.updateMovementIntelligence();
            // TODO: when for sneak
//            if ((mc.player.input.playerInput.sneak()) &&
//                    (int) (mc.player.getY() + mc.player.getVelocity().getY()) == (int) mc.player.getY() && (mc.player.getVelocity().getY() >= 0.2D || !updateY)) { // telly check bypass, doesn't run jumping else you fall off lol
//                this.blockCache = null;
//            }
        } else {
            this.blockCache = null;
            this.rotation = null;
        }

        if (rotation != null) {
            if (!settings.isSnapRotationsEnabled() || blockCache != null) {
                final IRotationModel model = createEffectiveRotationModel();
                RotationHelper.getHandler().rotate(
                        rotation.rotation(),
                        model
                );
            }
        }
    }

    private boolean isYawDiagonal() {
        final float direction = Math.abs(MoveUtility.getDirectionDegrees() % 90);
        final int range = 30;
        return direction > 45 - range && direction < 45 + range;
    }

    private int placeTick;

    private void updateMovementIntelligence() {
        if (this.settings.isMovementIntelligence()) {
            if (mc.player.isOnGround() || !PlayerUtility.isBoxEmpty(mc.player.getBoundingBox().offset(0.0D, mc.player.getVelocity().getY(), 0.0D))) {
                final Vec2f currentRotation = rotation != null ? rotation.rotation() : RotationUtility.getRotation();
                this.intelligentRotation = RotationUtility.getPriorityAngle(currentRotation, this.settings.getMovementIntelligenceSteps(), this.settings.isMovementSnapping(), this.settings.isDiagonalMovement());
            }
        }
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (event.getPacket() instanceof ItemPickupAnimationS2CPacket pickup
                && mc.player != null
                && pickup.getCollectorEntityId() == mc.player.getId()) {
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            this.realStackSizeMap.put(
                    selectedSlot,
                    this.realStackSizeMap.getOrDefault(selectedSlot, mc.player.getMainHandStack().getCount() - pickup.getStackAmount()) + pickup.getStackAmount()
            );
        }
    }

    private int getPlaceableBlock() {
        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.player.getInventory().getMainStacks().get(i);
            if (itemStack.getItem() instanceof BlockItem blockItem
                    && this.realStackSizeMap.getOrDefault(i, itemStack.getCount()) > 0 &&
                    InventoryUtility.isGoodBlock(blockItem.getBlock())) {
                return i;
            }
        }
        return -1;
    }

    private boolean updateData() {
        if (this.blockCache != null && this.canReuseCachedBlockData(this.blockCache)) {
            this.rotation = this.blockCache.rotation();
            return true;
        }

        blockCache = getBlockData();

        if (blockCache != null) {
            this.rotation = blockCache.rotation;
            return true;
        }

        final int lookaheadTicks = DEFAULT_LOOKAHEAD_TICKS;
        final PlayerSimulation simulation = new PlayerSimulation(mc.player);
        final OtherClientPlayerEntity entity = simulation.getSimulatedEntity();
        for (int i = 0; i < lookaheadTicks; i++) {
            simulation.simulateTick();
            final BlockData simulatedData = getBlockData(entity.getBlockPos().down(), entity);
            if (simulatedData != null) {
                rotation = simulatedData.rotation;
                break;
            }
        }

        return blockCache != null;
    }

    private boolean canReuseCachedBlockData(final BlockData cachedData) {
        if (cachedData == null || cachedData.rotation() == null || cachedData.blockWithDirection() == null) {
            return false;
        }

        return this.isCachedBlockDataReachable(
                cachedData.rotation().rotation(),
                cachedData.blockWithDirection().blockPos(),
                cachedData.blockWithDirection().direction()
        );
    }

    private boolean isCachedBlockDataReachable(final Vec2f rotation, final BlockPos pos, final Direction face) {
        final net.minecraft.util.hit.HitResult hitResult = RaycastUtility.raycastBlock(4.5, 1.0F, false, rotation.x, rotation.y);
        if (hitResult instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getBlockPos().equals(pos) && blockHitResult.getSide() == face;
        }
        return false;
    }

    private RaytracedRotation getRotation(BlockWithDirection data, Vec3d start) {
        final Vec2f sortingAngle;
        if (this.intelligentRotation != null) {
            sortingAngle = this.intelligentRotation;
        } else {
            sortingAngle = rotation != null ? rotation.rotation() : RotationUtility.getRotation();
        }
        return RotationUtility.getRotationFromRaycastedBlock(data.blockPos, data.direction, sortingAngle, start);
    }

    private BlockData getBlockData() {
        return getBlockData(mc.player.getBlockPos().withY(sameYPos).down(), mc.player);
    }

    private BlockData getBlockData(final BlockPos targetBlockPos, final PlayerEntity entity) {
        if (mc.world.getBlockState(targetBlockPos).isReplaceable()) {
            final BlockPos.Mutable blockPos = new BlockPos.Mutable();
            final List<BlockWithDirection> blockList = new ArrayList<>();

            final int range = DEFAULT_BLOCK_SEARCH_RANGE;
            for (int y = 0; y > -range; y--) {
                for (int x = 0; x < range; x++) {
                    for (int z = 0; z < range; z++) {
                        for (int sign : SEARCH_SIGNS) {
                            blockPos.set(targetBlockPos.getX() + (x * sign), targetBlockPos.getY() + (y * sign), targetBlockPos.getZ() + (z * sign));
                            if (!mc.world.getBlockState(blockPos).isReplaceable()) continue;

                            for (Direction direction : DIRECTIONS) {
                                final BlockPos block = blockPos.offset(direction);

                                if (!mc.world.getBlockState(block).isReplaceable()) {
                                    blockList.add(new BlockWithDirection(block, direction.getOpposite()));
                                }
                            }
                        }
                    }
                }
            }

            if (blockList.isEmpty()) {
                return null;
            }

            blockList.sort(Comparator.comparingDouble(data -> data.blockPos.offset(data.direction).getSquaredDistance(targetBlockPos)));

            final Vec3d eyePos = entity.getEntityPos().add(0.0D, entity.getStandingEyeHeight(), 0.0D);
            for (final BlockWithDirection block : blockList) {
                final RaytracedRotation rotation = this.getRotation(block, eyePos);
                if (rotation != null) {
                    return new BlockData(block, rotation);
                }
            }
        }
        return null;
    }

    private record SkipTickRecoveryTarget(BlockPos targetPos, BlockPos supportPos, Direction face, Vec2f rotation, float rotationDifference, double distanceSq) {
    }

    public record BlockWithDirection(BlockPos blockPos, Direction direction) {
    }

    public record BlockData(BlockWithDirection blockWithDirection, RaytracedRotation rotation) {
    }

    @Override
    public void renderIsland(DrawContext context, float posX, float posY, float width, float height, float progress) {
        this.dynamicIsland.render(context, posX, posY);
    }

    public ScaffoldSettings getSettings() {
        return settings;
    }

    public ScaffoldSettings.Mode getEffectiveMode() {
        return (this.upTellyBypassActive || this.upTellyBypassRecovering) ? ScaffoldSettings.Mode.VANILLA : this.settings.getMode().getValue();
    }

    public IRotationModel createEffectiveRotationModel() {
        return (this.upTellyBypassActive || this.upTellyBypassRecovering) ? InstantRotationModel.INSTANCE : this.settings.createRotationModel();
    }

    private boolean shouldUseUpTellyBypass() {
        if (mc.player == null) {
            return false;
        }

        if (!this.settings.isUpTellyBypass()) {
            return false;
        }

        if (!this.settings.getMode().is(ScaffoldSettings.Mode.HEYPIXEL)) {
            return false;
        }

        if (!this.settings.isRotationModel(EnumRotationModel.HEYPIXEL)) {
            return false;
        }

        if (!this.settings.isTelly() || !mc.options.jumpKey.isPressed()) {
            return false;
        }

        return mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed();
    }

    public boolean isSkipTickRecoveryActive() {
        return skipTickRecoveryActive;
    }

    public void setSkipTickRecoveryActive(final boolean skipTickRecoveryActive) {
        this.skipTickRecoveryActive = skipTickRecoveryActive;
        if (!skipTickRecoveryActive) {
            SkipTickUtility.reset();
        }
        this.skipTickRecoveryCandidateTicks = 0;
        this.skipTickRecoveryBlockPos = null;
        this.skipTickRecoveryFace = null;
    }

    public void markSkipTickRecoveryFailed() {
        this.skipTickRecoveryFailed = true;
        this.skipTickRecoveryActive = false;
        SkipTickUtility.reset();
        this.skipTickRecoveryCandidateTicks = 0;
        this.skipTickRecoveryBlockPos = null;
        this.skipTickRecoveryFace = null;
    }

    @Override
    public float getIslandWidth() {
        return this.dynamicIsland.getWidth();
    }

    @Override
    public float getIslandHeight() {
        return this.dynamicIsland.getHeight();
    }

    @Override
    public int getIslandPriority() {
        return 1;
    }

}
