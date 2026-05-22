package wtf.uitems.mixin;

import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityRenderManager.class)
//10j3k -- entity rendering is batched, this is useless
public abstract class EntityRenderDispatcherMixin {

    @Shadow
    public abstract <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S state);

    private EntityRenderDispatcherMixin() {
    }

//    @ModifyExpressionValue(method = "render", at = @At(value = "HEAD"))
//    private <E extends Entity, S extends EntityRenderState> S updateEntityInState(S renderState, CameraRenderState cameraRenderState, double d, double e, double f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue) {
////        ((EntityRenderStateAccess) renderState).opal$setEntity(this.getRenderer(renderState).getEntity());
//        return renderState;
//    }

}
