package org.blackum.blackaddons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.util.mc.TabListUtils;
import org.blackum.blackaddons.feature.dungeon.tracker.SoloClearsTracker;
import org.blackum.blackaddons.gui.screen.feature.PartyFinderScreen;
import org.blackum.blackaddons.gui.screen.feature.SoloLeaderboardScreen;

import java.util.List;

public class DungeonCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> pfNode() {
        return ClientCommands.literal("pf").executes(ctx -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new PartyFinderScreen());
            }
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> leaderboardNode() {
        return ClientCommands.literal("leaderboard")
                .executes(ctx -> {
                    if (Blackaddons.screenOpener != null)
                        Blackaddons.screenOpener.accept(new SoloLeaderboardScreen("F7"));
                    return 1;
                })
                .then(ClientCommands.argument("floor", StringArgumentType.string())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                new String[]{"F7", "M7"}, builder))
                        .executes(ctx -> {
                            String floor = StringArgumentType.getString(ctx, "floor");
                            if (Blackaddons.screenOpener != null)
                                Blackaddons.screenOpener.accept(new SoloLeaderboardScreen(floor));
                            return 1;
                        }));
    }

    public static void registerStandaloneLeaderboards(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        for (String alias : new String[]{"leaderboard", "lb"}) {
            dispatcher.register(ClientCommands.literal(alias)
                    .executes(ctx -> {
                        if (Blackaddons.screenOpener != null)
                            Blackaddons.screenOpener.accept(new SoloLeaderboardScreen("F7"));
                        return 1;
                    })
                    .then(ClientCommands.argument("floor", StringArgumentType.string())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                    new String[]{"F7", "M7"}, builder))
                            .executes(ctx -> {
                                String floor = StringArgumentType.getString(ctx, "floor");
                                if (Blackaddons.screenOpener != null)
                                    Blackaddons.screenOpener.accept(new SoloLeaderboardScreen(floor));
                                return 1;
                            })));
        }
    }
    public static LiteralArgumentBuilder<FabricClientCommandSource> tabdebugNode() {
        return ClientCommands.literal("tabdebug").executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player == null) return;
                List<String> tab = TabListUtils.getTabListLines();
                mc.player.sendSystemMessage(Component.literal("§b§l=== Tab List (" + tab.size() + " lines) ==="));
                for (String line : tab) {
                    StringBuilder hex = new StringBuilder();
                    for (char c : line.toCharArray()) {
                        if (c > 127) hex.append(String.format("[U+%04X]", (int) c));
                    }
                    String suffix = hex.length() > 0 ? " §8" + hex : "";
                    mc.player.sendSystemMessage(Component.literal("§7| §r" + line + suffix));
                }
                List<String> footer = TabListUtils.getFooterLines();
                mc.player.sendSystemMessage(Component.literal("§b§l=== Footer (" + footer.size() + " lines) ==="));
                for (String line : footer) {
                    mc.player.sendSystemMessage(Component.literal("§7| §r" + line));
                }
            });
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> scdebugNode() {
        return ClientCommands.literal("scdebug").executes(ctx -> {
            Minecraft.getInstance().execute(SoloClearsTracker::dumpDebugInfo);
            return 1;
        });
    }
}
