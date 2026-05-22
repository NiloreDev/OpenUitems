package wtf.uitems.client.feature.module.impl.movement;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.event.impl.game.input.MoveInputEvent;
import wtf.uitems.event.impl.game.player.movement.PostMovementPacketEvent;
import wtf.uitems.event.impl.game.player.movement.StuckInBlockEvent;
import wtf.uitems.event.subscriber.Subscribe;

import static wtf.uitems.client.Constants.mc;

public final class NoWebModule extends Module {

    private final ModeProperty<NoWebMode> mode = new ModeProperty<>("Mode", NoWebMode.VANILLA);

    private int playerInWebTick = 0;
    private int ticksInWeb = 0;

    public NoWebModule() {
        super("NoWeb", "Allows you to move faster in cobwebs.", ModuleCategory.MOVEMENT);
        this.addProperties(this.mode);
    }

    @Subscribe
    public void onPostMovementPacket(final PostMovementPacketEvent event) {
        if (this.playerInWebTick < mc.player.age) {
            this.ticksInWeb = 0;
        }
    }

    @Subscribe
    public void onMoveInput(final MoveInputEvent event) {
        if (this.ticksInWeb > 1 && (this.mode.getValue() == NoWebMode.FAST || this.mode.getValue() == NoWebMode.Heypixel)) {
            event.setJump(false);
        }
    }

    @Subscribe
    public void onStuckInBlock(final StuckInBlockEvent event) {
        if (event.getState().isOf(Blocks.COBWEB)) {
            this.playerInWebTick = mc.player.age;
            this.ticksInWeb++;

            switch (this.mode.getValue()) {
                case VANILLA -> event.setCancelled();
                case FAST, Heypixel -> {
                    if (this.ticksInWeb > 5) {
                        final Vec3d multiplier = new Vec3d(0.88, 1.88, 0.88);
                        event.setMultiplier(multiplier);
                    }
                }
            }
        }
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().name();
    }

    public enum NoWebMode {
        VANILLA, FAST, Heypixel
    }
}
