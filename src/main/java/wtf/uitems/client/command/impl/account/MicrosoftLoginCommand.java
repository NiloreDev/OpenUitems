package wtf.uitems.client.command.impl.account;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.uitems.client.command.Command;
import wtf.uitems.utility.misc.Multithreading;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.utility.security.SessionUtility;

import java.util.function.Supplier;

import static wtf.uitems.client.Constants.mc;

public final class MicrosoftLoginCommand extends Command {

    public MicrosoftLoginCommand() {
        super("mslogin", "Logs you into a Microsoft account using a token.", "microsoftlogin", "tokenlogin");
    }

    @Override
    protected void onCommand(final LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ChatUtility.error("Usage: .mslogin <token> | .mslogin access <token> | .mslogin refresh <clientId> <refreshToken>");
            return 1;
        });

        builder.then(literal("access")
                .then(argument("token", StringArgumentType.greedyString())
                        .executes(context -> {
                            handleLogin("token", () -> SessionUtility.loginWithToken(StringArgumentType.getString(context, "token")));
                            return 1;
                        })));

        builder.then(literal("refresh")
                .then(argument("clientId", StringArgumentType.word())
                        .then(argument("token", StringArgumentType.greedyString())
                                .executes(context -> {
                                    final String clientId = StringArgumentType.getString(context, "clientId");
                                    final String refreshToken = StringArgumentType.getString(context, "token");
                                    handleLogin("refresh token", () -> SessionUtility.loginWithMicrosoftRefreshToken(clientId, refreshToken));
                                    return 1;
                                }))));

        builder.then(argument("token", StringArgumentType.greedyString())
                .executes(context -> {
                    handleLogin("token", () -> SessionUtility.loginWithToken(StringArgumentType.getString(context, "token")));
                    return 1;
                }));
    }

    private void handleLogin(final String mode, final Supplier<SessionUtility.MicrosoftLoginResult> task) {
        ChatUtility.print("Logging in with Microsoft " + mode + "...");

        Multithreading.runAsync(() -> {
            final SessionUtility.MicrosoftLoginResult result = task.get();
            mc.execute(() -> {
                if (result.success()) {
                    SessionUtility.applyMinecraftSession(result);
                    ChatUtility.success("Logged in as " + result.profileName());
                } else {
                    ChatUtility.error(result.message());
                }
            });
        });
    }

}
