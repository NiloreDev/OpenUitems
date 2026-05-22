package wtf.uitems.client.verify;

import wtf.uitems.client.OpalClient;
import wtf.uitems.utility.socket.user.LocalUser;
import wtf.uitems.utility.socket.user.UserRole;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class VerifyManager {
    private static VerifyManager instance;

    private final PublicTransport transport = new PublicTransport();
    private boolean authenticated = true;
    private String username;
    private long expireAt;
    private Consumer<VerifyResult> callback;
    private String lastReceivedWhisperUsername;

    public record VerifyResult(boolean success, String message, Type type) {
        public enum Type {
            LOGIN, REGISTER, RECHARGE
        }
    }

    public static VerifyManager getInstance() {
        if (instance == null) {
            instance = new VerifyManager();
        }
        return instance;
    }

    public void setCallback(Consumer<VerifyResult> callback) {
        this.callback = callback;
    }

    public void init() {
        this.authenticated = true;
        ensureLocalUser();
    }

    public void reconnect() {
        init();
    }

    public void login(String username, String password) {
        this.username = username;
        this.authenticated = true;
        ensureLocalUser();
        publishResult(VerifyResult.Type.LOGIN, "Verification server removed in the public build.");
    }

    public void register(String username, String password, String cardKey) {
        this.username = username;
        this.authenticated = true;
        ensureLocalUser();
        publishResult(VerifyResult.Type.REGISTER, "Registration is disabled in the public build.");
    }

    public void registerOrRecharge(String username, String password, String cardKey) {
        this.username = username;
        this.authenticated = true;
        ensureLocalUser();
        publishResult(VerifyResult.Type.RECHARGE, "Recharge is disabled in the public build.");
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getLastReceivedWhisperUsername() {
        return lastReceivedWhisperUsername;
    }

    public void setLastReceivedWhisperUsername(String lastReceivedWhisperUsername) {
        this.lastReceivedWhisperUsername = lastReceivedWhisperUsername;
    }

    public String getUsername() {
        if (username == null || username.isBlank()) {
            return getInGameUsername();
        }
        return username;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public PublicTransport getTransport() {
        return transport;
    }

    private void publishResult(VerifyResult.Type type, String message) {
        if (callback != null) {
            callback.accept(new VerifyResult(true, message, type));
        }
    }

    private void ensureLocalUser() {
        OpalClient client = OpalClient.getInstance();
        if (client == null || client.getUser() != null) {
            return;
        }

        String displayName = getUsername();
        if (displayName == null || displayName.isBlank()) {
            displayName = "Developer";
        }
        client.setUser(new LocalUser(1, displayName, UserRole.USER));
    }

    private String getInGameUsername() {
        if (wtf.uitems.client.Constants.mc == null || wtf.uitems.client.Constants.mc.getSession() == null) {
            return null;
        }
        return wtf.uitems.client.Constants.mc.getSession().getUsername();
    }

    public static final class PublicTransport {
        private final Map<String, String> userToIgnMap = new ConcurrentHashMap<>();
        private final Map<String, String> ignToUserMap = new ConcurrentHashMap<>();

        public String getName(String ign) {
            return ignToUserMap.get(ign);
        }

        public String getIgn(String name) {
            return userToIgnMap.get(name);
        }

        public Map<String, String> getUserToIgnMap() {
            return Collections.unmodifiableMap(userToIgnMap);
        }

        public Map<String, String> getIgnToUserMap() {
            return Collections.unmodifiableMap(ignToUserMap);
        }

        public void sendChat(String message) {
        }

        public void sendInGameUsername() {
        }

        public void sendInGameUsername(String username) {
        }

        public void register(String username, String password, String hwid, java.util.Set<String> qqSet, String phone, String cardKey) {
            VerifyManager.getInstance().register(username, password, cardKey);
        }

        public void recharge(String username, String cardKey) {
            VerifyManager.getInstance().registerOrRecharge(username, "", cardKey);
        }

        public void uploadCloudConfig(String name, String content) {
        }

        public void getCloudConfig(String name) {
        }

        public void getCloudConfig(String owner, String name) {
        }

        public void listCloudConfigs() {
        }
    }
}
