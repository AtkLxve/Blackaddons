package org.blackum.blackaddons.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.feature.profile.ProfileStateManager;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.service.BotIntegration;

public class ProfileCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> pvNode() {
        return ClientCommandManager.literal("pv")
                .executes(ctx -> {
                    String player = Minecraft.getInstance().getUser().getName();
                    ProfileStateManager.getInstance().loadProfileAndOpen(player, null, false);
                    return 1;
                })
                .then(ClientCommandManager.argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                        .executes(ctx -> {
                            String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                            ProfileStateManager.getInstance().loadProfileAndOpen(ign, null, false);
                            return 1;
                        })
                        .then(ClientCommandManager.literal("force")
                                .executes(ctx -> {
                                    String ign = StringArgumentType.getString(ctx, Constants.CMD_ARG_IGN);
                                    ProfileStateManager.getInstance().loadProfileAndOpen(ign, null, true);
                                    return 1;
                                })));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> dailyNode() {
        return ClientCommandManager.literal("daily")
                .executes(ctx -> {
                    String player = Minecraft.getInstance().getUser().getName();
                    NotificationManager.addNotification("Daily Sync", "Syncing stats with bot...", NotificationType.INFO);

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
    }
}
