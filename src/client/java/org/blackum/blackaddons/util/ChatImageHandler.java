package org.blackum.blackaddons.util;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatImageHandler {
    public static final Pattern DISCORD_IMAGE_PATTERN = Pattern.compile(Constants.DISCORD_IMAGE_REGEX);

    public static Component handleMessage(Component message) {
        String text = message.getString();
        if (text.contains(Constants.PREVIEW_LABEL.trim())) {
            return message;
        }

        Matcher matcher = DISCORD_IMAGE_PATTERN.matcher(text);
        MutableComponent mutableMessage = null;

        while (matcher.find()) {
            if (mutableMessage == null) {
                mutableMessage = message.copy();
            }

            String url = matcher.group();
            MutableComponent previewComponent = Component.literal(Constants.PREVIEW_LABEL)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent.RunCommand("/ba_preview " + url))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal(Constants.PREVIEW_HOVER))));

            mutableMessage.append(previewComponent);
        }

        return mutableMessage != null ? mutableMessage : message;
    }
}
