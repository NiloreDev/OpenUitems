package wtf.uitems.client.feature.module.impl.movement;

import net.minecraft.item.BlockItem;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.event.impl.game.player.movement.ClipAtLedgeEvent;
import wtf.uitems.event.subscriber.Subscribe;

import static wtf.uitems.client.Constants.mc;

public final class SafeWalkModule extends Module {

    private final BooleanProperty holdingBlockCheck = new BooleanProperty("Holding block check", false),
            directionCheck = new BooleanProperty("Direction check", false);

    public SafeWalkModule() {
        super("Safe Walk", "Allows you to move around without falling off blocks.", ModuleCategory.MOVEMENT);
        addProperties(holdingBlockCheck, directionCheck);
    }

    @Subscribe
    public void onClipAtLedge(final ClipAtLedgeEvent event) {
        boolean holdingBlockItem = mc.player.getInventory().getSelectedStack().getItem() instanceof BlockItem;

        boolean holdingCondition = !holdingBlockCheck.getValue() || holdingBlockItem;
        boolean directionCondition = !directionCheck.getValue() || mc.options.backKey.isPressed();

        if (holdingCondition && directionCondition) {
            event.setClip(true);
        }
    }

}
