package wtf.uitems.client.feature.module.impl.visual;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;

public final class NoHurtCameraModule extends Module {
    public NoHurtCameraModule() {
        super("No Hurt Camera", "Disables the camera tilt when damaged.", ModuleCategory.VISUAL);
        addProperties(this.hideModelDamage);
    }

    private final BooleanProperty hideModelDamage = new BooleanProperty("No player model hurt", false);

    public boolean isHideModelDamage() {
        return this.hideModelDamage.getValue();
    }
}
