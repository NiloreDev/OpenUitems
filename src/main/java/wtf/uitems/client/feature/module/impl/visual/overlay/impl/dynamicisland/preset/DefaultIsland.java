package wtf.uitems.client.feature.module.impl.visual.overlay.impl.dynamicisland.preset;

import com.ibm.icu.impl.Pair;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.ReleaseInfo;
import wtf.uitems.client.feature.helper.impl.LocalDataWatch;
import wtf.uitems.client.feature.helper.impl.server.KnownServer;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.impl.dynamicisland.IslandTrigger;
import wtf.uitems.client.renderer.NVGRenderer;
import wtf.uitems.client.renderer.repository.FontRepository;
import wtf.uitems.client.renderer.text.NVGTextRenderer;
import wtf.uitems.utility.render.ClientTheme;
import wtf.uitems.utility.render.ColorUtility;

import static wtf.uitems.client.Constants.mc;

@RequiredArgsConstructor
public class DefaultIsland implements IslandTrigger {
    private final HUDModule module;
    private float width;

    private float calculateWidth() {
        final NVGTextRenderer titleFont = FontRepository.getFont("productsans-bold");
        final NVGTextRenderer footerFont = FontRepository.getFont("productsans-medium");

        final String opalText = ReleaseInfo.NAME;
        final String releaseType = ReleaseInfo.CHANNEL.toString();
        final String releaseVersion = "ver" + ReleaseInfo.VERSION;

        String serverAddress = "singleplayer";
        String serverPing = "0 ms";

        if (mc.getNetworkHandler() != null) {
            final ServerInfo serverInfo = mc.getNetworkHandler().getServerInfo();
            if (serverInfo != null) {
                final KnownServer currentKnownServer = LocalDataWatch.get().getKnownServerManager().getCurrentServer();

                serverAddress = currentKnownServer != null && currentKnownServer.getProxyServer() != null
                        ? currentKnownServer.getProxyServer().getName().toLowerCase()
                        : module.isDynamicIslandHideServerIP()? "multiplayer"
                        : serverInfo.address.toLowerCase();

                serverAddress = serverAddress.length() > 20
                        ? serverAddress.substring(0, 20 - 3) + "..."
                        : serverAddress;

                long latency = 0;

                final PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.getSession().getUuidOrNull());
                if (playerListEntry != null) {
                    latency = playerListEntry.getLatency();
                }

                if (latency < 2) {
                    latency = serverInfo.ping;
                }

                serverPing = latency + " ms";
            }
        }

        final float titleTextSize = 11.5f;
        final float secondaryTextSize = 7;
        final float footerTextSize = 6;

        final float releaseInfoWidth = Math.max(
                titleFont.getStringWidth(releaseType, secondaryTextSize),
                footerFont.getStringWidth(releaseVersion, footerTextSize)
        );

        return 14 + titleFont.getStringWidth(opalText, titleTextSize) + releaseInfoWidth + titleFont.getStringWidth(serverAddress, secondaryTextSize) + 35;
    }

    @Override
    public void renderIsland(DrawContext context, float posX, float posY, float width, float height, float progress) {
        final NVGTextRenderer titleFont = FontRepository.getFont("productsans-bold");
        final NVGTextRenderer footerFont = FontRepository.getFont("productsans-medium");

        final String opalText = ReleaseInfo.NAME;
        final String releaseType = ReleaseInfo.CHANNEL.toString();
        final String releaseVersion = "ver" + ReleaseInfo.VERSION;

        String serverAddress = "singleplayer";
        String serverPing = "0 ms";

        if (mc.getNetworkHandler() != null) {
            final ServerInfo serverInfo = mc.getNetworkHandler().getServerInfo();
            if (serverInfo != null) {
                final KnownServer currentKnownServer = LocalDataWatch.get().getKnownServerManager().getCurrentServer();

                serverAddress = currentKnownServer != null && currentKnownServer.getProxyServer() != null
                        ? currentKnownServer.getProxyServer().getName().toLowerCase()
                        : module.isDynamicIslandHideServerIP()? "multiplayer"
                        : serverInfo.address.toLowerCase();

                serverAddress = serverAddress.length() > 20
                        ? serverAddress.substring(0, 20 - 3) + "..."
                        : serverAddress;

                long latency = 0;

                final PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.getSession().getUuidOrNull());
                if (playerListEntry != null) {
                    latency = playerListEntry.getLatency();
                }

                if (latency < 2) {
                    latency = serverInfo.ping;
                }

                serverPing = latency + " ms";
            }
        }

        final float titleTextSize = 11.5f;
        final float secondaryTextSize = 7;
        final float footerTextSize = 6;

        final float releaseInfoWidth = Math.max(
                titleFont.getStringWidth(releaseType, secondaryTextSize),
                footerFont.getStringWidth(releaseVersion, footerTextSize)
        );

        final ClientTheme theme = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class).getThemeMode().getValue();
        final Pair<Integer, Integer> colors = theme.getColors();

        // Icon position and size
        float iconX = posX + 6;
        float iconY = posY + 6;
        float iconSize = 16;

        // Draw Uitems Logo
        drawUitemsLogo(iconX, iconY, iconSize, colors.second, colors.first);

        final float textStart = posX + 24.5f;
        titleFont.drawGradientString(opalText, textStart, posY + 12 + 0.5F + 6, titleTextSize, colors.second, colors.first);

        float textXOffset = 3.3f;
        final float releaseTypeStart = textStart + titleFont.getStringWidth(opalText, titleTextSize) + textXOffset;
        NVGRenderer.rect(releaseTypeStart, posY + 8.5f, 0.75F, 12, ColorUtility.MUTED_COLOR);

        titleFont.drawString(releaseType, releaseTypeStart + 4, posY + 17.5f / 1.3f, secondaryTextSize, -1);
        footerFont.drawString(releaseVersion, releaseTypeStart + 4, posY + 19.5f, footerTextSize, ColorUtility.MUTED_COLOR);

        final float serverIPStart = releaseTypeStart + 4 + releaseInfoWidth + textXOffset;
        NVGRenderer.rect(serverIPStart, posY + 8.5f, 0.75F, 12, ColorUtility.MUTED_COLOR);

        titleFont.drawString(serverAddress, serverIPStart + 4, posY + 17.5f / 1.3f, secondaryTextSize, -1);
        footerFont.drawString(serverPing, serverIPStart + 4, posY + 19.5f, footerTextSize, ColorUtility.MUTED_COLOR);
    }

    private void drawUitemsLogo(float x, float y, float size, int color1, int color2) {
        float padding = size * 0.15f;
        float innerSize = size - padding * 2;
        float strokeWidth = size * 0.12f;

        // Draw outer U shape
        NVGRenderer.roundedRectVaryingGradient(
                x + padding, y + padding,
                innerSize, innerSize,
                0, 0, innerSize / 2f, innerSize / 2f,
                color1, color2, 0
        );

        // Mask the middle part of the U
        NVGRenderer.roundedRectVarying(
                x + padding + strokeWidth, y + padding,
                innerSize - strokeWidth * 2, innerSize - strokeWidth,
                0, 0, (innerSize - strokeWidth * 2) / 2.2f, (innerSize - strokeWidth * 2) / 2.2f,
                ColorUtility.applyOpacity(0xFF090909, 150) // 使用半透明遮罩，增加层次感
        );
    }

    @Override
    public float getIslandWidth() {
        this.width = calculateWidth();
        return this.width;
    }

    @Override
    public float getIslandHeight() {
        return 28;
    }

    @Override
    public int getIslandPriority() {
        return -5;
    }
}
