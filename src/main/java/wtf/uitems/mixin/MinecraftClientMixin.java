package wtf.uitems.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.ReleaseInfo;
import wtf.uitems.client.feature.helper.impl.player.mouse.MouseButton;
import wtf.uitems.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.module.impl.movement.InventoryMoveModule;
import wtf.uitems.client.feature.module.impl.visual.AnimationsModule;
import wtf.uitems.duck.ClientPlayerEntityAccess;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.game.JoinWorldEvent;
import wtf.uitems.event.impl.game.PostGameTickEvent;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.ScheduledExecutablesEvent;
import wtf.uitems.event.impl.game.input.MouseHandleInputEvent;
import wtf.uitems.event.impl.game.input.PostHandleInputEvent;
import wtf.uitems.event.impl.game.player.interaction.AttackDelayEvent;
import wtf.uitems.event.impl.game.player.interaction.ItemUseEvent;
import wtf.uitems.event.impl.game.player.interaction.SwingEvent;
import wtf.uitems.event.impl.game.player.interaction.block.BlockPlacedEvent;
import wtf.uitems.event.impl.game.server.ServerDisconnectEvent;
import wtf.uitems.event.impl.render.ResolutionChangeEvent;
import wtf.uitems.utility.player.PlayerUtility;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    protected abstract boolean doAttack();

    @Shadow
    protected int attackCooldown;

    @Shadow
    @Nullable
    public HitResult crosshairTarget;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    private MinecraftClientMixin() {
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void postInitialization(final CallbackInfo ci) {
        OpalClient.getInstance().runPostInitializations();
    }

    @Inject(
            method = "getWindowTitle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void injectWindowTitle(final CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ReleaseInfo.NAME + " ver." + ReleaseInfo.VERSION);
    }

    @Inject(
            method = "handleInputEvents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0),
            cancellable = true
    )
    private void handleInputEventsMouse(final CallbackInfo info) {
        final MouseHandleInputEvent event = new MouseHandleInputEvent();
        EventDispatcher.dispatch(event);
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(
            method = "handleInputEvents",
            at = @At("TAIL")
    )
    private void handleInputEventsTail(final CallbackInfo ci) {
        MouseHelper.getInstance().tick();

        EventDispatcher.dispatch(new PostHandleInputEvent());
    }

    @Redirect(
            method = "doAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V")
    )
    private void redirectAttackSwings(ClientPlayerEntity instance, Hand hand) {
        final MouseButton leftButton = MouseHelper.getLeftButton();
        if (leftButton.isShowSwings()) {
            instance.swingHand(hand);
        } else {
            instance.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            EventDispatcher.dispatch(new SwingEvent(hand));
        }
    }

    @Redirect(
            method = "doItemUse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V")
    )
    private void redirectUseSwings(ClientPlayerEntity instance, Hand hand) {
        final MouseButton rightButton = MouseHelper.getRightButton();
        if (rightButton.isShowSwings()) {
            instance.swingHand(hand);
        } else {
            instance.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            EventDispatcher.dispatch(new SwingEvent(hand));
        }
    }

    @Inject(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;unpressAll()V", shift = At.Shift.AFTER)
    )
    private void hookSetScreen(Screen screen, CallbackInfo ci) {
        wtf.uitems.client.renderer.shader.ShaderFramebuffer.markBlurDirty();
        if (OpalClient.getInstance().isPostInitialization()) {
            final InventoryMoveModule inventoryMove = OpalClient.getInstance().getModuleRepository().getModule(InventoryMoveModule.class);
            if (inventoryMove.isEnabled() && !inventoryMove.isBlocked()) {
                PlayerUtility.updateMovementKeyStates();
            }
        }
    }

    @Redirect(
            method = "handleInputEvents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;setSelectedSlot(I)V")
    )
    private void redirectSelectedSlot(PlayerInventory instance, int value) {
        SlotHelper slotHelper = SlotHelper.getInstance();
        if (slotHelper.isActive()) {
            if (slotHelper.getSilence() != SlotHelper.Silence.NONE) {
                slotHelper.setVisualSlot(value);
            }
        } else {
            instance.setSelectedSlot(value);
        }
    }

    @Inject(
            method = "handleInputEvents",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;socialInteractionsKey:Lnet/minecraft/client/option/KeyBinding;", shift = At.Shift.BEFORE)
    )
    private void postSlotHandleInput(CallbackInfo ci) {
        SlotHelper.getInstance().sync(false, false);
    }

    @Redirect(
            method = "handleInputEvents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z")
    )
    private boolean redirectIsPressed(KeyBinding instance) {
        final MouseButton mouseButton = MouseHelper.getButtonFromBinding(instance);
        if (mouseButton != null) {
            return mouseButton.isPressed();
        }
        return instance.isPressed();
    }

    @Redirect(
            method = "handleInputEvents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z"),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;useKey:Lnet/minecraft/client/option/KeyBinding;", ordinal = 1),
                    to = @At("TAIL")
            )
    )
    private boolean redirectWasPressed(KeyBinding instance) {
        final MouseButton mouseButton = MouseHelper.getButtonFromBinding(instance);
        if (mouseButton != null) {
            return mouseButton.wasPressed();
        }
        return instance.wasPressed();
    }

    @Redirect(
            method = "handleInputEvents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 11)
    )
    private boolean redirectUsingAttack(KeyBinding instance, @Local LocalBooleanRef bl3) {
        if (this.isSwingWhileUsing() && MouseHelper.getLeftButton().wasPressed()) {
            final boolean currentValue = bl3.get();
            final boolean newValue = currentValue | doAttack();
            bl3.set(newValue);
            return true;
        }
        return false;
    }

    @Inject(
            method = "doItemUse",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hookItemUse(CallbackInfo ci) {
        final ItemUseEvent event = new ItemUseEvent();
        EventDispatcher.dispatch(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleInputEvents",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;useKey:Lnet/minecraft/client/option/KeyBinding;", ordinal = 1)
    )
    private void onItemUseMouseHandle(CallbackInfo ci) {
        final AnimationsModule animationsModule = OpalClient.getInstance().getModuleRepository().getModule(AnimationsModule.class);
        final MouseButton leftButton = MouseHelper.getLeftButton();
        if (animationsModule.isEnabled() && animationsModule.isSwingWhileUsing() && leftButton.isPressed() && leftButton.isShowSwings()) {
            if ((this.crosshairTarget != null && this.crosshairTarget.getType() == HitResult.Type.BLOCK) || leftButton.wasPressed()) {
                if (this.player instanceof ClientPlayerEntityAccess access) {
                    access.opal$swingHandClientside(Hand.MAIN_HAND);
                }
            }
        }

        //noinspection StatementWithEmptyBody
        while (leftButton.wasPressed()) ;
    }

    @Unique
    private boolean isSwingWhileUsing() {
        final wtf.uitems.client.feature.module.impl.visual.AnimationsModule animationsModule = OpalClient.getInstance().getModuleRepository().getModule(wtf.uitems.client.feature.module.impl.visual.AnimationsModule.class);
        return animationsModule.isEnabled() && animationsModule.isSwingWhileUsing();
    }

    @Redirect(
            method = "doAttack",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;attackCooldown:I", opcode = Opcodes.PUTFIELD)
    )
    private void onAttackCooldown(MinecraftClient instance, int value) {
        final AttackDelayEvent event = new AttackDelayEvent(value);
        EventDispatcher.dispatch(event);
        this.attackCooldown = event.getDelay();
    }

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;runTasks()V", shift = At.Shift.BEFORE)
    )
    private void onGameLoop(boolean tick, CallbackInfo ci, @Local(ordinal = 0) int ticks) {
        EventDispatcher.dispatch(new ScheduledExecutablesEvent(ticks > 0));
    }

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void tickHead(final CallbackInfo info) {
        EventDispatcher.dispatch(new PreGameTickEvent());
    }

    @Inject(
            method = "joinWorld",
            at = @At("HEAD")
    )
    private void hookJoinWorld(ClientWorld world, CallbackInfo ci) {
        EventDispatcher.dispatch(new JoinWorldEvent());
        final wtf.uitems.client.verify.VerifyManager vm = wtf.uitems.client.verify.VerifyManager.getInstance();
        if (vm.getTransport() != null) {
            vm.getTransport().sendInGameUsername();
        }
    }

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void tickTail(final CallbackInfo info) {
        EventDispatcher.dispatch(new PostGameTickEvent());
    }

    @Inject(
            method = "onDisconnected",
            at = @At("HEAD")
    )
    private void disconnected(final CallbackInfo ci) {
        EventDispatcher.dispatch(new ServerDisconnectEvent());
    }

    @Inject(
            method = "isTelemetryEnabledByApi",
            at = @At("HEAD"),
            cancellable = true
    )
    private void disableTelemetry(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    // @Inject(method = "getRealmsPeriodicCheckers", at = @At("HEAD"), cancellable = true)
    // private void disableRealms(CallbackInfoReturnable<Object> cir) {
    //    cir.setReturnValue(null);
    // }

    @Inject(
            method = "onResolutionChanged",
            at = @At("HEAD")
    )
    private void resolutionChange(CallbackInfo ci) {
        EventDispatcher.dispatch(new ResolutionChangeEvent());
    }

    @Inject(
            method = "doItemUse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ActionResult$Success;swingSource()Lnet/minecraft/util/ActionResult$SwingSource;", ordinal = 1)
    )
    private void hookBlockPlaceEvent(CallbackInfo ci, @Local BlockHitResult blockHitResult) {
        EventDispatcher.dispatch(new BlockPlacedEvent(blockHitResult));
    }

    @Redirect(
            method = "doItemUse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;resetEquipProgress(Lnet/minecraft/util/Hand;)V", ordinal = 0)
    )
    private void redirectResetEquipProgress(HeldItemRenderer instance, Hand hand) {
        // prevent equip progress reset if placing a block but the visual item is not a block
        SlotHelper slotHelper = SlotHelper.getInstance();
        if (hand == Hand.MAIN_HAND && slotHelper.isActive() && !(slotHelper.getMainHandStack(player).getItem() instanceof BlockItem)) {
            return;
        }
        instance.resetEquipProgress(hand);
    }

}
