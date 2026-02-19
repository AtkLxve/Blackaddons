package org.blackum.blackaddons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.cheats.AutoTNT;
import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.features.RngTracker;
import org.blackum.blackaddons.manager.CommandManager;
import org.blackum.blackaddons.manager.DebugOverlayManager;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.gui.screen.DemoScreen;
import org.blackum.blackaddons.gui.screen.TestMenuScreen;
import org.blackum.blackaddons.util.BotIntegration;
import org.blackum.blackaddons.util.IrcClient;

public class BlackaddonsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Blackaddons.LOGGER.info("Initializing client...");
        AutoTNT.register();

        ConfigManager.load();
        BotIntegration.fetchVerificationKey();
        IrcClient.getInstance().connect();

        Blackaddons.guiOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> client.setScreen(new DemoScreen()));
        };

        Blackaddons.testMenuOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> client.setScreen(new TestMenuScreen()));
        };

        Blackaddons.mainGuiOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> client.setScreen(new BlackAddonsGUI()));
        };

        Blackaddons.notificationTrigger = (message) -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> NotificationManager.addNotification("Notification", message, NotificationType.INFO));
        };

        DebugOverlayManager.register();
        CommandManager.register();

        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            if (!(Minecraft.getInstance().screen instanceof BaseScreen)) {
                NotificationManager.getInstance().render(graphics);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> NotificationManager.getInstance().tick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            net.minecraft.network.chat.Component handled = org.blackum.blackaddons.util.ChatImageHandler
                    .handleMessage(message);
            RngTracker.onChatMessage(handled);
            org.blackum.blackaddons.features.DungeonJoinHandler.onChatMessage(handled);
            org.blackum.blackaddons.manager.PartyFinderManager.getInstance().onChatMessage(handled);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            net.minecraft.network.chat.Component handled = org.blackum.blackaddons.util.ChatImageHandler
                    .handleMessage(message);
            RngTracker.onChatMessage(handled);
            org.blackum.blackaddons.features.DungeonJoinHandler.onChatMessage(handled);
            org.blackum.blackaddons.manager.PartyFinderManager.getInstance().onChatMessage(handled);
        });

        Blackaddons.LOGGER.info("Client initialization completed");
    }
}
