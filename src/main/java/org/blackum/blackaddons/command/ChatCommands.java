package org.blackum.blackaddons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.feature.chat.ChatActionExecutor;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.feature.chat.IrcClient;
import org.blackum.blackaddons.gui.screen.feature.ImagePreviewScreen;
import org.blackum.blackaddons.gui.screen.feature.IrcScreen;

public class ChatCommands {

    private static int handleIrcChatMode(FabricClientCommandSource source, boolean enabled) {
        ConfigManager.data.ircChatMode = enabled;
        ConfigManager.save();
        source.sendFeedback(enabled
                ? ChatUtils.success("IRC chat mode enabled. Normal chat now goes to IRC.")
                : ChatUtils.error("IRC chat mode disabled. Use the IRC prefix to chat in IRC."));
        return 1;
    }

    private static int sendIrcChatModeStatus(FabricClientCommandSource source) {
        boolean enabled = ConfigManager.data.ircChatMode;
        source.sendFeedback(enabled
                ? ChatUtils.success("IRC chat mode is enabled.")
                : ChatUtils.error("IRC chat mode is disabled."));
        return 1;
    }

    private static int handleActionTriggerMode(FabricClientCommandSource source, boolean enabled) {
        ConfigManager.data.actionTriggersEnabled = enabled;
        ConfigManager.save();
        if (!enabled) {
            ChatActionExecutor.getInstance().clearPendingActions();
        }
        source.sendFeedback(enabled
                ? ChatUtils.success("Action triggers enabled.")
                : ChatUtils.error("Action triggers disabled."));
        return 1;
    }

    private static int sendActionTriggerModeStatus(FabricClientCommandSource source) {
        boolean enabled = ConfigManager.data.actionTriggersEnabled;
        source.sendFeedback(enabled
                ? ChatUtils.success("Action triggers are enabled.")
                : ChatUtils.error("Action triggers are disabled."));
        return 1;
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> actionTriggerNode(String name) {
        return ClientCommands.literal(name)
                .executes(ctx -> handleActionTriggerMode(ctx.getSource(), !ConfigManager.data.actionTriggersEnabled))
                .then(ClientCommands.literal("on")
                        .executes(ctx -> handleActionTriggerMode(ctx.getSource(), true)))
                .then(ClientCommands.literal("off")
                        .executes(ctx -> handleActionTriggerMode(ctx.getSource(), false)))
                .then(ClientCommands.literal("toggle")
                        .executes(ctx -> handleActionTriggerMode(ctx.getSource(), !ConfigManager.data.actionTriggersEnabled)))
                .then(ClientCommands.literal("status")
                        .executes(ctx -> sendActionTriggerModeStatus(ctx.getSource())));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> ircNode() {
        return ClientCommands.literal("irc")
                .then(ClientCommands.literal("on")
                        .executes(ctx -> handleIrcChatMode(ctx.getSource(), true)))
                .then(ClientCommands.literal("off")
                        .executes(ctx -> handleIrcChatMode(ctx.getSource(), false)))
                .then(ClientCommands.literal("toggle")
                        .executes(ctx -> handleIrcChatMode(ctx.getSource(), !ConfigManager.data.ircChatMode)))
                .then(ClientCommands.literal("status")
                        .executes(ctx -> sendIrcChatModeStatus(ctx.getSource())))
                .then(ClientCommands.literal("msg")
                        .then(ClientCommands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String messageArg = StringArgumentType.getString(ctx, "message");
                                    if (messageArg != null) {
                                        IrcClient.getInstance().sendMessage(messageArg);
                                    }
                                    return 1;
                                })))
                .executes(ctx -> {
                    if (Blackaddons.screenOpener != null) {
                        Blackaddons.screenOpener.accept(new IrcScreen());
                    }
                    return 1;
                });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> previewNode() {
        return ClientCommands.literal("preview")
                .then(ClientCommands.argument("url", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String url = StringArgumentType.getString(ctx, "url");
                            if (Blackaddons.screenOpener != null) {
                                Blackaddons.screenOpener.accept(new ImagePreviewScreen(url, McCompat.getScreen(Minecraft.getInstance())));
                            }
                            return 1;
                        }));
    }

    public static void registerStandaloneIrc(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("irc")
                .then(ClientCommands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String messageArg = StringArgumentType.getString(ctx, "message");
                            if (messageArg != null) {
                                IrcClient.getInstance().sendMessage(messageArg);
                            }
                            return 1;
                        }))
                .executes(ctx -> {
                    if (Blackaddons.screenOpener != null) {
                        Blackaddons.screenOpener.accept(new IrcScreen());
                    }
                    return 1;
                }));
    }
}
