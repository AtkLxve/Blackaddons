package org.blackum.blackaddons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.util.mc.TabListUtils;
import org.blackum.blackaddons.feature.dungeon.score.DungeonScore;
import org.blackum.blackaddons.gui.screen.feature.PartyFinderScreen;
import org.blackum.blackaddons.gui.screen.feature.SoloLeaderboardScreen;

public class DungeonCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> pfNode() {
        return ClientCommandManager.literal("pf").executes(ctx -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new PartyFinderScreen());
            }
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> scoreNode() {
        return ClientCommandManager.literal("score").executes(ctx -> {
            for (String line : DungeonScore.getScoreBreakdown()) {
                ctx.getSource().sendFeedback(Component.literal(line));
            }
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> tablistNode() {
        return ClientCommandManager.literal("tablist").executes(ctx -> {
            ctx.getSource().sendFeedback(Component.literal("§e--- Current Tablist ---"));
            for (String line : TabListUtils.getTabListLines()) {
                ctx.getSource().sendFeedback(Component.literal("§7- " + line));
            }
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> leaderboardNode() {
        return ClientCommandManager.literal("leaderboard")
                .executes(ctx -> {
                    if (Blackaddons.screenOpener != null)
                        Blackaddons.screenOpener.accept(new SoloLeaderboardScreen("F7"));
                    return 1;
                })
                .then(ClientCommandManager.argument("floor", StringArgumentType.string())
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
            dispatcher.register(ClientCommandManager.literal(alias)
                    .executes(ctx -> {
                        if (Blackaddons.screenOpener != null)
                            Blackaddons.screenOpener.accept(new SoloLeaderboardScreen("F7"));
                        return 1;
                    })
                    .then(ClientCommandManager.argument("floor", StringArgumentType.string())
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
}
