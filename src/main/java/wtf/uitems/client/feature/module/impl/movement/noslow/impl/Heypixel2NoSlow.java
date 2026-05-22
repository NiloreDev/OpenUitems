package wtf.uitems.client.feature.module.impl.movement.noslow.impl;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import wtf.uitems.client.feature.module.impl.movement.noslow.NoSlowModule;
import wtf.uitems.client.feature.module.property.impl.mode.ModuleMode;
import wtf.uitems.event.impl.game.player.movement.SlowdownEvent;
import wtf.uitems.event.subscriber.Subscribe;

import static wtf.uitems.client.Constants.mc;

public final class Heypixel2NoSlow extends ModuleMode<NoSlowModule> {

    private boolean wasUsingItem;

    public Heypixel2NoSlow(final NoSlowModule module) {
        super(module);
    }

    @Subscribe
    public void onSlowdown(final SlowdownEvent event) {
        if (mc.player == null) return;

        boolean isUsing = mc.player.isUsingItem();

        // When we start using an item, stop sprinting for one tick to bypass Grim/anti-cheat checks
        if (isUsing && !wasUsingItem) {
            mc.player.setSprinting(false);
        }
        wasUsingItem = isUsing;

        if (!isUsing) return;

        // 仅对食物和药水生效
        if (!isConsumable(mc.player.getActiveItem())) return;

        if (mc.player.getItemUseTimeLeft() % 3 == 0) {
            event.setCancelled();

            // Heypixel 绕过：左右后走时禁止触发疾跑
            // 只有当前进且左右按键未冲突时才允许疾跑
            boolean forward = mc.options.forwardKey.isPressed();
            boolean backward = mc.options.backKey.isPressed();
            boolean left = mc.options.leftKey.isPressed();
            boolean right = mc.options.rightKey.isPressed();

            if (forward && !backward && !left && !right) {
                if (!mc.player.isSprinting()) {
                    mc.player.setSprinting(true);
                }
            } else {
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                }
            }
        }
    }

    private boolean isConsumable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.contains(DataComponentTypes.FOOD) || stack.getItem() instanceof PotionItem;
    }

    @Override
    public Enum<?> getEnumValue() {
        return NoSlowModule.Mode.HEYPIXEL2;
    }
}
