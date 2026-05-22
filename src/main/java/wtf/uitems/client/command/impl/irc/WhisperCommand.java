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

public final class WhisperCommand extends Command {

    public WhisperCommand() {
        super("whisper", "Allows you to direct message Opal users.", "w", "msg");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("user", StringArgumentType.word()).executes(context -> {
            if (VerifyManager.getInstance().isAuthenticated()) {
                final String user = context.getArgument("user", String.class);

                ChatHelper.getInstance().setWhisperUsername(user);
                ChatHelper.getInstance().setChannel(ChatChannel.WHISPER);
            } else {
                ChatUtility.error("You are not connected to the IRC server!");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("user", StringArgumentType.word()).then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            if (VerifyManager.getInstance().isAuthenticated()) {
                final String user = context.getArgument("user", String.class);
                final String message = context.getArgument("message", String.class);

                VerifyManager.getInstance().getTransport().sendChat(".w " + user + " " + message);
            } else {
                ChatUtility.error("You are not connected to the IRC server!");
            }
            return SINGLE_SUCCESS;
        })));
    }
}
