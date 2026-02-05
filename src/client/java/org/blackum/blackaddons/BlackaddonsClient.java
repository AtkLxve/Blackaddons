package org.blackum.blackaddons;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

import org.blackum.blackaddons.gui.screen.DemoScreen;
import org.blackum.blackaddons.gui.screen.TestMenuScreen;

public class BlackaddonsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Blackaddons.LOGGER.info("Initializing client...");
        org.blackum.blackaddons.cheats.AutoTNT.register();
        org.blackum.blackaddons.config.ConfigManager.load();
        Blackaddons.guiOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                client.setScreen(new DemoScreen());
            });
        };

        Blackaddons.testMenuOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                client.setScreen(new TestMenuScreen());
            });
        };

        Blackaddons.mainGuiOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                client.setScreen(new org.blackum.blackaddons.gui.screen.BlackAddonsGUI());
            });
        };

        Blackaddons.notificationTrigger = (message) -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                org.blackum.blackaddons.gui.notification.NotificationManager.addNotification(
                        "Test Notification",
                        message,
                        org.blackum.blackaddons.gui.notification.NotificationType.INFO);
            });
        };

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            if (org.blackum.blackaddons.gui.screen.BaseScreen.showDebugOverlay) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.options.hideGui)
                    return;

                int x = org.blackum.blackaddons.gui.screen.BaseScreen.overlayX;
                int y = org.blackum.blackaddons.gui.screen.BaseScreen.overlayY;
                float scale = org.blackum.blackaddons.gui.screen.BaseScreen.overlayScale;
                int color = 0xFFFFFFFF;

                double windowWidth = mc.getWindow().getScreenWidth();
                double windowHeight = mc.getWindow().getScreenHeight();
                int scaledWidth = mc.getWindow().getGuiScaledWidth();
                int scaledHeight = mc.getWindow().getGuiScaledHeight();

                int finalMouseX = (int) (mc.mouseHandler.xpos() * ((double) scaledWidth / windowWidth));
                int finalMouseY = (int) (mc.mouseHandler.ypos() * ((double) scaledHeight / windowHeight));

                java.util.List<String> debugInfo = new java.util.ArrayList<>();
                debugInfo.add("§6[BlackAddons Debug]");
                debugInfo.add("Nick: " + mc.getUser().getName());
                debugInfo.add("VSync: " + mc.options.enableVsync().get());
                debugInfo.add("Mouse: " + finalMouseX + ", " + finalMouseY);
                debugInfo.add("Screen: " + (mc.screen != null ? mc.screen.getClass().getSimpleName() : "None"));

                debugInfo.add("");
                debugInfo.add("§6[Mod Hider Real Info]");
                String realBrand = "fabric";
                debugInfo.add("Real Brand: " + realBrand);

                int modCount = 0;
                int libCount = 0;
                for (net.fabricmc.loader.api.ModContainer mod : net.fabricmc.loader.api.FabricLoader.getInstance()
                        .getAllMods()) {
                    if ("builtin".equals(mod.getMetadata().getType()))
                        continue;
                    String type = mod.getMetadata().getType();
                    if (type.contains("library") || type.contains("api") ||
                            mod.getMetadata().getId().contains("library") ||
                            mod.getMetadata().getId().contains("api")) {
                        libCount++;
                    } else {
                        modCount++;
                    }
                }
                debugInfo.add("Real Mods: " + modCount);
                debugInfo.add("Real Libraries: " + libCount);

                debugInfo.add("");
                debugInfo.add("§6[Mod Hider Status]");
                debugInfo.add("Spoof Mode: " + org.blackum.blackaddons.modhider.ModHiderOptions.SPOOF_MODE.name());
                debugInfo.add("Hide Mods: " + org.blackum.blackaddons.modhider.ModHiderOptions.hideMods());
                debugInfo.add("Custom Client: " + org.blackum.blackaddons.modhider.ModHiderOptions.CUSTOM_CLIENT);
                debugInfo.add("Disable Payloads: "
                        + org.blackum.blackaddons.modhider.ModHiderOptions.DISABLE_CUSTOM_PAYLOADS);

                java.util.List<String> allowedModIds = new java.util.ArrayList<>();
                java.util.List<String> allowedLibIds = new java.util.ArrayList<>();
                java.util.List<String> hiddenModIds = new java.util.ArrayList<>();
                java.util.List<String> hiddenLibIds = new java.util.ArrayList<>();

                for (net.fabricmc.loader.api.ModContainer mod : net.fabricmc.loader.api.FabricLoader.getInstance()
                        .getAllMods()) {
                    if ("builtin".equals(mod.getMetadata().getType()))
                        continue;
                    String modId = mod.getMetadata().getId();
                    String type = mod.getMetadata().getType();
                    boolean isLibrary = type.contains("library") || type.contains("api") ||
                            modId.contains("library") || modId.contains("api");
                    boolean isAllowed = org.blackum.blackaddons.modhider.ModHiderOptions.ALLOWED_MODS.contains(modId);

                    if (isLibrary) {
                        if (isAllowed) {
                            allowedLibIds.add(modId);
                        } else {
                            hiddenLibIds.add(modId);
                        }
                    } else {
                        if (isAllowed) {
                            allowedModIds.add(modId);
                        } else {
                            hiddenModIds.add(modId);
                        }
                    }
                }

                debugInfo.add("Allowed Mods: " + allowedModIds.size());
                debugInfo.add("Allowed Libraries: " + allowedLibIds.size());

                if (!hiddenModIds.isEmpty()) {
                    debugInfo.add("");
                    debugInfo.add("§cHidden Mods:");
                    int count = 0;
                    for (String modId : hiddenModIds) {
                        if (count >= 5)
                            break;
                        debugInfo.add(" - " + modId);
                        count++;
                    }
                    if (hiddenModIds.size() > 5) {
                        debugInfo.add("  §7and " + (hiddenModIds.size() - 5) + " more mods");
                    }
                }

                if (!hiddenLibIds.isEmpty()) {
                    debugInfo.add("");
                    debugInfo.add("§cHidden Libraries:");
                    int count = 0;
                    for (String libId : hiddenLibIds) {
                        if (count >= 5)
                            break;
                        debugInfo.add("  " + libId);
                        count++;
                    }
                    if (hiddenLibIds.size() > 5) {
                        debugInfo.add("  §7and " + (hiddenLibIds.size() - 5) + " more libraries");
                    }
                }

                debugInfo.add("Allowed Channels: "
                        + org.blackum.blackaddons.modhider.ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.size());

                debugInfo.addAll(org.blackum.blackaddons.cheats.AutoTNT.getDebugInfo());

                graphics.pose().pushMatrix();
                graphics.pose().translate((float) x, (float) y);
                graphics.pose().scale(scale, scale);

                int lineY = 0;
                for (String line : debugInfo) {
                    graphics.drawString(mc.font, line, 0, lineY, color);
                    lineY += 10;
                }
                graphics.pose().popMatrix();
            }
        });

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            org.blackum.blackaddons.gui.notification.NotificationManager.getInstance().render(graphics);
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            org.blackum.blackaddons.gui.notification.NotificationManager.getInstance().tick();
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            org.blackum.blackaddons.config.ConfigManager.save();
        });

        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess) -> {
                    com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> command = net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                            .literal("ba")
                            .executes(ctx -> {
                                if (Blackaddons.mainGuiOpener != null)
                                    Blackaddons.mainGuiOpener.run();
                                return 1;
                            });

                    com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> testNode = net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                            .literal("test");

                    testNode.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("DebugGui")
                            .executes(ctx -> {
                                if (Blackaddons.guiOpener != null)
                                    Blackaddons.guiOpener.run();
                                return 1;
                            }));

                    testNode.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("TestMenu")
                            .executes(ctx -> {
                                if (Blackaddons.testMenuOpener != null)
                                    Blackaddons.testMenuOpener.run();
                                return 1;
                            }));

                    command.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("pv")
                            .executes(ctx -> {
                                String player = net.minecraft.client.Minecraft.getInstance().getUser().getName();
                                loadProfileAndOpen(player, false);
                                return 1;
                            })
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                    .argument("ign", com.mojang.brigadier.arguments.StringArgumentType.string())
                                    .executes(ctx -> {
                                        String player = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,
                                                "ign");
                                        loadProfileAndOpen(player, false);
                                        return 1;
                                    })
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                            .literal("force")
                                            .executes(ctx -> {
                                                String player = com.mojang.brigadier.arguments.StringArgumentType
                                                        .getString(ctx,
                                                                "ign");
                                                loadProfileAndOpen(player, true);
                                                return 1;
                                            }))));

                    command.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("daily")
                            .executes(ctx -> {
                                String player = net.minecraft.client.Minecraft.getInstance().getUser().getName();
                                org.blackum.blackaddons.gui.notification.NotificationManager.addNotification(
                                        "Daily Sync",
                                        "Syncing stats with bot...",
                                        org.blackum.blackaddons.gui.notification.NotificationType.INFO);

                                org.blackum.blackaddons.util.BotIntegration.sendDailySync(player)
                                        .thenAccept(success -> {
                                            if (success) {
                                                org.blackum.blackaddons.gui.notification.NotificationManager
                                                        .addNotification(
                                                                "Daily Sync",
                                                                "Stats synced successfully!",
                                                                org.blackum.blackaddons.gui.notification.NotificationType.SUCCESS);
                                            } else {
                                                org.blackum.blackaddons.gui.notification.NotificationManager
                                                        .addNotification(
                                                                "Daily Sync",
                                                                "Failed to sync stats.",
                                                                org.blackum.blackaddons.gui.notification.NotificationType.ERROR);
                                            }
                                        });
                                return 1;
                            }));

                    testNode.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("rng")
                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                    .argument("type", com.mojang.brigadier.arguments.StringArgumentType.string())
                                    .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider
                                            .suggest(new String[] { "rare", "crazy", "pray" }, builder))
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                            .argument("magic_find",
                                                    com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                                    .argument("item",
                                                            com.mojang.brigadier.arguments.StringArgumentType
                                                                    .greedyString())
                                                    .executes(context -> {
                                                        String typeArg = com.mojang.brigadier.arguments.StringArgumentType
                                                                .getString(context, "type").toLowerCase();
                                                        int mf = com.mojang.brigadier.arguments.IntegerArgumentType
                                                                .getInteger(context, "magic_find");
                                                        String item = com.mojang.brigadier.arguments.StringArgumentType
                                                                .getString(context, "item");

                                                        String typePrefix = "§6§lRARE";
                                                        if (typeArg.equals("crazy"))
                                                            typePrefix = "§d§lCRAZY RARE";
                                                        else if (typeArg.equals("pray"))
                                                            typePrefix = "§5§lPRAY TO RNGESUS";

                                                        String fakeMessage = typePrefix + " DROP! §r§f" + item
                                                                + " §r§b(+§r§b" + mf + "% §r§b✯ Magic Find§r§b)";

                                                        net.minecraft.client.Minecraft.getInstance().gui.getChat()
                                                                .addMessage(net.minecraft.network.chat.Component
                                                                        .literal(fakeMessage));

                                                        org.blackum.blackaddons.features.RngTracker
                                                                .onChatMessage(net.minecraft.network.chat.Component
                                                                        .literal(fakeMessage));
                                                        String title = "RNG Drop Tested";
                                                        String notificationMsg = item + " (" + typeArg + ")";
                                                        org.blackum.blackaddons.gui.notification.NotificationManager
                                                                .addNotification(title, notificationMsg,
                                                                        org.blackum.blackaddons.gui.notification.NotificationType.SUCCESS);

                                                        return 1;
                                                    })))));

                    testNode.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("GiveTNT")
                            .executes(ctx -> {
                                Minecraft client = Minecraft.getInstance();
                                if (client.player != null && client.player.connection != null) {
                                    client.player.connection
                                            .sendCommand("give @s tnt[custom_name='\"Superboom TNT\"'] 64");
                                    client.player.connection
                                            .sendCommand("give @s tnt[custom_name='\"Infinityboom TNT\"'] 64");
                                    client.player.connection.sendCommand(
                                            "give @s repeating_command_block[block_entity_data={id:\"minecraft:command_block\",Command:\"setblock ~ ~1 ~ cracked_stone_bricks\",auto:1b}] 1");
                                    client.player.connection.sendCommand(
                                            "give @s repeating_command_block[block_entity_data={id:\"minecraft:command_block\",Command:\"setblock ~ ~1 ~ stone_slab\",auto:1b}] 1");
                                }
                                return 1;
                            }));

                    command.then(testNode);

                    com.mojang.brigadier.tree.LiteralCommandNode<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> mainNode = dispatcher
                            .register(command);
                    dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("black")
                            .redirect(mainNode));
                    dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                            .literal("blackaddons").redirect(mainNode));
                });

        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message, overlay) ->

        {
            org.blackum.blackaddons.features.RngTracker.onChatMessage(message);
        });

        Blackaddons.LOGGER.info("Client initialization completed");
    }

    private void loadProfileAndOpen(String player, boolean force) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        mc.gui.getChat().addMessage(
                net.minecraft.network.chat.Component
                        .literal("§7[BlackAddons] Loading profile for " + player + (force ? " (Forced)" : "") + "..."));

        org.blackum.blackaddons.util.BotIntegration.getProfileStats(player, force).thenAccept(json -> {
            if (json == null) {
                mc.gui.getChat().addMessage(
                        net.minecraft.network.chat.Component.literal("§c[BlackAddons] Failed to fetch data."));
                return;
            }

            if (json.has("error")) {
                String err = json.get("error").getAsString();
                mc.gui.getChat()
                        .addMessage(net.minecraft.network.chat.Component.literal("§c[BlackAddons] Error: " + err));
                return;
            }

            com.google.gson.JsonObject data = null;
            if (json.has("data")) {
                data = json.getAsJsonObject("data");
            } else {
                mc.gui.getChat().addMessage(
                        net.minecraft.network.chat.Component.literal("§c[BlackAddons] Invalid response format."));
                return;
            }

            final com.google.gson.JsonObject finalData = data;
            mc.execute(() -> {
                mc.setScreen(
                        new org.blackum.blackaddons.gui.screen.ProfileViewerScreen(null, player, force, finalData));
            });
        }).exceptionally(e -> {
            mc.gui.getChat().addMessage(
                    net.minecraft.network.chat.Component.literal("§c[BlackAddons] Exception: " + e.getMessage()));
            return null;
        });
    }
}
