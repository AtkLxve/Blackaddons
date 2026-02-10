package org.blackum.blackaddons;

import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.cheats.AutoTNT;
import org.blackum.blackaddons.config.ConfigManagerV2;
import org.blackum.blackaddons.features.CommandUtils;
import org.blackum.blackaddons.features.RngTracker;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.DemoScreen;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.screen.TestMenuScreen;
import org.blackum.blackaddons.modhider.ModHiderOptions;
import org.blackum.blackaddons.util.BotIntegration;
import org.blackum.blackaddons.util.ProfileStateManager;

public class BlackaddonsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Blackaddons.LOGGER.info("Initializing client...");
        AutoTNT.register();

        ConfigManagerV2.load();

        BotIntegration.fetchVerificationKey();

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
                client.setScreen(new BlackAddonsGUI());
            });
        };

        Blackaddons.notificationTrigger = (message) -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                NotificationManager.addNotification(
                        "Test Notification",
                        message,
                        NotificationType.INFO);
            });
        };

        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            if (BaseScreen.showDebugOverlay) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.options.hideGui)
                    return;

                int x = BaseScreen.overlayX;
                int y = BaseScreen.overlayY;
                float scale = BaseScreen.overlayScale;
                int color = 0xFFFFFFFF;

                double windowWidth = mc.getWindow().getScreenWidth();
                double windowHeight = mc.getWindow().getScreenHeight();
                int scaledWidth = mc.getWindow().getGuiScaledWidth();
                int scaledHeight = mc.getWindow().getGuiScaledHeight();

                int finalMouseX = (int) (mc.mouseHandler.xpos() * ((double) scaledWidth / windowWidth));
                int finalMouseY = (int) (mc.mouseHandler.ypos() * ((double) scaledHeight / windowHeight));

                List<String> debugInfo = new ArrayList<>();
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
                for (ModContainer mod : FabricLoader.getInstance()
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
                debugInfo.add("Spoof Mode: " + ModHiderOptions.SPOOF_MODE.name());
                debugInfo.add("Hide Mods: " + ModHiderOptions.hideMods());
                debugInfo.add("Custom Client: " + ModHiderOptions.CUSTOM_CLIENT);
                debugInfo.add("Disable Payloads: "
                        + ModHiderOptions.DISABLE_CUSTOM_PAYLOADS);

                List<String> allowedModIds = new ArrayList<>();
                List<String> allowedLibIds = new ArrayList<>();
                List<String> hiddenModIds = new ArrayList<>();
                List<String> hiddenLibIds = new ArrayList<>();

                for (ModContainer mod : FabricLoader.getInstance()
                        .getAllMods()) {
                    if ("builtin".equals(mod.getMetadata().getType()))
                        continue;
                    String modId = mod.getMetadata().getId();
                    String type = mod.getMetadata().getType();
                    boolean isLibrary = type.contains("library") || type.contains("api") ||
                            modId.contains("library") || modId.contains("api");
                    boolean isAllowed = ModHiderOptions.ALLOWED_MODS.contains(modId);

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
                        + ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.size());

                debugInfo.addAll(AutoTNT.getDebugInfo());

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

        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            if (Minecraft.getInstance().screen instanceof BaseScreen)
                return;
            NotificationManager.getInstance().render(graphics);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            NotificationManager.getInstance().tick();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ConfigManagerV2.save();

            while (true) {
                System.out.println("who's closing the game now bitch");
            }
        });

        ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess) -> {
                    Command<FabricClientCommandSource> openGui = ctx -> {
                        if (Blackaddons.mainGuiOpener != null)
                            Blackaddons.mainGuiOpener.run();
                        return 1;
                    };

                    var testNode = ClientCommandManager.literal("test");

                    testNode.then(ClientCommandManager.literal("DebugGui")
                            .executes(ctx -> {
                                if (Blackaddons.guiOpener != null)
                                    Blackaddons.guiOpener.run();
                                return 1;
                            }));

                    testNode.then(ClientCommandManager.literal("TestMenu")
                            .executes(ctx -> {
                                if (Blackaddons.testMenuOpener != null)
                                    Blackaddons.testMenuOpener.run();
                                return 1;
                            }));

                    testNode.then(ClientCommandManager.literal("rng")
                            .then(ClientCommandManager
                                    .argument("type", StringArgumentType.string())
                                    .suggests((context, builder) -> SharedSuggestionProvider
                                            .suggest(new String[] { "rare", "crazy", "pray" }, builder))
                                    .then(ClientCommandManager
                                            .argument("magic_find",
                                                    IntegerArgumentType.integer(0))
                                            .then(ClientCommandManager
                                                    .argument("item",
                                                            StringArgumentType
                                                                    .greedyString())
                                                    .executes(context -> {
                                                        String typeArg = StringArgumentType
                                                                .getString(context, "type").toLowerCase();
                                                        int mf = IntegerArgumentType
                                                                .getInteger(context, "magic_find");
                                                        String item = StringArgumentType
                                                                .getString(context, "item");

                                                        String typePrefix = "§6§lRARE";
                                                        if (typeArg.equals("crazy"))
                                                            typePrefix = "§d§lCRAZY RARE";
                                                        else if (typeArg.equals("pray"))
                                                            typePrefix = "§5§lPRAY TO RNGESUS";

                                                        String fakeMessage = typePrefix + " DROP! §r§f" + item
                                                                + " §r§b(+§r§b" + mf + "% §r§b✯ Magic Find§r§b)";

                                                        Minecraft.getInstance().gui.getChat()
                                                                .addMessage(Component
                                                                        .literal(fakeMessage));

                                                        RngTracker
                                                                .onChatMessage(Component
                                                                        .literal(fakeMessage));
                                                        String title = "RNG Drop Tested";
                                                        String notificationMsg = item + " (" + typeArg + ")";
                                                        NotificationManager
                                                                .addNotification(title, notificationMsg,
                                                                        NotificationType.SUCCESS);

                                                        return 1;
                                                    })))));

                    testNode.then(ClientCommandManager.literal("GiveTNT")
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
                                            "give @s repeating_command_block[block_entity_data={id:\"minecraft:command_block\",Command:\"setblock ~ ~1 ~ smooth_stone_slab\",auto:1b}] 1");
                                }
                                return 1;
                            }));

                    var pvNode = ClientCommandManager.literal("pv")
                            .executes(ctx -> {
                                String player = Minecraft.getInstance().getUser().getName();
                                loadProfileAndOpen(player, false);
                                return 1;
                            })
                            .then(ClientCommandManager
                                    .argument("ign", StringArgumentType.string())
                                    .executes(ctx -> {
                                        String player = StringArgumentType.getString(ctx,
                                                "ign");
                                        loadProfileAndOpen(player, false);
                                        return 1;
                                    })
                                    .then(ClientCommandManager
                                            .literal("force")
                                            .executes(ctx -> {
                                                String player = StringArgumentType
                                                        .getString(ctx,
                                                                "ign");
                                                loadProfileAndOpen(player, true);
                                                return 1;
                                            })));

                    var dailyNode = ClientCommandManager.literal("daily")
                            .executes(ctx -> {
                                String player = Minecraft.getInstance().getUser().getName();
                                NotificationManager.addNotification(
                                        "Daily Sync",
                                        "Syncing stats with bot...",
                                        NotificationType.INFO);

                                BotIntegration.sendDailySync(player)
                                        .thenAccept(success -> {
                                            if (success) {
                                                NotificationManager
                                                        .addNotification(
                                                                "Daily Sync",
                                                                "Stats synced successfully!",
                                                                NotificationType.SUCCESS);
                                            } else {
                                                NotificationManager
                                                        .addNotification(
                                                                "Daily Sync",
                                                                "Failed to sync stats.",
                                                                NotificationType.ERROR);
                                            }
                                        });
                                return 1;
                            });

                    CommandUtils.register(dispatcher);
                    for (String alias : new String[] { "ba", "black", "blackaddons" }) {
                        var cmd = ClientCommandManager
                                .literal(alias)
                                .executes(openGui);
                        cmd.then(testNode);
                        cmd.then(pvNode);
                        cmd.then(dailyNode);
                        cmd.then(CommandUtils.subcommand);
                        dispatcher.register(cmd);
                    }

                });

        ClientReceiveMessageEvents.GAME.register((message, overlay) ->

        {
            RngTracker.onChatMessage(message);
        });

        Blackaddons.LOGGER.info("Client initialization completed");
    }

    private void loadProfileAndOpen(String player, boolean force) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.getChat().addMessage(
                Component
                        .literal("§7[BlackAddons] Loading profile for " + player + (force ? " (Forced)" : "") + "..."));

        ProfileStateManager.getInstance().getProfile(player, force).thenAccept(result -> {
            if (result == null) {
                mc.gui.getChat().addMessage(
                        Component.literal("§c[BlackAddons] Failed to fetch data."));
                NotificationManager.addNotification(
                        "Profile Error",
                        "Failed to fetch data from API.",
                        NotificationType.ERROR);
                return;
            }

            if (result.hasError()) {
                String err = result.getError();
                mc.gui.getChat()
                        .addMessage(Component.literal("§c[BlackAddons] Error: " + err));
                NotificationManager.addNotification(
                        "Profile Error",
                        err,
                        NotificationType.ERROR);
                return;
            }

            JsonObject data = result.getData();
            if (data == null) {
                mc.gui.getChat().addMessage(
                        Component.literal("§c[BlackAddons] Invalid response format."));
                NotificationManager.addNotification(
                        "Profile Error",
                        "Invalid response format.",
                        NotificationType.ERROR);
                return;
            }

            final JsonObject finalData = data;
            mc.execute(() -> {
                mc.setScreen(
                        new ProfileViewerScreen(null, player, force, finalData));
            });
        }).exceptionally(e -> {
            mc.gui.getChat().addMessage(
                    Component.literal("§c[BlackAddons] Exception: " + e.getMessage()));
            NotificationManager.addNotification(
                    "Profile Exception",
                    e.getMessage(),
                    NotificationType.ERROR);
            return null;
        });
    }
}
