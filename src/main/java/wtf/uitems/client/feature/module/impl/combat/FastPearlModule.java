package wtf.uitems.client.feature.module.impl.combat;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.press.MousePressEvent;
import wtf.uitems.event.subscriber.Subscribe;

import static wtf.uitems.client.Constants.mc;

public final class FastPearlModule extends Module {

    private final ModeProperty<ButtonMode> button = new ModeProperty<>("Button", ButtonMode.MIDDLE);
    private Stage stage = Stage.IDLE;
    private int oldSlot = -1;
    private int pearlSlot = -1;
    private int stageTicks;

    public FastPearlModule() {
        super("Fast Pearl", "Automatically throws an ender pearl when a key is pressed.", ModuleCategory.COMBAT);
        addProperties(button);
    }

    @Subscribe
    public void onMousePress(final MousePressEvent event) {
        if (mc.player == null || mc.currentScreen != null) return;

        final int pressedButton = event.getInteractionCode();
        final ButtonMode selectedMode = button.getValue();

        boolean triggered = false;
        if (selectedMode == ButtonMode.MIDDLE && pressedButton == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            triggered = true;
        } else if (selectedMode == ButtonMode.MOUSE4 && pressedButton == GLFW.GLFW_MOUSE_BUTTON_4) {
            triggered = true;
        } else if (selectedMode == ButtonMode.MOUSE5 && pressedButton == GLFW.GLFW_MOUSE_BUTTON_5) {
            triggered = true;
        }

        if (triggered) {
            startThrow();
        }
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.interactionManager == null) {
            resetState();
            return;
        }

        if (stage == Stage.IDLE) {
            return;
        }

        stageTicks++;
        if (stageTicks > 10) {
            resetState();
            return;
        }

        final PlayerInventory inventory = mc.player.getInventory();

        switch (stage) {
            case SWITCH_TO_PEARL -> {
                if (!isHotbarSlotValid(pearlSlot) || !inventory.getStack(pearlSlot).isOf(Items.ENDER_PEARL)) {
                    resetState();
                    return;
                }
                inventory.setSelectedSlot(pearlSlot);
                stage = Stage.THROW;
                stageTicks = 0;
            }
            case THROW -> {
                if (!mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
                    resetState();
                    return;
                }
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                stage = Stage.RESTORE;
                stageTicks = 0;
            }
            case RESTORE -> {
                if (isHotbarSlotValid(oldSlot)) {
                    inventory.setSelectedSlot(oldSlot);
                }
                resetState();
            }
            default -> resetState();
        }
    }

    private void startThrow() {
        if (stage != Stage.IDLE || mc.player == null) {
            return;
        }

        final PlayerInventory inventory = mc.player.getInventory();

        // Search hotbar (0-8)
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.ENDER_PEARL)) {
                pearlSlot = i;
                break;
            }
        }

        if (!isHotbarSlotValid(pearlSlot)) {
            pearlSlot = -1;
            return;
        }

        oldSlot = inventory.getSelectedSlot();
        stage = Stage.SWITCH_TO_PEARL;
        stageTicks = 0;
    }

    private boolean isHotbarSlotValid(final int slot) {
        return slot >= 0 && slot < 9;
    }

    private void resetState() {
        stage = Stage.IDLE;
        stageTicks = 0;
        oldSlot = -1;
        pearlSlot = -1;
    }

    private enum Stage {
        IDLE,
        SWITCH_TO_PEARL,
        THROW,
        RESTORE
    }

    public enum ButtonMode {
        MIDDLE("Middle"),
        MOUSE4("Mouse 4"),
        MOUSE5("Mouse 5");

        private final String name;

        ButtonMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
