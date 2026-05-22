package wtf.uitems.client.feature.module.impl.movement;

import net.minecraft.client.gui.screen.ChatScreen;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.screen.click.dropdown.DropdownClickGUI;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.player.PlayerUtility;

import static wtf.uitems.client.Constants.mc;

public final class InventoryMoveModule extends Module {

    public InventoryMoveModule() {
        super("Inventory Move", "Allows you to move while in inventories.", ModuleCategory.MOVEMENT);
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        if (this.isBlocked()) {
            return;
        }
        PlayerUtility.updateMovementKeyStates();
    }

    public boolean isBlocked() {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof DropdownClickGUI;
    }
}
