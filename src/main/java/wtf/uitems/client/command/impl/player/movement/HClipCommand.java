package wtf.uitems.client.command.impl.player.movement;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.MathHelper;
import wtf.uitems.client.command.Command;
import wtf.uitems.client.feature.helper.impl.player.rotation.RotationHelper;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.utility.player.MoveUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static wtf.uitems.client.Constants.mc;

public final class HClipCommand extends Command {

    public HClipCommand() {
        super("hclip", "Teleports you in the direction you are looking.", "h");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("distance", DoubleArgumentType.doubleArg()).executes(context -> {
            final double yaw = MoveUtility.getDirectionRadians(RotationHelper.getClientHandler().getYawOr(mc.player.getYaw()));

            final double distance = DoubleArgumentType.getDouble(context, "distance");
            mc.player.setPosition(mc.player.getX() - MathHelper.sin((float) yaw) * distance, mc.player.getY(), mc.player.getZ() + MathHelper.cos((float) yaw) * distance);
            ChatUtility.print("Clipped §l" + distance + "§7 block" + (distance == 1 ? "" : "s") + "!");
            return SINGLE_SUCCESS;
        }));
    }
}
