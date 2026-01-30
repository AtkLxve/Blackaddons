package org.blackum.blackaddons.mixin.client;

import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Thanks noamm
// @Pseudo
// @Mixin(targets = "moe.nea.firmament.features.misc.ModAnnouncer", remap =
// false)
// public class FirmamentMixin {
// @Dynamic
// @Inject(method = "onServerJoin", at = @At("HEAD"), cancellable = true,
// require = 0)
// private void stopFeedingEveryone(@Coerce Object event, CallbackInfo ci) {
// ci.cancel();
// Blackaddons.LOGGER.info("Prevented Firmament from announcing mods.");
// Minecraft.getInstance().execute(() -> {
// NotificationManager.addNotification(
// "Fuck Firmament",
// "Blocked Firmament Mod Announcer",
// NotificationType.WARNING);
// });
// }
// }
