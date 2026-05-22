package wtf.uitems.utility.misc.chat;

import com.ibm.icu.impl.Pair;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.visual.StreamerModeModule;
import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.socket.user.User;

import java.util.ArrayList;
import java.util.List;

import static wtf.uitems.client.Constants.mc;

public final class ChatUsernameDecorator {

    private static Style grayStyle;

    private ChatUsernameDecorator() {
    }

    public static OrderedText appendSocketUsernames(final OrderedText content) {
        return appendSocketUsernames(content, 1F);
    }

    public static OrderedText appendSocketUsernames(final OrderedText content, final float alphaFactor) {
        if (grayStyle == null) {
            grayStyle = Style.EMPTY.withColor(Formatting.GRAY);
        }

        final List<OrderedText> styledSegments = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();

        content.accept((index, style, codePoint) -> {
            styledSegments.add(OrderedText.styledForwardsVisitedString(String.valueOf((char) codePoint), style));
            builder.append((char) codePoint);
            return true;
        });

        final String text = builder.toString();
        int insertOffset = 0;

        final StreamerModeModule streamerModeModule = OpalClient.getInstance().getModuleRepository().getModule(StreamerModeModule.class);
        final java.util.Collection<net.minecraft.client.network.PlayerListEntry> tabPlayers = mc.getNetworkHandler() != null
                ? mc.getNetworkHandler().getPlayerList()
                : java.util.Collections.emptyList();

        for (final net.minecraft.client.network.PlayerListEntry tabEntry : tabPlayers) {
            final String playerName = tabEntry.getProfile().name();
            final String displayNameStr = wtf.uitems.client.feature.module.impl.combat.AntiBotModule.getDisplayNameForUuid(tabEntry.getProfile().id());

            int startingSearchIndex = 0;
            while (true) {
                int nameIndex = -1;
                String nameForLookup = null;

                if (displayNameStr != null) {
                    nameIndex = text.indexOf(displayNameStr, startingSearchIndex);
                    nameForLookup = displayNameStr;
                }

                if (nameIndex == -1) {
                    nameIndex = text.indexOf(playerName, startingSearchIndex);
                    nameForLookup = playerName;
                }

                if (nameIndex == -1 && streamerModeModule.isEnabled()) {
                    if (mc.player != null && tabEntry.getProfile().id().equals(mc.player.getUuid())) {
                        final String customUsername = streamerModeModule.getCustomUsername();
                        if (!customUsername.isEmpty()) {
                            nameIndex = text.indexOf(customUsername, startingSearchIndex);
                            nameForLookup = customUsername;
                        }
                    } else if (streamerModeModule.isHidingOtherUsernames()) {
                        final String placeholder = streamerModeModule.getOtherUsernamePlaceholder();
                        if (!placeholder.isEmpty()) {
                            nameIndex = text.indexOf(placeholder, startingSearchIndex);
                            nameForLookup = placeholder;
                        }
                    }
                }

                if (nameIndex == -1) {
                    break;
                }

                if (nameIndex > 0) {
                    final char beforeChar = text.charAt(nameIndex - 1);
                    if (!(beforeChar == ' ' || beforeChar == '<' || beforeChar == '[' || beforeChar == '（' || beforeChar == ']' || beforeChar == '>')) {
                        startingSearchIndex = nameIndex + nameForLookup.length();
                        continue;
                    }
                }

                final String displayName = getSocketDisplayName(tabEntry.getProfile().id(), playerName);

                if (displayName == null) {
                    break;
                }

                final int nameEndIndex = nameIndex + nameForLookup.length();
                final Pair<Integer, Integer> theme = ColorUtility.getClientTheme();
                final int animatedTheme = ColorUtility.interpolateColorsBackAndForth(16, Math.abs(displayName.hashCode() % 180), theme.first, theme.second);
                final float alpha = Math.max(0F, Math.min(1F, alphaFactor));
                final int themedFaded = ColorUtility.interpolateColors(0xFF7A7A7A, animatedTheme, alpha);
                final Style themeStyle = Style.EMPTY.withColor(themedFaded);

                int lookaheadStart = nameEndIndex;
                while (lookaheadStart < text.length() && text.charAt(lookaheadStart) == ' ') {
                    lookaheadStart++;
                }
                if (lookaheadStart < text.length() && (text.charAt(lookaheadStart) == '(' || text.charAt(lookaheadStart) == '（')) {
                    final String needle = "(" + displayName + ")";
                    final int existingIndex = text.indexOf(needle, lookaheadStart);
                    if (existingIndex != -1 && existingIndex - lookaheadStart <= 2) {
                        startingSearchIndex = nameEndIndex;
                        continue;
                    }
                }

                styledSegments.add(
                        nameEndIndex + insertOffset,
                        OrderedText.concat(
                                OrderedText.styledForwardsVisitedString(" (", grayStyle),
                                OrderedText.styledForwardsVisitedString(displayName, themeStyle),
                                OrderedText.styledForwardsVisitedString(")", grayStyle)
                        )
                );

                startingSearchIndex = nameEndIndex;
                insertOffset++;
            }
        }

        return OrderedText.concat(styledSegments);
    }

    public static String getSocketDisplayName(final java.util.UUID uuid, final String playerName) {
        String displayName = null;
        final wtf.uitems.client.verify.VerifyManager verifyManager = wtf.uitems.client.verify.VerifyManager.getInstance();
        if (verifyManager != null && verifyManager.getTransport() != null) {
            displayName = verifyManager.getTransport().getName(playerName);
        }

        if (displayName == null) {
            final User user = ClientSocket.getInstance().getUserOrNull(uuid);
            if (user != null) {
                displayName = user.getName();
            }
        }

        if (mc.player != null && uuid.equals(mc.player.getUuid()) && verifyManager != null) {
            final String verifyName = verifyManager.getUsername();
            if (verifyName != null && !verifyName.isEmpty()) {
                displayName = verifyName;
            }
        }

        return displayName;
    }
}
