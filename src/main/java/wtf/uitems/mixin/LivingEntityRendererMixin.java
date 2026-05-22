package wtf.uitems.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.visual.NoHurtCameraModule;
import wtf.uitems.client.feature.module.impl.visual.esp.ESPModule;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends EntityRenderer<T, S>
        implements FeatureRendererContext<S, M> {

    private LivingEntityRendererMixin(EntityRendererFactory.Context context) {
        super(context);
    }
// 10j3k chams are gone as entity rendering is now batched.
//    @WrapWithCondition(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
//    private boolean render$render(M instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlay, int color, S state, MatrixStack matrices, VertexConsumerProvider consumers, int i) {
//        final ChamsModule chamsModule = OpalClient.getInstance().getModuleRepository().getModule(ChamsModule.class);
//
//        if (!chamsModule.isEnabled() || !chamsModule.isColorOverlay() || !(((EntityRenderStateAccess) state).opal$getEntity() instanceof PlayerEntity))
//            return true;
//
//        instance.render(matrixStack, vertexConsumer, light, overlay, chamsModule.getRGBAColor());
//
//        return false;
//    }

//    @ModifyReturnValue(method = "getRenderLayer", at = @At("RETURN"))
//    private RenderLayer removePlayerTexture(RenderLayer original, S state, boolean showBody, boolean translucent, boolean showOutline) {
//        final ChamsModule chamsModule = OpalClient.getInstance().getModuleRepository().getModule(ChamsModule.class);
//
//        if (!chamsModule.isEnabled()
//                || !(((EntityRenderStateAccess) state).opal$getEntity() instanceof PlayerEntity)
//                || chamsModule.shouldKeepTextures()) {
//            return original;
//        }
//
//        return RenderLayer.getItemEntityTranslucentCull(Identifier.of("uitems", "textures/blank.png"));
//    }

//    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
//    private void setPolygonStates(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
//        Entity entity = ((EntityRenderStateAccess) state).opal$getEntity();
//        if (!(entity instanceof LivingEntity)) return;
//
//        final ChamsModule chamsModule = OpalClient.getInstance().getModuleRepository().getModule(ChamsModule.class);
//
//        if (chamsModule.isEnabled()) {
//            glEnable(GL_POLYGON_OFFSET_FILL);
//            glPolygonOffset(1.0f, -1100000.0f);
//        }
//    }
//
//    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
//    private void revertPolygonStates(S state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
//        Entity entity = ((EntityRenderStateAccess) state).opal$getEntity();
//        if (!(entity instanceof LivingEntity)) return;
//
//        final ChamsModule chamsModule = OpalClient.getInstance().getModuleRepository().getModule(ChamsModule.class);
//
//        if (chamsModule.isEnabled()) {
//            glPolygonOffset(1.0f, 1100000.0f);
//            glDisable(GL_POLYGON_OFFSET_FILL);
//        }
//    }

    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void hookHasLabel(T livingEntity, double d, CallbackInfoReturnable<Boolean> cir) {
        if (livingEntity instanceof PlayerEntity) {
            final ESPModule espModule = OpalClient.getInstance().getModuleRepository().getModule(ESPModule.class);
            // 当客户端自定义 NameTags 开启时，完全禁用原版头顶名称，避免重复显示
            if (espModule.isEnabled() && espModule.getSettings().areNameTagsEnabled()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void hookUpdateRenderStateTail(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci) {
        if (livingEntity instanceof ClientPlayerEntity) {
            final NoHurtCameraModule noHurtCameraModule = OpalClient.getInstance().getModuleRepository().getModule(NoHurtCameraModule.class);
            if (noHurtCameraModule.isEnabled() && noHurtCameraModule.isHideModelDamage()) {
                livingEntityRenderState.hurt = false;
            }
        }

        // IRC 名称拼接改为在 AbstractClientPlayerEntity 的 getName/getDisplayName 上处理
    }
}
