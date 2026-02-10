package org.blackum.blackaddons.features;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import org.blackum.blackaddons.config.ConfigManagerV2;
import org.blackum.blackaddons.util.ChatUtils;

import java.util.ArrayList;
import java.util.List;

import static org.blackum.blackaddons.util.MinecraftInstance.mc;

public class CommandUtils {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        ClientTickEvents.END_CLIENT_TICK.register(CommandUtils::onEndTick);

        ConfigManagerV2.data.knownAliases.forEach((name, command) -> {
            dispatcher.register(ClientCommandManager.literal(name)
                    .executes(context -> {
                        assert mc.player != null;
                        mc.player.connection.sendCommand(command);
                        return 1;
                    }));
        });
    }

    static void onEndTick(Minecraft client) {}

    static ArgumentBuilder<FabricClientCommandSource, ?> add = ClientCommandManager.literal("add")
            .then(ClientCommandManager.argument("alias", StringArgumentType.string())
            .then(ClientCommandManager.argument("real_command", StringArgumentType.string())
            .executes(ctx -> {
                String name = StringArgumentType.getString(ctx, "alias");
                String desc = StringArgumentType.getString(ctx, "real_command");

                ConfigManagerV2.data.knownAliases.put(name, desc);
                ConfigManagerV2.save();


                ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                    dispatcher.register(ClientCommandManager.literal(name).executes(context -> {
                        assert mc.player != null;
                        mc.player.connection.sendCommand(desc);
                        return 1;
                    }));
                });

                ChatUtils.send_debug("Added: " + name + " -> " + desc + " (swap lobbies to see changes)");

                return 1;
            })));

    static ArgumentBuilder<FabricClientCommandSource, ?> del = ClientCommandManager.literal("del")
            .then(ClientCommandManager.argument("alias", StringArgumentType.string())
            .suggests((ctx, builder) -> {
                List<String> existing = new ArrayList<>();

                ConfigManagerV2.data.knownAliases.forEach((alias, command) -> { existing.add(alias); });
                ConfigManagerV2.save();

                return SharedSuggestionProvider.suggest(existing, builder);
            })
            .executes(ctx -> {
                String name = StringArgumentType.getString(ctx, "alias");

                ConfigManagerV2.data.knownAliases.remove(name);
                ConfigManagerV2.save();

                ChatUtils.send_debug("Removed: " + name + " (swap lobbies to see changes)");
                return 1;
            }));

    static ArgumentBuilder<FabricClientCommandSource, ?> list = ClientCommandManager.literal("list")
            .executes(ctx -> {
                ChatUtils.send_debug("Aliases: ");
                ConfigManagerV2.data.knownAliases.forEach((alias, command) -> {
                    ChatUtils.send_debug(alias + " -> " + command);
                });

                return 1;
            });

    public static LiteralArgumentBuilder<FabricClientCommandSource> subcommand = ClientCommandManager.literal("commandaliases")
            .then(add)
            .then(del)
            .then(list);
}

