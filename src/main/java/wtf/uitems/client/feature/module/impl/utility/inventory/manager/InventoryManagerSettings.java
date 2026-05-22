package wtf.uitems.client.feature.module.impl.utility.inventory.manager;

import wtf.uitems.client.feature.module.property.impl.GroupProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.bool.MultipleBooleanProperty;
import wtf.uitems.client.feature.module.property.impl.number.BoundedNumberProperty;
import wtf.uitems.client.feature.module.property.impl.number.NumberProperty;

import wtf.uitems.client.feature.module.property.impl.mode.ModeProperty;

public final class InventoryManagerSettings {

    private final BoundedNumberProperty delay;
    private final BooleanProperty gRandom;

    private final MultipleBooleanProperty slots;
    private final NumberProperty swordSlot, pickaxeSlot, axeSlot, blockSlot, bucketSlot, enderPearlSlot /*bowSlot, potionSlot, goldenAppleSlot*/;

    private final ModeProperty<OffhandMode> offhandMode;

    // TODO: implement the commented out stuff
    public InventoryManagerSettings(final InventoryManagerModule module) {
        this.delay = new BoundedNumberProperty("Delay", 50, 100, 0, 400, 5);
        this.gRandom = new BooleanProperty("G random", false);

        this.offhandMode = new ModeProperty<>("Offhand", OffhandMode.NONE);

        this.slots = new MultipleBooleanProperty("Slots",
                new BooleanProperty("Sword", true),
                new BooleanProperty("Pickaxe", true),
                new BooleanProperty("Axe", true),
                new BooleanProperty("Blocks", true),
                new BooleanProperty("Buckets", true),
                new BooleanProperty("Ender Pearls", true)
//                new BooleanProperty("Bow", true),
//                new BooleanProperty("Potions", true),
//                new BooleanProperty("Golden Apples", true),
        );

        this.swordSlot = new NumberProperty("Sword Slot", 1, 1, 9, 1).hideIf(() -> !slots.getProperty("Sword").getValue());
        this.pickaxeSlot = new NumberProperty("Pickaxe Slot", 2, 1, 9, 1).hideIf(() -> !slots.getProperty("Pickaxe").getValue());
        this.axeSlot = new NumberProperty("Axe Slot", 3, 1, 9, 1).hideIf(() -> !slots.getProperty("Axe").getValue());
        this.blockSlot = new NumberProperty("Block Slot", 4, 1, 9, 1).hideIf(() -> !slots.getProperty("Blocks").getValue());
        this.bucketSlot = new NumberProperty("Bucket Slot", 5, 1, 9, 1).hideIf(() -> !slots.getProperty("Buckets").getValue());
        this.enderPearlSlot = new NumberProperty("Ender Pearl Slot", 6, 1, 9, 1).hideIf(() -> !slots.getProperty("Ender Pearls").getValue());
//        this.bowSlot = new NumberProperty("Bow Slot", 5, 1, 9, 1).hideIf(() -> !slots.getProperty("Bow").getValue());
//        this.potionSlot = new NumberProperty("Potion Slot", 6, 1, 9, 1).hideIf(() -> !slots.getProperty("Potions").getValue());
//        this.goldenAppleSlot = new NumberProperty("Golden Apple Slot", 7, 1, 9, 1).hideIf(() -> !slots.getProperty("Golden Apples").getValue());

        module.addProperties(gRandom, delay, offhandMode, new GroupProperty("Slots", slots, swordSlot, pickaxeSlot, axeSlot, blockSlot, bucketSlot, enderPearlSlot /*bowSlot, potionSlot, goldenAppleSlot*/));
    }

    public Double getDelay() {
        return gRandom.getValue() ? delay.getRandomGaussian() : delay.getRandomValue();
    }

    public MultipleBooleanProperty getSlots() {
        return slots;
    }

    public int getSwordSlot() {
        return swordSlot.getValue().intValue();
    }

    public int getPickaxeSlot() {
        return pickaxeSlot.getValue().intValue();
    }

    public int getAxeSlot() {
        return axeSlot.getValue().intValue();
    }

    public int getBlockSlot() {
        return blockSlot.getValue().intValue();
    }

    public int getBucketSlot() {
        return bucketSlot.getValue().intValue();
    }

    public int getEnderPearlSlot() {
        return enderPearlSlot.getValue().intValue();
    }

    public ModeProperty<OffhandMode> getOffhandMode() {
        return offhandMode;
    }

    public enum OffhandMode {
        NONE, GOLDEN_APPLE, PROJECTILES
    }
}
