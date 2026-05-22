package wtf.uitems.client.feature.module.impl.movement.noslow;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.movement.noslow.impl.Heypixel2NoSlow;
import wtf.uitems.client.feature.module.impl.movement.noslow.impl.HeypixelNoSlow;
import wtf.uitems.client.feature.module.impl.movement.noslow.impl.Heypixel3NoSlow;
import wtf.uitems.client.feature.module.impl.movement.noslow.impl.UniversalNoSlow;
import wtf.uitems.client.feature.module.impl.movement.noslow.impl.VanillaNoSlow;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.subscriber.Subscribe;

import static wtf.uitems.client.Constants.mc;

public final class NoSlowModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", this, Mode.VANILLA);

    private final BooleanProperty allowSprinting = new BooleanProperty("Allow sprinting", true);

    private Action action = Action.NONE;

    public NoSlowModule() {
        super("No Slow", "Removes vanilla slowdowns such as item usage.", ModuleCategory.MOVEMENT);
        addModuleModes(mode, new VanillaNoSlow(this), new UniversalNoSlow(this), new HeypixelNoSlow(this), new Heypixel2NoSlow(this), new Heypixel3NoSlow(this));
        addProperties(mode, allowSprinting);
    }

    @Subscribe(priority = 2)
    public void onPreGameTick(final PreGameTickEvent event) {
        if (mc.player == null || mc.currentScreen != null || mc.getOverlay() != null) {
            this.action = Action.NONE;
            return;
        }

        SlotHelper slotHelper = SlotHelper.getInstance();
        ItemStack mainHandStack = slotHelper.getSilence() == SlotHelper.Silence.FULL ? slotHelper.getMainHandStack(mc.player) : mc.player.getMainHandStack();
        switch (mainHandStack.getUseAction()) {
            case BLOCK -> action = Action.BLOCKABLE;
            case NONE -> action = mainHandStack.isIn(ItemTags.SWORDS) ? Action.BLOCKABLE : Action.NONE;
            case BOW -> action = Action.BOW;
            default -> action = Action.USEABLE;
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
    }

    @Override
    public String getSuffix() {
        return mode.getValue().toString();
    }

    public Action getAction() {
        return action;
    }

    public enum Mode {
        VANILLA("Vanilla"),
        UNIVERSAL("Universal"),
        HEYPIXEL("Heypixel"),
        HEYPIXEL2("Heypixel 2"),
        HEYPIXEL3("Heypixel 3");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Action {
        BLOCKABLE,
        USEABLE,
        BOW,
        NONE
    }

    public boolean isSprintingAllowed() {
        return allowSprinting.getValue();
    }

}
