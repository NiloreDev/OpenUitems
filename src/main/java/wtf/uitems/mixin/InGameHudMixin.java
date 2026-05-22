package wtf.uitems.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.module.impl.visual.AnimationsModule;
import wtf.uitems.client.feature.module.impl.visual.StreamerModeModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.repository.ModuleRepository;
import wtf.uitems.client.renderer.MinecraftRenderer;
import wtf.uitems.client.renderer.overlay.ClientInGameOverlay;
import wtf.uitems.client.renderer.shader.ShaderFramebuffer;
import wtf.uitems.client.verify.VerifyManager;
import wtf.uitems.client.screen.click.AbstractClickGui;
import wtf.uitems.utility.render.ColorUtility;
import wtf.uitems.utility.render.SidebarEntry;
import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.utility.socket.user.User;

import java.util.Comparator;
import java.util.List;

import static wtf.uitems.client.Constants.mc;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Final
    @Shadow
    private static Comparator<ScoreboardEntry> SCOREBOARD_ENTRY_COMPARATOR;

    @Unique
    private static Text SB_GRAY_OPENING_PARENTHESIS;
    @Unique
    private static Text SB_GRAY_CLOSING_PARENTHESIS;
    @Unique
    private static Text SB_EMPTY_TEXT;
    @Unique
    private static final java.util.Map<String, String> SB_DISPLAY_NAME_CACHE = new java.util.HashMap<>();
    @Unique
    private static long sbCacheExpiry = 0L;

    private InGameHudMixin() {
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
            )
    )
    private void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        final boolean clickGuiOpen = mc.currentScreen instanceof AbstractClickGui;
        if (!overlayModule.hasOverlayContent() && !clickGuiOpen) {
            MinecraftRenderer.render();
            return;
        }

        // rendering previous draws to fix layering
        // TOO MANY BUGS, breaks blur with vignette, causes shit to render over mc guis, etc
        // we are probably gonna have to find a way to mixin to their rendering :(
//        final GameRendererAccessor gameRenderer = (GameRendererAccessor) mc.gameRenderer;
//        final GuiRenderer guiRenderer = gameRenderer.getGuiRenderer();
//        final FogRenderer fogRenderer = gameRenderer.getFogRenderer();
//        guiRenderer.render(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
        ClientInGameOverlay.applyPostProcessing(context, tickCounter.getTickProgress(false));

        ((GameRendererAccessor) mc.gameRenderer).getGuiRenderer().render(((GameRendererAccessor) mc.gameRenderer).getFogRenderer().getFogBuffer(FogRenderer.FogType.NONE));
        ((GameRendererAccessor) mc.gameRenderer).getGuiState().clear();

        ClientInGameOverlay.render(tickCounter.getTickProgress(false), context);

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                ShaderFramebuffer.getGlowFramebuffer().getColorAttachment(),
                0,
                ShaderFramebuffer.getGlowFramebuffer().getDepthAttachment(),
                1
        );
        MinecraftRenderer.render();
    }

    @Redirect(
            method = "tick()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSelectedStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack getMainHandStack(PlayerInventory instance) {
        SlotHelper slotHelper = SlotHelper.getInstance();
        if (slotHelper.isActive() && slotHelper.getSilence() == SlotHelper.Silence.FULL) {
            return slotHelper.getMainHandStack(mc.player);
        }
        return instance.getSelectedStack();
    }

    @Redirect(
            method = "renderHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSelectedSlot()I")
    )
    private int onRenderHotbarSlot(PlayerInventory instance) {
        return SlotHelper.getInstance().getSelectedSlot(instance);
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getOffHandStack()Lnet/minecraft/item/ItemStack;"), method = "renderHotbar")
    public ItemStack hideOffhandSlot(PlayerEntity player) {
        ItemStack realStack = player.getOffHandStack();
        final AnimationsModule animationsModule = OpalClient.getInstance().getModuleRepository().getModule(AnimationsModule.class);
        if (animationsModule.isEnabled() &&
                animationsModule.isHideShieldSlotInHotbar() &&
                realStack.getItem() instanceof ShieldItem &&
                animationsModule.isHideShield()) {
            return ItemStack.EMPTY;
        }
        return realStack;
    }

    @ModifyArg(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"),
            index = 5
    )
    private boolean hookScoreboardTextShadow(boolean shadow) {
        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        return overlayModule.isEnabled() && overlayModule.isScoreboardTextShadow();
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"))
    private void resetScoreboardRectDimensions(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientInGameOverlay.resetScoreboardRectDimensions();
    }

    /**
     * @author senoe
     * @reason sucks to modify position normally
     */
    @Overwrite
    private void renderScoreboardSidebar(DrawContext drawContext, ScoreboardObjective objective) {
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);

        SidebarEntry[] sidebarEntrys = scoreboard.getScoreboardEntries(objective)
                .stream()
                .filter(score -> !score.hidden())
                .sorted(SCOREBOARD_ENTRY_COMPARATOR)
                .limit(15L)
                .map(scoreboardEntry -> {
                    Team team = scoreboard.getScoreHolderTeam(scoreboardEntry.owner());
                    Text textx = scoreboardEntry.name();
                    Text text2 = Team.decorateName(team, textx);
                    Text text3 = scoreboardEntry.formatted(numberFormat);
                    int ix = mc.textRenderer.getWidth(text3);
                    return new SidebarEntry(text2, text3, ix);
                })
                .toArray(SidebarEntry[]::new);
        Text text = objective.getDisplayName();
        int i = mc.textRenderer.getWidth(text);
        int j = i;
        int k = mc.textRenderer.getWidth(": ");

        for (SidebarEntry sidebarEntry : sidebarEntrys) {
            j = Math.max(j, mc.textRenderer.getWidth(sidebarEntry.name()) + (sidebarEntry.scoreWidth() > 0 ? k + sidebarEntry.scoreWidth() : 0));
        }

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();
        final HUDModule overlayModule = moduleRepository.getModule(HUDModule.class);
        final StreamerModeModule streamerModeModule = moduleRepository.getModule(StreamerModeModule.class);

        final boolean textShadow = overlayModule.isEnabled() && overlayModule.isScoreboardTextShadow();

        final boolean hideServerId = streamerModeModule.isEnabled() && streamerModeModule.isHidingServerId();

        final float moduleListHeight = overlayModule.getToggledModules().getSettings().isOffsetScoreboard()
                ? overlayModule.getToggledModules().getTotalHeight()
                : 0;

        final float scale = overlayModule.getScoreboardScale();

        int m = sidebarEntrys.length;
        int n = m * 9;
        int o = (int) (drawContext.getScaledWindowHeight() / scale / 2 + n / 3F);
        int q = (int) (drawContext.getScaledWindowWidth() / scale - j - 3);
        int r = (int) (drawContext.getScaledWindowWidth() / scale - 3 + 2);
//        int s = mc.options.getTextBackgroundColor(0.3F);
//        int t = mc.options.getTextBackgroundColor(0.4F);
        int u = o - m * 9;

        if (moduleListHeight != 0 && (moduleListHeight + 20) / scale > u) {
            final int adjustedHeight = (int) ((moduleListHeight + 20) / scale);
            final int difference = adjustedHeight - u;
            u = adjustedHeight;
            o += difference;
        }

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale);

//        // header bg
//        drawContext.fill(q - 2, u - 9 - 1, r, u - 1, t);
//        // entry bg
//        drawContext.fill(q - 2, u - 1, r, o, s);

        drawContext.drawText(mc.textRenderer, text, q + j / 2 - i / 2 - 1, u - 9, Colors.WHITE, textShadow);

        for (int v = 0; v < m; v++) {
            SidebarEntry sidebarEntry2 = sidebarEntrys[v];
            int w = u + v * 9;

            Text name = sidebarEntry2.name();
            final String nameStr = name.getString();

            if (hideServerId && v == 0 && nameStr.contains("/") && nameStr.contains("  ")) {
                final String[] parts = nameStr.split(" {2}");
                if (parts.length > 1) {
                    name = Text.literal("§7" + parts[0] + "  §8§k" + parts[1]);
                }
            }

            boolean replacedFooter = false;
            if (streamerModeModule.isEnabled() && v == m - 1 && nameStr.contains("布吉岛")) {
                final int themeColor = ColorUtility.getClientTheme().first;
                name = Text.literal("Uitems.today").styled(style -> style.withColor(themeColor));
                replacedFooter = true;
            }

            if (streamerModeModule.isEnabled() && !replacedFooter) {
                // Rebuild cache once per second instead of iterating tab list every frame
                final long now2 = System.currentTimeMillis();
                if (now2 > sbCacheExpiry || SB_DISPLAY_NAME_CACHE.isEmpty()) {
                    SB_DISPLAY_NAME_CACHE.clear();
                    if (mc.getNetworkHandler() != null) {
                        for (final var tabEntry : mc.getNetworkHandler().getPlayerList()) {
                            final String ign = tabEntry.getProfile().name();
                            String displayName = null;
                            final VerifyManager verifyManager2 = VerifyManager.getInstance();
                            if (verifyManager2 != null && verifyManager2.getTransport() != null) {
                                displayName = verifyManager2.getTransport().getName(ign);
                            }
                            if (displayName == null) {
                                final User user = ClientSocket.getInstance().getUserOrNull(tabEntry.getProfile().id());
                                if (user != null) displayName = user.getName();
                            }
                            if (displayName != null) SB_DISPLAY_NAME_CACHE.put(ign, displayName);
                        }
                    }
                    sbCacheExpiry = now2 + 1000L;
                }
                String appendDisplay = null;
                for (final var entry : SB_DISPLAY_NAME_CACHE.entrySet()) {
                    final String ign = entry.getKey();
                    if (nameStr.contains(ign)) {
                        appendDisplay = entry.getValue();
                        break;
                    }
                }
                if (appendDisplay != null) {
                    final String baseString = name.getString();
                    if (baseString == null || !baseString.contains("(" + appendDisplay + ")")) {
                        if (SB_GRAY_OPENING_PARENTHESIS == null) {
                            SB_GRAY_OPENING_PARENTHESIS = Text.literal(" " + Formatting.GRAY + "(");
                            SB_GRAY_CLOSING_PARENTHESIS = Text.literal(Formatting.GRAY + ")");
                            SB_EMPTY_TEXT = Text.empty();
                        }
                        final int themeColor = ColorUtility.getClientTheme().first;
                        final Text coloredDisplay = Text.literal(appendDisplay).styled(style -> style.withColor(themeColor));
                        name = Texts.join(
                                List.of(
                                        name,
                                        SB_GRAY_OPENING_PARENTHESIS,
                                        coloredDisplay,
                                        SB_GRAY_CLOSING_PARENTHESIS
                                ),
                                SB_EMPTY_TEXT
                        );
                    }
                }
            }

            drawContext.drawText(mc.textRenderer, name, q - 1, w, Colors.WHITE, textShadow);
            drawContext.drawText(mc.textRenderer, sidebarEntry2.score(), r - sidebarEntry2.scoreWidth() - 1, w, Colors.WHITE, textShadow);
        }

        drawContext.getMatrices().popMatrix();

        ClientInGameOverlay.setSbRectX((q - 2 - 2 - 0.5F) * scale);
        ClientInGameOverlay.setSbRectY((u - 9 - 1 - 1) * scale);
        ClientInGameOverlay.setSbRectWidth(((r - 0.5F) * scale) - ClientInGameOverlay.getSbRectX());
        ClientInGameOverlay.setSbRectHeight(((m * 9) + 13) * scale);
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(CallbackInfo ci) {
        final HUDModule overlayModule = OpalClient.getInstance().getModuleRepository().getModule(HUDModule.class);
        if (overlayModule.isEnabled() && !overlayModule.isStatusEffectOverlayEnabled()) {
            ci.cancel();
        }
    }

}
