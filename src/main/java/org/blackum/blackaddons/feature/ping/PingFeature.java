package org.blackum.blackaddons.feature.ping;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import org.blackum.blackaddons.feature.chat.ChatUtils;

public class PingFeature {
    private static long sendingTime = 0L;

    public static void sendPing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            sendingTime = System.currentTimeMillis();
            mc.getConnection().send(new ServerboundPingRequestPacket(sendingTime));
        }
    }

    public static boolean onPongReceive(long pingTime) {
        if (sendingTime != 0L && sendingTime == pingTime) {
            long latency = System.currentTimeMillis() - sendingTime;
            Minecraft.getInstance().execute(() -> {
                ChatUtils.send_debug("Ping: " + latency + "ms");
            });
            sendingTime = 0L;
            return true;
        }
        return false;
    }
}
