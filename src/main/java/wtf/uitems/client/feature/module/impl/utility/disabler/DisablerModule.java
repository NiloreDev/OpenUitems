package wtf.uitems.client.feature.module.impl.utility.disabler;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.utility.disabler.impl.CubecraftDisabler;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;

public final class DisablerModule extends Module {
    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", this, Mode.CUBECRAFT);

    public DisablerModule() {
        super("Disabler", "Lessens anti-cheat strength.", ModuleCategory.UTILITY);
        addProperties(mode);
        addModuleModes(mode, new CubecraftDisabler(this));
    }

    @Override
    public String getSuffix() {
        return mode.getValue().toString();
    }

    public enum Mode {
        CUBECRAFT("test");

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
