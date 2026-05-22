package wtf.uitems.utility.security;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.session.Session;
import org.jetbrains.annotations.Nullable;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.socket.ClientSocket;
import wtf.uitems.mixin.MinecraftClientAccessor;
import wtf.uitems.utility.socket.user.LocalUser;
import wtf.uitems.utility.socket.user.UserRole;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static wtf.uitems.client.Constants.mc;
public final class SessionUtility {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String DEFAULT_CLIENT_ID = "00000000402b5328";

    private SessionUtility() {
    }

    @Nullable
    public static String getKeystoreToken() {
        if (mc.getSession() == null) {
            return null;
        }

        final String accessToken = mc.getSession().getAccessToken();
        return accessToken == null || accessToken.isBlank() ? null : accessToken;
    }

    public static MicrosoftLoginResult loginWithMicrosoftAccessToken(final String accessToken) {
        return loginWithMicrosoftAccessToken(accessToken, null);
    }

    public static MicrosoftLoginResult loginWithMicrosoftAccessToken(final String accessToken, @Nullable final String clientId) {
        if (accessToken == null || accessToken.isBlank()) {
            return MicrosoftLoginResult.failure("Access token is empty.");
        }

        try {
            final MicrosoftLoginResult minecraftSession = tryMinecraftAccessToken(accessToken, clientId);
            if (minecraftSession != null) {
                return minecraftSession;
            }

            final XboxToken xboxToken = authenticateXbox(accessToken);
            final XstsToken xstsToken = authorizeXsts(xboxToken.token());
            final MinecraftToken minecraftToken = loginWithXbox(xstsToken.token(), xboxToken.uhs());
            final MinecraftProfile profile = fetchMinecraftProfile(minecraftToken.accessToken());
            final UUID profileUuid = parseMinecraftUuid(profile.id());
            final Session session = new Session(
                    profile.name(),
                    profileUuid,
                    minecraftToken.accessToken(),
                    clientId == null || clientId.isBlank() ? Optional.empty() : Optional.of(clientId),
                    xboxToken.uhs() == null || xboxToken.uhs().isBlank() ? Optional.empty() : Optional.of(xboxToken.uhs())
            );

            return new MicrosoftLoginResult(
                    true,
                    "Logged in as " + profile.name(),
                    session,
                    profileUuid,
                    profile.name(),
                    minecraftToken.accessToken(),
                    xboxToken.uhs(),
                    null,
                    clientId
            );
        } catch (Exception e) {
            return MicrosoftLoginResult.failure(extractErrorMessage(e));
        }
    }

    public static MicrosoftLoginResult loginWithToken(final String token) {
        return loginWithToken(token, null);
    }

    public static MicrosoftLoginResult loginWithToken(final String token, @Nullable final String clientId) {
        final String effectiveClientId = clientId == null || clientId.isBlank() ? DEFAULT_CLIENT_ID : clientId.trim();
        final MicrosoftLoginResult microsoftResult = loginWithMicrosoftAccessToken(token, effectiveClientId);
        if (microsoftResult.success()) {
            return microsoftResult;
        }

        final MicrosoftLoginResult refreshResult = loginWithMicrosoftRefreshToken(effectiveClientId, token);
        if (refreshResult.success()) {
            return refreshResult;
        }

        return MicrosoftLoginResult.failure(
                "Token login failed. Tried Minecraft session token, Microsoft access token, and refresh token."
        );
    }

    public static MicrosoftLoginResult loginWithMicrosoftRefreshToken(final String clientId, final String refreshToken) {
        final String effectiveClientId = normalizeClientId(clientId);
        if (refreshToken == null || refreshToken.isBlank()) {
            return MicrosoftLoginResult.failure("Refresh token is empty.");
        }

        try {
            final String microsoftAccessToken = exchangeRefreshToken(effectiveClientId, refreshToken);
            final MicrosoftLoginResult result = loginWithMicrosoftAccessToken(microsoftAccessToken, effectiveClientId);
            return result.success()
                    ? new MicrosoftLoginResult(
                    true,
                    result.message(),
                    result.session(),
                    result.profileUuid(),
                    result.profileName(),
                    result.minecraftAccessToken(),
                    result.xuid(),
                    microsoftAccessToken,
                    effectiveClientId
            )
                    : result;
        } catch (Exception e) {
            return MicrosoftLoginResult.failure(extractErrorMessage(e));
        }
    }

    @Nullable
    private static MicrosoftLoginResult tryMinecraftAccessToken(final String token, @Nullable final String clientId) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            return null;
        }

        final JsonObject json = parseObject(response.body());
        final String id = requireString(json, "id", "Minecraft profile response is missing the UUID.");
        final String name = requireString(json, "name", "Minecraft profile response is missing the username.");
        final UUID profileUuid = parseMinecraftUuid(id);
        final Session session = new Session(
                name,
                profileUuid,
                token,
                clientId == null || clientId.isBlank() ? Optional.empty() : Optional.of(clientId),
                Optional.empty()
        );

        return new MicrosoftLoginResult(
                true,
                "Logged in as " + name,
                session,
                profileUuid,
                name,
                token,
                null,
                null,
                clientId
        );
    }

    public static void applyMinecraftSession(final MicrosoftLoginResult result) {
        if (result == null || !result.success() || result.session() == null) {
            return;
        }

        ((MinecraftClientAccessor) mc).setSession(result.session());

        if (OpalClient.getInstance() != null) {
            OpalClient.getInstance().setUser(new LocalUser(1, result.profileName(), UserRole.USER));
        }

        final ClientSocket socket = ClientSocket.getInstance();
        if (socket != null && socket.isConnected()) {
            socket.syncAccount();
        }
    }

    private static CompletableFuture<Boolean> requestHandoffAuthorization() {
        return CompletableFuture.completedFuture(true);
    }

    /**
     * @return true if authorization was successful
     */
    public static boolean requestHandoffAuthorizationOrStop() {
        return true;
    }

    private static String exchangeRefreshToken(final String clientId, final String refreshToken) throws IOException, InterruptedException {
        final String body = "client_id=" + encodeForm(clientId)
                + "&grant_type=refresh_token"
                + "&refresh_token=" + encodeForm(refreshToken)
                + "&scope=" + encodeForm("XboxLive.signin offline_access");

        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        final JsonObject json = parseObject(response.body());

        if (response.statusCode() / 100 != 2 || !json.has("access_token")) {
            throw new IOException(readErrorMessage(json, "Microsoft token exchange failed with HTTP " + response.statusCode()));
        }

        return json.get("access_token").getAsString();
    }

    private static XboxToken authenticateXbox(final String microsoftAccessToken) throws IOException, InterruptedException {
        final JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + microsoftAccessToken);

        final JsonObject payload = new JsonObject();
        payload.add("Properties", properties);
        payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        payload.addProperty("TokenType", "JWT");

        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                .timeout(Duration.ofSeconds(15))
                .header("x-xbl-contract-version", "1")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        final HttpResponse<String> httpResponse = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        final JsonObject response = parseObject(httpResponse.body());

        if (httpResponse.statusCode() / 100 != 2) {
            throw new IOException(readErrorMessage(response, "Xbox Live authentication failed with HTTP " + httpResponse.statusCode()));
        }

        final String token = requireString(response, "Token", "Xbox Live authentication did not return a token.");
        final String uhs = extractUserHash(response);
        return new XboxToken(token, uhs);
    }

    private static XstsToken authorizeXsts(final String xboxToken) throws IOException, InterruptedException {
        final JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");

        final JsonArray userTokens = new JsonArray();
        userTokens.add(xboxToken);
        properties.add("UserTokens", userTokens);

        final JsonObject payload = new JsonObject();
        payload.add("Properties", properties);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .timeout(Duration.ofSeconds(15))
                .header("x-xbl-contract-version", "1")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        final HttpResponse<String> httpResponse = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        final JsonObject response = parseObject(httpResponse.body());

        if (httpResponse.statusCode() / 100 != 2) {
            throw new IOException(readErrorMessage(response, "XSTS authorization failed with HTTP " + httpResponse.statusCode()));
        }

        final String token = requireString(response, "Token", "XSTS authorization did not return a token.");
        final String uhs = extractUserHash(response);
        return new XstsToken(token, uhs);
    }

    private static MinecraftToken loginWithXbox(final String xstsToken, @Nullable final String userHash) throws IOException, InterruptedException {
        final JsonObject payload = new JsonObject();
        payload.addProperty("identityToken", "XBL3.0 x=" + (userHash == null ? "" : userHash) + ";" + xstsToken);

        final JsonObject response = sendJson("https://api.minecraftservices.com/authentication/login_with_xbox", payload);
        final String accessToken = requireString(response, "access_token", "Minecraft login did not return an access token.");
        return new MinecraftToken(accessToken);
    }

    private static MinecraftProfile fetchMinecraftProfile(final String accessToken) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        final JsonObject json = parseObject(response.body());

        if (response.statusCode() / 100 != 2) {
            throw new IOException(readErrorMessage(json, "Minecraft profile request failed with HTTP " + response.statusCode()));
        }

        final String id = requireString(json, "id", "Minecraft profile response is missing the UUID.");
        final String name = requireString(json, "name", "Minecraft profile response is missing the username.");
        return new MinecraftProfile(id, name);
    }

    private static JsonObject sendJson(final String url, final JsonObject payload) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        final JsonObject json = parseObject(response.body());

        if (response.statusCode() / 100 != 2) {
            throw new IOException(readErrorMessage(json, "Request failed with HTTP " + response.statusCode()));
        }

        return json;
    }

    private static JsonObject parseObject(final String body) {
        final JsonElement element = JsonParser.parseString(body == null || body.isBlank() ? "{}" : body);
        return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static String requireString(final JsonObject object, final String key, final String fallback) throws IOException {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            final String value = object.get(key).getAsString();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        throw new IOException(fallback);
    }

    private static String extractUserHash(final JsonObject object) {
        try {
            final JsonObject displayClaims = object.getAsJsonObject("DisplayClaims");
            if (displayClaims != null && displayClaims.has("xui")) {
                final JsonArray xui = displayClaims.getAsJsonArray("xui");
                if (xui != null && !xui.isEmpty()) {
                    final JsonObject entry = xui.get(0).getAsJsonObject();
                    if (entry.has("uhs") && !entry.get("uhs").isJsonNull()) {
                        return entry.get("uhs").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String readErrorMessage(final JsonObject object, final String fallback) {
        if (object.has("error_description")) {
            return object.get("error_description").getAsString();
        }
        if (object.has("errorMessage")) {
            return object.get("errorMessage").getAsString();
        }
        if (object.has("Message")) {
            return object.get("Message").getAsString();
        }
        if (object.has("error")) {
            return object.get("error").getAsString();
        }
        if (object.has("XErr")) {
            return "Xbox error " + object.get("XErr").getAsString();
        }
        return fallback;
    }

    private static String extractErrorMessage(final Exception exception) {
        final String message = exception.getMessage();
        return message == null || message.isBlank() ? "Microsoft login failed." : message;
    }

    private static UUID parseMinecraftUuid(final String rawUuid) {
        final String dashed = rawUuid.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(dashed);
    }

    private static String normalizeClientId(final String clientId) {
        return clientId == null || clientId.isBlank() ? DEFAULT_CLIENT_ID : clientId.trim();
    }

    private static String encodeForm(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record MicrosoftLoginResult(
            boolean success,
            String message,
            @Nullable Session session,
            @Nullable UUID profileUuid,
            @Nullable String profileName,
            @Nullable String minecraftAccessToken,
            @Nullable String xuid,
            @Nullable String microsoftAccessToken,
            @Nullable String clientId
    ) {
        public static MicrosoftLoginResult failure(final String message) {
            return new MicrosoftLoginResult(false, message, null, null, null, null, null, null, null);
        }
    }

    private record XboxToken(String token, @Nullable String uhs) {
    }

    private record XstsToken(String token, @Nullable String uhs) {
    }

    private record MinecraftToken(String accessToken) {
    }

    private record MinecraftProfile(String id, String name) {
    }

}
