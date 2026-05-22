package wtf.uitems.event.impl.game.player.movement;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.event.EventCancellable;

public final class StuckInBlockEvent extends EventCancellable {

    private final BlockState state;
    private Vec3d multiplier;

    public StuckInBlockEvent(final BlockState state, final Vec3d multiplier) {
        this.state = state;
        this.multiplier = multiplier;
    }

    public BlockState getState() {
        return state;
    }

    public Vec3d getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Vec3d multiplier) {
        this.multiplier = multiplier;
    }
}
