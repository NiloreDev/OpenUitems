package wtf.uitems.client.feature.module.impl.movement;

import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.subscriber.Subscribe;

import static wtf.uitems.client.Constants.mc;

public final class SprintModule extends Module {

    private final BooleanProperty omniSprint = new BooleanProperty("Omnidirectional", false);

    public SprintModule() {
        super("Sprint", "Modifies the logic behind sprinting.", ModuleCategory.MOVEMENT);
        addProperties(omniSprint);
        setEnabled(true);
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        mc.options.sprintKey.setPressed(true);
    }

    public static boolean isOmniSprint() {
        final SprintModule sprintModule = OpalClient.getInstance().getModuleRepository().getModule(SprintModule.class);
        return sprintModule.isEnabled() && sprintModule.omniSprint.getValue();
    }
}
