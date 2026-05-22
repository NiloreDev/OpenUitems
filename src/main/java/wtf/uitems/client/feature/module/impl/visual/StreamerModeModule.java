package wtf.uitems.client.feature.module.impl.visual;

import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.client.feature.module.property.impl.StringProperty;
import wtf.uitems.client.feature.module.property.impl.bool.BooleanProperty;
import wtf.uitems.event.impl.game.chat.ChatReceivedEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.utility.misc.chat.ChatUtility;

import static wtf.uitems.client.Constants.mc;

public final class StreamerModeModule extends Module {

    private final BooleanProperty hideServerId = new BooleanProperty("Hide server ID", true);
    private final BooleanProperty hideUsername = new BooleanProperty("Hide username", true);
    private final StringProperty customUsername = new StringProperty("Custom username", "You").hideIf(() -> !hideUsername.getValue());
    private final BooleanProperty hideOtherUsernames = new BooleanProperty("Hide others' usernames", false);
    private final StringProperty otherUsernamePlaceholder = new StringProperty("Others placeholder", "Player").hideIf(() -> !hideOtherUsernames.getValue());

    public StreamerModeModule() {
        super("Streamer Mode", "Features for content creators.", ModuleCategory.VISUAL);
        this.addProperties(hideServerId, hideUsername, customUsername, hideOtherUsernames, otherUsernamePlaceholder);
    }

    @Subscribe
    public void onChatReceived(final ChatReceivedEvent event) {
        if (hideServerId.getValue()) {
            final String message = event.getText().getString();

            if (message.startsWith("Sending you to ")) {
                event.setCancelled();

                final String serverId = message.replace("Sending you to ", "").replace("!", "");
                ChatUtility.display(Text.literal("§aSending you to §k" + serverId + "§r§a!"));
            }
        }
    }

    public String filter(String text) {
        if (hideUsername.getValue()) {
            final String customUsername = this.getCustomUsername();
            if (!customUsername.isEmpty()) {
                // Initial session name
                if (mc.getSession() != null && mc.getSession().getUsername() != null) {
                    text = StringUtils.replaceIgnoreCase(text, mc.getSession().getUsername(), customUsername);
                }
                
                // Real-time username from mc.player
                if (mc.player != null) {
                    final String profileName = mc.player.getGameProfile().name();
                    if (profileName != null) {
                        text = StringUtils.replaceIgnoreCase(text, profileName, customUsername);
                    }
                    final String displayName = mc.player.getName() != null ? mc.player.getName().getString() : null;
                    if (displayName != null && !displayName.isEmpty()) {
                        text = StringUtils.replaceIgnoreCase(text, displayName, customUsername);
                    }
                }

                // Real-time username from PlayerListEntry (most reliable for proxy servers)
                if (mc.getNetworkHandler() != null && mc.player != null) {
                    final var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                    if (entry != null && entry.getProfile() != null && entry.getProfile().name() != null) {
                        final String entryName = entry.getProfile().name();
                        text = StringUtils.replaceIgnoreCase(text, entryName, customUsername);
                    }
                    if (entry != null && entry.getDisplayName() != null) {
                        final String entryDisplayName = entry.getDisplayName().getString();
                        if (!entryDisplayName.isEmpty()) {
                            text = StringUtils.replaceIgnoreCase(text, entryDisplayName, customUsername);
                        }
                    }
                }
            }
        }

        if (hideOtherUsernames.getValue() && mc.getNetworkHandler() != null) {
            final String placeholder = otherUsernamePlaceholder.getValue().trim();
            if (!placeholder.isEmpty()) {
                String selfName = null;
                if (mc.player != null) {
                    selfName = mc.player.getGameProfile().name();
                } else if (mc.getSession() != null) {
                    selfName = mc.getSession().getUsername();
                }
                for (final var entry : mc.getNetworkHandler().getPlayerList()) {
                    if (entry.getProfile() == null || entry.getProfile().name() == null) continue;
                    final String name = entry.getProfile().name();
                    if (selfName != null && name.equalsIgnoreCase(selfName)) continue;
                    text = StringUtils.replaceIgnoreCase(text, name, placeholder);
                }
            }
        }

        return text;
    }

    public boolean isHidingServerId() {
        return hideServerId.getValue();
    }

    public boolean isHidingOtherUsernames() {
        return hideOtherUsernames.getValue();
    }

    public String getOtherUsernamePlaceholder() {
        return otherUsernamePlaceholder.getValue().trim();
    }

    public String getCustomUsername() {
        return customUsername.getValue().trim();
    }

}
