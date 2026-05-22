package wtf.uitems.client.feature.module.impl.combat.velocity;

import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.combat.velocity.impl.Heypixel3Velocity;
import wtf.uitems.client.feature.module.impl.combat.velocity.impl.HeypixelTestVelocity;
import wtf.uitems.client.feature.module.impl.combat.velocity.impl.DelayVelocity;
import wtf.uitems.client.feature.module.impl.combat.velocity.impl.BufferVelocity;
import wtf.uitems.client.feature.module.impl.combat.velocity.impl.NormalVelocity;
import wtf.uitems.client.feature.module.impl.movement.flight.FlightModule;
import wtf.uitems.client.feature.module.impl.movement.longjump.LongJumpModule;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.feature.module.repository.ModuleRepository;

import static wtf.uitems.client.Constants.mc;

public final class VelocityModule extends Module {
    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", this, Mode.NORMAL);

    public VelocityModule() {
        super("Velocity", "Reduces or nullifies your players velocity when being hit.", ModuleCategory.COMBAT);
        this.addProperties(this.mode);
        addModuleModes(mode, new NormalVelocity(this), new Heypixel3Velocity(this), new DelayVelocity(this), new BufferVelocity(this), new HeypixelTestVelocity(this));
    }

    @Override
    public String getSuffix() {
        return ((VelocityMode) this.getActiveMode()).getSuffix();
    }

    public boolean isInvalid() {
        if (mc.player == null) {
            return true;
        }

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();
        if (moduleRepository.getModule(LongJumpModule.class).isEnabled()
                || moduleRepository.getModule(FlightModule.class).isEnabled()) {
            return true;
        }

        return false;
    }

    public enum Mode {
        NORMAL("Normal"),
        HEYPIXEL("Heypixel"),
        DELAY("Delay"),
        BUFFER("Buffer"),
        HEYPIXEL_TEST("Heypixel Test");

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
