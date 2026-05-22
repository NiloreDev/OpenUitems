package wtf.uitems.utility.misc.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import wtf.uitems.client.ReleaseInfo;
import wtf.uitems.utility.socket.user.ResolvedUser;
import wtf.uitems.utility.socket.user.UserRole;

import java.util.EnumSet;
import java.util.Set;

import static wtf.uitems.client.Constants.mc;

public final class ChatUtility {

    private ChatUtility() {
    }

    public static void debug(final Object o) {
        if (ReleaseInfo.CHANNEL != ReleaseInfo.ReleaseChannel.DEVELOPMENT) {
            return;
        }
        final Text text = Text.literal("[").formatted(Formatting.GRAY)
                .append(Text.literal("DEBUG").formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal("] ").formatted(Formatting.GRAY))
                .append(o.toString());
        display(text);
    }

    public static void print(final Object o) {
        final Text text = Text.literal("[").formatted(Formatting.GRAY)
                .append(Text.literal(ReleaseInfo.NAME.toUpperCase()).formatted(Formatting.AQUA))
                .append(Text.literal("] ").formatted(Formatting.GRAY))
                .append(o.toString());
        display(text);
    }

    public static void error(final Object o) {
        final Text text = Text.literal("[").formatted(Formatting.GRAY)
                .append(Text.literal("ERROR").formatted(Formatting.RED))
                .append(Text.literal("] ").formatted(Formatting.GRAY))
                .append(o.toString());
        display(text);
    }

    public static void success(final Object o) {
        final Text text = Text.literal("[").formatted(Formatting.GRAY)
                .append(Text.literal("SUCCESS").formatted(Formatting.GREEN))
                .append(Text.literal("] ").formatted(Formatting.GRAY))
                .append(o.toString());
        display(text);
    }

    // 0 = broadcast, 1 = whisper recv, 2 = whisper sent
    public static void irc(final int mode, final ResolvedUser user, final String message) {
        final MutableText messageText = user.getRole() == UserRole.DEVELOPER
                ? translateAlternateColorCodes(message)
                : Text.literal(message);

        final Text text = Text.literal("[").formatted(Formatting.GRAY)
                .append(Text.literal("#").formatted(Formatting.GOLD))
                .append(Text.literal("] ").formatted(Formatting.GRAY))
                .append(mode > 0 ? Text.literal(mode == 1 ? "From " : "To ").formatted(Formatting.GRAY) : Text.empty())
                .append(Text.literal(user.getName()).withColor(user.getRole().getColor().getRgb()))
                .append(Text.literal(": ").formatted(Formatting.GRAY))
                .append(messageText.formatted(Formatting.WHITE));

        display(text);
    }

    public static void display(final Text text) {
        if (mc.player == null) {
            return;
        }
        mc.inGameHud.getChatHud().addMessage(text);
    }

    public static void send(final String content) {
        if (mc.player == null) {
            return;
        }
        mc.player.networkHandler.sendChatMessage(content);
    }

    public static void sendCommand(final String command) {
        if (mc.player == null) {
            return;
        }
        mc.player.networkHandler.sendChatCommand(command);
    }


    public static MutableText translateAlternateColorCodes(final String str) {
        final MutableText mutableText = Text.empty();
        final char[] chars = str.toCharArray();

        Set<Formatting> activeFormats = EnumSet.noneOf(Formatting.class);

        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];

            if (c == '&' && i + 1 < chars.length) {
                final char nextChar = chars[i + 1];
                final Formatting formatting = Formatting.byCode(nextChar);

                if (formatting != null) {
                    i++;

                    if (formatting == Formatting.RESET) {
                        activeFormats.clear();
                    } else {
                        activeFormats.add(formatting);
                    }

                    continue;
                }
            }

            Style style = Style.EMPTY;
            for (Formatting format : activeFormats) {
                style = style.withFormatting(format);
            }

            mutableText.append(Text.literal(String.valueOf(c)).setStyle(style));
        }

        return mutableText;
    }


}
