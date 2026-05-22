package wtf.uitems.client.feature.module.impl.movement.flight;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.movement.flight.impl.*;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;

public final class FlightModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", this, Mode.VANILLA);

    public FlightModule() {
        super("Flight", "You grow wings in real life.", ModuleCategory.MOVEMENT);
        addProperties(mode);
        addModuleModes(mode, new VanillaFlight(this), new FireballFlight(this), new AirWalkFlight(this),
                new BloxdFlight(this));
    }

    @Override
    public String getSuffix() {
        return mode.getValue().toString();
    }

    public enum Mode {
        VANILLA("Vanilla"),
        FIREBALL("Fireball"),
        AIR_WALK("Air Walk"),
        BLOXD("Bloxd");

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
