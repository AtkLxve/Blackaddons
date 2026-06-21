package org.blackum.blackaddons.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import com.google.gson.JsonObject;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.model.DungeonFloor;
import org.blackum.blackaddons.common.util.mc.ScoreboardUtils;
import org.blackum.blackaddons.common.util.mc.TabListUtils;
import org.blackum.blackaddons.feature.waypoint.AlignUtils;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.feature.chat.ChatActionManager;
import org.blackum.blackaddons.feature.dungeon.listener.DungeonJoinHandler;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMap;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMapSerializer;
import org.blackum.blackaddons.feature.dungeon.score.DungeonScore;
import org.blackum.blackaddons.feature.dungeon.tracker.SoloClearSampler;
import org.blackum.blackaddons.feature.dungeon.util.DungeonUtils;
import org.blackum.blackaddons.feature.party.PartyFinderManager;
import org.blackum.blackaddons.feature.rng.RngTracker;
import org.blackum.blackaddons.feature.profile.ProfileStateManager;
import org.blackum.blackaddons.feature.rotation.RotationManager;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.service.BotIntegration;
import org.blackum.blackaddons.service.MojangAuthService;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import java.net.URI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TestCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> node() {
        var testNode = ClientCommands.literal("test");

        testNode.then(ClientCommands.literal("DebugGui")
                .executes(ctx -> {
                    if (Blackaddons.guiOpener != null)
                        Blackaddons.guiOpener.run();
                    return 1;
                }));

        testNode.then(ClientCommands.literal("TestMenu")
                .executes(ctx -> {
                    if (Blackaddons.testMenuOpener != null)
                        Blackaddons.testMenuOpener.run();
                    return 1;
                }));

        testNode.then(ClientCommands.literal("score")
                .executes(ctx -> {
                    for (String line : DungeonScore.getScoreBreakdown()) {
                        ctx.getSource().sendFeedback(Component.literal(line));
                    }
                    return 1;
                }));

        testNode.then(ClientCommands.literal("tablist")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("§e--- Current Tablist ---"));
                    for (String line : TabListUtils.getTabListLines()) {
                        ctx.getSource().sendFeedback(Component.literal("§7- " + line));
                    }
                    return 1;
                }));

        testNode.then(ClientCommands.literal("resetcache")
                .executes(ctx -> {
                    ProfileStateManager.getInstance().clearAllCaches();
                    ctx.getSource().sendFeedback(Component.literal("All caches cleared."));
                    return 1;
                }));

        testNode.then(ClientCommands.literal("resetalign")
                .executes(ctx -> {
                    AlignUtils.resetSessionStats();
                    ctx.getSource().sendFeedback(Component.literal("Alignment session stats reset."));
                    return 1;
                }));

        testNode.then(ClientCommands.literal("dungeonforce")
                .executes(ctx -> {
                    LocationUtils.debugDungeonMode = !LocationUtils.debugDungeonMode;
                    ctx.getSource().sendFeedback(Component.literal("Debug dungeon mode: "
                            + (LocationUtils.debugDungeonMode ? "§aON" : "§cOFF")));
                    return 1;
                }));

        testNode.then(ClientCommands.literal("resetdungeon")
                .executes(ctx -> {
                    DungeonMap.reset();
                    ctx.getSource().sendFeedback(Component.literal("Dungeon map reset."));
                    return 1;
                }));

        testNode.then(ClientCommands.literal("rotate")
                .then(ClientCommands.argument("yaw", FloatArgumentType.floatArg(-180, 180))
                        .then(ClientCommands.argument("pitch", FloatArgumentType.floatArg(-90, 90))
                                .executes(ctx -> {
                                    float yaw = FloatArgumentType.getFloat(ctx, "yaw");
                                    float pitch = FloatArgumentType.getFloat(ctx, "pitch");
                                    RotationManager.getInstance().rotateTo(yaw, pitch);
                                    ctx.getSource().sendFeedback(Component.literal("Rotating to yaw=" + yaw + " pitch=" + pitch));
                                    return 1;
                                }))));

        testNode.then(ClientCommands.literal("rotateTo")
                .then(ClientCommands.argument("x", FloatArgumentType.floatArg())
                        .then(ClientCommands.argument("y", FloatArgumentType.floatArg())
                                .then(ClientCommands.argument("z", FloatArgumentType.floatArg())
                                        .executes(ctx -> {
                                            float x = FloatArgumentType.getFloat(ctx, "x");
                                            float y = FloatArgumentType.getFloat(ctx, "y");
                                            float z = FloatArgumentType.getFloat(ctx, "z");
                                            RotationManager.getInstance().rotateToBlock(x, y, z);
                                            ctx.getSource().sendFeedback(Component.literal("Rotating to block " + x + " " + y + " " + z));
                                            return 1;
                                        })))));

        testNode.then(ClientCommands.literal("rng")
                .then(ClientCommands.argument(Constants.CMD_ARG_TYPE, StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                new String[]{Constants.DROP_TYPE_RARE, Constants.DROP_TYPE_CRAZY, Constants.DROP_TYPE_PRAY},
                                builder))
                        .then(ClientCommands.argument(Constants.CMD_ARG_MAGIC_FIND, IntegerArgumentType.integer(0))
                                .then(ClientCommands.argument(Constants.CMD_ARG_ITEM, StringArgumentType.greedyString())
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

                                            Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.literal(fakeMessage));
                                            RngTracker.onChatMessage(Component.literal(fakeMessage));

                                            NotificationManager.addNotification("RNG Drop Tested",
                                                    item + " (" + typeArg + ")", NotificationType.SUCCESS);
                                            return 1;
                                        })))));

        testNode.then(ClientCommands.literal("GiveTNT")
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

        testNode.then(ClientCommands.literal("allnames")
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

        testNode.then(ClientCommands.literal("dungeonjoin")
                .then(ClientCommands.argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                        .executes(ctx -> {
                            String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                            String fakeMessage = "Party Finder > " + ign + " joined the dungeon group! (Berserk Level 1)";
                            Component component = Component.literal(fakeMessage);
                            Minecraft.getInstance().gui.getChat().addClientSystemMessage(component);
                            DungeonJoinHandler.onChatMessage(component);
                            ChatActionManager.getInstance().onChatMessage(component);
                            return 1;
                        })));

        testNode.then(ClientCommands.literal("testinvite")
                .then(ClientCommands.argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                        .executes(ctx -> {
                            String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                            String fakeMessage = "[VIP] " + ign
                                    + " has invited you to join their party!\nYou have 60 seconds to accept. Click here to join!";
                            Component component = Component.literal(fakeMessage);
                            Minecraft.getInstance().gui.getChat().addClientSystemMessage(component);
                            ChatActionManager.getInstance().onChatMessage(component);
                            return 1;
                        })));

        testNode.then(ClientCommands.literal("testjoin")
                .then(ClientCommands.argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                        .executes(ctx -> {
                            String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                            String fakeMessage = ign + " whispers to you: [BlackAddons] join party request - id:e1bc825d";
                            Component component = Component.literal(fakeMessage);
                            Minecraft.getInstance().gui.getChat().addClientSystemMessage(component);
                            PartyFinderManager.getInstance().onChatMessage(component);
                            return 1;
                        })
                        .then(ClientCommands.argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                                    String id = StringArgumentType.getString(ctx, "id");
                                    String fakeMessage = ign + " whispers to you: [BlackAddons] join party request - id:" + id;
                                    Component component = Component.literal(fakeMessage);
                                    Minecraft.getInstance().gui.getChat().addClientSystemMessage(component);
                                    PartyFinderManager.getInstance().onChatMessage(component);
                                    return 1;
                                }))));

        testNode.then(ClientCommands.literal("setid")
                .then(ClientCommands.argument("id", StringArgumentType.string())
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

        testNode.then(ClientCommands.literal("lbsave")
                .executes(ctx -> {
                    int n = SoloClearSampler.captureSample();
                    if (n > 0) {
                        ctx.getSource().sendFeedback(Component.literal(
                                "§a[lbsave] Saved sample #" + n + " → §7" + SoloClearSampler.getSamplesFile()));
                    } else {
                        ctx.getSource().sendFeedback(Component.literal(
                                "§c[lbsave] Failed to save sample (see logs)"));
                    }
                    return 1;
                }));

        testNode.then(ClientCommands.literal("lbsend")
                .executes(ctx -> runLbSend(ctx.getSource())));

        testNode.then(ClientCommands.literal("authcheck")
                .executes(ctx -> {
                    String serverId = MojangAuthService.generateServerId();
                    ctx.getSource().sendFeedback(Component.literal("§e[AuthCheck] Activating Mojang auth check..."));

                    MojangAuthService.joinServer(serverId).thenAccept(ok -> {
                        Minecraft.getInstance().execute(() -> {
                            if (ok) {
                                try {
                                    String player = Minecraft.getInstance().getUser().getName();
                                    String url = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username="
                                            + player + "&serverId=" + serverId;

                                    var linkComponent = Component.literal("§b§n[Click here to verify]")
                                            .withStyle(style -> style
                                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Open Mojang Session Verification in browser"))));

                                    ctx.getSource().sendFeedback(Component.literal("§a[AuthCheck] Successfully joined Mojang session! ")
                                            .append(linkComponent));
                                } catch (Exception e) {
                                    ctx.getSource().sendFeedback(Component.literal("§c[AuthCheck] Error creating verification link: " + e.getMessage()));
                                }
                            } else {
                                ctx.getSource().sendFeedback(Component.literal("§c[AuthCheck] Mojang auth check failed (joinServer returned false)."));
                            }
                        });
                    }).exceptionally(ex -> {
                        Minecraft.getInstance().execute(() -> {
                            ctx.getSource().sendFeedback(Component.literal("§c[AuthCheck] Mojang auth check failed with exception: " + ex.getMessage()));
                        });
                        return null;
                    });

                    return 1;
                }));

        return testNode;
    }

    private static int runLbSend(FabricClientCommandSource source) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getUser() == null) {
            source.sendFeedback(Component.literal("§c[lbsend] Minecraft not ready."));
            return 0;
        }

        DungeonFloor floor = LocationUtils.getCurrentFloor();
        String floorName = floor != null ? floor.getDisplayName() : "F7";
        if (!floorName.equals("F7") && !floorName.equals("M7")) {
            floorName = "F7";
        }

        List<String> rawScoreboard = new ArrayList<>(ScoreboardUtils.getSidebarLines());
        List<String> rawTablist = new ArrayList<>(TabListUtils.getRawTabListLines());
        List<String> cleanTablist = TabListUtils.getTabListLines();
        DungeonUtils.DungeonStats stats = DungeonUtils.parseDungeonStats(cleanTablist);
        Map<String, Integer> components = new LinkedHashMap<>(DungeonScore.getScoreComponents());

        String time = parseScoreboardTime(rawScoreboard);
        if (time == null) time = "01:30";
        final String normalizedTime = normalizeTime(time);

        boolean mimicKilled = DungeonScore.isMimicKilled() || stats.mimicKilled;
        boolean princeDefeated = DungeonScore.isPrinceKilled() || stats.princeKilled;

        final String player = mc.getUser().getName();
        final String playerUuid = mc.getUser().getProfileId().toString();
        final String submittedFloor = floorName;
        final int submittedSecrets = stats.secretsFound;
        final int submittedDeaths = stats.deaths;
        final int submittedCrypts = stats.crypts;
        final List<String> submittedPuzzles = new ArrayList<>(stats.completedPuzzles);
        final boolean submittedPrince = princeDefeated;
        final boolean submittedMimic = mimicKilled;
        final boolean needsVerification = false;
        final long enterClock = System.currentTimeMillis() - parseTimeMs(normalizedTime);
        final long clearClock = System.currentTimeMillis();
        final String serverId = MojangAuthService.generateServerId();
        final JsonObject mapData = DungeonMapSerializer.serialize();

        source.sendFeedback(Component.literal(
                "§e[lbsend] Sending " + submittedFloor + " " + normalizedTime + " for " + player + "..."));

        MojangAuthService.joinServer(serverId)
                .thenCompose(ok -> {
                    if (!ok) {
                        Blackaddons.LOGGER.warn("[lbsend] joinServer failed");
                        return CompletableFuture.<Boolean>completedFuture(false);
                    }
                    return BotIntegration.preVerifyMojang(player, playerUuid, serverId);
                })
                .thenCompose(preVerified -> {
                    Blackaddons.LOGGER.info("[lbsend] Mojang pre-verify: {}", preVerified);
                    return BotIntegration.sendSoloClear(player, playerUuid, submittedFloor, normalizedTime,
                            submittedSecrets, submittedDeaths, submittedCrypts,
                            submittedPuzzles, submittedPrince, submittedMimic, needsVerification,
                            rawScoreboard, rawTablist, components,
                            -1L, -1L, enterClock, clearClock, serverId, mapData);
                })
                .thenAccept(res -> mc.execute(() -> {
                    if (res != null) {
                        source.sendFeedback(Component.literal("§a[lbsend] Bot accepted submission."));
                    } else {
                        source.sendFeedback(Component.literal("§c[lbsend] Bot rejected or returned no body. Check bot logs."));
                    }
                }));

        return 1;
    }

    private static String parseScoreboardTime(List<String> rawLines) {
        java.util.regex.Pattern strip = java.util.regex.Pattern.compile("§.");
        java.util.regex.Pattern time = java.util.regex.Pattern.compile("(?i)Time Elapsed:\\s*([0-9][0-9msh:\\s]*s?)");
        for (String raw : rawLines) {
            String clean = raw == null ? "" : strip.matcher(raw).replaceAll("");
            java.util.regex.Matcher m = time.matcher(clean);
            if (m.find()) return m.group(1).trim();
        }
        return null;
    }

    private static String normalizeTime(String raw) {
        if (raw == null) return "00:00";
        if (raw.matches("\\d+:\\d+.*")) return raw;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:(\\d+)m)?\\s*(?:(\\d+)s)?").matcher(raw);
        if (m.find()) {
            int mins = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            int secs = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            return String.format("%02d:%02d", mins, secs);
        }
        return raw;
    }

    private static long parseTimeMs(String mmss) {
        try {
            String[] parts = mmss.split(":");
            int mins = Integer.parseInt(parts[0]);
            int secs = parts.length > 1 ? Integer.parseInt(parts[1].split("\\.")[0]) : 0;
            return (mins * 60L + secs) * 1000L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
