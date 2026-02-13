package org.blackum.blackaddons.mixin.client;

import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.modhider.SpoofMode;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.util.PacketLogger;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.util.PayloadHelper;
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
                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.OFF) {
                    return;
                }

                // Firmament stop fedding (in case they change stuff around)
                // if (payload.type().id().toString().startsWith("firmament")) {
                // PacketLogger.logBlockedPacket("Firmament Block",
                // payload);
                // Minecraft.getInstance().execute(() -> {
                // NotificationManager.addNotification(
                // "Fuck Firmament",
                // "Blocked Firmament packet: " + payload.type().id(),
                // NotificationType.WARNING);
                // });
                // ci.cancel();
                // return;
                // }

                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.VANILLA) {
                    PacketLogger.logBlockedPacket("ModHider (Vanilla)", payload);
                    Minecraft.getInstance().execute(() -> {
                        NotificationManager.addNotification(
                                "Mod Hider",
                                "Blocked payload: " + payload.type().id(),
                                NotificationType.INFO);
                    });
                    ci.cancel();
                    return;
                }
                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.MODDED) {
                    return;
                }
                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.CUSTOM &&
                        ConfigManager.data.modHiderDisableCustomPayloads) {
                    String id = payload.type().id().toString();
                    if (id.equals("minecraft:register") || id.equals("minecraft:unregister")) {
                        return;
                    }

                    for (String channel : ConfigManager.data.modHiderAllowedCustomPayloadChannels) {
                        if (id.toLowerCase().startsWith(channel.toLowerCase())) {
                            return;
                        }
                    }
                    PacketLogger.logBlockedPacket("ModHider (Custom)", payload);
                    Minecraft.getInstance().execute(() -> {
                        NotificationManager.addNotification(
                                "Mod Hider",
                                "Blocked payload: " + payload.type().id(),
                                NotificationType.INFO);
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
                if (ConfigManager.data.modHiderSpoofMode == SpoofMode.CUSTOM
                        && ConfigManager.data.modHiderDisableCustomPayloads) {
                    CustomPacketPayload newPayload = PayloadHelper.createRegisterPayload(
                            payload,
                            new HashSet<>(ConfigManager.data.modHiderAllowedCustomPayloadChannels));
                    if (newPayload != null) {
                        return new ServerboundCustomPayloadPacket(newPayload);
                    }
                }
            }
        }
        return packet;
    }
}
