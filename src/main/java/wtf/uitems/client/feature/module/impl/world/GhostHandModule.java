package wtf.uitems.client.feature.module.impl.world;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.event.impl.game.input.MouseHandleInputEvent;
import wtf.uitems.event.impl.game.player.interaction.ItemUseEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.mixin.KeyBindingAccessor;
import wtf.uitems.utility.player.PlayerUtility;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static wtf.uitems.client.Constants.mc;

public final class GhostHandModule extends Module {

    private static final Set<Block> BLOCKS = new HashSet<>();

    static {
        BLOCKS.add(Blocks.CHEST);
        BLOCKS.add(Blocks.ENDER_CHEST);
        BLOCKS.add(Blocks.TRAPPED_CHEST);
        BLOCKS.add(Blocks.SHULKER_BOX);
    }

    private final BooleanProperty debug = new BooleanProperty("Debug", false);

    public GhostHandModule() {
        super("Ghost Hand", "Allows you to interact with containers through blocks.", ModuleCategory.WORLD);
        addProperties(debug);
    }

    private long lastInteractTime = 0;
    private boolean sessionActive = false;
    private boolean waitingRelease = false;
    private boolean hadScreen = false;
    private int sessionTimeout = 0;

    @Subscribe
    public void onMouseHandleInput(final MouseHandleInputEvent event) {
        if (mc.currentScreen != null || mc.player == null || mc.player.isUsingItem()) {
            return;
        }

        final BlockHitResult targetHit = findGhostTargetHit();
        if (targetHit == null) {
            return;
        }

        if (System.currentTimeMillis() - lastInteractTime < 200) {
            return;
        }

        if (!MouseHelper.getRightButton().wasPressed()) {
            return;
        }

        final var result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, targetHit);
        if (result.isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
            lastInteractTime = System.currentTimeMillis();
            sessionActive = true;
            waitingRelease = false;
            hadScreen = false;
            sessionTimeout = 40;
            event.setCancelled();
        }
    }

    @Subscribe
    public void onPreTick(final PreGameTickEvent event) {
        if (!sessionActive) {
            return;
        }

        if (sessionTimeout > 0) {
            sessionTimeout--;
        } else {
            sessionActive = false;
            waitingRelease = false;
            hadScreen = false;
            return;
        }

        if (mc.currentScreen != null) {
            hadScreen = true;
        } else if (hadScreen) {
            waitingRelease = true;
        }

        if (waitingRelease) {
            if (!PlayerUtility.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
                if (mc.options != null && mc.options.useKey != null) {
                    mc.options.useKey.setPressed(false);
                    ((KeyBindingAccessor) mc.options.useKey).callReset();
                }
                MouseHelper.getRightButton().setDisabled();
                sessionActive = false;
                waitingRelease = false;
                hadScreen = false;
            }
        }
    }

    @Subscribe
    public void onItemUse(final ItemUseEvent event) {
        if (sessionActive) {
            event.setCancelled();
            if (mc.options != null && mc.options.useKey != null) {
                mc.options.useKey.setPressed(false);
                ((KeyBindingAccessor) mc.options.useKey).callReset();
            }
            MouseHelper.getRightButton().setDisabled();
        }
    }

    private BlockHitResult findGhostTargetHit() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return null;
        }

        if (mc.crosshairTarget instanceof BlockHitResult blockHit) {
            final BlockEntity be = mc.world.getBlockEntity(blockHit.getBlockPos());
            if (be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity || be instanceof ShulkerBoxBlockEntity) {
                return null;
            }
        }

        final Vec3d eyePos = mc.player.getEyePos();
        final Vec3d lookVec = mc.player.getRotationVec(1.0F);
        final Vec3d reachEnd = eyePos.add(lookVec.multiply(4.5));

        BlockHitResult fakeHit = null;
        double closestDist = Double.MAX_VALUE;

        final List<BlockEntity> blockEntities = new ArrayList<>();
        
        final int radius = 2; 
        final int playerX = mc.player.getBlockX() >> 4;
        final int playerZ = mc.player.getBlockZ() >> 4;
        
        for (int x = playerX - radius; x <= playerX + radius; x++) {
            for (int z = playerZ - radius; z <= playerZ + radius; z++) {
                final var chunk = mc.world.getChunk(x, z);
                if (chunk != null) {
                    blockEntities.addAll(chunk.getBlockEntities().values());
                }
            }
        }
        
        for (final BlockEntity be : blockEntities) {
            if (!(be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity || be instanceof ShulkerBoxBlockEntity)) {
                continue;
            }

            final Box box = getContainerBox(be);
            if (box == null) continue;

            final Optional<Vec3d> hit = box.raycast(eyePos, reachEnd);
            if (hit.isPresent()) {
                final double dist = hit.get().distanceTo(eyePos);
                if (dist < closestDist) {
                    closestDist = dist;
                    fakeHit = new BlockHitResult(hit.get(), Direction.UP, be.getPos(), false);
                }
            }
        }

        if (fakeHit != null && debug.getValue()) {
            final BlockEntity be = mc.world.getBlockEntity(fakeHit.getBlockPos());
            if (be != null) {
                ChatUtility.print("GhostHand: Interacting with " + be.getCachedState().getBlock().getName().getString() + " at " + be.getPos().toShortString());
            }
        }

        return fakeHit;
    }

    private Box getContainerBox(BlockEntity be) {
        final BlockPos pos = be.getPos();
        final Box baseBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        
        if (be instanceof ChestBlockEntity) {
            final var state = be.getCachedState();
            if (!state.contains(ChestBlock.CHEST_TYPE)) {
                return baseBox;
            }
            
            final ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type == ChestType.SINGLE) {
                return baseBox;
            }
            
            if (type == ChestType.LEFT) {
                return null;
            }
            
            // RIGHT chest, find the LEFT one
            final Direction facing = state.get(ChestBlock.FACING);
            final Direction side = facing.rotateYClockwise(); // RIGHT to find LEFT is Clockwise
            final BlockPos otherPos = pos.offset(side);
            
            return baseBox.union(new Box(otherPos.getX(), otherPos.getY(), otherPos.getZ(), otherPos.getX() + 1, otherPos.getY() + 1, otherPos.getZ() + 1));
        }
        
        return baseBox;
    }
}
