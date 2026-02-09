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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.HashSet;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    public void sendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo ci) {
        if (((Connection) (Object) this).isMemoryConnection()) {
            return;
        }

        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {

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
                    String id = payload.type().id().toString();
                    if (id.equals("minecraft:register") || id.equals("minecraft:unregister")) {
                        return;
                    }

                    for (String channel : ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS) {
                        if (id.toLowerCase().startsWith(channel.toLowerCase())) {
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

    @ModifyVariable(method = "sendPacket", at = @At("HEAD"), argsOnly = true)
    private Packet<?> modifyPacket(Packet<?> packet) {
        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
            String id = payload.type().id().toString();
            if (id.equals("minecraft:register") || id.equals("minecraft:unregister")) {
                if (ModHiderOptions.SPOOF_MODE == SpoofMode.CUSTOM && ModHiderOptions.DISABLE_CUSTOM_PAYLOADS) {
                    CustomPacketPayload newPayload = org.blackum.blackaddons.util.PayloadHelper.createRegisterPayload(
                            payload,
                            new HashSet<>(ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS));
                    if (newPayload != null) {
                        return new ServerboundCustomPayloadPacket(newPayload);
                    }
                }
            }
        }
        return packet;
    }
}
