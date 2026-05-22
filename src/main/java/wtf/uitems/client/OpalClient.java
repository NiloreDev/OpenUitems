package wtf.uitems.client;

import org.freedesktop.dbus.spi.transport.ITransportProvider;
import wtf.uitems.client.binding.repository.BindRepository;
import wtf.uitems.client.command.impl.config.ConfigCommand;
import wtf.uitems.client.command.impl.irc.OnlineCommand;
import wtf.uitems.client.command.impl.irc.ReplyCommand;
import wtf.uitems.client.command.impl.irc.WhisperCommand;
import wtf.uitems.client.command.impl.account.MicrosoftLoginCommand;
import wtf.uitems.client.command.impl.module.BindCommand;
import wtf.uitems.client.command.impl.module.ToggleCommand;
import wtf.uitems.client.command.impl.player.FriendCommand;
import wtf.uitems.client.command.impl.player.UsernameCommand;
import wtf.uitems.client.command.impl.player.movement.HClipCommand;
import wtf.uitems.client.command.impl.player.movement.VClipCommand;
import wtf.uitems.client.command.repository.CommandRepository;
import wtf.uitems.client.feature.helper.impl.LocalDataWatch;
import wtf.uitems.client.feature.helper.impl.chat.ChatHelper;
import wtf.uitems.client.feature.helper.impl.player.mouse.MouseHelper;
import wtf.uitems.client.feature.helper.impl.player.slot.SlotHelper;
import wtf.uitems.client.feature.helper.impl.player.swing.SwingDelay;
import wtf.uitems.client.feature.helper.impl.player.timer.TimerHelper;
import wtf.uitems.client.feature.helper.impl.render.FadingBlockHelper;
import wtf.uitems.client.feature.helper.impl.render.ScreenPositionManager;
import wtf.uitems.client.feature.module.impl.combat.*;
import wtf.uitems.client.feature.module.impl.combat.criticals.CriticalsModule;
import wtf.uitems.client.feature.module.impl.combat.killaura.KillAuraModule;
import wtf.uitems.client.feature.module.impl.combat.velocity.VelocityModule;
import wtf.uitems.client.feature.module.impl.movement.*;
import wtf.uitems.client.feature.module.impl.movement.flight.FlightModule;
import wtf.uitems.client.feature.module.impl.movement.longjump.LongJumpModule;
import wtf.uitems.client.feature.module.impl.movement.noslow.NoSlowModule;
import wtf.uitems.client.feature.module.impl.movement.speed.SpeedModule;
import wtf.uitems.client.feature.module.impl.utility.*;
import wtf.uitems.client.feature.module.impl.utility.disabler.DisablerModule;
import wtf.uitems.client.feature.module.impl.utility.inventory.AutoArmorModule;
import wtf.uitems.client.feature.module.impl.utility.inventory.ChestStealerModule;
import wtf.uitems.client.feature.module.impl.utility.inventory.manager.InventoryManagerModule;
import wtf.uitems.client.feature.module.impl.utility.nofall.NoFallModule;
import wtf.uitems.client.feature.module.impl.visual.*;
import wtf.uitems.client.feature.module.impl.visual.esp.ESPModule;
import wtf.uitems.client.feature.module.impl.visual.overlay.HUDModule;
import wtf.uitems.client.feature.module.impl.world.FastBreakModule;
import wtf.uitems.client.feature.module.impl.world.GhostHandModule;
import wtf.uitems.client.feature.module.impl.world.TimerModule;
import wtf.uitems.client.feature.module.impl.world.breaker.BreakerModule;
import wtf.uitems.client.feature.module.impl.world.scaffold.ScaffoldModule;
import wtf.uitems.client.feature.module.repository.ModuleRepository;
import wtf.uitems.client.notification.NotificationManager;
import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.event.EventDispatcher;
import wtf.uitems.event.impl.client.PostClientInitializationEvent;
import wtf.uitems.protection.annotation.Bootstrap;
import wtf.uitems.client.verify.VerifyManager;
import wtf.uitems.utility.data.SaveUtility;
import wtf.uitems.utility.socket.user.LocalUser;

import java.util.ServiceLoader;
public final class OpalClient {

    private final NotificationManager notificationManager;
    private final BindRepository bindRepository;

    private CommandRepository commandRepository;
    private ModuleRepository moduleRepository;
    // private ScriptRepository scriptRepository;
    private LocalUser user;

    private boolean postInitialization;
    private boolean ghostHand;
    private boolean useVanillaMainMenu;

    private OpalClient() {
        this.notificationManager = new NotificationManager();
        this.bindRepository = new BindRepository();
    }

    @Bootstrap
    public void runPostInitializations() {
        long start = System.currentTimeMillis();

        System.setProperty("java.net.preferIPv4Stack", "true");

        // load service for linux
        ServiceLoader.load(ITransportProvider.class, this.getClass().getClassLoader());

//        uncomment this when building
//        final ModContainerImpl container = (ModContainerImpl) FabricLoader.getInstance().getModContainer("uitems").orElseThrow();
//        for (final Path path : container.getCodeSourcePaths()) {
//            if (!path.getFileSystem().getClass().getName().startsWith("wtf.opal") || path.getFileSystem().getClass().getSimpleName().contains("FileSystem")) {
//                Callables.throwError(1_1);
//                return;
//            }
//        }

        ClientSocket.setInstance();

//        if (this.user != null) {
//            Callables.throwError(1_2);
//            return;
//        }

        ClientSocket.getInstance().connect();
        new Thread(() -> {
            VerifyManager.getInstance().init();
        }).start();

        this.runHelperInitializations();
        this.registerFabricEvents();

        if (this.moduleRepository == null) {
            this.moduleRepository = ModuleRepository.fromModules(
                    // Combat
                    new KillAuraModule(),
                    new CrystalAuraModule(),
                    new ReachModule(),
                    new AutoClickerModule(),
                    new AutoShootModule(),
                    new FastPearlModule(),
                    new AutoPearlModule(),
                    new BacktrackModule(),
                    new CriticalsModule(),
                    new VelocityModule(),
                    new AntiBotModule(),
                    new TeamsModule(),
                    new AutoHeadModule(),
                    // Visual
                    new ClickGUIModule(),
                    new FullBrightModule(),
                    new AnimationsModule(),
                    new HUDModule(),
                    new ChamsModule(),
                    new ESPModule(),
                    new ChestESPModule(),
                    new BreakProgressModule(),
                    new CapeModule(),
                    new AmbienceModule(),
                    new AttackEffectsModule(),
                    new StreamerModeModule(),
                    new NoHurtCameraModule(),
                    // World
                    new ScaffoldModule(),
                    new TimerModule(),
                    new BreakerModule(),
                    new FastBreakModule(),
                    new GhostHandModule(),
                    // Movement
                    new FlightModule(),
                    new SpeedModule(),
                    new NoWebModule(),
                    new JumpCooldownModule(),
                    new SprintModule(),
                    new KeepSprintModule(),
                    new MovementFixModule(),
                    new NoSlowModule(),
                    new InventoryMoveModule(),
                    new TargetStrafeModule(),
                    new LongJumpModule(),
                    new SafeWalkModule(),
                    new StuckModule(),
                    // Utility
                    new FastUseModule(),
                    new NoFallModule(),
                    new ChestStealerModule(),
                    new InventoryManagerModule(),
                    new AutoArmorModule(),
                    new DisablerModule(),
                    new AutoToolModule(),
                    new AutoChestModule(),
                    new BucketHelperModule(),
                    new IRCModule(),
                    new BlinkModule(),
                    new SpammerModule()
            );
        }

        if (this.commandRepository == null) {
            this.commandRepository = CommandRepository.builder()
                    .putAll(
                            new ToggleCommand(),
                            new BindCommand(),
                            new ConfigCommand(),
                            new OnlineCommand(),
                            new ReplyCommand(),
                            new VClipCommand(),
                            new HClipCommand(),
                            new WhisperCommand(),
                            new UsernameCommand(),
                            new FriendCommand(),
                            new MicrosoftLoginCommand()
                    ).build();

        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));

        this.postInitialization = true;
        EventDispatcher.dispatch(new PostClientInitializationEvent());

        
    }
    private void runHelperInitializations() {
        LocalDataWatch.setInstance();
        MouseHelper.setInstance();
        SwingDelay.setInstance();
        SlotHelper.setInstance();
        ChatHelper.setInstance();
        TimerHelper.setInstance();
        FadingBlockHelper.setInstance();
        ScreenPositionManager.setInstance();
    }
    private void registerFabricEvents() {
    }
    private void onShutdown() {
        System.out.println("Shutting down UitemsClient...");
        if (this.moduleRepository != null) {
            final ClickGUIModule clickGui = this.moduleRepository.getModule(ClickGUIModule.class);
            if (clickGui != null) {
                clickGui.setEnabled(false);
            }
        }

        SaveUtility.saveLocalConfig("default");
        SaveUtility.saveBindings();
        System.out.println("UitemsClient shutdown completed.");
    }
    public boolean isPostInitialization() {
        return postInitialization;
    }

    public ModuleRepository getModuleRepository() {
        return moduleRepository;
    }

    public boolean isGhostHand() {
        return ghostHand;
    }

    public void setGhostHand(boolean ghostHand) {
        this.ghostHand = ghostHand;
    }

    public boolean isUseVanillaMainMenu() {
        return useVanillaMainMenu;
    }

    public void setUseVanillaMainMenu(boolean useVanillaMainMenu) {
        this.useVanillaMainMenu = useVanillaMainMenu;
    }

    public BindRepository getBindRepository() {
        return bindRepository;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

//    public ScriptRepository getScriptRepository() {
//        return scriptRepository;
//    }

    public LocalUser getUser() {
        return user;
    }
    public void setUser(final LocalUser user) {
        this.user = user;
    }

    private static OpalClient instance;

    public static OpalClient getInstance() {
        return instance;
    }
    public static void setInstance() {
        instance = new OpalClient();
    }

}
