package juggernighti.mc.home;

import com.google.gson.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class HomeMod implements ModInitializer {

    public static final String MOD_ID = "home";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Path CONFIG_DIR = Path.of("config/juggernighti/home/");
    private static final Path SAVE_FILE = CONFIG_DIR.resolve("player_homes.json");

    // Store home locations keyed by player UUID
    private HashMap<String, String> playerHomes = new HashMap<>();

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


    @Override
    public void onInitialize() {
        try {
            LOGGER.info("juggernighti.mc.home.HomeMod started");
            loadPlayerHomes();

            // Register commands
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                dispatcher.register(literal("home")
                        .then(literal("set")
                                .then(argument("homeName", StringArgumentType.string())
                                        .executes(this::setHomeCommand)))
                        .then(literal("remove")
                                .then(argument("homeName", StringArgumentType.string())
                                        .executes(this::removeHomeCommand)))
                        .then(argument("homeName", StringArgumentType.string())
                                .executes(this::homeCommand))
                        .executes(this::listCommand)
                );
            });
        } catch (Error e) {
            LOGGER.error("Fail:", e);
        }
    }

    // Command to set home
    private int setHomeCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            String homeName = StringArgumentType.getString(context, "homeName");

            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                source.sendError(Text.literal("This command can only be used by a player.")
                        .styled(style -> style.withColor(Formatting.RED))
                );
                return 0;
            }

            //UUID playerId = player.getUuid();
            BlockPos playerPos = player.getBlockPos();


            //World world = player.getWorld();
            boolean isTeleported = player.teleport(playerPos.getX() + 0.5D, playerPos.getY() + 1.5D, playerPos.getZ() + 0.5D, true);
            if (isTeleported) {
                final String pos = playerPos.getX() + ";" + playerPos.getY() + ";" + playerPos.getZ() + ";";

                // Get or create the player's homes map
                playerHomes.put(homeName, pos);
                player.sendMessage(Text.literal("Home '" + homeName + "' set at: " + playerPos.toShortString())
                        .styled(style -> style.withColor(Formatting.GOLD))
                );

                savePlayerHomes();
            } else {
                player.sendMessage(Text.literal("ERROR: can't create home '" + homeName + "' at: " + playerPos.toShortString())
                        .styled(style -> style.withColor(Formatting.RED))
                );
            }
            return Command.SINGLE_SUCCESS;
        } catch (Error e) {
            LOGGER.error("Fail:", e);
        }
        return 2;
    }

    // Command to set home
    private int removeHomeCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            String homeName = StringArgumentType.getString(context, "homeName");

            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                source.sendError(Text.literal("This command can only be used by a player.")
                        .styled(style -> style.withColor(Formatting.RED))
                );
                return 0;
            }

            String message = playerHomes.remove(homeName);
            savePlayerHomes();
            player.sendMessage(Text.literal("Deleted home:" + homeName + ", " + message)
                    .styled(style -> style.withColor(Formatting.GOLD))
            );
            return Command.SINGLE_SUCCESS;
        } catch (Error e) {
            LOGGER.error("Fail:", e);
        }
        return 2;
    }

    // Command to teleport home
    private int homeCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            String homeName = StringArgumentType.getString(context, "homeName");

            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                source.sendError(Text.literal("This command can only be used by a player.")
                        .styled(style -> style.withColor(Formatting.RED))
                );
                return 0;
            }

            //UUID playerId = player.getUuid();
            // Check if the player has the specified home
            if (!playerHomes.containsKey(homeName)) {
                player.sendMessage(Text.literal("No home found with the name '"
                        + homeName + "'. Use /home set <name> first.")
                        .styled(style -> style.withColor(Formatting.RED))
                );
                return 0;
            }

            BlockPos homePos = convertBlockPos(playerHomes.get(homeName));

            //World world = player.getWorld();
            boolean isTeleported = player.teleport(homePos.getX() + 0.5D, homePos.getY() + 1.5D, homePos.getZ() + 0.5D, true);
            if (isTeleported) {
                player.sendMessage(Text.literal("Teleported to home '" + homeName + "' at: " + homePos.toShortString())
                        .styled(style -> style.withColor(Formatting.GOLD))
                );
            } else {
                player.sendMessage(Text.literal("ERROR can't teleported to home '" + homeName + "' at: " + homePos.toShortString())
                        .styled(style -> style.withColor(Formatting.RED))
                );
            }
            return Command.SINGLE_SUCCESS;
        } catch (Error e) {
            LOGGER.error("Fail:", e);
        }
        return 2;
    }

    private int listCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();

            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                source.sendError(Text.literal("This command can only be used by a player.")
                        .styled(style -> style.withColor(Formatting.RED))
                );
                return 0;
            }

            StringBuilder message = new StringBuilder("----------\n");
            for (String home : playerHomes.keySet()) {
                message.append(home).append("\n");
            }
            message.append("----------");
            player.sendMessage(Text.literal(message.toString()), false);

            return Command.SINGLE_SUCCESS;
        } catch (Error e) {
            LOGGER.error("Fail:", e);
        }
        return 2;
    }


    // Save player homes to a file
    private void savePlayerHomes() {
        try {
            LOGGER.info(SAVE_FILE.toAbsolutePath().toString());

            // Ensure the directory exists
            if (!Files.exists(CONFIG_DIR)) {
                try {
                    Files.createDirectories(CONFIG_DIR);
                } catch (IOException e) {
                    LOGGER.error("Failed to created save file!", e);
                }
            }

            try (Writer writer = Files.newBufferedWriter(SAVE_FILE.toAbsolutePath())) {
                GSON.toJson(playerHomes, writer);
            } catch (IOException e) {
                LOGGER.error("Failed to save players!", e);
            }
        } catch (Error e) {
            LOGGER.error("Fail:", e);
        }
    }

    // Load player homes from a file
    private void loadPlayerHomes() {
        try {
            if (Files.exists(SAVE_FILE)) {
                HashMap<String, String> saveHomes = new HashMap<>();
                try (Reader reader = Files.newBufferedReader(SAVE_FILE)) {
                    saveHomes = GSON.fromJson(reader, HashMap.class);
                } catch (IOException e) {
                    LOGGER.error("Failed to load players!", e);
                }

                for (String home : saveHomes.keySet()) {
                    String values = saveHomes.get(home);
                    playerHomes.put(home, values);
                }
            }
        } catch (Error e) {
            LOGGER.error("Fail:", e);
        }
    }

    private BlockPos convertBlockPos(String posString) {
        String[] pos = posString.split(";");
        return new BlockPos(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2]));
    }
}
