package wtf.uitems.client.feature.module.impl.movement;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.event.impl.game.player.movement.KeepSprintEvent;
import wtf.uitems.event.subscriber.Subscribe;

public final class KeepSprintModule extends Module {

    public KeepSprintModule() {
        super("Keep Sprint", "Prevents sprint reset after attacks.", ModuleCategory.MOVEMENT);
        setEnabled(true);
    }

    @Subscribe
    public void onKeepSprint(final KeepSprintEvent event) {
        event.setCancelled();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
    }

    @Override
    public String getSuffix() {
        return "Vanilla";
    }
}
