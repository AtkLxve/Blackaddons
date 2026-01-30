package org.blackum.blackaddons.payload;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PayloadManager {
    public static List<PayloadOverride> overrides = new ArrayList<>();

    public static List<RecordedPayload> recordedPayloads = Collections.synchronizedList(new ArrayList<>());

    public static void clearRecordedPayloads() {
        recordedPayloads.clear();
    }

    public static void addOverride(PayloadOverride override) {
        overrides.add(override);
    }

    public static void removeOverride(PayloadOverride override) {
        overrides.remove(override);
    }

    public static void record(CustomPacketPayload payload) {
        try {
            String hex = getPayloadHex(payload);
            String channel = getType(payload);

            synchronized (recordedPayloads) {
                recordedPayloads.removeIf(p -> p.channel.equals(channel));

                RecordedPayload rec = new RecordedPayload(channel, hex, System.currentTimeMillis());
                recordedPayloads.add(0, rec);
                if (recordedPayloads.size() > 100) {
                    recordedPayloads.remove(recordedPayloads.size() - 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CustomPacketPayload getReplacement(CustomPacketPayload original) {
        if (overrides.isEmpty())
            return null;

        try {
            String originalHex = getPayloadHex(original);
            String channel = getType(original);
            String normalizedOriginal = originalHex.replaceAll("\\s+", "");

            for (PayloadOverride override : overrides) {
                if (!override.enabled)
                    continue;
                if (!channel.equals(override.channel))
                    continue;

                if (override.originalData != null && !override.originalData.isEmpty()
                        && !override.originalData.equals("*")) {
                    String normalizedOverride = override.originalData.replaceAll("\\s+", "");
                    if (originalHex.equals("[Typed Payload]"))
                        continue;
                    if (!originalHex.equalsIgnoreCase(override.originalData) &&
                            !normalizedOriginal.equalsIgnoreCase(normalizedOverride))
                        continue;
                }

                // net.minecraft.client.Minecraft.getInstance().gui.getChat().addMessage(
                // net.minecraft.network.chat.Component.literal("§c[BlackAddons] §aOverrode
                // payload: " + channel));

                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    org.blackum.blackaddons.gui.notification.NotificationManager.addNotification(
                            "Payload Overridden",
                            "Replaced: " + channel,
                            org.blackum.blackaddons.gui.notification.NotificationType.INFO);
                });
                Object idObj = original.type().id();
                return createPayloadFromHex(idObj, override.replacementData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getType(CustomPacketPayload payload) {
        return payload.type().id().toString();
    }

    private static String getPayloadHex(CustomPacketPayload payload) {
        if (payload instanceof DiscardedPayload) {
            try {
                for (java.lang.reflect.Field field : payload.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    Object val = field.get(payload);
                    if (val instanceof io.netty.buffer.ByteBuf) {
                        io.netty.buffer.ByteBuf buf = (io.netty.buffer.ByteBuf) val;
                        byte[] bytes = new byte[buf.writerIndex()];
                        buf.getBytes(0, bytes);
                        return bytesToHex(bytes);
                    }
                }
            } catch (Exception e) {
                return "[Error extracting: " + e.getMessage() + "]";
            }
            return payload.toString();
        }
        return payload.toString();
    }

    private static CustomPacketPayload createPayloadFromHex(Object id, String hexKey) {
        try {
            byte[] data = hexToBytes(hexKey.replaceAll("\\s+", ""));
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            java.lang.reflect.Constructor<?> ctor = DiscardedPayload.class.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            return (CustomPacketPayload) ctor.newInstance(id, buf);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
