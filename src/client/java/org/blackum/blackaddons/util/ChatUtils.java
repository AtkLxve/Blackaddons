package org.blackum.blackaddons.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mutable;

import static org.blackum.blackaddons.util.MinecraftInstance.mc;

public class ChatUtils {

    static MutableComponent BuildGradient(String text, int startRgb, int endRgb) {
        MutableComponent result = Component.empty();
        int length = text.length();

        if (length <= 1) {
            return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(startRgb)));
        }

        int sr = (startRgb >> 16) & 0xFF;
        int sg = (startRgb >> 8) & 0xFF;
        int sb = startRgb & 0xFF;

        int er = (endRgb >> 16) & 0xFF;
        int eg = (endRgb >> 8) & 0xFF;
        int eb = endRgb & 0xFF;

        for (int i = 0; i < text.length(); ++i) {
            float ratio = (float) i / (length - 1);

            int r = Math.round(sr + ratio * (er - sr));
            int g = Math.round(sg + ratio * (eg - sg));
            int b = Math.round(sb + ratio * (eb - sb));

            int rgb = (r << 16) | (g << 8) | b;

            MutableComponent charText = Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));

            result.append(charText);
        }

        return result;
    }

    private static final MutableComponent PREFIX = Component.empty()
            .append(ChatFormatting.BLACK + "[")
            .append(BuildGradient("BlackAddons", 0x332640, 0x623d94))
            .append(ChatFormatting.BLACK + "] ");

    public static MutableComponent getPrefix() {
        return PREFIX.copy();
    }

    public static MutableComponent getMessage(String text) {
        return getPrefix().append(Component.literal(ChatFormatting.RESET + text));
    }

    public static void send_debug(String txt) {
        mc.gui.getChat().addMessage(getMessage(txt));
    }
}
