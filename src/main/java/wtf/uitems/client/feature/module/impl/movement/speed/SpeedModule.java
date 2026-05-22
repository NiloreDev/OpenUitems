package wtf.uitems.client.feature.module.impl.movement.speed;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.movement.speed.impl.*;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;

public final class SpeedModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", this, Mode.VANILLA);

    public SpeedModule() {
        super("Speed", "You become a cheetah in real life.", ModuleCategory.MOVEMENT);
        addProperties(mode);
        addModuleModes(mode, new VanillaSpeed(this), new StrafeSpeed(this), new MushMCSpeed(this));
    }

    @Override
    public String getSuffix() {
        return mode.getValue().toString();
    }

    public enum Mode {
        VANILLA("Vanilla"),
        STRAFE("Strafe"),
        MUSHMC("MushMC");

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
