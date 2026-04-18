package org.blackum.blackaddons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
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
        return ClientCommandManager.literal(name)
                .executes(ctx -> handleActionTriggerMode(ctx.getSource(), !ConfigManager.data.actionTriggersEnabled))
                .then(ClientCommandManager.literal("on")
                        .executes(ctx -> handleActionTriggerMode(ctx.getSource(), true)))
                .then(ClientCommandManager.literal("off")
                        .executes(ctx -> handleActionTriggerMode(ctx.getSource(), false)))
                .then(ClientCommandManager.literal("toggle")
                        .executes(ctx -> handleActionTriggerMode(ctx.getSource(), !ConfigManager.data.actionTriggersEnabled)))
                .then(ClientCommandManager.literal("status")
                        .executes(ctx -> sendActionTriggerModeStatus(ctx.getSource())));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> ircNode() {
        return ClientCommandManager.literal("irc")
                .then(ClientCommandManager.literal("on")
                        .executes(ctx -> handleIrcChatMode(ctx.getSource(), true)))
                .then(ClientCommandManager.literal("off")
                        .executes(ctx -> handleIrcChatMode(ctx.getSource(), false)))
                .then(ClientCommandManager.literal("toggle")
                        .executes(ctx -> handleIrcChatMode(ctx.getSource(), !ConfigManager.data.ircChatMode)))
                .then(ClientCommandManager.literal("status")
                        .executes(ctx -> sendIrcChatModeStatus(ctx.getSource())))
                .then(ClientCommandManager.literal("msg")
                        .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
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
        return ClientCommandManager.literal("preview")
                .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String url = StringArgumentType.getString(ctx, "url");
                            if (Blackaddons.screenOpener != null) {
                                Blackaddons.screenOpener.accept(new ImagePreviewScreen(url, Minecraft.getInstance().screen));
                            }
                            return 1;
                        }));
    }

    public static void registerStandaloneIrc(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("irc")
                .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
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
