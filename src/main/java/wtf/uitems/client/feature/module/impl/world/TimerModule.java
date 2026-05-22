package wtf.uitems.client.feature.module.impl.world;

import wtf.uitems.client.feature.helper.impl.player.timer.TimerHelper;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.subscriber.Subscribe;

public final class TimerModule extends Module {

    private final NumberProperty gameSpeed = new NumberProperty("Game speed", "x", 2F, 0.05F, 10F, 0.05F);

    public TimerModule() {
        super("Timer", "Modifies your game speed.", ModuleCategory.WORLD);

        addProperties(gameSpeed);
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        TimerHelper.getInstance().timer = gameSpeed.getValue().floatValue();
    }

    @Override
    protected void onDisable() {
        TimerHelper.getInstance().timer = 1F;
        super.onDisable();
    }
}
