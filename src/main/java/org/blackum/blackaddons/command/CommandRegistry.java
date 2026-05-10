package org.blackum.blackaddons.command;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.module.AutoModule;

@AutoModule(order = 900)
public class CommandRegistry {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            Command<FabricClientCommandSource> openGui = ctx -> {
                if (Blackaddons.mainGuiOpener != null)
                    Blackaddons.mainGuiOpener.run();
                return 1;
            };

            for (String alias : new String[]{Constants.BASE_COMMAND, "black", "blackaddons"}) {
                var cmd = ClientCommandManager.literal(alias).executes(openGui);

                cmd.then(TestCommands.node());
                if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                    cmd.then(ProfileCommands.dailyNode());
                }
                cmd.then(ProfileCommands.pvNode());
                cmd.then(ChatCommands.ircNode());
                cmd.then(ChatCommands.actionTriggerNode("em"));
                cmd.then(ChatCommands.actionTriggerNode("editmode"));
                cmd.then(CommandUtils.subcommand);
                cmd.then(DungeonCommands.pfNode());
                cmd.then(ChatCommands.previewNode());
                cmd.then(DungeonCommands.leaderboardNode());
                cmd.then(MiscCommands.hudNode());
                cmd.then(MiscCommands.pingNode());

                dispatcher.register(cmd);
            }

            ChatCommands.registerStandaloneIrc(dispatcher);
            DungeonCommands.registerStandaloneLeaderboards(dispatcher);
            CommandUtils.register(dispatcher);
        });
    }
}
