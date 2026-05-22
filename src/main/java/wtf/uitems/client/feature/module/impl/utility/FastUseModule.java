package wtf.uitems.client.feature.module.impl.utility;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.mixin.MinecraftClientAccessor;

import static wtf.uitems.client.Constants.mc;

public final class FastUseModule extends Module {

    private final BooleanProperty fastPlaceEnabled = new BooleanProperty("Enabled", true);

    public FastUseModule() {
        super("Fast Use", "Uses things faster.", ModuleCategory.UTILITY);
        this.addProperties(new GroupProperty("Placements", fastPlaceEnabled));
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        if (!fastPlaceEnabled.getValue()) return;

        final MinecraftClientAccessor minecraftClientAccessor = (MinecraftClientAccessor) mc;

        minecraftClientAccessor.setItemUseCooldown(0);
    }

}
