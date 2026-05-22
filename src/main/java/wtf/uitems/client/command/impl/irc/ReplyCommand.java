package wtf.uitems.client.command.impl.irc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.uitems.client.command.Command;
import wtf.uitems.client.feature.helper.impl.chat.ChatHelper;
import wtf.uitems.client.verify.VerifyManager;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.utility.socket.ChatChannel;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class ReplyCommand extends Command {

    public ReplyCommand() {
        super("reply", "Reply to a user's whisper.", "r");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            if (VerifyManager.getInstance().isAuthenticated()) {
                final String username = VerifyManager.getInstance().getLastReceivedWhisperUsername();
                if (username == null) {
                    ChatUtility.error("There is nobody to reply to!");
                } else {
                    final String message = context.getArgument("message", String.class);
                    VerifyManager.getInstance().getTransport().sendChat(".w " + username + " " + message);
                }
            } else {
                ChatUtility.error("You are not connected to the IRC server!");
            }
            return SINGLE_SUCCESS;
        }));

        builder.executes(context -> {
            if (VerifyManager.getInstance().isAuthenticated()) {
                final String username = VerifyManager.getInstance().getLastReceivedWhisperUsername();
                if (username == null) {
                    ChatUtility.error("There is nobody to reply to!");
                } else {
                    ChatHelper.getInstance().setWhisperUsername(username);
                    ChatHelper.getInstance().setChannel(ChatChannel.WHISPER);
                }
            } else {
                ChatUtility.error("You are not connected to the IRC server!");
            }
            return SINGLE_SUCCESS;
        });
    }
}
