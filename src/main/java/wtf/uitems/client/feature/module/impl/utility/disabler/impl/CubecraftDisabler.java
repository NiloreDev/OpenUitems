package wtf.uitems.client.feature.module.impl.utility.disabler.impl;

import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import wtf.uitems.client.feature.helper.impl.player.packet.blockage.block.holder.BlockHolder;
import wtf.uitems.client.feature.helper.impl.player.packet.blockage.impl.InboundNetworkBlockage;
import wtf.uitems.client.feature.module.impl.utility.disabler.DisablerModule;
import wtf.uitems.client.feature.module.property.impl.mode.ModuleMode;
import wtf.uitems.event.impl.game.JoinWorldEvent;
import wtf.uitems.event.impl.game.packet.InstantaneousReceivePacketEvent;
import wtf.uitems.event.impl.game.player.movement.PreMovementPacketEvent;
import wtf.uitems.event.impl.game.player.teleport.PreTeleportEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.mixin.CommonPingS2CPacketAccessor;
import wtf.uitems.utility.misc.time.Stopwatch;

public final class CubecraftDisabler extends ModuleMode<DisablerModule> {
    public CubecraftDisabler(DisablerModule module) {
        super(module);
    }

    private final BlockHolder blockHolder = new BlockHolder(InboundNetworkBlockage.get());
    private final Stopwatch flagStopwatch = new Stopwatch();

    @Subscribe
    public void onPreMovementPacket(final PreMovementPacketEvent event) {
        if (this.flagStopwatch.hasTimeElapsed(200L)) {
            this.blockHolder.block(p -> p, p -> p instanceof CommonPingS2CPacket);
        } else {
            this.blockHolder.release();
        }

        if (this.flagStopwatch.hasTimeElapsed(3000L)) {
            event.setY(event.getY() + 11);
        }
    }

    private boolean cancel;

    @Subscribe
    public void onInstantaneousReceivePacket(final InstantaneousReceivePacketEvent event) {
        if (event.getPacket() instanceof KeepAliveS2CPacket) {
            event.setCancelled();
        } else if (event.getPacket() instanceof CommonPingS2CPacket ping) {
            final CommonPingS2CPacketAccessor accessor = (CommonPingS2CPacketAccessor) ping;
//            System.out.println(ping.getParameter());
        }
    }

    @Subscribe
    public void onPreTeleport(final PreTeleportEvent event) {
//        System.out.println("tp");
        this.blockHolder.release();
        this.flagStopwatch.reset();
        this.cancel = true;
    }

    @Subscribe
    public void onJoinWorld(final JoinWorldEvent event) {
        this.blockHolder.release();
        this.cancel = true;
    }

    @Override
    public void onDisable() {
        this.blockHolder.release();
        super.onDisable();
    }

    @Override
    public Enum<?> getEnumValue() {
        return DisablerModule.Mode.CUBECRAFT;
    }
}
