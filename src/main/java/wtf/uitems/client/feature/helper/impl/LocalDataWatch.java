package wtf.uitems.client.feature.helper.impl;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import wtf.uitems.client.feature.helper.IHelper;
import wtf.uitems.client.feature.helper.impl.server.KnownServerManager;
import wtf.uitems.client.feature.helper.impl.target.TargetList;
import wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.uitems.client.feature.module.impl.visual.esp.ESPModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.client.socket.data.ConfigCache;
import wtf.uitems.client.socket.packet.impl.c2s.C2SAccountResolvePacket;
import wtf.uitems.client.socket.packet.impl.c2s.config.C2SConfigListPacket;
import wtf.uitems.client.socket.packet.types.ConfigListRequestType;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.game.PreGameTickEvent;
import wtf.uitems.event.impl.game.packet.ReceivePacketEvent;
import wtf.uitems.event.impl.game.packet.SendPacketEvent;
import wtf.uitems.event.impl.game.player.PlayerCreateEvent;
import wtf.uitems.event.impl.game.player.interaction.AttackEvent;
import wtf.uitems.event.impl.game.player.movement.PostMoveEvent;
import wtf.uitems.event.impl.game.player.movement.step.StepSuccessEvent;
import wtf.uitems.event.impl.game.server.ServerConnectEvent;
import wtf.uitems.event.impl.game.server.ServerDisconnectEvent;
import wtf.uitems.event.subscriber.Subscribe;
import wtf.uitems.mixin.ClientCommonNetworkHandlerAccessor;
import wtf.uitems.mixin.PlayerInteractEntityC2SPacketAccessor;
import wtf.uitems.utility.misc.chat.ChatUtility;
import wtf.uitems.utility.misc.math.RandomUtility;
import wtf.uitems.utility.misc.time.Stopwatch;
import wtf.uitems.utility.player.PlayerUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static wtf.uitems.client.Constants.mc;

public final class LocalDataWatch implements IHelper {

    private LocalDataWatch() {
    }

    private final KnownServerManager knownServerManager = new KnownServerManager();

    private TargetList targetList = new TargetList();
    private final List<String> friendList = new CopyOnWriteArrayList<>();
    private final List<String> strengthedPlayerList = new ArrayList<>();
    public static final ConcurrentHashMap<String, Integer> SCOREBOARD_HEALTHS = new ConcurrentHashMap<>();

    public int airTicks, groundTicks, ticksSinceStepped, ticksSinceTeleport;
    public final Stopwatch velocityStopwatch = new Stopwatch(0);

    // Expires after 10 ticks
    public final Pair<Integer, LivingEntity> lastEntityAttack = new Pair<>(0, null);

    @Subscribe
    public void onAttack(final AttackEvent event) {
        if (event.getTarget() instanceof LivingEntity livingEntity) {
            lastEntityAttack.setLeft(0);
            lastEntityAttack.setRight(livingEntity);
        }
    }

    @Subscribe
    public void onServerConnect(ServerConnectEvent event) {
        RandomUtility.resetJoinRandom();

        

//        if (this.knownServerManager.getCurrentServer() instanceof HypixelServer) {
//            final ProtocolVersion selectedVersion = ViaFabricPlus.getImpl().getTargetVersion();
//            final ProtocolVersion optimalVersion = ProtocolVersion.getProtocol(SharedConstants.getProtocolVersion());
//
//            if (selectedVersion != optimalVersion) {
//                mc.setScreen(
//                        new DisconnectedScreen(
//                                new MultiplayerScreen(null),
//                                Text.literal(Formatting.GRAY + "Failed to connect to Hypixel"),
//                                Text.literal(
//                                        "Opal's Hypixel bypasses are not made for the Minecraft version \nyou selected in ViaFabricPlus. " +
//                                                "Please select " + Formatting.GREEN + Formatting.BOLD + optimalVersion.getName() + Formatting.RESET
//                                                + " in \nyour ViaFabricPlus settings."
//                                )
//                        )
//                );
//
//                event.setCancelled();
//            }
//        }

        ClientSocket.getInstance().syncAccount();
    }

    @Subscribe
    public void onServerDisconnect(ServerDisconnectEvent event) {
        this.knownServerManager.resetServer();
        this.targetList = new TargetList();
        SCOREBOARD_HEALTHS.clear();

        ClientSocket.getInstance().getUserCache().clear();

        if (mc.getNetworkHandler() != null) {
            final ClientCommonNetworkHandlerAccessor accessor = (ClientCommonNetworkHandlerAccessor) mc.getNetworkHandler();
            accessor.getServerCookies().clear();
        }
    }

    @Subscribe
    public void onPlayerCreate(PlayerCreateEvent event) {
        this.targetList = new TargetList();
    }

    @Subscribe
    public void onPreGameTick(PreGameTickEvent event) {
        if (this.targetList != null && this.shouldUpdateTargetList()) {
            final int playerAge = mc.player != null ? mc.player.age : 0;
            final int updateInterval = this.shouldUpdateTargetListEveryTick() ? 1 : 4;
            if (updateInterval == 1 || playerAge % updateInterval == 0) {
                this.targetList.tick();
            }
        }

        ticksSinceStepped++;
        ticksSinceTeleport++;

        if (mc.currentScreen == null && mc.getOverlay() == null && PlayerUtility.isKeyPressed(GLFW.GLFW_KEY_PERIOD)) {
            mc.setScreen(new ChatScreen(".", false));
        }

        final ClientSocket socket = ClientSocket.getInstance();
        final ConfigCache configCache = socket.getConfigCache();

        if (mc.currentScreen instanceof ChatScreen) {
            if (!configCache.isRequestPacketSent()) {
                socket.sendPacket(new C2SConfigListPacket(ConfigListRequestType.SUGGESTION));
                configCache.setRequestPacketSent(true);
            }
        } else {
            configCache.setRequestPacketSent(false);
        }

        lastEntityAttack.setLeft(lastEntityAttack.getLeft() + 1);
        if (lastEntityAttack.getLeft() > 10) {
            lastEntityAttack.setRight(null);
        }
    }

    @Subscribe
    public void onStepSuccess(final StepSuccessEvent event) {
        ticksSinceStepped = 0;
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof PlayerListS2CPacket playerListPacket) {
            final ClientSocket socket = ClientSocket.getInstance();
            if (!socket.isAuthenticated()) {
                return;
            }

            final Set<UUID> checkedUsers = socket.getUserCache().getCheckedUUIDs();

            for (final PlayerListS2CPacket.Entry entry : playerListPacket.getEntries()) {
                final UUID uuid = entry.profileId();
                if (!checkedUsers.contains(uuid)) {
                    checkedUsers.add(uuid);
                    socket.sendPacket(new C2SAccountResolvePacket(uuid));
                }
            }
        } else if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            ticksSinceTeleport = 0;
        } else if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (mc.player != null && packet.getEntityId() == mc.player.getId()) {
                this.velocityStopwatch.reset();
            }
        } else if (event.getPacket() instanceof ScoreboardScoreUpdateS2CPacket packet) {
            final String objective = packet.objectiveName();
            if (objective.equalsIgnoreCase("health") || objective.equalsIgnoreCase("belowHealth")) {
                SCOREBOARD_HEALTHS.put(packet.scoreHolderName(), packet.score());
            }
        }
    }

    @Subscribe
    public void onSendPacket(SendPacketEvent event) {
        boolean noSlowDebug = false;
        if (noSlowDebug) {
            switch (event.getPacket()) {
                case PlayerInteractEntityC2SPacket interact -> {
                    PlayerInteractEntityC2SPacketAccessor accessor = (PlayerInteractEntityC2SPacketAccessor) interact;
                    ChatUtility.error("ENT_" + accessor.getEntityId() + mc.player.age);
                }
                case PlayerInteractItemC2SPacket interact -> {
                    ChatUtility.error("ITEM_INTERACT" + mc.player.age + " " + interact.getHand() + " " + interact.getSequence());
                }
                case PlayerInteractBlockC2SPacket interact -> {
                    ChatUtility.error("BLOCK_INTERACT" + mc.player.age + " " + interact.getHand() + " " + interact.getSequence());
                }
                case UpdateSelectedSlotC2SPacket slot -> {
                    ChatUtility.error("SLOT" + mc.player.age + " " + slot.getSelectedSlot());
                }
                case ClientCommandC2SPacket command -> {
                    ChatUtility.error("COMMAND" + mc.player.age + " " + command.getMode().name());
                }
                case PlayerActionC2SPacket action -> {
                    ChatUtility.error("ACTION" + mc.player.age + " " + action.getAction());
                }
                default -> {
                }
            }
        }
    }

    private Vec3d prevVelocity;

    @Subscribe(priority = -10)
    public void onPostMoveLow(final PostMoveEvent event) {
        this.prevVelocity = mc.player.getVelocity();
    }

    public Vec3d getPrevVelocity() {
        return prevVelocity;
    }

    public static TargetList getTargetList() {
        return instance.targetList;
    }

    public static List<String> getFriendList() {
        return instance.friendList;
    }

    public List<String> getStrengthedPlayerList() {
        return strengthedPlayerList;
    }

    public KnownServerManager getKnownServerManager() {
        return knownServerManager;
    }

    private boolean shouldUpdateTargetList() {
        final var moduleRepository = wtf.uitems.client.OpalClient.getInstance().getModuleRepository();
        if (moduleRepository.getModule(KillAuraModule.class).isEnabled()) {
            return true;
        }
        if (moduleRepository.getModule(HUDModule.class).isTargetInfoActive()) {
            return true;
        }
        return moduleRepository.getModule(HUDModule.class).hasOverlayContent()
                && moduleRepository.getModule(ESPModule.class).isEnabled();
    }

    private boolean shouldUpdateTargetListEveryTick() {
        final var moduleRepository = wtf.uitems.client.OpalClient.getInstance().getModuleRepository();
        if (moduleRepository.getModule(KillAuraModule.class).isEnabled()) {
            return true;
        }
        return moduleRepository.getModule(HUDModule.class).isTargetInfoActive();
    }

    private static LocalDataWatch instance;

    public static LocalDataWatch get() {
        return instance;
    }

    public static void setInstance() {
        instance = new LocalDataWatch();
        EventDispatcher.subscribe(instance);
    }

}
