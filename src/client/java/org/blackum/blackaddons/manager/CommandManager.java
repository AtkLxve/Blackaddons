package org.blackum.blackaddons.manager;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.features.CommandUtils;
import org.blackum.blackaddons.features.RngTracker;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.util.BotIntegration;
import org.blackum.blackaddons.util.ProfileStateManager;

public class CommandManager {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            Command<FabricClientCommandSource> openGui = ctx -> {
                if (Blackaddons.mainGuiOpener != null)
                    Blackaddons.mainGuiOpener.run();
                return 1;
            };

            var testNode = ClientCommandManager.literal("test");

            testNode.then(ClientCommandManager.literal("DebugGui")
                    .executes(ctx -> {
                        if (Blackaddons.guiOpener != null)
                            Blackaddons.guiOpener.run();
                        return 1;
                    }));

            testNode.then(ClientCommandManager.literal("TestMenu")
                    .executes(ctx -> {
                        if (Blackaddons.testMenuOpener != null)
                            Blackaddons.testMenuOpener.run();
                        return 1;
                    }));

            testNode.then(ClientCommandManager.literal("rng")
                    .then(ClientCommandManager.argument("type", StringArgumentType.string())
                            .suggests((context, builder) -> SharedSuggestionProvider
                                    .suggest(new String[] { "rare", "crazy", "pray" }, builder))
                            .then(ClientCommandManager.argument("magic_find", IntegerArgumentType.integer(0))
                                    .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                String typeArg = StringArgumentType.getString(context, "type")
                                                        .toLowerCase();
                                                int mf = IntegerArgumentType.getInteger(context, "magic_find");
                                                String item = StringArgumentType.getString(context, "item");

                                                String typePrefix = "§6§lRARE";
                                                if (typeArg.equals("crazy"))
                                                    typePrefix = "§d§lCRAZY RARE";
                                                else if (typeArg.equals("pray"))
                                                    typePrefix = "§5§lPRAY TO RNGESUS";

                                                String fakeMessage = typePrefix + " DROP! §r§f" + item + " §r§b(+§r§b"
                                                        + mf + "% §r§b✯ Magic Find§r§b)";

                                                Minecraft.getInstance().gui.getChat()
                                                        .addMessage(Component.literal(fakeMessage));
                                                RngTracker.onChatMessage(Component.literal(fakeMessage));

                                                NotificationManager.addNotification("RNG Drop Tested",
                                                        item + " (" + typeArg + ")", NotificationType.SUCCESS);
                                                return 1;
                                            })))));

            testNode.then(ClientCommandManager.literal("GiveTNT")
                    .executes(ctx -> {
                        Minecraft client = Minecraft.getInstance();
                        var player = client.player;
                        if (player != null) {
                            var connection = player.connection;
                            if (connection != null) {
                                connection.sendCommand("give @s tnt[custom_name='\"Superboom TNT\"'] 64");
                                connection.sendCommand("give @s tnt[custom_name='\"Infinityboom TNT\"'] 64");
                                connection.sendCommand(
                                        "give @s repeating_command_block[block_entity_data={id:\"minecraft:command_block\",Command:\"setblock ~ ~1 ~ cracked_stone_bricks\",auto:1b}] 1");
                                connection.sendCommand(
                                        "give @s repeating_command_block[block_entity_data={id:\"minecraft:command_block\",Command:\"setblock ~ ~1 ~ smooth_stone_slab\",auto:1b}] 1");
                            }
                        }
                        return 1;
                    }));

            var pvNode = ClientCommandManager.literal("pv")
                    .executes(ctx -> {
                        String player = Minecraft.getInstance().getUser().getName();
                        ProfileStateManager.getInstance().loadProfileAndOpen(player, false);
                        return 1;
                    })
                    .then(ClientCommandManager.argument("ign", StringArgumentType.string())
                            .executes(ctx -> {
                                String player = StringArgumentType.getString(ctx, "ign");
                                ProfileStateManager.getInstance().loadProfileAndOpen(player, false);
                                return 1;
                            })
                            .then(ClientCommandManager.literal("force")
                                    .executes(ctx -> {
                                        String player = StringArgumentType.getString(ctx, "ign");
                                        ProfileStateManager.getInstance().loadProfileAndOpen(player, true);
                                        return 1;
                                    })));

            var dailyNode = ClientCommandManager.literal("daily")
                    .executes(ctx -> {
                        String player = Minecraft.getInstance().getUser().getName();
                        NotificationManager.addNotification("Daily Sync", "Syncing stats with bot...",
                                NotificationType.INFO);

                        BotIntegration.sendDailySync(player).thenAccept(success -> {
                            if (success) {
                                NotificationManager.addNotification("Daily Sync", "Stats synced successfully!",
                                        NotificationType.SUCCESS);
                            } else {
                                NotificationManager.addNotification("Daily Sync", "Failed to sync stats.",
                                        NotificationType.ERROR);
                            }
                        });
                        return 1;
                    });

            CommandUtils.register(dispatcher);
            for (String alias : new String[] { "ba", "black", "blackaddons" }) {
                var cmd = ClientCommandManager.literal(alias).executes(openGui);
                cmd.then(testNode);
                cmd.then(pvNode);
                cmd.then(dailyNode);
                cmd.then(CommandUtils.subcommand);
                dispatcher.register(cmd);
            }
        });
    }
}
