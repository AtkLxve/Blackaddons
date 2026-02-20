package org.blackum.blackaddons.mixin.gui;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.modhider.ComponentUtils;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;
import java.util.stream.Stream;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.core.util.Constants;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    public static class Helper {
        private static final Set<String> serversAttemptedReadingMods = new HashSet<>();

        public static void showServerAttemptedReadingModsNotification() {
            Minecraft mc = Minecraft.getInstance();
            ServerData serverData = mc.getCurrentServer();
            if (serverData != null) {
                String ip = serverData.ip;
                if (serversAttemptedReadingMods.contains(ip)) {
                    return;
                }
                serversAttemptedReadingMods.add(ip);
            }

            mc.execute(() -> NotificationManager.addNotification(
                    Constants.MOD_DETECTION_TITLE,
                    Constants.MOD_DETECTION_MESSAGE,
                    NotificationType.WARNING));
        }
    }

    @Redirect(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
    public Stream<String> init(Stream<Component> instance, Function<Component, String> function) {
        return instance.map(message -> {
            if (ConfigManager.data.hideMods()) {
                String str = ComponentUtils.getString(message);
                if (!str.equals(message.getString())) {
                    Helper.showServerAttemptedReadingModsNotification();
                }
                return str;
            }
            return message.getString();
        });
    }
}
