package org.blackum.blackaddons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.manager.CommandManager;
import org.blackum.blackaddons.core.manager.CustomNameManager;
import org.blackum.blackaddons.core.manager.DebugOverlayManager;
import org.blackum.blackaddons.core.manager.PartyFinderManager;
import org.blackum.blackaddons.core.manager.UpdateManager;
import org.blackum.blackaddons.gui.render.DebugBoxRenderer;
import org.blackum.blackaddons.gui.render.WaypointRenderer;
import org.blackum.blackaddons.feature.chat.ChatImageHandler;
import org.blackum.blackaddons.feature.chat.ChatActionManager;
import org.blackum.blackaddons.feature.chat.IrcClient;
import org.blackum.blackaddons.feature.chat.IrcPrefixManager;
import org.blackum.blackaddons.feature.cheat.AutoBM;
import org.blackum.blackaddons.feature.cheat.AutoSS;
import org.blackum.blackaddons.feature.cheat.AutoTNT;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.blackum.blackaddons.feature.cheat.Perspective;
import org.blackum.blackaddons.feature.cheat.RelicLook;
import org.blackum.blackaddons.core.util.LocationUtils;
import org.blackum.blackaddons.core.util.AlignUtils;
import org.blackum.blackaddons.core.util.Scheduler;
import org.blackum.blackaddons.feature.dungeon.DungeonJoinHandler;
import org.blackum.blackaddons.feature.dungeon.DungeonListener;
import org.blackum.blackaddons.feature.dungeon.SoloClearsTracker;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMap;
import org.blackum.blackaddons.feature.dungeon.solvers.puzzles.WaterBoardHandler;
import org.blackum.blackaddons.feature.dungeon.solvers.puzzles.WaterBoardSolver;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMapHud;
import org.blackum.blackaddons.feature.dungeon.map.DungeonScoreboard;
import org.blackum.blackaddons.feature.dungeon.map.DungeonWorldScanner;
import org.blackum.blackaddons.feature.dungeon.map.RoomData;
import org.blackum.blackaddons.feature.rng.RngTracker;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.gui.render.font.CustomFontRenderer;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.DemoScreen;
import org.blackum.blackaddons.gui.screen.TestMenuScreen;
import org.blackum.blackaddons.integration.BotIntegration;

public class BlackaddonsClient implements ClientModInitializer {
    private static boolean internalChatMsg = false;
    private static Screen pendingScreen = null;

    @Override
    public void onInitializeClient() {
        Blackaddons.LOGGER.info("Initializing client...");
        IrcPrefixManager.getPrefix(); 
        AutoTNT.register();
        RelicLook.register();
        AutoSS.register();
        AutoBM.register();
        Freecam.register();
        Perspective.register();
        Scheduler.register();
        LocationUtils.register();
        AlignUtils.register();
        WaterBoardHandler.register();

        ConfigManager.load();
        if (ConfigManager.data.customTextEnabled) {
            CustomFontRenderer.getInstance().init();
        }
        RoomData.loadRooms();
        DungeonWorldScanner.register();
        DungeonScoreboard.register();
        BotIntegration.authenticateWithMojang();
        CustomNameManager.getInstance().fetch();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IrcPrefixManager.resetCache();
            IrcClient.getInstance().connect();
            UpdateManager.check();
            DungeonMap.reset();
            DungeonWorldScanner.reset();
            DungeonScoreboard.reset();
            WaterBoardSolver.reset();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            IrcClient.getInstance().disconnect();
            DungeonMap.reset();
            DungeonWorldScanner.reset();
            DungeonScoreboard.reset();
            WaterBoardSolver.reset();
        });

        Blackaddons.guiOpener = () -> {
            pendingScreen = new DemoScreen();
        };

        Blackaddons.testMenuOpener = () -> {
            pendingScreen = new TestMenuScreen();
        };

        Blackaddons.screenOpener = (screen) -> {
            pendingScreen = screen;
        };

        Blackaddons.mainGuiOpener = () -> {
            pendingScreen = new BlackAddonsGUI();
        };

        Blackaddons.notificationTrigger = (message) -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> NotificationManager.addNotification("Notification", message, NotificationType.INFO));
        };

        DebugOverlayManager.register();
        CommandManager.register();

        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            DungeonMapHud.render(graphics);
            WaterBoardSolver.renderHUD(graphics);
            if (!(Minecraft.getInstance().screen instanceof BaseScreen)) {
                NotificationManager.getInstance().render(graphics);
            }
        });

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> {
            WaypointRenderer.render(context.matrices().last().pose(), context.consumers(), 0.0f);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;
            DebugBoxRenderer.render(
                    context.matrices().last().pose(),
                    context.consumers(),
                    mc.gameRenderer.getMainCamera().position(),
                    LocationUtils.getDebugBoxes()
            );
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> AlignUtils.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ++DungeonListener.currentTime;
            NotificationManager.getInstance().tick();
            SoloClearsTracker.tick();
            if (pendingScreen != null) {
                client.setScreen(pendingScreen);
                pendingScreen = null;
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (internalChatMsg) {
                internalChatMsg = false;
                return true;
            }

            if (ConfigManager.data.ircChatMode) {
                IrcClient.getInstance().sendMessage(message.trim());
                return false;
            }

            String prefix = IrcPrefixManager.getPrefix();

            if (prefix.equals("#") && message.startsWith("##")) {
                internalChatMsg = true;
                Minecraft.getInstance().player.connection.sendChat(message.substring(1));
                return false;
            }

            if (message.startsWith(prefix)) {
                IrcClient.getInstance().sendMessage(message.substring(prefix.length()).trim());
                return false;
            }
            return true;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            Component handled = ChatImageHandler
                    .handleMessage(message);
            RngTracker.onChatMessage(handled);
            DungeonJoinHandler.onChatMessage(handled);
            DungeonScoreboard.onChatMessage(handled);
            PartyFinderManager.getInstance().onChatMessage(handled);
            ChatActionManager.getInstance().onChatMessage(handled);
            SoloClearsTracker.onChatMessage(handled);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            Component handled = ChatImageHandler
                    .handleMessage(message);
            RngTracker.onChatMessage(handled);
            DungeonJoinHandler.onChatMessage(handled);
            DungeonScoreboard.onChatMessage(handled);
            PartyFinderManager.getInstance().onChatMessage(handled);
            ChatActionManager.getInstance().onChatMessage(handled);
            SoloClearsTracker.onChatMessage(handled);
        });

        Blackaddons.LOGGER.info("Client initialization completed");
    }
}
