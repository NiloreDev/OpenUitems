package wtf.uitems.client.feature.module.impl.movement.noslow.impl;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import wtf.uitems.client.feature.module.impl.movement.noslow.NoSlowModule;
import wtf.uitems.client.feature.module.property.impl.mode.ModuleMode;
import wtf.uitems.duck.ClientConnectionAccess;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.impl.game.packet.SendPacketEvent;
import wtf.uitems.event.impl.game.player.movement.SlowdownEvent;
import wtf.uitems.event.subscriber.Subscribe;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static wtf.uitems.client.Constants.mc;
/*
 * NoSlow by Uitems
 * yufei233
 */
public final class Heypixel3NoSlow extends ModuleMode<NoSlowModule> {

    private enum Step {
        NONE,
        CANCEL_PONG,
        SWAP_HANDS,
        USING
    }

    private Step step = Step.NONE;
    private int noUsingItemTicks = 0;
    private final Queue<CommonPongC2SPacket> queuedPongs = new ConcurrentLinkedQueue<>();
    private Hand targetHand = Hand.MAIN_HAND;
    private boolean swapSent = false;
    private boolean forcedUseKeyDown = false;
    private int restoreMainSlot = -1;
    private boolean switchMainSlotAfterSwap = false;

    public Heypixel3NoSlow(final NoSlowModule module) {
        super(module);
    }

    private boolean isConsumable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.contains(DataComponentTypes.FOOD) || stack.getItem() instanceof PotionItem;
    }

    private boolean isWhitelistedItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof BucketItem || stack.getItem() == Items.END_CRYSTAL;
    }

    private void sendPacketSilent(Packet<?> packet) {
        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() instanceof ClientConnectionAccess access) {
            access.opal$sendPacketSilent(packet);
        }
    }

    private void releaseQueuedPongs() {
        CommonPongC2SPacket pong;
        while ((pong = queuedPongs.poll()) != null) {
            sendPacketSilent(pong);
        }
    }

    private void abort(boolean revertSwap) {
        releaseQueuedPongs();
        if (revertSwap && this.swapSent) {
            sendPacketSilent(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ORIGIN,
                    Direction.DOWN
            ));
        }
        this.step = Step.NONE;
        this.noUsingItemTicks = 0;
        this.swapSent = false;
        this.targetHand = Hand.MAIN_HAND;
        if (this.restoreMainSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(this.restoreMainSlot);
        }
        this.restoreMainSlot = -1;
        this.switchMainSlotAfterSwap = false;
        if (this.forcedUseKeyDown && mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
        this.forcedUseKeyDown = false;
    }

    @Override
    public void onDisable() {
        abort(true);
        super.onDisable();
    }

    @Subscribe
    public void onTick(PreGameTickEvent event) {
        if (mc.player == null) {
            return;
        }

        if (isWhitelistedItem(mc.player.getMainHandStack()) || isWhitelistedItem(mc.player.getOffHandStack())) {
            if (this.step != Step.NONE) {
                abort(true);
            }
            return;
        }

        boolean using = mc.player.isUsingItem() && mc.player.getItemUseTimeLeft() > 0 && isConsumable(mc.player.getActiveItem());
        if (using) {
            if (this.step == Step.NONE) {
                Hand usedHand = mc.player.getActiveHand();
                this.targetHand = usedHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
                ItemStack opposite = mc.player.getStackInHand(this.targetHand);
                if (isConsumable(opposite)) {
                    return;
                }

                this.switchMainSlotAfterSwap = usedHand == Hand.MAIN_HAND
                        && this.targetHand == Hand.OFF_HAND
                        && mc.player.getOffHandStack().getItem() instanceof ShieldItem;

                this.step = Step.CANCEL_PONG;
                this.swapSent = false;
                if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                    sendPacketSilent(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                }
            }
            if (mc.options != null && this.step != Step.USING) {
                mc.options.useKey.setPressed(false);
                this.forcedUseKeyDown = true;
            }
        }

        if (this.step == Step.USING) {
            if (using) {
                this.noUsingItemTicks = 0;
            } else {
                this.noUsingItemTicks++;
                if (this.noUsingItemTicks >= 5) {
                    releaseQueuedPongs();
                    sendPacketSilent(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                            BlockPos.ORIGIN,
                            Direction.DOWN
                    ));
                    abort(false);
                }
            }
        } else {
            this.noUsingItemTicks = 0;
        }
    }

    @Subscribe
    public void onSlowdown(SlowdownEvent event) {
        if (mc.player == null) {
            return;
        }
        if (this.step != Step.USING) {
            return;
        }
        if (!mc.player.isUsingItem() || mc.player.getItemUseTimeLeft() <= 0 || !isConsumable(mc.player.getActiveItem())) {
            return;
        }
        event.setCancelled();
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null) {
            return;
        }
        if (this.step == Step.USING) {
            if (!event.isSneak()) {
                mc.player.setSprinting(true);
            }
        }
    }

    @Subscribe
    public void onSendPacket(SendPacketEvent event) {
        if (mc.player == null) {
            return;
        }
        Packet<?> packet = event.getPacket();

        if (packet instanceof CommonPongC2SPacket && this.step != Step.NONE) {
            event.setCancelled();
            this.queuedPongs.add((CommonPongC2SPacket) packet);
            if (this.step == Step.CANCEL_PONG) {
                this.step = Step.SWAP_HANDS;
                this.swapSent = true;
                sendPacketSilent(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                ));
            }
            return;
        }

        if (packet instanceof PlayerActionC2SPacket actionPacket
                && actionPacket.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
                && this.step == Step.USING) {
            abort(true);
            return;
        }

        if (packet instanceof PlayerInteractItemC2SPacket interactPacket
                && this.step == Step.USING
                && this.swapSent
                && interactPacket.getHand() != this.targetHand) {
            event.setCancelled();
            Hand hand = this.targetHand;
            sendPacketSilent(new PlayerInteractItemC2SPacket(hand, interactPacket.getSequence(), mc.player.getYaw(), mc.player.getPitch()));
        }
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.player == null) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (this.step == Step.SWAP_HANDS) {
            if (packet instanceof ScreenHandlerSlotUpdateS2CPacket || packet instanceof InventoryS2CPacket) {
                if (this.switchMainSlotAfterSwap) {
                    final int currentSlot = mc.player.getInventory().getSelectedSlot();
                    final int switchSlot = findSafeMainHandSlot(currentSlot);
                    if (switchSlot != currentSlot) {
                        this.restoreMainSlot = currentSlot;
                        mc.player.getInventory().setSelectedSlot(switchSlot);
                    }
                    this.switchMainSlotAfterSwap = false;
                }
                if (mc.options != null) {
                    mc.options.useKey.setPressed(true);
                    this.forcedUseKeyDown = true;
                }
                this.step = Step.USING;
                this.noUsingItemTicks = 0;
            }
        }
    }

    @Override
    public Enum<?> getEnumValue() {
        return NoSlowModule.Mode.HEYPIXEL3;
    }

    private int findSafeMainHandSlot(final int currentSlot) {
        if (mc.player == null) {
            return currentSlot;
        }

        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) {
                continue;
            }

            final ItemStack stack = mc.player.getInventory().getStack(i);
            if (!(stack.getItem() instanceof ShieldItem)) {
                return i;
            }
        }

        return currentSlot;
    }
}
