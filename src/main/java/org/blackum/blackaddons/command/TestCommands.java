package org.blackum.blackaddons.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.feature.waypoint.AlignUtils;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.feature.chat.ChatActionManager;
import org.blackum.blackaddons.feature.dungeon.listener.DungeonJoinHandler;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMap;
import org.blackum.blackaddons.feature.party.PartyFinderManager;
import org.blackum.blackaddons.feature.rng.RngTracker;
import org.blackum.blackaddons.feature.rotation.RotationManager;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;

public class TestCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> node() {
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

        testNode.then(ClientCommandManager.literal("resetalign")
                .executes(ctx -> {
                    AlignUtils.resetSessionStats();
                    ctx.getSource().sendFeedback(Component.literal("Alignment session stats reset."));
                    return 1;
                }));

        testNode.then(ClientCommandManager.literal("dungeonforce")
                .executes(ctx -> {
                    LocationUtils.debugDungeonMode = !LocationUtils.debugDungeonMode;
                    ctx.getSource().sendFeedback(Component.literal("Debug dungeon mode: "
                            + (LocationUtils.debugDungeonMode ? "§aON" : "§cOFF")));
                    return 1;
                }));

        testNode.then(ClientCommandManager.literal("resetdungeon")
                .executes(ctx -> {
                    DungeonMap.reset();
                    ctx.getSource().sendFeedback(Component.literal("Dungeon map reset."));
                    return 1;
                }));

        testNode.then(ClientCommandManager.literal("rotate")
                .then(ClientCommandManager.argument("yaw", FloatArgumentType.floatArg(-180, 180))
                        .then(ClientCommandManager.argument("pitch", FloatArgumentType.floatArg(-90, 90))
                                .executes(ctx -> {
                                    float yaw = FloatArgumentType.getFloat(ctx, "yaw");
                                    float pitch = FloatArgumentType.getFloat(ctx, "pitch");
                                    RotationManager.getInstance().rotateTo(yaw, pitch);
                                    ctx.getSource().sendFeedback(Component.literal("Rotating to yaw=" + yaw + " pitch=" + pitch));
                                    return 1;
                                }))));

        testNode.then(ClientCommandManager.literal("rotateTo")
                .then(ClientCommandManager.argument("x", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("y", FloatArgumentType.floatArg())
                                .then(ClientCommandManager.argument("z", FloatArgumentType.floatArg())
                                        .executes(ctx -> {
                                            float x = FloatArgumentType.getFloat(ctx, "x");
                                            float y = FloatArgumentType.getFloat(ctx, "y");
                                            float z = FloatArgumentType.getFloat(ctx, "z");
                                            RotationManager.getInstance().rotateToBlock(x, y, z);
                                            ctx.getSource().sendFeedback(Component.literal("Rotating to block " + x + " " + y + " " + z));
                                            return 1;
                                        })))));

        testNode.then(ClientCommandManager.literal("rng")
                .then(ClientCommandManager.argument(Constants.CMD_ARG_TYPE, StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                new String[]{Constants.DROP_TYPE_RARE, Constants.DROP_TYPE_CRAZY, Constants.DROP_TYPE_PRAY},
                                builder))
                        .then(ClientCommandManager.argument(Constants.CMD_ARG_MAGIC_FIND, IntegerArgumentType.integer(0))
                                .then(ClientCommandManager.argument(Constants.CMD_ARG_ITEM, StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String typeArg = StringArgumentType.getString(context, Constants.CMD_ARG_TYPE).toLowerCase();
                                            int mf = IntegerArgumentType.getInteger(context, Constants.CMD_ARG_MAGIC_FIND);
                                            String item = StringArgumentType.getString(context, Constants.CMD_ARG_ITEM);

                                            String typePrefix = ChatFormatting.GOLD + "" + ChatFormatting.BOLD + "RARE";
                                            if (typeArg.equals(Constants.DROP_TYPE_CRAZY))
                                                typePrefix = ChatFormatting.LIGHT_PURPLE + "" + ChatFormatting.BOLD + "CRAZY RARE";
                                            else if (typeArg.equals(Constants.DROP_TYPE_PRAY))
                                                typePrefix = ChatFormatting.DARK_PURPLE + "" + ChatFormatting.BOLD + "PRAY TO RNGESUS";

                                            String fakeMessage = typePrefix + " DROP! "
                                                    + ChatFormatting.RESET + "" + ChatFormatting.WHITE + item + " "
                                                    + ChatFormatting.RESET + "" + ChatFormatting.AQUA + "(+"
                                                    + ChatFormatting.RESET + "" + ChatFormatting.AQUA + mf + "% "
                                                    + ChatFormatting.RESET + "" + ChatFormatting.AQUA + "✯ Magic Find"
                                                    + ChatFormatting.RESET + "" + ChatFormatting.AQUA + ")";

                                            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(fakeMessage));
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
                            connection.sendCommand("give @s repeating_command_block[block_entity_data={id:\"minecraft:command_block\",Command:\"setblock ~ ~1 ~ cracked_stone_bricks\",auto:1b}] 1");
                            connection.sendCommand("give @s repeating_command_block[block_entity_data={id:\"minecraft:command_block\",Command:\"setblock ~ ~1 ~ smooth_stone_slab\",auto:1b}] 1");
                        }
                    }
                    return 1;
                }));

        testNode.then(ClientCommandManager.literal("allnames")
                .executes(ctx -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.level != null && client.getConnection() != null) {
                        client.execute(() -> {
                            net.minecraft.world.scores.Scoreboard scoreboard = client.level.getScoreboard();
                            net.minecraft.world.scores.Objective obj = scoreboard.getObjective("allNamesObj");
                            if (obj == null) {
                                obj = scoreboard.addObjective("allNamesObj",
                                        net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
                                        Component.literal("Test"),
                                        net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER,
                                        true, null);
                            }
                            scoreboard.setDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR, obj);

                            int score = 0;
                            for (net.minecraft.client.multiplayer.PlayerInfo info : client.getConnection().getOnlinePlayers()) {
                                String name = info.getProfile().name();
                                scoreboard.getOrCreatePlayerScore(() -> name, obj).set(score++);
                            }

                            NotificationManager.addNotification("Test",
                                    "Created scoreboard with all online players.", NotificationType.SUCCESS);
                        });
                    }
                    return 1;
                }));

        testNode.then(ClientCommandManager.literal("dungeonjoin")
                .then(ClientCommandManager.argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                        .executes(ctx -> {
                            String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                            String fakeMessage = "Party Finder > " + ign + " joined the dungeon group! (Berserk Level 1)";
                            Component component = Component.literal(fakeMessage);
                            Minecraft.getInstance().gui.getChat().addMessage(component);
                            DungeonJoinHandler.onChatMessage(component);
                            ChatActionManager.getInstance().onChatMessage(component);
                            return 1;
                        })));

        testNode.then(ClientCommandManager.literal("testinvite")
                .then(ClientCommandManager.argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                        .executes(ctx -> {
                            String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                            String fakeMessage = "[VIP] " + ign
                                    + " has invited you to join their party!\nYou have 60 seconds to accept. Click here to join!";
                            Component component = Component.literal(fakeMessage);
                            Minecraft.getInstance().gui.getChat().addMessage(component);
                            ChatActionManager.getInstance().onChatMessage(component);
                            return 1;
                        })));

        testNode.then(ClientCommandManager.literal("testjoin")
                .then(ClientCommandManager.argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                        .executes(ctx -> {
                            String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                            String fakeMessage = ign + " whispers to you: [BlackAddons] join party request - id:e1bc825d";
                            Component component = Component.literal(fakeMessage);
                            Minecraft.getInstance().gui.getChat().addMessage(component);
                            PartyFinderManager.getInstance().onChatMessage(component);
                            return 1;
                        })
                        .then(ClientCommandManager.argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                                    String id = StringArgumentType.getString(ctx, "id");
                                    String fakeMessage = ign + " whispers to you: [BlackAddons] join party request - id:" + id;
                                    Component component = Component.literal(fakeMessage);
                                    Minecraft.getInstance().gui.getChat().addMessage(component);
                                    PartyFinderManager.getInstance().onChatMessage(component);
                                    return 1;
                                }))));

        testNode.then(ClientCommandManager.literal("setid")
                .then(ClientCommandManager.argument("id", StringArgumentType.string())
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            try {
                                java.lang.reflect.Field field = PartyFinderManager.getInstance().getClass()
                                        .getDeclaredField("currentPartyId");
                                field.setAccessible(true);
                                field.set(PartyFinderManager.getInstance(), id);
                                NotificationManager.addNotification("Test", "Set party ID to " + id,
                                        NotificationType.SUCCESS);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return 1;
                        })));

        return testNode;
    }
}
