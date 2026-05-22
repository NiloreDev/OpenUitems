package wtf.uitems.client.feature.module.impl.visual;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.lwjgl.glfw.GLFW;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.binding.type.InputType;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.client.screen.click.AbstractClickGui;
import wtf.uitems.client.screen.click.dropdown.DropdownClickGUI;
import wtf.uitems.client.screen.click.jello.JelloClickGUI;
import wtf.uitems.client.screen.click.modern.ModernClickGUI;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.render.RenderBloomEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.player.PlayerUtility;

import static wtf.uitems.client.Constants.mc;

public final class ClickGUIModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.MODERN);
    private final BooleanProperty allowMovement = new BooleanProperty("Allow movement", true);

    public ClickGUIModule() {
        super("Click GUI", "A display for interacting with client features.", ModuleCategory.VISUAL);
        addProperties(mode, allowMovement);
        OpalClient.getInstance().getBindRepository().getBindingService().register(GLFW.GLFW_KEY_RIGHT_SHIFT, this, InputType.KEYBOARD);
    }

    @Override
    protected void onEnable() {
        mc.setScreen(mode.getValue().getScreen());
    }

    @Override
    protected void onDisable() {
        if (mc.currentScreen instanceof AbstractClickGui clickGui) {
            clickGui.close();
        }
    }

    @Subscribe
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.currentScreen instanceof AbstractClickGui) {
            if (!allowMovement.getValue()) return;
            if (AbstractClickGui.selectingBind || AbstractClickGui.typingString) {
                PlayerUtility.unpressMovementKeyStates();
            } else {
                PlayerUtility.updateMovementKeyStates();
            }
        }
    }

    @Subscribe
    public void onBloomRender(final RenderBloomEvent event) {
        if (mc.currentScreen instanceof AbstractClickGui clickGui) {
            clickGui.doRender(event.drawContext(), -1, -1, event.tickDelta());
        }
    }

    @RequiredArgsConstructor @Getter
    public enum Mode {
        DROPDOWN(new DropdownClickGUI()),
        MODERN(new ModernClickGUI()),
        JELLO(new JelloClickGUI());
        final AbstractClickGui screen;
    }
}
