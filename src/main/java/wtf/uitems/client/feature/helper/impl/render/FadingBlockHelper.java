package wtf.uitems.client.feature.helper.impl.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.client.feature.helper.IHelper;
import wtf.uitems.client.renderer.world.WorldRenderer;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.render.RenderWorldEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.misc.time.Stopwatch;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.CustomRenderLayers;

import java.util.ArrayList;
import java.util.List;

public final class FadingBlockHelper implements IHelper {

    private final List<FadingBlock> fadingBlocks = new ArrayList<>();
    private BufferAllocator cachedAllocator;
    private WorldRenderer cachedWorldRenderer;
    private VertexConsumerProvider.Immediate cachedVcp;

    private FadingBlockHelper() {
    }

    @Subscribe
    public void onRenderWorld(final RenderWorldEvent event) {
        if (this.fadingBlocks.isEmpty()) {
            return;
        }

        this.fadingBlocks.removeIf(FadingBlock::isRemovable);
        if (this.fadingBlocks.isEmpty()) {
            return;
        }

        if (this.cachedAllocator == null) {
            this.cachedAllocator = new BufferAllocator(1024);
            this.cachedVcp = VertexConsumerProvider.immediate(this.cachedAllocator);
            this.cachedWorldRenderer = new WorldRenderer(this.cachedVcp);
        }
        final WorldRenderer rc = this.cachedWorldRenderer;

        for (final FadingBlock fadingBlock : this.fadingBlocks) {
            final float progress = (float) fadingBlock.getRemainingLifetime() / fadingBlock.lifetime;
            final float fillAlpha = ((fadingBlock.fillColor >> 24) & 0xFF) / 255.F;
            final float outlineAlpha = ((fadingBlock.outlineColor >> 24) & 0xFF) / 255.F;

            final Vec3d startVec = new Vec3d(fadingBlock.blockPos.getX(), fadingBlock.blockPos.getY(), fadingBlock.blockPos.getZ());
            final Vec3d dimensions = new Vec3d(1, 1, 1);

            rc.drawFilledCube(event.matrixStack(), CustomRenderLayers.getPositionColorQuads(true), startVec, dimensions, ColorUtility.applyOpacity(fadingBlock.fillColor, fillAlpha * progress));
        }

        this.cachedVcp.draw();

    }

    public void addFadingBlock(final FadingBlock fadingBlock) {
        this.fadingBlocks.add(fadingBlock);
    }

    public boolean hasFadingBlocks() {
        return !this.fadingBlocks.isEmpty();
    }

    public static class FadingBlock {

        private final Stopwatch stopwatch = new Stopwatch();

        private final BlockPos blockPos;
        private final int outlineColor, fillColor;
        private final long lifetime;

        public FadingBlock(final BlockPos blockPos, final int outlineColor, final int fillColor, final long lifetime) {
            this.blockPos = blockPos;
            this.outlineColor = outlineColor;
            this.fillColor = fillColor;
            this.lifetime = lifetime;
            this.stopwatch.reset();
        }

        public long getRemainingLifetime() {
            return Math.max(0, this.lifetime - this.stopwatch.getTime());
        }

        public boolean isRemovable() {
            return this.stopwatch.hasTimeElapsed(this.lifetime);
        }

    }

    private static FadingBlockHelper instance;

    public static FadingBlockHelper getInstance() {
        return instance;
    }

    public static void setInstance() {
        instance = new FadingBlockHelper();
        EventDispatcher.subscribe(instance);
    }

}
