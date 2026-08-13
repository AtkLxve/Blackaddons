package org.blackum.blackaddons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.feature.cheat.CritterESP;

public final class CritterCommands {
    private CritterCommands() {}

    public static LiteralArgumentBuilder<FabricClientCommandSource> node() {
        return ClientCommands.literal("critesp").executes(ctx -> {
            ConfigManager.data.CritterEspEnabled = !ConfigManager.data.CritterEspEnabled;
            ConfigManager.save();
            if (ConfigManager.data.CritterEspEnabled) {
                ChatUtils.send_debug("Critter ESP: §aON");
                CritterESP.scanAndReport();
            } else {
                ChatUtils.send_debug("Critter ESP: §cOFF");
            }
            return 1;
        });
    }

    public static void registerStandalone(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(node());
    }
}
