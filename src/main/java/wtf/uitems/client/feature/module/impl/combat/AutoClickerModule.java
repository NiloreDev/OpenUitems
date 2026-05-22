package wtf.uitems.client.feature.module.impl.combat;

import net.minecraft.util.hit.HitResult;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.uitems.client.feature.helper.impl.player.swing.CPSProperty;
import wtf.uitems.client.feature.helper.impl.player.swing.SwingDelay;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.bool.MultipleBooleanProperty;
import wtf.uitems.event.impl.game.input.MouseHandleInputEvent;
import wtf.uitems.event.impl.game.player.interaction.AttackDelayEvent;
import wtf.uitems.event.subscriber.Subscribe;

import static wtf.uitems.client.Constants.mc;

public final class AutoClickerModule extends Module {

    private final MultipleBooleanProperty mouseButtons = new MultipleBooleanProperty("Mouse buttons",
            new BooleanProperty("Left", true),
            new BooleanProperty("Right", false)
    );
    private final CPSProperty cpsProperty = new CPSProperty(this);
    private final BooleanProperty requirePressed = new BooleanProperty("Require pressed", true);

    public AutoClickerModule() {
        super("Auto Clicker", "Clicks for you automatically.", ModuleCategory.COMBAT);
        addProperties(mouseButtons, requirePressed);
    }

    @Subscribe
    public void onHandleInput(final MouseHandleInputEvent event) {
        final wtf.uitems.client.feature.module.impl.visual.AnimationsModule animationsModule = OpalClient.getInstance().getModuleRepository().getModule(wtf.uitems.client.feature.module.impl.visual.AnimationsModule.class);
        final boolean allowSwingWhenUsing = animationsModule.isEnabled() && animationsModule.isSwingWhileUsing();
        if (mc.player.isUsingItem() && !allowSwingWhenUsing) {
            return;
        }

        if (SwingDelay.isSwingAvailable(cpsProperty)) {
            if (mouseButtons.getProperty("Left").getValue() && mc.crosshairTarget != null && mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
                if (!requirePressed.getValue() || mc.options.attackKey.isPressed()) {
                    MouseHelper.getLeftButton().setPressed();
                }
            }

            if (mouseButtons.getProperty("Right").getValue()) {
                if (!requirePressed.getValue() || mc.options.useKey.isPressed()) {
                    MouseHelper.getRightButton().setPressed();
                }
            }
        }
    }

    @Subscribe
    public void onAttackCooldown(AttackDelayEvent event) {
        event.setDelay(0);
    }

}
