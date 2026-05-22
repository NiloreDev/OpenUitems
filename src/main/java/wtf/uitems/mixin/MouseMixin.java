package wtf.uitems.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.player.PlayerInventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.OpalClient;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.game.input.MouseUpdateEvent;
import wtf.uitems.event.impl.press.MousePressEvent;

import static wtf.uitems.client.Constants.mc;
@Mixin(Mouse.class)
public final class MouseMixin {

    private MouseMixin() {
    }

    private static int cycleSlot(int sign, int current, int size) {
        if (sign > 0) {
            return (current - 1 + size) % size;
        } else if (sign < 0) {
            return (current + 1) % size;
        }
        return current;
    }

    @Inject(
            method = "onMouseButton",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action == 1) {
            if (mc.currentScreen instanceof TitleScreen && OpalClient.getInstance() != null && OpalClient.getInstance().isUseVanillaMainMenu()) {
                final var windowObj = mc.getWindow();
                final double scale = windowObj.getScaleFactor();
                final double mouseX = mc.mouse.getX() / scale;
                final double mouseY = mc.mouse.getY() / scale;
                int x = 6;
                int y = 6;
                int w = 110;
                int h = 18;
                if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h && (input.button() == 0 || input.button() == -1)) {
                    OpalClient.getInstance().setUseVanillaMainMenu(false);
                    try {
                        Class<?> screenClass = Class.forName("wtf.uitems.client.screen.mainmenu.OpalMainMenuScreen");
                        mc.setScreen((net.minecraft.client.gui.screen.Screen) screenClass.getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ci.cancel();
                    return;
                }
            }

            if (input.button() == -1) {
                return;
            }

            EventDispatcher.dispatch(new MousePressEvent(input.button()));
        }
    }

    @Shadow
    private double cursorDeltaX;
    @Shadow
    private double cursorDeltaY;

    @Unique
    private MouseUpdateEvent event;

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void opal$onMouseScrollHead(long windowHandle, double horizontalAmount, double verticalAmount, CallbackInfo ci) {
        final int sign = verticalAmount > 0 ? 1 : (verticalAmount < 0 ? -1 : 0);
        if (sign != 0 && mc.player != null && mc.currentScreen == null) {
            final PlayerInventory inv = mc.player.getInventory();
            final SlotHelper slotHelper = SlotHelper.getInstance();
            if (slotHelper != null && slotHelper.isActive()) {
                if (slotHelper.getSilence() != SlotHelper.Silence.NONE) {
                    slotHelper.setVisualSlot(cycleSlot(sign, slotHelper.getVisualSlot(), PlayerInventory.getHotbarSize()));
                } else {
                    final int current = ((PlayerInventoryAccessor) inv).getSelectedSlot();
                    inv.setSelectedSlot(cycleSlot(sign, current, PlayerInventory.getHotbarSize()));
                }
            } else {
                final int current = ((PlayerInventoryAccessor) inv).getSelectedSlot();
                inv.setSelectedSlot(cycleSlot(sign, current, PlayerInventory.getHotbarSize()));
            }
            ci.cancel();
        }
    }

    @Redirect(
            method = "onMouseScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;setSelectedSlot(I)V")
    )
    private void setSelectedSlot(PlayerInventory instance, int slot, @Local int i) {
        SlotHelper slotHelper = SlotHelper.getInstance();
        if (slotHelper.isActive()) {
            if (slotHelper.getSilence() != SlotHelper.Silence.NONE) {
                slotHelper.setVisualSlot(cycleSlot(i, slotHelper.getVisualSlot(), PlayerInventory.getHotbarSize()));
            }
        } else {
            instance.setSelectedSlot(slot);
        }
    }

    @Redirect(
            method = "updateMouse",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;smoothCameraEnabled:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean redirectCheck(GameOptions instance, @Local(ordinal = 3) double multiplier) {
        this.event = new MouseUpdateEvent(this.cursorDeltaX, this.cursorDeltaY, multiplier, this.unlockCursorRun);
        EventDispatcher.dispatch(this.event);
        return instance.smoothCameraEnabled && !this.event.isHandled();
    }

    @Unique
    private boolean unlockCursorRun;

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;isCursorLocked()Z")
    )
    private boolean redirectTickCursorLock(Mouse instance) {
        if (instance.isCursorLocked()) {
            return true;
        }
        if (RotationHelper.getHandler().isUnlockCursor()) {
            this.unlockCursorRun = true;
            return true;
        }
        return false;
    }

    @Inject(
            method = "updateMouse",
            at = @At("TAIL")
    )
    private void updateMouseTail(double timeDelta, CallbackInfo ci) {
        RotationHelper.getClientHandler().onPostMouseUpdate();
        this.unlockCursorRun = false;
        this.event = null;
    }

    @Redirect(
            method = "updateMouse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingSpyglass()Z")
    )
    private boolean redirectCheck(ClientPlayerEntity instance) {
        return instance.isUsingSpyglass() && (this.event == null || !this.event.isHandled());
    }

    @Redirect(
            method = "updateMouse",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Mouse;cursorDeltaX:D", opcode = Opcodes.GETFIELD)
    )
    private double redirectCursorX(Mouse instance) {
        return this.event == null ? 0 : this.event.getDeltaX();
    }

    @Redirect(
            method = "updateMouse",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Mouse;cursorDeltaY:D", opcode = Opcodes.GETFIELD)
    )
    private double redirectCursorY(Mouse instance) {
        return this.event == null ? 0 : this.event.getDeltaY();
    }
}
