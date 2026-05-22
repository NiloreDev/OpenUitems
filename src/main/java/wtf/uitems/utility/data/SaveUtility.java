package wtf.uitems.utility.data;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.ibm.icu.impl.Pair;
import wtf.uitems.client.OpalClient;
import wtf.uitems.client.binding.BindingService;
import wtf.uitems.client.binding.IBindable;
import wtf.uitems.client.binding.type.InputType;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.UnknownModuleException;
import wtf.uitems.client.feature.module.property.Property;
import wtf.uitems.utility.data.serializer.PairSerializer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static wtf.uitems.client.Constants.DIRECTORY;
public final class SaveUtility {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Pair.class, new PairSerializer())
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static final BindingService BINDING_SERVICE = OpalClient.getInstance().getBindRepository().getBindingService();
    private static final File CONFIGS_DIR = new File(DIRECTORY, "configs");
    private static final File CREDENTIALS_FILE = new File(DIRECTORY, "credentials.json");
    private static long lastSave = 0;
    private static final long SAVE_DELAY = 1000; // 1 second

    private SaveUtility() {
    }

    public static void saveLocalConfigDebounced(final String name) {
        if (System.currentTimeMillis() - lastSave > SAVE_DELAY) {
            lastSave = System.currentTimeMillis();
            wtf.uitems.utility.misc.Multithreading.runAsync(() -> saveLocalConfig(name));
        }
    }

    public static void saveBindings() {
        try {
            if (!DIRECTORY.exists()) {
                DIRECTORY.mkdir();
            }

            final File file = new File(DIRECTORY, "bindings.json");

            Files.writeString(
                    file.toPath(),
                    GSON.toJson(buildBindingsJson())
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadBindings() {
        try (final FileReader reader = new FileReader(new File(DIRECTORY, "bindings.json"))) {
            applyBindingsJson(JsonParser.parseReader(reader).getAsJsonArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static record Credentials(String username, String password) {}

    public static void saveCredentials(final String username, final String password) {
        try {
            if (!DIRECTORY.exists()) {
                DIRECTORY.mkdir();
            }
            final JsonObject obj = new JsonObject();
            obj.addProperty("username", username);
            obj.addProperty("password", password);
            Files.writeString(CREDENTIALS_FILE.toPath(), GSON.toJson(obj));
        } catch (IOException ignored) {
        }
    }

    public static Credentials loadCredentials() {
        try {
            if (!CREDENTIALS_FILE.exists()) {
                return null;
            }
            try (final FileReader reader = new FileReader(CREDENTIALS_FILE)) {
                final JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                final String username = obj.has("username") ? obj.get("username").getAsString() : "";
                final String password = obj.has("password") ? obj.get("password").getAsString() : "";
                if (username.isEmpty() || password.isEmpty()) {
                    return null;
                }
                return new Credentials(username, password);
            }
        } catch (Exception e) {
            // Corrupted or incompatible credentials file should not break verify screen rendering.
            deleteCredentials();
            return null;
        }
    }

    public static void deleteCredentials() {
        if (CREDENTIALS_FILE.exists()) {
            // noinspection ResultOfMethodCallIgnored
            CREDENTIALS_FILE.delete();
        }
    }

    public static void saveConfig(final String name) {
        saveLocalConfig(name);
    }

    public static boolean loadConfig(final String jsonString) {
        try {
            final JsonElement root = JsonParser.parseString(jsonString);
            final List<?> jsonModules;

            if (root.isJsonObject()) {
                final JsonObject rootObject = root.getAsJsonObject();
                final JsonArray modulesArray = rootObject.has("modules") && rootObject.get("modules").isJsonArray()
                        ? rootObject.getAsJsonArray("modules")
                        : new JsonArray();
                jsonModules = GSON.fromJson(modulesArray, List.class);

                if (rootObject.has("bindings") && rootObject.get("bindings").isJsonArray()) {
                    BINDING_SERVICE.getBindingMap().clear();
                    applyBindingsJson(rootObject.getAsJsonArray("bindings"));
                }
            } else {
                jsonModules = GSON.fromJson(root, List.class);
            }

            for (final Object jsonModuleObj : jsonModules) {
                final LinkedTreeMap<?, ?> jsonModule = (LinkedTreeMap<?, ?>) jsonModuleObj;
                final String jsonModuleID = (String) jsonModule.get("name");
                final Boolean jsonEnabled = (Boolean) jsonModule.get("enabled");
                final Boolean jsonVisible = (Boolean) jsonModule.get("visible");
                final List<?> jsonProperties = (List<?>) jsonModule.get("properties");

                for (final Module clientModule : OpalClient.getInstance().getModuleRepository().getModules()) {
                    if (jsonModuleID.equals(clientModule.getId())) {

                        if (jsonEnabled != null && jsonEnabled != clientModule.isEnabled()) {
                            clientModule.setEnabled(jsonEnabled);
                        }
                        if (jsonVisible != null && jsonVisible != clientModule.isVisible()) {
                            clientModule.setVisible(jsonVisible);
                        }

                        for (final Object jsonPropertyObj : jsonProperties) {
                            final LinkedTreeMap<?, ?> jsonProperty = (LinkedTreeMap<?, ?>) jsonPropertyObj;
                            final String propertyName = (String) jsonProperty.get("name");
                            final Object propertyValue = jsonProperty.get("value");

                            for (final Property<?> clientProperty : clientModule.getPropertyList()) {
                                if (propertyName.equals(clientProperty.getId())) {
                                    clientProperty.applyValue(propertyValue);
                                }
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void saveLocalConfig() {
        try {
            if (!DIRECTORY.exists()) {
                DIRECTORY.mkdir();
            }
            final File file = new File(DIRECTORY, "config.json");
            Files.writeString(
                    file.toPath(),
                    GSON.toJson(buildConfigJson())
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveLocalConfig(final String name) {
        try {
            if (!CONFIGS_DIR.exists()) {
                CONFIGS_DIR.mkdirs();
            }
            final File file = new File(CONFIGS_DIR, name.toLowerCase() + ".json");
            Files.writeString(
                    file.toPath(),
                    GSON.toJson(buildConfigJson())
            );
            System.out.println("Config saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean loadLocalConfig(final String name) {
        try {
            final File file = new File(CONFIGS_DIR, name.toLowerCase() + ".json");
            if (file.exists()) {
                final String json = Files.readString(file.toPath());
                return loadConfig(json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteLocalConfig(final String name) {
        final File file = new File(CONFIGS_DIR, name.toLowerCase() + ".json");
        return file.exists() && file.delete();
    }

    public static List<String> listLocalConfigs() {
        final List<String> result = new ArrayList<>();
        if (CONFIGS_DIR.exists() && CONFIGS_DIR.isDirectory()) {
            final File[] files = CONFIGS_DIR.listFiles((dir, filename) -> filename.endsWith(".json"));
            if (files != null) {
                for (final File f : files) {
                    final String n = f.getName();
                    result.add(n.substring(0, n.length() - ".json".length()));
                }
            }
        }
        return result;
    }

    public static void loadLocalConfigIfExists() {
        try {
            final File defaultNamed = new File(CONFIGS_DIR, "default.json");
            if (defaultNamed.exists()) {
                final String json = Files.readString(defaultNamed.toPath());
                loadConfig(json);
                return;
            }
            final File legacy = new File(DIRECTORY, "config.json");
            if (legacy.exists()) {
                final String json = Files.readString(legacy.toPath());
                loadConfig(json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonObject buildConfigJson() {
        final JsonObject root = new JsonObject();
        root.add("modules", GSON.toJsonTree(OpalClient.getInstance().getModuleRepository().getModules()));
        root.add("bindings", buildBindingsJson());
        return root;
    }

    private static JsonArray buildBindingsJson() {
        final JsonArray bindingsArray = new JsonArray();
        for (final Pair<Integer, InputType> binding : BINDING_SERVICE.getBindingMap().keySet()) {
            final JsonObject bindingJson = new JsonObject();
            bindingJson.addProperty("keyCode", binding.first);

            final JsonArray bindablesArray = new JsonArray();
            for (IBindable bindable : BINDING_SERVICE.getBindingMap().get(binding)) {
                if (bindable instanceof Module module) {
                    final JsonObject moduleJson = new JsonObject();
                    moduleJson.addProperty("module", module.getId());
                    bindablesArray.add(moduleJson);
                } else if (bindable instanceof Config config) {
                    final JsonObject configJson = new JsonObject();
                    configJson.addProperty("config", config.getName());
                    bindablesArray.add(configJson);
                }
            }

            bindingJson.add("bindables", bindablesArray);
            bindingsArray.add(bindingJson);
        }
        return bindingsArray;
    }

    private static void applyBindingsJson(final JsonArray bindingsArray) {
        for (final JsonElement bindingElement : bindingsArray) {
            final JsonObject bindingJson = bindingElement.getAsJsonObject();

            final int keyCode = bindingJson.get("keyCode").getAsInt();
            final InputType inputType = keyCode < 10 ? InputType.MOUSE : InputType.KEYBOARD;

            final JsonArray bindablesArray = bindingJson.getAsJsonArray("bindables");
            for (final JsonElement bindableElement : bindablesArray) {
                final JsonObject bindableJson = bindableElement.getAsJsonObject();

                try {
                    if (bindableJson.has("module")) {
                        final String moduleID = bindableJson.get("module").getAsString();
                        final Module module = OpalClient.getInstance().getModuleRepository().getModule(moduleID);
                        BINDING_SERVICE.register(keyCode, module, inputType);
                    } else if (bindableJson.has("config")) {
                        final String configName = bindableJson.get("config").getAsString();
                        final Config config = new Config(configName);
                        BINDING_SERVICE.register(keyCode, config, inputType);
                    }
                } catch (UnknownModuleException e) {
                    System.err.println("Skipping unknown module binding: " + e.getMessage());
                }
            }
        }
    }

}
