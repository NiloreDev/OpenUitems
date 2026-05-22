package wtf.uitems.client.feature.module.impl.visual;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.ColorProperty;
import wtf.uitems.duck.WorldAccessor;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.CustomRenderLayers;
import wtf.uitems.event.impl.game.JoinWorldEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.impl.game.packet.SendPacketEvent;
import wtf.uitems.event.impl.render.RenderWorldEvent;
import wtf.uitems.event.subscriber.Subscribe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static wtf.uitems.client.Constants.mc;

public final class ChestESPModule extends Module {

    private final BooleanProperty chest = new BooleanProperty("Chest", true);
    private final ColorProperty chestColor = new ColorProperty("Chest Color", 0xFFFFFF00);
    private final BooleanProperty enderChest = new BooleanProperty("Ender Chest", true);
    private final ColorProperty enderChestColor = new ColorProperty("Ender Chest Color", 0xFFFF00FF);
    private final BooleanProperty shulker = new BooleanProperty("Shulker", true);
    private final ColorProperty shulkerColor = new ColorProperty("Shulker Color", 0xFFFF0000);

    private final ColorProperty openedColor = new ColorProperty("Opened Color", 0xFF666666);
    private final BooleanProperty throughWalls = new BooleanProperty("Through Walls", true);

    private final Set<BlockPos> openedBlocks = new HashSet<>();
    private BufferAllocator cachedAllocator;
    private wtf.uitems.client.renderer.world.WorldRenderer cachedWorldRenderer;
    private net.minecraft.client.render.VertexConsumerProvider.Immediate cachedVcp;
    private final List<BlockEntity> cachedFallbackBlockEntities = new ArrayList<>();
    private int cachedFallbackRefreshAge = -1;
    private int cachedFallbackChunkX = Integer.MIN_VALUE;
    private int cachedFallbackChunkZ = Integer.MIN_VALUE;

    public ChestESPModule() {
        super("Chest ESP", "Highlights chests and other containers.", ModuleCategory.VISUAL);
        addProperties(chest, chestColor, enderChest, enderChestColor, shulker, shulkerColor, openedColor, throughWalls);
    }

    @Override
    protected void onDisable() {
        openedBlocks.clear();
        super.onDisable();
    }

    @Subscribe
    public void onJoinWorld(final JoinWorldEvent event) {
        openedBlocks.clear();
    }

    @Subscribe
    public void onSendPacket(final SendPacketEvent event) {
        if (event.getPacket() instanceof PlayerInteractBlockC2SPacket interact) {
            final BlockPos pos = interact.getBlockHitResult().getBlockPos();
            markOpened(pos);
        }
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (event.getPacket() instanceof BlockEventS2CPacket blockEvent) {
            // For chests/ender chests/shulkers:
            // type 1, data > 0 usually means opening (lid moving)
            if (blockEvent.getData() > 0) {
                markOpened(blockEvent.getPos());
            }
        }
    }

    private void markOpened(final BlockPos pos) {
        if (mc.world == null) return;

        final BlockEntity blockEntity = mc.world.getBlockEntity(pos);
        if (blockEntity instanceof ChestBlockEntity) {
            openedBlocks.add(pos);
            final net.minecraft.block.BlockState state = mc.world.getBlockState(pos);
            if (state.contains(ChestBlock.CHEST_TYPE)) {
                final ChestType type = state.get(ChestBlock.CHEST_TYPE);
                if (type != ChestType.SINGLE) {
                    final BlockPos otherPos = pos.offset(ChestBlock.getFacing(state));
                    openedBlocks.add(otherPos);
                }
            }
        } else if (blockEntity instanceof EnderChestBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) {
            openedBlocks.add(pos);
        }
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (mc.world == null || mc.player == null) return;

        final List<BlockEntity> blockEntities = this.getBlockEntities();
        if (blockEntities.isEmpty()) {
            return;
        }

        if (cachedAllocator == null) {
            cachedAllocator = new BufferAllocator(4096);
            cachedVcp = net.minecraft.client.render.VertexConsumerProvider.immediate(cachedAllocator);
            cachedWorldRenderer = new wtf.uitems.client.renderer.world.WorldRenderer(cachedVcp);
        }
        final wtf.uitems.client.renderer.world.WorldRenderer rc = cachedWorldRenderer;

        for (final BlockEntity blockEntity : blockEntities) {
            final BlockPos pos = blockEntity.getPos();
            int color = 0;

            if (openedBlocks.contains(pos)) {
                color = openedColor.getValue();
            } else if (chest.getValue() && blockEntity instanceof ChestBlockEntity) {
                color = chestColor.getValue();
            } else if (enderChest.getValue() && blockEntity instanceof EnderChestBlockEntity) {
                color = enderChestColor.getValue();
            } else if (shulker.getValue() && blockEntity instanceof ShulkerBoxBlockEntity) {
                color = shulkerColor.getValue();
            }

            if (color != 0) {
                final Vec3d startVec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                final Vec3d dimensions = new Vec3d(1, 1, 1);

                rc.drawFilledCube(
                        event.matrixStack(),
                        CustomRenderLayers.getPositionColorQuads(throughWalls.getValue()),
                        startVec,
                        dimensions,
                        ColorUtility.applyOpacity(color, 0.25F)
                );
            }
        }

        cachedVcp.draw();
    }

    private List<BlockEntity> getBlockEntities() {
        final List<BlockEntity> directEntities = ((WorldAccessor) mc.world).getBlockEntities();
        if (!directEntities.isEmpty()) {
            return directEntities;
        }

        final int playerChunkX = mc.player.getBlockPos().getX() >> 4;
        final int playerChunkZ = mc.player.getBlockPos().getZ() >> 4;
        final int age = mc.player.age;

        if (this.cachedFallbackRefreshAge < 0
                || age - this.cachedFallbackRefreshAge >= 20
                || this.cachedFallbackChunkX != playerChunkX
                || this.cachedFallbackChunkZ != playerChunkZ) {
            this.cachedFallbackBlockEntities.clear();

            final int radius = 4;
            for (int x = playerChunkX - radius; x <= playerChunkX + radius; x++) {
                for (int z = playerChunkZ - radius; z <= playerChunkZ + radius; z++) {
                    final net.minecraft.world.chunk.WorldChunk chunk = mc.world.getChunk(x, z);
                    if (chunk != null) {
                        this.cachedFallbackBlockEntities.addAll(chunk.getBlockEntities().values());
                    }
                }
            }

            this.cachedFallbackRefreshAge = age;
            this.cachedFallbackChunkX = playerChunkX;
            this.cachedFallbackChunkZ = playerChunkZ;
        }

        return this.cachedFallbackBlockEntities;
    }
}
