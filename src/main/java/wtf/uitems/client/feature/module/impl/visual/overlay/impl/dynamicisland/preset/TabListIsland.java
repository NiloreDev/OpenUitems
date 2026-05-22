package wtf.uitems.client.feature.module.impl.visual.overlay.impl.dynamicisland.preset;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.dynamicisland.IslandTrigger;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static wtf.uitems.client.Constants.mc;

public class TabListIsland implements IslandTrigger {
    private static final NVGTextRenderer FONT = FontRepository.getFont("Mifont");
    private static final NVGTextRenderer ICON_FONT = FontRepository.getFont("materialicons-regular");
    
    private float width = 150;
    private float height = 28;

    @Override
    public boolean isIslandVisible() {
        return mc.getNetworkHandler() != null && mc.options.playerListKey.isPressed();
    }

    @Override
    public void renderIsland(DrawContext context, float posX, float posY, float width, float height, float progress) {
        if (mc.getNetworkHandler() == null) return;

        Collection<PlayerListEntry> players = mc.getNetworkHandler().getPlayerList();
        List<PlayerListEntry> sortedPlayers = players.stream()
                .sorted(Comparator.comparing(p -> p.getProfile().name()))
                .collect(Collectors.toList());

        float x = posX + 10;
        float y = posY + 8;
        
        // Render Title
        ICON_FONT.drawString("\ue7ef", x, y + 10, 10, -1);
        FONT.drawString("Players Online: " + players.size(), x + 15, y + 8, 8, -1);

        y += 18;

        // Render Players
        float itemHeight = 12;
        for (int i = 0; i < sortedPlayers.size(); i++) {
            PlayerListEntry player = sortedPlayers.get(i);
            String name = player.getProfile().name();
            int ping = player.getLatency();
            
            int pingColor = ping < 100 ? 0xFF55FF55 : (ping < 200 ? 0xFFFFFF55 : 0xFFFF5555);
            
            String displayName = null;
            final wtf.uitems.client.verify.VerifyManager verifyManager = wtf.uitems.client.verify.VerifyManager.getInstance();
            if (verifyManager != null && verifyManager.getTransport() != null) {
                displayName = verifyManager.getTransport().getName(name);
            }
            if (displayName == null) {
                final wtf.uitems.client.socket.ClientSocket socket = wtf.uitems.client.socket.ClientSocket.getInstance();
                final wtf.uitems.utility.socket.user.User user = socket.getUserOrNull(player.getProfile().id());
                if (user != null) {
                    displayName = user.getName();
                }
            }

            String renderName = name;
            if (displayName != null) {
                renderName = name + " (" + displayName + ")";
            }

            FONT.drawString(renderName, x, y + 8, 7, -1);
            
            String pingStr = ping + "ms";
            float pingWidth = FONT.getStringWidth(pingStr, 6);
            FONT.drawString(pingStr, posX + width - 10 - pingWidth, y + 8, 6, pingColor);
            
            y += itemHeight;
        }
    }

    @Override
    public float getIslandWidth() {
        return 160;
    }

    @Override
    public float getIslandHeight() {
        if (mc.getNetworkHandler() == null) return 28;
        int count = mc.getNetworkHandler().getPlayerList().size();
        return 35 + (count * 12) + 5;
    }

    @Override
    public int getIslandPriority() {
        return 10;
    }
}
