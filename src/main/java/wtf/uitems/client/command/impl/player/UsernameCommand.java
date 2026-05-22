package wtf.uitems.client.command.impl.player;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.uitems.client.command.Command;
import wtf.uitems.utility.misc.chat.ChatUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static wtf.uitems.client.Constants.mc;

public final class UsernameCommand extends Command {

    public UsernameCommand() {
        super("username", "Copies your username to your clipboard.", "ign");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            mc.keyboard.setClipboard(mc.player.getName().getString());

            ChatUtility.print("Your username has been copied!");
            return SINGLE_SUCCESS;
        });
    }
}
