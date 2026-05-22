package wtf.uitems.client.socket;

import net.minecraft.client.session.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.ReleaseInfo;
import wtf.uitems.client.feature.module.impl.visual.CapeModule;
import wtf.uitems.client.feature.module.repository.ModuleRepository;
import wtf.uitems.client.notification.NotificationType;
import wtf.uitems.client.socket.data.ConfigCache;
import wtf.uitems.client.socket.data.UserCache;
import wtf.uitems.client.socket.data.VariableCache;
import wtf.uitems.client.socket.packet.api.C2SPacket;
import wtf.uitems.client.socket.packet.api.S2CPacket;
import wtf.uitems.client.socket.packet.api.ServerPacketConsumer;
import wtf.uitems.client.socket.packet.impl.c2s.C2SAccountUpdatePacket;
import wtf.uitems.protection.annotation.NativeExclude;
import wtf.uitems.utility.socket.EncryptionContext;
import wtf.uitems.utility.socket.buffer.BufferReader;
import wtf.uitems.utility.socket.user.User;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static wtf.uitems.client.Constants.mc;

import wtf.uitems.client.verify.VerifyManager;
import wtf.uitems.utility.socket.user.LocalUser;
import wtf.uitems.utility.socket.user.UserRole;
public final class ClientSocket {

    private final List<ServerPacketConsumer> serverPacketConsumers;
    private final VariableCache variableCache;
    private final ConfigCache configCache;
    private final UserCache userCache;

    private ScheduledExecutorService reconnectScheduler;
    private ExecutorService listenerThread;

    private EncryptionContext encryptionContext;
    private Object socket;

    private boolean connected, authenticated, blockMainThread;
    private String lastReceivedWhisperUsername;

    private int keepAliveCount;
    private long magicNumber;

    private static ClientSocket instance;
    private static int connectionCount;

    private ClientSocket() {
        this.serverPacketConsumers = new ArrayList<>();
        this.variableCache = new VariableCache(this);
        this.configCache = new ConfigCache();
        this.userCache = new UserCache();
        this.blockMainThread = true;
    }

    public void connect() {
        if (this.connected) {
            return;
        }

        // Mock user for offline/bypassed mode
        String username = VerifyManager.getInstance().getUsername();
        if (username == null || username.isEmpty()) {
            username = mc.getSession().getUsername();
        }
        LocalUser mockUser = new LocalUser(
            1, 
            username != null && !username.isEmpty() ? username : "Developer", 
            UserRole.USER
        );
        OpalClient.getInstance().setUser(mockUser);

        this.connected = true;
        this.authenticated = true;

        OpalClient.getInstance().getNotificationManager()
                .builder(NotificationType.SUCCESS)
                .duration(2000)
                .title("Connection established")
                .description("Public build runs without verification.")
                .buildAndPublish();
    }

    private boolean handleIncomingPackets(final boolean blocking) {
        // Disabled for bypass
        return true;
    }

    private S2CPacket createPacket(final int packetId, final BufferReader reader) throws Exception {
        return null;
    }

    public void sendPacket(final C2SPacket packet) {
        // Disabled for verification bypass
    }

    @Nullable
    @NativeExclude
    public User getUserOrNull(@NotNull final UUID uuid) {
        if (!this.connected) {
            return null;
        }

        if (uuid.equals(mc.getSession().getUuidOrNull())) {
            return OpalClient.getInstance().getUser();
        }

        return this.userCache.getResolvedUsers().get(uuid);
    }

    public void syncAccount() {
        if (!this.connected || this.encryptionContext == null) {
            return;
        }

        final Session session = mc.getSession();
        if (session.getUuidOrNull() == null) {
            return;
        }

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();
        final CapeModule capeModule = moduleRepository != null
                ? moduleRepository.getModule(CapeModule.class)
                : null;

        this.sendPacket(
                new C2SAccountUpdatePacket(
                        session.getUuidOrNull(),
                        session.getAccessToken(),
                        capeModule != null && capeModule.isEnabled() ? capeModule.getType().getSlug() : null
                )
        );
    }

    public void close() {
        this.connected = this.authenticated = false;
        this.encryptionContext = null;

        this.keepAliveCount = 0;
        this.magicNumber = 0L;

        this.userCache.clear();
        this.variableCache.clear();

        try {
            if (this.socket != null) {
                ((SSLSocket) this.socket).close();
            }
        } catch (IOException ignored) {
        }

        System.clearProperty(String.valueOf(Integer.parseInt(ReleaseInfo.VERSION.replaceAll("[^0-9]", "")) * 89));
    }

    public void reconnect() {
        // No-op
    }

    public void registerServerPacketConsumer(final ServerPacketConsumer consumer) {
        this.serverPacketConsumers.add(consumer);
    }

    @NativeExclude
    public static ClientSocket getInstance() {
        return instance;
    }

    public static void setInstance() {
        instance = new ClientSocket();
    }

    @NativeExclude
    public boolean isConnected() {
        return this.connected;
    }

    @NativeExclude
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    public VariableCache getVariableCache() {
        return variableCache;
    }

    public ConfigCache getConfigCache() {
        return configCache;
    }

    public UserCache getUserCache() {
        return userCache;
    }

    public EncryptionContext getEncryptionContext() {
        return encryptionContext;
    }

    public void setEncryptionContext(final EncryptionContext encryptionContext) {
        this.encryptionContext = encryptionContext;
    }

    public void setAuthenticated(final boolean authenticated) {
        this.authenticated = authenticated;
    }

    public void setBlockMainThread(final boolean blockMainThread) {
        this.blockMainThread = blockMainThread;
    }

    public String getLastReceivedWhisperUsername() {
        return lastReceivedWhisperUsername;
    }

    public void setLastReceivedWhisperUsername(final String lastReceivedWhisperUsername) {
        this.lastReceivedWhisperUsername = lastReceivedWhisperUsername;
    }

    public int getKeepAliveCount() {
        return keepAliveCount;
    }

    public void incrementKeepAliveCount() {
        this.keepAliveCount++;
        System.setProperty(
                String.valueOf(Integer.parseInt(ReleaseInfo.VERSION.replaceAll("[^0-9]", "")) * 89),
                String.valueOf(48 - this.keepAliveCount)
        );
    }

    public static int getConnectionCount() {
        return connectionCount;
    }

    public long getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(final long magicNumber) {
        this.magicNumber = magicNumber;
    }

}
