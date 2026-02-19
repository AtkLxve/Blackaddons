package org.blackum.blackaddons.util;

import java.util.HashMap;
import java.util.Map;

public class EmojiUtils {
    private static final Map<String, String> EMOJI_MAP = new HashMap<>();

    static {
        EMOJI_MAP.put(":heart:", "❤");
        EMOJI_MAP.put(":wilted_rose:", "🥀");
        EMOJI_MAP.put(":fire:", "🔥");
        EMOJI_MAP.put(":skull:", "💀");
        EMOJI_MAP.put(":thumbsup:", "👍");
        EMOJI_MAP.put(":smile:", "😊");
        EMOJI_MAP.put(":check:", "✔");
        EMOJI_MAP.put(":cross:", "❌");
        EMOJI_MAP.put(":star:", "⭐");
        EMOJI_MAP.put(":eyes:", "👀");
        EMOJI_MAP.put(":thinking:", "🤔");
    }

    public static Map<String, String> getEmojiMap() {
        return EMOJI_MAP;
    }

    public static String replaceEmojis(String text) {
        if (text == null || text.isEmpty())
            return text;

        String result = text;
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
