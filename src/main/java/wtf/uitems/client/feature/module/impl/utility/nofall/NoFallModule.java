package wtf.uitems.client.feature.module.impl.utility.nofall;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.utility.nofall.impl.AutoMLGNoFall;
import wtf.uitems.client.feature.module.impl.utility.nofall.impl.SpoofNoFall;
import wtf.uitems.client.feature.module.impl.utility.nofall.impl.HeypixelNoFall;
import wtf.uitems.client.feature.module.impl.utility.nofall.impl.Heypixel2NoFall;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.event.impl.game.player.movement.PreMovementPacketEvent;
import wtf.uitems.event.subscriber.Subscribe;

import static wtf.uitems.client.Constants.mc;

public final class NoFallModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", this, Mode.SPOOF);
    private double fallDistance;

    public NoFallModule() {
        super("No Fall", "Removes your players fall damage.", ModuleCategory.UTILITY);
        addProperties(mode);
        addModuleModes(mode, new SpoofNoFall(this), new AutoMLGNoFall(this), new HeypixelNoFall(this), new Heypixel2NoFall(this));
    }

    @Subscribe
    public void onPreMovementPacket(final PreMovementPacketEvent event) {
        if (mc.player.fallDistance == 0) {
            syncFallDifference();
        }
    }

    public void syncFallDifference() {
        this.fallDistance = mc.player.fallDistance;
    }

    public double getFallDifference() {
        if (mc.player.getAbilities().allowFlying) {
            return 0;
        }
        return mc.player.fallDistance - this.fallDistance;
    }

    @Override
    protected void onEnable() {
        this.fallDistance = 0;

        super.onEnable();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
    }

    @Override
    public String getSuffix() {
        return mode.getValue().toString();
    }

    public enum Mode {
        SPOOF("Spoof"),
        AUTO_MLG("Auto MLG"),
        HEYPIXEL("Heypixel"),
        HEYPIXEL2("Heypixel 2");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
