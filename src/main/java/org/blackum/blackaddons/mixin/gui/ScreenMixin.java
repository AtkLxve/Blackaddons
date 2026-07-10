package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "clickCommandAction", at = @At("HEAD"), cancellable = true)
    private static void onClickCommandAction(LocalPlayer player, String command,
            Screen screen, CallbackInfo ci) {
        if (ConfigManager.data.disableCommandConfirmation) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.connection != null) {
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                mc.player.connection.sendCommand(command);
                ci.cancel();
            }
        }
    }
}
