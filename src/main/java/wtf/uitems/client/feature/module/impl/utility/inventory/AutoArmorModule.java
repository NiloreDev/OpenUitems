package wtf.uitems.client.feature.module.impl.utility.inventory;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.uitems.client.feature.module.impl.movement.InventoryMoveModule;
import wtf.uitems.client.feature.module.impl.utility.inventory.manager.InventoryManagerModule;
import wtf.uitems.client.feature.module.property.impl.number.BoundedNumberProperty;
import wtf.uitems.client.feature.module.repository.ModuleRepository;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.player.InventoryUtility;

import java.util.*;
import java.util.stream.Collectors;

import static wtf.uitems.client.Constants.mc;

public final class AutoArmorModule extends Module {

    private final BoundedNumberProperty delay = new BoundedNumberProperty("Delay", 50, 100, 0, 400, 5);

    public AutoArmorModule() {
        super("Auto Armor", "Automatically equips the best armor possible.", ModuleCategory.UTILITY);
        addProperties(delay);
    }

    @Subscribe
    public void onPreGameTickEvent(final PreGameTickEvent event) {
        if (mc.player == null) return;

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();

        if (!(mc.currentScreen instanceof InventoryScreen) && !moduleRepository.getModule(InventoryMoveModule.class).isEnabled())
            return;

        final KillAuraModule killAuraModule = moduleRepository.getModule(KillAuraModule.class);
        if (killAuraModule.isEnabled() && killAuraModule.getTargeting().isTargetSelected()) {
            return;
        }

        // server-specific restrictions removed

        final ScreenHandler screenHandler = mc.player.currentScreenHandler;

        if (!(screenHandler instanceof PlayerScreenHandler playerHandler)) {
            return;
        }

        final InventoryManagerModule managerModule = moduleRepository.getModule(InventoryManagerModule.class);

        if (!screenHandler.getCursorStack().isEmpty() && InventoryUtility.isArmor(screenHandler.getCursorStack())) {
            if (managerModule.canMove(delay.getRandomValue().longValue()) && equipCursorArmor(playerHandler)) {
                managerModule.stopwatch.reset();
            }
            return;
        }

        final List<Slot> bestArmor = getBestArmor(playerHandler);

        InventoryUtility.filterSlots(playerHandler, slot -> !slot.getStack().isEmpty() && InventoryUtility.isArmor(slot.getStack()), true).forEach(validSlot -> {
            final ItemStack itemStack = validSlot.getStack();

            if (bestArmor.stream().noneMatch(armor -> armor.getStack() == itemStack)) {
                if (!managerModule.canMove(delay.getRandomValue().longValue())) return;

                InventoryUtility.drop(playerHandler, validSlot.id);

                managerModule.stopwatch.reset();
            }
        });

        bestArmor.forEach(equipmentSlotPair -> {
            final List<ItemStack> armorStacks = getArmorStacks();
            Collections.shuffle(armorStacks);

            if (armorStacks.stream().noneMatch(armor -> equipmentSlotPair.getStack() == armor)) {
                if (!managerModule.canMove(delay.getRandomValue().longValue())) return;

                InventoryUtility.shiftClick(playerHandler, equipmentSlotPair.id, 0);

                managerModule.stopwatch.reset();
            }
        });
    }

    private List<Slot> getBestArmor(final PlayerScreenHandler screenHandler) {
        return Arrays.stream(EquipmentSlot.values())
                .map(slotType -> InventoryUtility.filterSlots(screenHandler, slot -> {
                            if (slot.getStack().isEmpty() || !InventoryUtility.isArmor(slot.getStack())) {
                                return false;
                            }
                            final EquippableComponent equippable = slot.getStack().getComponents().get(DataComponentTypes.EQUIPPABLE);
                            return equippable != null && equippable.slot() == slotType;
                        }, false)
                        .stream()
                        .max(Comparator.comparing(slot -> InventoryUtility.getArmorValue(slot.getStack())))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<ItemStack> getArmorStacks() {
        final List<ItemStack> armorStacks = new ArrayList<>();

        for (final EquipmentSlot slot : EquipmentSlot.values()) {
            final ItemStack equippedStack = mc.player.getEquippedStack(slot);
            if (!equippedStack.isEmpty() && InventoryUtility.isArmor(equippedStack)) {
                armorStacks.add(equippedStack);
            }
        }

        return armorStacks;
    }

    private boolean equipCursorArmor(final PlayerScreenHandler screenHandler) {
        final ItemStack cursorStack = screenHandler.getCursorStack();
        final EquippableComponent equippable = cursorStack.getComponents().get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) {
            return false;
        }

        final int armorSlotId = getArmorSlotId(equippable.slot());
        if (armorSlotId == -1) {
            return false;
        }

        mc.interactionManager.clickSlot(screenHandler.syncId, armorSlotId, 0, SlotActionType.PICKUP, mc.player);
        return true;
    }

    private int getArmorSlotId(final EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }

}
