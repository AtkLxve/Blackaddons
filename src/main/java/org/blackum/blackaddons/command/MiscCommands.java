package org.blackum.blackaddons.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.feature.ping.PingFeature;
import org.blackum.blackaddons.gui.screen.overlay.OverlayEditScreen;

public final class MiscCommands {
    private MiscCommands() {}

    public static LiteralArgumentBuilder<FabricClientCommandSource> hudNode() {
        return ClientCommands.literal("hud").executes(ctx -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new OverlayEditScreen(null));
            }
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> pingNode() {
        return ClientCommands.literal("ping").executes(ctx -> {
            PingFeature.sendPing();
            return 1;
        });
    }
}
