package wtf.uitems.client.feature.module.impl.movement;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.duck.ClientConnectionAccess;
import wtf.uitems.mixin.PlayerMoveC2SPacketAccessor;
import wtf.uitems.event.impl.game.JoinWorldEvent;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.impl.game.packet.SendPacketEvent;
import wtf.uitems.event.impl.game.player.movement.PreMovementPacketEvent;
import wtf.uitems.event.subscriber.Subscribe;

import java.util.concurrent.ConcurrentLinkedQueue;

import static wtf.uitems.client.Constants.mc;

public final class StuckModule extends Module {

    private final ConcurrentLinkedQueue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private Packet<?> interactPacket;
    private int interactStage = 0;
    private double frozenX, frozenY, frozenZ;
    private long enableTime;

    public StuckModule() {
        super("Stuck", "Freezes your position on the server while allowing you to look around.", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        this.packetQueue.clear();
        this.interactPacket = null;
        this.interactStage = 0;
        this.enableTime = System.currentTimeMillis();
        if (mc.player != null) {
            this.frozenX = mc.player.getX();
            this.frozenY = mc.player.getY();
            this.frozenZ = mc.player.getZ();
        }
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) {
            // 在关闭瞬间，强制将客户端当前的视角和坐标同步给服务器
            // 这样服务器收到的第一个包就是最新的，不会触发回弹修正
            sendPacketSilent(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.player.getYaw(), mc.player.getPitch(),
                    mc.player.isOnGround(), false
            ));
        }

        // 释放所有缓存的包（如 Pong 等），确保连接不掉线
        while (!this.packetQueue.isEmpty()) {
            sendPacketSilent(this.packetQueue.poll());
        }
        
        super.onDisable();
    }

    @Subscribe
    public void onPreMovementPacket(final PreMovementPacketEvent event) {
        // 安全机制：如果卡空超过 3.5 秒（自救失败或卡死），自动关闭 Stuck
        if (System.currentTimeMillis() - this.enableTime > 3500) {
            this.setEnabled(false);
            return;
        }

        if (mc.player == null) return;

        // 强制重置速度，防止客户端因为物理逻辑移动
        mc.player.setVelocity(0, 0, 0);

        // 处理缓存的交互包（如拉弓等动作）
        if (this.interactStage == 1) {
            this.interactStage = 2;
            
            // 发送最新的转向包，确保动作指向正确方向
            sendPacketSilent(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false));
            
            // 先处理队列中的 Pong 包，确保时序正确
            while (!this.packetQueue.isEmpty()) {
                sendPacketSilent(this.packetQueue.poll());
            }

            if (this.interactPacket != null) {
                sendPacketSilent(this.interactPacket);
                this.interactPacket = null;
            }
        }
    }

    @Subscribe
    public void onMoveInput(final MoveInputEvent event) {
        // 屏蔽所有键盘输入导致的位移
        event.setForward(0.0F);
        event.setSideways(0.0F);
        event.setJump(false);
        event.setSneak(false);
    }

    @Subscribe
    public void onSendPacket(final SendPacketEvent event) {
        Packet<?> packet = event.getPacket();

        // 核心：拦截所有位移包，让服务器认为你没动
        if (packet instanceof PlayerMoveC2SPacket movePacket) {
            // 允许纯旋转包通过，这样在 Stuck 状态下转头，服务器也能同步视角
            if (movePacket instanceof PlayerMoveC2SPacket.LookAndOnGround) {
                return;
            }

            // 如果是包含坐标的包（Full 或 Position），我们强制锁定其坐标为开启 Stuck 时的坐标
            // 这样既能保持“原地不动”的效果，又能允许视角旋转（如果是 Full 包）
            if (movePacket instanceof PlayerMoveC2SPacket.Full || movePacket instanceof PlayerMoveC2SPacket.PositionAndOnGround) {
                final PlayerMoveC2SPacketAccessor accessor = (PlayerMoveC2SPacketAccessor) movePacket;
                accessor.setX(this.frozenX);
                accessor.setY(this.frozenY);
                accessor.setZ(this.frozenZ);
                return;
            }
            
            event.setCancelled();
        } 
        // 缓存心跳包，防止卡空时间过长导致掉线
        else if (packet instanceof CommonPongC2SPacket) {
            this.packetQueue.add(packet);
            event.setCancelled();
        } 
        // 拦截交互包，放入缓存异步处理
        else if (packet instanceof PlayerInteractItemC2SPacket || packet instanceof PlayerActionC2SPacket) {
            if (shouldBufferInteraction(packet)) {
                this.interactPacket = packet;
                this.interactStage = 1;
                event.setCancelled();
            }
        }
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        // 关键点：拦截所有服务器发来的位置/视角修正包
        // 只要这个包被拦截，客户端视角就不会因为服务器同步而“回弹”
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            event.setCancelled();
        }
    }

    @Subscribe
    public void onJoinWorld(final JoinWorldEvent event) {
        this.setEnabled(false);
    }

    private boolean shouldBufferInteraction(Packet<?> packet) {
        if (packet instanceof PlayerInteractItemC2SPacket useItem) {
            ItemStack item = mc.player.getStackInHand(useItem.getHand());
            if (item.isOf(net.minecraft.item.Items.ENDER_PEARL)) {
                return false;
            }
            return !item.getComponents().contains(DataComponentTypes.FOOD) && !(item.getItem() instanceof BowItem);
        } else if (packet instanceof PlayerActionC2SPacket action) {
            return action.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM && mc.player.getActiveItem().getItem() instanceof BowItem;
        }
        return false;
    }

    private void sendPacketSilent(Packet<?> packet) {
        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() instanceof ClientConnectionAccess access) {
            access.opal$sendPacketSilent(packet);
        }
    }
}
