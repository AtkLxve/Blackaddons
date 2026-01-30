package org.blackum.blackaddons.mixin.client;

import org.blackum.blackaddons.modhider.ModHiderOptions;
import org.blackum.blackaddons.modhider.SpoofMode;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    public void sendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo ci) {
        if (((Connection) (Object) this).isMemoryConnection()) {
            return;
        }

        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
            // Record everything first, even if blocked later
            org.blackum.blackaddons.payload.PayloadManager.record(payload);

            // Check overrides FIRST
            if (processPayloadOverride(payload, channelFutureListener, bl, ci)) {
                return;
            }

            if (!(payload instanceof DiscardedPayload) && !(payload instanceof BrandPayload)) {
                if (ModHiderOptions.SPOOF_MODE == SpoofMode.OFF) {
                    return;
                }

                // Firmament stop fedding (in case they change stuff around)
                // if (payload.type().id().toString().startsWith("firmament")) {
                // org.blackum.blackaddons.util.PacketLogger.logBlockedPacket("Firmament Block",
                // payload);
                // net.minecraft.client.Minecraft.getInstance().execute(() -> {
                // org.blackum.blackaddons.gui.notification.NotificationManager.addNotification(
                // "Fuck Firmament",
                // "Blocked Firmament packet: " + payload.type().id(),
                // org.blackum.blackaddons.gui.notification.NotificationType.WARNING);
                // });
                // ci.cancel();
                // return;
                // }

                if (ModHiderOptions.SPOOF_MODE == SpoofMode.VANILLA) {
                    org.blackum.blackaddons.util.PacketLogger.logBlockedPacket("ModHider (Vanilla)", payload);
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        org.blackum.blackaddons.gui.notification.NotificationManager.addNotification(
                                "Mod Hider",
                                "Blocked payload: " + payload.type().id(),
                                org.blackum.blackaddons.gui.notification.NotificationType.INFO);
                    });
                    ci.cancel();
                    return;
                }
                if (ModHiderOptions.SPOOF_MODE == SpoofMode.MODDED) {
                    return;
                }
                if (ModHiderOptions.SPOOF_MODE == SpoofMode.CUSTOM &&
                        ModHiderOptions.DISABLE_CUSTOM_PAYLOADS) {
                    for (String channel : ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS) {
                        if (payload.type().id().toString().toLowerCase().startsWith(channel.toLowerCase())) {
                            return;
                        }
                    }
                    org.blackum.blackaddons.util.PacketLogger.logBlockedPacket("ModHider (Custom)", payload);
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        org.blackum.blackaddons.gui.notification.NotificationManager.addNotification(
                                "Mod Hider",
                                "Blocked payload: " + payload.type().id(),
                                org.blackum.blackaddons.gui.notification.NotificationType.INFO);
                    });
                    ci.cancel();
                }
            }
        }
    }

    private boolean processPayloadOverride(CustomPacketPayload payload, ChannelFutureListener channelFutureListener,
            boolean bl, CallbackInfo ci) {
        CustomPacketPayload replacement = org.blackum.blackaddons.payload.PayloadManager.getReplacement(payload);
        if (replacement != null) {
            ((Connection) (Object) this).send(new ServerboundCustomPayloadPacket(replacement), channelFutureListener,
                    bl);
            ci.cancel();
            return true;
        }
        return false;
    }
}
