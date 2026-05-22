package wtf.uitems.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.*;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.helper.impl.render.FadingBlockHelper;
import wtf.uitems.client.feature.module.impl.visual.NoHurtCameraModule;
import wtf.uitems.client.feature.module.impl.combat.BacktrackModule;
import wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.uitems.client.feature.module.impl.movement.TargetStrafeModule;
import wtf.uitems.client.feature.module.impl.visual.ChestESPModule;
import wtf.uitems.client.renderer.overlay.ClientInGameOverlay;
import wtf.uitems.client.renderer.shader.ShaderFramebuffer;
import wtf.uitems.client.screen.click.AbstractClickGui;
import wtf.uitems.client.verify.VerifyManager;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.render.RenderWorldEvent;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Final @Shadow
    private BufferBuilderStorage buffers;

    @Shadow @Final
    private MinecraftClient client;
    @Shadow @Final
    private GuiRenderState guiState;
    @Shadow @Final
    private GuiRenderer guiRenderer;
    @Shadow @Final
    private FogRenderer fogRenderer;
    @Unique
    private boolean passThroughBlocks;

    @Inject(method = "onResized", at = @At("HEAD"))
    private void hookOnResized(int width, int height, CallbackInfo ci) {
        ShaderFramebuffer.onResized(width, height);
    }



    @WrapOperation(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"))
    private void hookRenderWorld(WorldRenderer instance, ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, Operation<Void> original, @Local(ordinal = 1) final Matrix4f matrix4f2) {
        original.call(instance, allocator, tickCounter, renderBlockOutline, camera, positionMatrix, matrix4f, projectionMatrix, fogBuffer, fogColor, renderSky);

        if (!this.hasRenderWorldWork()) {
            return;
        }

        final MatrixStack stack = new MatrixStack();
        stack.multiplyPositionMatrix(positionMatrix);

        EventDispatcher.dispatch(new RenderWorldEvent(this.buffers.getEntityVertexConsumers(), stack, tickCounter.getTickProgress(false)));

        // restore state like the original world rendering code did
        // GlStateManager._depthMask(true);
        // GlStateManager._disableBlend();
    }

    @Redirect(
            method = "findCrosshairTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;")
    )
    private HitResult.Type redirectBlockHitResultType(HitResult instance) {
        if (passThroughBlocks) {
            passThroughBlocks = false;
            return HitResult.Type.MISS;
        }

        return instance.getType();
    }

    @Redirect(
            method = "findCrosshairTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;squaredDistanceTo(Lnet/minecraft/util/math/Vec3d;)D", ordinal = 0)
    )
    private double redirectPassedThroughBlockDistance(Vec3d instance, Vec3d vec, @Local(ordinal = 1, argsOnly = true) double entityInteractionRange, @Local(argsOnly = true) float tickDelta) {
        return instance.squaredDistanceTo(vec);
    }

    @Inject(
            method = "tiltViewWhenHurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hookTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (OpalClient.getInstance().getModuleRepository().getModule(NoHurtCameraModule.class).isEnabled()) {
            ci.cancel();
        }
    }

    // @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    // private void hookRenderWeather(RenderTickCounter tickCounter, CallbackInfo ci) {
    //     if (OpalClient.getInstance().getModuleRepository().getModule(AmbienceModule.class).isCusomWeather()) {
    //         // GlStateManager._depthMask(true);
    //         // GlStateManager._disableBlend();
    //         ci.cancel();
    //     }
    // }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void postRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext context) {
        // Do not run custom GUI post-pass on menu/login screens.
        if (this.client.world == null || !VerifyManager.getInstance().isAuthenticated()) {
            return;
        }

        if (!(this.client.currentScreen instanceof AbstractClickGui)) {
            return;
        }

        this.guiState.clear();
        ClientInGameOverlay.renderClickGui(
                context, tickCounter.getTickProgress(false)
        );
        this.guiRenderer.render(this.fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
    }

    private boolean hasRenderWorldWork() {
        final var moduleRepository = OpalClient.getInstance().getModuleRepository();
        if (moduleRepository.getModule(KillAuraModule.class).isEnabled()) {
            return true;
        }
        if (moduleRepository.getModule(BacktrackModule.class).isEnabled()) {
            return true;
        }
        if (moduleRepository.getModule(TargetStrafeModule.class).isEnabled()) {
            return true;
        }
        if (moduleRepository.getModule(ChestESPModule.class).isEnabled()) {
            return true;
        }
        if (moduleRepository.getModule(wtf.uitems.client.feature.module.impl.world.scaffold.ScaffoldModule.class).isEnabled()) {
            return true;
        }

        final FadingBlockHelper fadingBlockHelper = FadingBlockHelper.getInstance();
        return fadingBlockHelper != null && fadingBlockHelper.hasFadingBlocks();
    }
}
