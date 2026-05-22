package wtf.uitems.client.feature.module.impl.utility.inventory.manager;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.uitems.client.feature.module.impl.movement.InventoryMoveModule;
import wtf.uitems.client.feature.module.impl.world.scaffold.ScaffoldModule;
import wtf.uitems.client.feature.module.repository.ModuleRepository;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.misc.time.Stopwatch;
import wtf.uitems.utility.player.InventoryUtility;
import wtf.uitems.utility.player.PlayerUtility;

import java.util.Comparator;

import static wtf.uitems.client.Constants.mc;

public final class InventoryManagerModule extends Module {

    private final InventoryManagerSettings settings = new InventoryManagerSettings(this);

    public final Stopwatch stopwatch = new Stopwatch();

    public InventoryManagerModule() {
        super("Inventory Manager", "Manages your inventory.", ModuleCategory.UTILITY);
    }

    @Subscribe
    public void onPreGameTickEvent(final PreGameTickEvent event) {
        if (mc.player == null) return;

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();

        if (!(mc.currentScreen instanceof InventoryScreen) && !moduleRepository.getModule(InventoryMoveModule.class).isEnabled())
            return;

        final KillAuraModule killAuraModule = moduleRepository.getModule(KillAuraModule.class);
        final ScaffoldModule scaffoldModule = moduleRepository.getModule(ScaffoldModule.class);
        if ((killAuraModule.isEnabled() && killAuraModule.getTargeting().isTargetSelected())
                || scaffoldModule.isEnabled()) {
            return;
        }

        final boolean blitz = false;

        final ScreenHandler screenHandler = mc.player.currentScreenHandler;

        if (!(screenHandler instanceof PlayerScreenHandler playerHandler)) {
            return;
        }

        final Slot bestSword = getBestWeapon(playerHandler);
        final Slot preferredSwordSlot = screenHandler.getSlot(settings.getSwordSlot() + 35);

        final Slot bestPickaxe = getBestPickaxe(playerHandler);
        final Slot preferredPickaxeSlot = screenHandler.getSlot(settings.getPickaxeSlot() + 35);

        final Slot bestAxe = getBestAxe(playerHandler);
        final Slot preferredAxeSlot = screenHandler.getSlot(settings.getAxeSlot() + 35);

        final Slot mostBlocks = getMostBlocks(playerHandler);
        final Slot preferredBlockSlot = screenHandler.getSlot(settings.getBlockSlot() + 35);

        final Slot bestBucket = getBestBucket(playerHandler);
        final Slot preferredBucketSlot = screenHandler.getSlot(settings.getBucketSlot() + 35);

        final Slot bestPearl = getBestPearl(playerHandler);
        final Slot preferredPearlSlot = screenHandler.getSlot(settings.getEnderPearlSlot() + 35);

        final Slot bestOffhand = getBestOffhand(playerHandler);
        final Slot offhandSlot = screenHandler.getSlot(45);

        if (settings.getOffhandMode().getValue() != InventoryManagerSettings.OffhandMode.NONE
                && screenHandler.getCursorStack().isEmpty()
                && canMove(settings.getDelay().longValue())) {
            arrangeOffhand(screenHandler, offhandSlot, bestOffhand);
        }

        if (settings.getSlots().getProperty("Sword").getValue() && canMove(settings.getDelay().longValue())) {
            arrangeBestSword(screenHandler, preferredSwordSlot, bestSword);
        }

        if (settings.getSlots().getProperty("Pickaxe").getValue() && canMove(settings.getDelay().longValue())) {
            arrangeBestPickaxe(screenHandler, preferredPickaxeSlot, bestPickaxe);
        }

        if (settings.getSlots().getProperty("Axe").getValue() && canMove(settings.getDelay().longValue())) {
            arrangeBestAxe(screenHandler, preferredAxeSlot, bestAxe);
        }

        if (settings.getSlots().getProperty("Blocks").getValue() && canMove(settings.getDelay().longValue())) {
            arrangeMostBlocks(screenHandler, preferredBlockSlot, mostBlocks);
        }

        if (settings.getSlots().getProperty("Buckets").getValue() && canMove(settings.getDelay().longValue())) {
            arrangeBestBucket(screenHandler, preferredBucketSlot, bestBucket);
        }

        if (settings.getSlots().getProperty("Ender Pearls").getValue() && canMove(settings.getDelay().longValue())) {
            arrangeBestPearl(screenHandler, preferredPearlSlot, bestPearl);
        }

        if (screenHandler.getCursorStack().isEmpty()) {
            InventoryUtility.filterSlots(playerHandler, slot -> !slot.getStack().isEmpty(), true).forEach(validSlot -> {
                if (!canMove(settings.getDelay().longValue()) || !screenHandler.getCursorStack().isEmpty()) {
                    return;
                }

                if (!canMove(settings.getDelay().longValue()) || !screenHandler.getCursorStack().isEmpty()) return;

                if (shouldDropExcess(playerHandler, validSlot)) {
                    InventoryUtility.drop(playerHandler, validSlot.id);
                    stopwatch.reset();
                    return;
                }

                if (shouldKeepSwordWhenSharpAxe(playerHandler, validSlot)) {
                    return;
                }

                if (InventoryUtility.isGoodItem(validSlot.getStack())) {
                    return;
                }

                if (validSlot.getStack().getItem().getComponents().get(DataComponentTypes.EQUIPPABLE) != null) {
                    return;
                }

                if (validSlot.getIndex() == preferredSwordSlot.getIndex() && (validSlot.getStack().isIn(ItemTags.SWORDS) || validSlot.getStack().getItem() instanceof AxeItem)) {
                    return;
                }
                if (validSlot.getIndex() == preferredPickaxeSlot.getIndex() && validSlot.getStack().isIn(ItemTags.PICKAXES)) {
                    return;
                }
                if (validSlot.getIndex() == preferredAxeSlot.getIndex() && validSlot.getStack().getItem() instanceof AxeItem) {
                    return;
                }
                if (validSlot.getIndex() == preferredBucketSlot.getIndex() && validSlot.getStack().getItem() instanceof BucketItem) {
                    return;
                }
                if (validSlot.getIndex() == preferredPearlSlot.getIndex() && validSlot.getStack().getItem() == Items.ENDER_PEARL) {
                    return;
                }
                if (validSlot.getStack().getItem() instanceof BucketItem) {
                    return;
                }

                if (settings.getOffhandMode().getValue() == InventoryManagerSettings.OffhandMode.PROJECTILES && (validSlot.getStack().getItem() == Items.EGG || validSlot.getStack().getItem() == Items.SNOWBALL)) {
                    return;
                }

                if (bestOffhand != null && validSlot.id == bestOffhand.id) {
                    return;
                }

                if (validSlot.getStack().getName().getStyle().isEmpty() || blitz && validSlot.getStack().getItem() != Items.NETHER_STAR) { // blitz star
                    InventoryUtility.drop(playerHandler, validSlot.id);
                    stopwatch.reset();
                }
            });
        }
    }

    @Subscribe
    public void onReceivePacket(final ReceivePacketEvent event) {
        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket slotUpdate
                && slotUpdate.getStack().getItem() != Items.AIR
                && mc.player != null
                && slotUpdate.getSyncId() == mc.player.playerScreenHandler.syncId) {
            stopwatch.reset();
        }
    }

    private void arrangeBestSword(final ScreenHandler screenHandler, final Slot preferredSwordSlot, final Slot bestSwordSlot) {
        if (bestSwordSlot != null && bestSwordSlot.getIndex() != preferredSwordSlot.getIndex()) {
            ItemStack bestSword = bestSwordSlot.getStack();
            ItemStack preferredSword = preferredSwordSlot.getStack();

            double bestValue = valueForSwordSlot(bestSword);
            double preferredValue = valueForSwordSlot(preferredSword);

            if (bestValue > preferredValue) {
                InventoryUtility.swap(screenHandler, bestSwordSlot.id, preferredSwordSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    private Slot getBestWeapon(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot -> slot.getStack().isIn(ItemTags.SWORDS) || slot.getStack().getItem() instanceof AxeItem, false)
                .stream()
                .max(Comparator.comparing(weaponSlot -> valueForSwordSlot(weaponSlot.getStack())))
                .orElse(null);
    }

    private double getWeaponValue(final ItemStack stack) {
        return valueForSwordSlot(stack);
    }

    private Slot getBestSword(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot -> slot.getStack().isIn(ItemTags.SWORDS) || InventoryUtility.isSpecialItem(slot.getStack()), false)
                .stream()
                .max(Comparator.comparing(swordSlot -> {
                    ItemStack stack = swordSlot.getStack();
                    if (stack.isIn(ItemTags.SWORDS)) {
                        return InventoryUtility.getSwordValue(stack);
                    } else {
                        // For special axes used as swords
                        return InventoryUtility.getToolValue(stack);
                    }
                }))
                .orElse(null);
    }

    private void arrangeBestPickaxe(final ScreenHandler screenHandler, final Slot preferredPickaxeSlot, final Slot bestPickaxeSlot) {
        if (bestPickaxeSlot != null && bestPickaxeSlot.getIndex() != preferredPickaxeSlot.getIndex()) {
            double bestPickaxeValue = InventoryUtility.getToolValue(bestPickaxeSlot.getStack());
            double preferredPickaxeValue = InventoryUtility.getToolValue(preferredPickaxeSlot.getStack());

            if (bestPickaxeValue > preferredPickaxeValue) {
                InventoryUtility.swap(screenHandler, bestPickaxeSlot.id, preferredPickaxeSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    private Slot getBestPickaxe(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot -> slot.getStack().isIn(ItemTags.PICKAXES), false)
                .stream()
                .max(Comparator.comparing(pickaxeSlot -> InventoryUtility.getToolValue(pickaxeSlot.getStack())))
                .orElse(null);
    }

    private void arrangeBestAxe(final ScreenHandler screenHandler, final Slot preferredAxeSlot, final Slot bestAxeSlot) {
        if (bestAxeSlot != null && bestAxeSlot.getIndex() != preferredAxeSlot.getIndex()) {
            // If the best axe is already in the sword slot, don't try to move it to the axe slot
            final Slot preferredSwordSlot = screenHandler.getSlot(settings.getSwordSlot() + 35);
            if (bestAxeSlot.getIndex() == preferredSwordSlot.getIndex()) {
                return;
            }

            double bestAxeValue = InventoryUtility.getToolValueNoDura(bestAxeSlot.getStack());
            if (bestAxeSlot.getStack().getItem() == Items.DIAMOND_AXE) bestAxeValue += 1_000_000.0;
            double preferredAxeValue = InventoryUtility.getToolValueNoDura(preferredAxeSlot.getStack());
            if (preferredAxeSlot.getStack().getItem() == Items.DIAMOND_AXE) preferredAxeValue += 1_000_000.0;

            if (bestAxeValue > preferredAxeValue) {
                InventoryUtility.swap(screenHandler, bestAxeSlot.id, preferredAxeSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    private Slot getBestAxe(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot -> slot.getStack().getItem() instanceof AxeItem, false)
                .stream()
                .filter(slot -> {
                    // Don't consider the item in the sword slot as the "best axe" if it's a special axe
                    final Slot preferredSwordSlot = screenHandler.getSlot(settings.getSwordSlot() + 35);
                    if (slot.getIndex() == preferredSwordSlot.getIndex()) return false;
                    return !isSharpnessAxe(slot.getStack());
                })
                .max(Comparator.comparing(axeSlot -> {
                    ItemStack s = axeSlot.getStack();
                    double v = InventoryUtility.getToolValueNoDura(s);
                    if (s.getItem() == Items.DIAMOND_AXE) v += 1_000_000.0;
                    return v;
                }))
                .orElse(null);
    }

    private boolean isSharpnessAxe(final ItemStack stack) {
        if (!(stack.getItem() instanceof AxeItem)) return false;
        return InventoryUtility.calculateEnchantmentLevel(stack, Enchantments.SHARPNESS) >= 8;
    }

    private double getSharpAxeSwordScore(final ItemStack stack) {
        double score = PlayerUtility.getStackAttackDamage(stack);
        final int sharpLevel = InventoryUtility.calculateEnchantmentLevel(stack, Enchantments.SHARPNESS) + 1;
        score *= sharpLevel;
        score += InventoryUtility.calculateEnchantmentLevel(stack, Enchantments.FIRE_ASPECT);
        return score;
    }

    private double valueForSwordSlot(final ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        if (stack.isIn(ItemTags.SWORDS)) {
            return InventoryUtility.getSwordValue(stack);
        }
        if (isSharpnessAxe(stack)) {
            return 1_000_000.0 + getSharpAxeSwordScore(stack);
        }
        return 0.0;
    }

    private Slot getMostBlocks(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot ->
                                slot.getStack().getItem() instanceof BlockItem blockItem &&
                                        slot.getStack().getCount() > 0 &&
                                        InventoryUtility.isGoodBlock(blockItem.getBlock())
                        , false)
                .stream()
                .max(Comparator.comparing(blockSlot -> blockSlot.getStack().getCount()))
                .orElse(null);
    }

    private void arrangeMostBlocks(final ScreenHandler screenHandler, final Slot preferredBlockSlot, final Slot mostBlockSlot) {
        if (mostBlockSlot != null && mostBlockSlot.getIndex() != preferredBlockSlot.getIndex()) {
            double mostBlockCount = mostBlockSlot.getStack().getCount();
            double preferredBlockValue = preferredBlockSlot.getStack().getCount();

            if (mostBlockCount > preferredBlockValue) {
                InventoryUtility.swap(screenHandler, mostBlockSlot.id, preferredBlockSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    private void arrangeBestBucket(final ScreenHandler screenHandler, final Slot preferredBucketSlot, final Slot bestBucketSlot) {
        if (bestBucketSlot != null && bestBucketSlot.getIndex() != preferredBucketSlot.getIndex()) {
            final ItemStack bestBucket = bestBucketSlot.getStack();
            final ItemStack preferredBucket = preferredBucketSlot.getStack();

            if (!(preferredBucket.getItem() instanceof BucketItem) ||
                    (bestBucket.getItem() == Items.WATER_BUCKET && preferredBucket.getItem() != Items.WATER_BUCKET)) {
                InventoryUtility.swap(screenHandler, bestBucketSlot.id, preferredBucketSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    private Slot getBestBucket(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot -> slot.getStack().getItem() instanceof BucketItem, false)
                .stream()
                .max(Comparator.comparing(bucketSlot -> {
                    final Item item = bucketSlot.getStack().getItem();
                    if (item == Items.WATER_BUCKET) return 2;
                    if (item != Items.BUCKET) return 1;
                    return 0;
                }))
                .orElse(null);
    }

    private void arrangeBestPearl(final ScreenHandler screenHandler, final Slot preferredPearlSlot, final Slot bestPearlSlot) {
        if (bestPearlSlot != null && bestPearlSlot.getIndex() != preferredPearlSlot.getIndex()) {
            final ItemStack bestPearl = bestPearlSlot.getStack();
            final ItemStack preferredPearl = preferredPearlSlot.getStack();

            if (preferredPearl.getItem() != Items.ENDER_PEARL || bestPearl.getCount() > preferredPearl.getCount()) {
                InventoryUtility.swap(screenHandler, bestPearlSlot.id, preferredPearlSlot.id - 36);
                stopwatch.reset();
            }
        }
    }

    private Slot getBestPearl(final ScreenHandler screenHandler) {
        return InventoryUtility.filterSlots(screenHandler, slot -> slot.getStack().getItem() == Items.ENDER_PEARL, false)
                .stream()
                .max(Comparator.comparingInt(slot -> slot.getStack().getCount()))
                .orElse(null);
    }

    private void arrangeOffhand(final ScreenHandler screenHandler, final Slot offhandSlot, final Slot bestOffhandSlot) {
        final ItemStack offhandStack = offhandSlot.getStack();

        if (!offhandStack.isEmpty() && offhandStack.getCount() < offhandStack.getMaxCount()) {
            final Slot refillSource = getOffhandRefillSource(screenHandler, offhandSlot, offhandStack);
            if (refillSource != null) {
                mergeStackIntoOffhand(screenHandler, offhandSlot, refillSource);
                return;
            }
        }

        if (bestOffhandSlot != null && bestOffhandSlot.getIndex() != offhandSlot.getIndex()) {
            mc.interactionManager.clickSlot(screenHandler.syncId, bestOffhandSlot.id, 40, SlotActionType.SWAP, mc.player);
            stopwatch.reset();
        }
    }

    private Slot getOffhandRefillSource(final ScreenHandler screenHandler, final Slot offhandSlot, final ItemStack offhandStack) {
        return InventoryUtility.filterSlots(screenHandler, slot -> {
            if (slot.id == offhandSlot.id) return false;
            final ItemStack stack = slot.getStack();
            return !stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, offhandStack);
        }, false).stream().max(Comparator.comparingInt(slot -> slot.getStack().getCount())).orElse(null);
    }

    private void mergeStackIntoOffhand(final ScreenHandler screenHandler, final Slot offhandSlot, final Slot sourceSlot) {
        mc.interactionManager.clickSlot(screenHandler.syncId, sourceSlot.id, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(screenHandler.syncId, offhandSlot.id, 0, SlotActionType.PICKUP, mc.player);

        if (!screenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(screenHandler.syncId, sourceSlot.id, 0, SlotActionType.PICKUP, mc.player);
        }

        stopwatch.reset();
    }

    private Slot getBestOffhand(final ScreenHandler screenHandler) {
        final InventoryManagerSettings.OffhandMode mode = settings.getOffhandMode().getValue();
        if (mode == InventoryManagerSettings.OffhandMode.NONE) {
            return null;
        }

        return InventoryUtility.filterSlots(screenHandler, slot -> {
            final ItemStack stack = slot.getStack();
            if (stack.isEmpty()) return false;

            return switch (mode) {
                case GOLDEN_APPLE -> stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
                case PROJECTILES -> stack.getItem() == Items.EGG || stack.getItem() == Items.SNOWBALL;
                default -> false;
            };
        }, false).stream().max(
                Comparator.comparingInt((Slot slot) -> slot.getStack().getCount())
                        .thenComparingInt(slot -> offhandItemPriority(slot.getStack(), mode))
                        .thenComparingInt(slot -> slot.id == 45 ? 1 : 0)
        ).orElse(null);
    }

    private int offhandItemPriority(final ItemStack stack, final InventoryManagerSettings.OffhandMode mode) {
        return switch (mode) {
            case GOLDEN_APPLE -> stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE ? 2 : 1;
            case PROJECTILES -> stack.getItem() == Items.SNOWBALL ? 2 : 1;
            default -> 0;
        };
    }

    public boolean canMove(final long delay) {
        if (delay == 0) return true;

        return stopwatch.hasTimeElapsed(delay);
    }

    private boolean shouldDropExcess(final PlayerScreenHandler screenHandler, final Slot slot) {
        final ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return false;

        if (stack.getItem() instanceof ArrowItem) {
            Slot keep = InventoryUtility.filterSlots(screenHandler, s -> s.getStack().getItem() instanceof ArrowItem, false)
                    .stream().max(Comparator.comparingInt(s -> s.getStack().getCount())).orElse(null);
            return keep != null && slot.id != keep.id;
        }

        if (stack.getItem() instanceof BowItem) {
            var bows = InventoryUtility.filterSlots(screenHandler, s -> s.getStack().getItem() instanceof BowItem, false);
            if (bows.isEmpty()) return false;
            Slot bestPower = bows.stream().max(Comparator.comparingInt(s -> InventoryUtility.calculateEnchantmentLevel(s.getStack(), Enchantments.POWER))).orElse(null);
            Slot bestPunch = bows.stream().filter(s -> InventoryUtility.calculateEnchantmentLevel(s.getStack(), Enchantments.PUNCH) > 0)
                    .max(Comparator.comparingInt(s -> InventoryUtility.calculateEnchantmentLevel(s.getStack(), Enchantments.POWER))).orElse(null);
            boolean keepPunch = bestPunch != null;
            if (keepPunch) {
                if (slot.id == bestPower.id || slot.id == bestPunch.id) return false;
                return true;
            } else {
                if (slot.id == bestPower.id) return false;
                return true;
            }
        }

        if (stack.getItem() instanceof CrossbowItem) {
            var xs = InventoryUtility.filterSlots(screenHandler, s -> s.getStack().getItem() instanceof CrossbowItem, false);
            if (xs.size() <= 1) return false;
            Slot best = xs.stream().max(Comparator.comparingInt(s ->
                    InventoryUtility.calculateEnchantmentLevel(s.getStack(), Enchantments.QUICK_CHARGE) * 3
                            + InventoryUtility.calculateEnchantmentLevel(s.getStack(), Enchantments.PIERCING) * 2
                            + (InventoryUtility.calculateEnchantmentLevel(s.getStack(), Enchantments.MULTISHOT) > 0 ? 1 : 0)
            )).orElse(null);
            return best != null && slot.id != best.id;
        }

        if (stack.isIn(ItemTags.SWORDS)) {
            boolean hasSharpAxe = InventoryUtility.filterSlots(screenHandler, s -> s.getStack().getItem() instanceof AxeItem, false)
                    .stream()
                    .anyMatch(s -> InventoryUtility.calculateEnchantmentLevel(s.getStack(), Enchantments.SHARPNESS) >= 8);
            if (hasSharpAxe) {
                Slot keepSword = InventoryUtility.filterSlots(screenHandler, s -> s.getStack().isIn(ItemTags.SWORDS), false)
                        .stream()
                        .max(Comparator.comparingDouble(s -> InventoryUtility.getSwordValue(s.getStack())))
                        .orElse(null);
                return keepSword != null && slot.id != keepSword.id;
            }
        }

        return false;
    }

    private boolean shouldKeepSwordWhenSharpAxe(final PlayerScreenHandler screenHandler, final Slot slot) {
        final ItemStack stack = slot.getStack();
        if (!stack.isIn(ItemTags.SWORDS)) return false;
        boolean hasSharpAxe = InventoryUtility.filterSlots(screenHandler, s -> s.getStack().getItem() instanceof AxeItem, false)
                .stream()
                .anyMatch(s -> InventoryUtility.calculateEnchantmentLevel(s.getStack(), Enchantments.SHARPNESS) >= 8);
        if (!hasSharpAxe) return false;
        Slot keepSword = InventoryUtility.filterSlots(screenHandler, s -> s.getStack().isIn(ItemTags.SWORDS), false)
                .stream()
                .max(Comparator.comparingDouble(s -> InventoryUtility.getSwordValue(s.getStack())))
                .orElse(null);
        return keepSword != null && slot.id == keepSword.id;
    }

}
