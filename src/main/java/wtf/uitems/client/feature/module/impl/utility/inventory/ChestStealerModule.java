package wtf.uitems.client.feature.module.impl.utility.inventory;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.client.feature.module.property.impl.number.BoundedNumberProperty;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.misc.time.Stopwatch;
import wtf.uitems.utility.player.InventoryUtility;

import java.util.*;
import java.util.stream.IntStream;

import static wtf.uitems.client.Constants.mc;

public final class ChestStealerModule extends Module {

    private final Stopwatch stopwatch = new Stopwatch();

    private final BooleanProperty smart = new BooleanProperty("Smart", true);
    private final BooleanProperty highlight = new BooleanProperty("Highlight items", true).hideIf(() -> !smart.getValue());

    private final BoundedNumberProperty delay = new BoundedNumberProperty("Delay", 50, 100, 0, 400, 5);


    public ChestStealerModule() {
        super("Chest Stealer", "Steals only useful or upgraded items from chests.", ModuleCategory.UTILITY);
        addProperties(smart, highlight, delay);
    }

    @Subscribe
    public void onPreGameTickEvent(final PreGameTickEvent event) {
        if (!(mc.currentScreen instanceof GenericContainerScreen container)) return;

        final GenericContainerScreenHandler screenHandler = container.getScreenHandler();
        final Inventory chestInventory = screenHandler.getInventory();

        if (chestInventory.isEmpty() || InventoryUtility.isInventoryFull()) {
            container.close();
            return;
        }

        final Map<EquipmentSlot, ItemStack> bestChestArmor = getBestChestArmor(chestInventory);
        final ItemStack bestChestSword = getBestChestSword(chestInventory);
        final ItemStack bestChestPickaxe = getBestChestTool(chestInventory, ItemTags.PICKAXES);
        final ItemStack bestChestAxe = getBestChestTool(chestInventory, ItemTags.AXES);

        boolean tookItem = false;

        for (int i = 0; i < chestInventory.size(); i++) {
            final ItemStack stack = chestInventory.getStack(i);
            if (stack.isEmpty()) continue;

            if (canMove() && (shouldTake(stack, bestChestArmor, bestChestSword, bestChestPickaxe, bestChestAxe) || !smart.getValue())) {
                InventoryUtility.shiftClick(screenHandler, i, 0);
                stopwatch.reset();
                tookItem = true;
                break;
            }
        }

        if (smart.getValue() && !tookItem) {
            boolean hasValuableLeft = false;
            for (int i = 0; i < chestInventory.size(); i++) {
                final ItemStack stack = chestInventory.getStack(i);
                if (stack.isEmpty()) continue;

                if (shouldTake(stack, bestChestArmor, bestChestSword, bestChestPickaxe, bestChestAxe)) {
                    hasValuableLeft = true;
                    break;
                }
            }

            if (!hasValuableLeft) {
                container.close();
            }
        }

        // Pearl fixing logic

    }

    public BooleanProperty getHighlight() {
        return highlight;
    }

    public BooleanProperty getSmart() {
        return smart;
    }

    public boolean shouldTake(ItemStack stack,
                              Map<EquipmentSlot, ItemStack> bestChestArmor,
                              ItemStack bestChestSword,
                              ItemStack bestChestPickaxe,
                              ItemStack bestChestAxe) {
        final Item item = stack.getItem();

        if (item == Items.TNT) return true;
        if (item == Items.COMPASS) return getItemCount(Items.COMPASS) < 1;
        if (item == Items.LAVA_BUCKET) return getItemCount(Items.LAVA_BUCKET) < 1;
        if (item == Items.WATER_BUCKET) return getItemCount(Items.WATER_BUCKET) < 2;

        if (item instanceof ArrowItem) {
            int have = 0;
            for (int i = 0; i < 45; i++) {
                final ItemStack inv = mc.player.getInventory().getStack(i);
                if (inv.getItem() instanceof ArrowItem) {
                    have += inv.getCount();
                }
            }
            return have < 64;
        }

        if (item instanceof BowItem) {
            int bestPower = 0;
            boolean hasPunch = false;
            for (int i = 0; i < 45; i++) {
                final ItemStack inv = mc.player.getInventory().getStack(i);
                if (inv.getItem() instanceof BowItem) {
                    bestPower = Math.max(bestPower, InventoryUtility.calculateEnchantmentLevel(inv, Enchantments.POWER));
                    if (InventoryUtility.calculateEnchantmentLevel(inv, Enchantments.PUNCH) > 0) {
                        hasPunch = true;
                    }
                }
            }
            final int power = InventoryUtility.calculateEnchantmentLevel(stack, Enchantments.POWER);
            final int punch = InventoryUtility.calculateEnchantmentLevel(stack, Enchantments.PUNCH);
            if (punch > 0 && !hasPunch) {
                return true;
            }
            return power > bestPower;
        }

        if (item instanceof CrossbowItem) {
            boolean hasCrossbow = false;
            for (int i = 0; i < 45; i++) {
                final ItemStack inv = mc.player.getInventory().getStack(i);
                if (inv.getItem() instanceof CrossbowItem) {
                    hasCrossbow = true;
                    break;
                }
            }
            return !hasCrossbow;
        }

        if (InventoryUtility.isGoodItem(stack)) {
            return true;
        }

        if (InventoryUtility.isSpecialItem(stack)) {
            return true;
        }

        if (stack.isIn(ItemTags.SWORDS)) {
            final ItemStack bestInventorySword = getBestInventorySword();
            final double value = InventoryUtility.getSwordValueNoDura(stack);
            final double current = InventoryUtility.getSwordValueNoDura(bestInventorySword);

            if (isDiamondItem(stack) && !isDiamondItem(bestInventorySword)) {
                return stack == bestChestSword;
            }

            return stack == bestChestSword && value > current;
        }

        if (stack.isIn(ItemTags.PICKAXES)) {
            final ItemStack bestInventoryTool = getBestInventoryTool(ItemTags.PICKAXES);
            final double value = InventoryUtility.getToolValueNoDura(stack);
            final double current = InventoryUtility.getToolValueNoDura(bestInventoryTool);

            if (isDiamondItem(stack) && !isDiamondItem(bestInventoryTool)) {
                return stack == bestChestPickaxe;
            }

            return stack == bestChestPickaxe && value > current;
        }

        if (stack.isIn(ItemTags.AXES)) {
            final ItemStack bestInventoryAxe = getBestInventoryAxe();
            final double value = InventoryUtility.getToolValueNoDura(stack);
            final double current = InventoryUtility.getToolValueNoDura(bestInventoryAxe);

            if (isDiamondItem(stack) && !isDiamondItem(bestInventoryAxe)) {
                return stack == bestChestAxe;
            }

            return stack == bestChestAxe && value > current;
        }

        if (!InventoryUtility.isArmor(stack)) return false;

        final EquippableComponent equip = stack.getComponents().get(DataComponentTypes.EQUIPPABLE);
        if (equip == null) return false;

        final EquipmentSlot slot = equip.slot();
        final ItemStack bestInventoryArmor = getBestInventoryArmor(slot);
        final ItemStack bestInChest = bestChestArmor.getOrDefault(slot, ItemStack.EMPTY);

        if (stack != bestInChest) return false;


        final double stackValue = InventoryUtility.getArmorValueNoDura(stack);
        final double inventoryValue = InventoryUtility.getArmorValueNoDura(bestInventoryArmor);

        if (isDiamondArmor(stack) && !isDiamondArmor(bestInventoryArmor)) {
            return true;
        }

        return stackValue > inventoryValue;

    }

    public Map<EquipmentSlot, ItemStack> getBestChestArmor(Inventory chest) {
        final Map<EquipmentSlot, ItemStack> result = new HashMap<>();
        final Map<EquipmentSlot, List<ItemStack>> bySlot = new HashMap<>();

        for (int i = 0; i < chest.size(); i++) {
            final ItemStack stack = chest.getStack(i);
            if (!InventoryUtility.isArmor(stack)) continue;
            final EquippableComponent equip = stack.getComponents().get(DataComponentTypes.EQUIPPABLE);
            if (equip == null) continue;
            bySlot.computeIfAbsent(equip.slot(), k -> new ArrayList<>()).add(stack);
        }

        bySlot.forEach((slot, stacks) -> {
            result.put(slot, stacks.stream().max(getArmorComparator()).orElse(ItemStack.EMPTY));
        });

        return result;
    }

    public ItemStack getBestChestSword(Inventory chest) {
        return IntStream.range(0, chest.size())
                .mapToObj(chest::getStack)
                .filter(stack -> stack.isIn(ItemTags.SWORDS))
                .max(getSwordComparator())
                .orElse(ItemStack.EMPTY);
    }

    public ItemStack getBestChestTool(Inventory chest, TagKey<Item> tag) {
        return IntStream.range(0, chest.size())
                .mapToObj(chest::getStack)
                .filter(stack -> stack.isIn(tag))
                .max(getToolComparator())
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack getBestInventorySword() {
        return IntStream.range(0, 45)
                .mapToObj(i -> mc.player.getInventory().getStack(i))
                .filter(stack -> stack.isIn(ItemTags.SWORDS))
                .max(getSwordComparator())
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack getBestInventoryTool(TagKey<Item> tag) {
        return IntStream.range(0, 45)
                .mapToObj(i -> mc.player.getInventory().getStack(i))
                .filter(stack -> stack.isIn(tag))
                .max(getToolComparator())
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack getBestInventoryAxe() {
        return IntStream.range(0, 45)
                .mapToObj(i -> mc.player.getInventory().getStack(i))
                .filter(stack -> stack.getItem() instanceof AxeItem)
                .max(getToolComparator())
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack getBestInventoryArmor(EquipmentSlot slot) {
        return IntStream.range(0, 45)
                .mapToObj(i -> mc.player.getInventory().getStack(i))
                .filter(stack -> {
                    if (!InventoryUtility.isArmor(stack)) return false;
                    final EquippableComponent equip = stack.getComponents().get(DataComponentTypes.EQUIPPABLE);
                    return equip != null && equip.slot() == slot;
                })
                .max(getArmorComparator())
                .orElse(ItemStack.EMPTY);
    }

    private Comparator<ItemStack> getArmorComparator() {
        return (s1, s2) -> {
            if (isDiamondArmor(s1) && !isDiamondArmor(s2)) return 1;
            if (!isDiamondArmor(s1) && isDiamondArmor(s2)) return -1;
            return Double.compare(InventoryUtility.getArmorValueNoDura(s1), InventoryUtility.getArmorValueNoDura(s2));
        };
    }

    private Comparator<ItemStack> getSwordComparator() {
        return (s1, s2) -> {
            if (isDiamondItem(s1) && !isDiamondItem(s2)) return 1;
            if (!isDiamondItem(s1) && isDiamondItem(s2)) return -1;
            return Double.compare(InventoryUtility.getSwordValueNoDura(s1), InventoryUtility.getSwordValueNoDura(s2));
        };
    }

    private Comparator<ItemStack> getToolComparator() {
        return (s1, s2) -> {
            if (isDiamondItem(s1) && !isDiamondItem(s2)) return 1;
            if (!isDiamondItem(s1) && isDiamondItem(s2)) return -1;
            return Double.compare(InventoryUtility.getToolValueNoDura(s1), InventoryUtility.getToolValueNoDura(s2));
        };
    }

    public boolean canMove() {
        final long delayMs = delay.getRandomValue().longValue();
        return delayMs == 0 || stopwatch.hasTimeElapsed(delayMs);
    }

    private boolean isDiamondArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        final Item item = stack.getItem();
        return item == Items.DIAMOND_HELMET
                || item == Items.DIAMOND_CHESTPLATE
                || item == Items.DIAMOND_LEGGINGS
                || item == Items.DIAMOND_BOOTS;
    }

    private boolean isDiamondItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        final Item item = stack.getItem();
        return item == Items.DIAMOND_SWORD
                || item == Items.DIAMOND_PICKAXE
                || item == Items.DIAMOND_AXE
                || item == Items.DIAMOND_SHOVEL
                || item == Items.DIAMOND_HOE;
    }

    private int getItemCount(Item item) {
        int count = 0;
        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
