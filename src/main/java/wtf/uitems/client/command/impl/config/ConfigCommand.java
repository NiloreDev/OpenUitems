package wtf.uitems.client.command.impl.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.uitems.client.command.Command;
import wtf.uitems.client.command.arguments.ConfigArgumentType;
import wtf.uitems.utility.data.SaveUtility;
import wtf.uitems.utility.misc.chat.ChatUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class ConfigCommand extends Command {

    public ConfigCommand() {
        super("Config", "Interacts with configs.", "c");
    }

    @Override
    protected void onCommand(final LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("save").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();

            SaveUtility.saveLocalConfig(configName);
            ChatUtility.success("Saved config '" + configName + "' locally.");

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("list").executes(context -> {
            final var names = SaveUtility.listLocalConfigs();
            if (names.isEmpty()) {
                ChatUtility.print("No local configs found.");
            } else {
                ChatUtility.print("Configs (" + names.size() + "): " + String.join(", ", names));
            }

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("load").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();

            if (SaveUtility.loadLocalConfig(configName)) {
                ChatUtility.success("Loaded config '" + configName + "'.");
            } else {
                ChatUtility.error("Failed to load config '" + configName + "'.");
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("delete").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();

            if (SaveUtility.deleteLocalConfig(configName)) {
                ChatUtility.success("Deleted config '" + configName + "'.");
            } else {
                ChatUtility.error("Config '" + configName + "' does not exist.");
            }

            return SINGLE_SUCCESS;
        })));
    }
}
