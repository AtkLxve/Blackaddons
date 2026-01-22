package org.blackum.blackaddons.modhider;

import java.util.HashSet;
import java.util.Set;

/**
 * Ported from ClientSpoofer-1.21.11. Backed by {@link org.blackum.blackaddons.config.ConfigManager}.
 */
public class ModHiderOptions {
    public static SpoofMode SPOOF_MODE = SpoofMode.VANILLA;
    public static String CUSTOM_CLIENT = "fabric";
    public static boolean HIDE_MODS = true;
    public static boolean DISABLE_CUSTOM_PAYLOADS = true;
    public static Set<String> ALLOWED_MODS = new HashSet<>();
    public static Set<String> ALLOWED_CUSTOM_PAYLOAD_CHANNELS = new HashSet<>();

    public static boolean hideMods() {
        return switch (SPOOF_MODE) {
            case VANILLA, MODDED -> true;
            case CUSTOM -> HIDE_MODS;
            case OFF -> false;
        };
    }
}

