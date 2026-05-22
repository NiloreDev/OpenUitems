package wtf.uitems.client.command.impl.irc;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.uitems.client.command.Command;
import wtf.uitems.client.verify.VerifyManager;
import wtf.uitems.utility.misc.chat.ChatUtility;

import java.util.Map;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class OnlineCommand extends Command {

    public OnlineCommand() {
        super("online", "Displays the online Opal users to you.");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (VerifyManager.getInstance().isAuthenticated()) {
                final Map<String, String> userToIgnMap = VerifyManager.getInstance().getTransport().getUserToIgnMap();
                if (userToIgnMap.isEmpty()) {
                    ChatUtility.print("There are no other users online.");
                } else {
                    ChatUtility.print("Online Users (" + userToIgnMap.size() + "):");
                    userToIgnMap.forEach((user, ign) -> {
                        ChatUtility.print("- " + user + " (IGN: " + ign + ")");
                    });
                }
            } else {
                ChatUtility.error("You are not connected to the IRC server!");
            }
            return SINGLE_SUCCESS;
        });
    }
}
