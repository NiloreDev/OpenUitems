package wtf.uitems.client.feature.helper.impl.player.swing;

import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;

import java.util.function.BooleanSupplier;

public final class CPSProperty {

    private final NumberProperty delay;
    private final GroupProperty groupProperty;

    public CPSProperty(final Module parent) {
        this(parent, "CPS", false);
    }

    public CPSProperty(final Module parent, final String groupName, final boolean unused) {
        this.delay = new NumberProperty("CPS", 10, 1, 20, 1);

        this.groupProperty = new GroupProperty(groupName, this.delay);
        parent.addProperties(this.groupProperty);
    }

    public CPSProperty hideIf(BooleanSupplier hiddenSupplier) {
        this.groupProperty.hideIf(hiddenSupplier);
        return this;
    }

    public int getCPS() {
        return this.delay.getValue().intValue();
    }

    public int getClickDelay() {
        return 1000 / getCPS();
    }

    private long nextClick;

    public void resetClick() {
        this.nextClick = getClickDelay();
    }

    public long getNextClick() {
        return nextClick;
    }
}
