package wtf.uitems.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.utility.socket.user.User;
import wtf.uitems.utility.render.ColorUtility;

import java.util.List;

@Mixin(PlayerListHud.class)
public final class PlayerListHudMixin {

    @Unique
    private static Text GRAY_OPENING_PARENTHESIS;

    @Unique
    private static Text GRAY_CLOSING_PARENTHESIS;

    @Unique
    private static Text EMPTY_TEXT;

    @Unique
    private float opal$offset;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void preRender(DrawContext context, int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        if (overlayModule.isEnabled() && overlayModule.isTabListIsland()) {
            ci.cancel();
            return;
        }
        if (overlayModule.isEnabled() && !overlayModule.isDynamicIslandLeftAligned() && overlayModule.getDynamicIsland().isActive()) {
            this.opal$offset = overlayModule.getDynamicIsland().getAnimatedY() + overlayModule.getDynamicIsland().getAnimatedHeight() - 10;
            if (this.opal$offset > 0) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(0, this.opal$offset);
            }
        } else {
            this.opal$offset = 0;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void postRender(DrawContext context, int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        if (this.opal$offset > 0) {
            context.getMatrices().popMatrix();
        }
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;")
    )
    private Text redirectPlayerName(PlayerListHud instance, PlayerListEntry entry) {
        final Text playerNameText = instance.getPlayerName(entry);

        String displayName = null;
        final String ign = entry.getProfile().name();

        final wtf.uitems.client.verify.VerifyManager verifyManager = wtf.uitems.client.verify.VerifyManager.getInstance();
        if (verifyManager != null && verifyManager.getTransport() != null) {
            displayName = verifyManager.getTransport().getName(ign);
        }

        if (displayName == null) {
            final User user = ClientSocket.getInstance().getUserOrNull(entry.getProfile().id());
            if (user != null) {
                displayName = user.getName();
            }
        }

        if (displayName == null) {
            return playerNameText;
        }

        final String baseString = playerNameText.getString();
        if (baseString != null && baseString.contains("(" + displayName + ")")) {
            return playerNameText;
        }

        if (GRAY_OPENING_PARENTHESIS == null) {
            GRAY_OPENING_PARENTHESIS = Text.literal(" " + Formatting.GRAY + "(");
            GRAY_CLOSING_PARENTHESIS = Text.literal(Formatting.GRAY + ")");
            EMPTY_TEXT = Text.empty();
        }

        final int themeColor = ColorUtility.getClientTheme().first;
        final Text coloredDisplay = Text.literal(displayName).styled(style -> style.withColor(themeColor));

        return Texts.join(
                List.of(
                        playerNameText,
                        GRAY_OPENING_PARENTHESIS,
                        coloredDisplay,
                        GRAY_CLOSING_PARENTHESIS
                ),
                EMPTY_TEXT
        );
    }

}
