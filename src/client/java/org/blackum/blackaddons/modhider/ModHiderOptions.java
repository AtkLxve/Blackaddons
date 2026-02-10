package org.blackum.blackaddons.modhider;

import java.util.HashSet;
import java.util.Set;

/**
 * Ported from ClientSpoofer-1.21.11. Backed by
 * {@link org.blackum.blackaddons.config.ConfigManager}.
 */
public class ModHiderOptions {
    public SpoofMode SPOOF_MODE = SpoofMode.VANILLA;
    public String CUSTOM_CLIENT = "fabric";
    public boolean HIDE_MODS = true;
    public boolean DISABLE_CUSTOM_PAYLOADS = true;
    public Set<String> ALLOWED_MODS = new HashSet<>();
    public Set<String> ALLOWED_CUSTOM_PAYLOAD_CHANNELS = new HashSet<>();
    public final Set<String> FABRIC_DEFAULT_CHANNELS = Set.of(
            "fabric:attachment_sync_v1",
            "fabric:recipe_sync",
            "fabric-screen-handler-api-v1:open_screen");

    public boolean hideMods() {
        return switch (SPOOF_MODE) {
            case VANILLA, MODDED -> true;
            case CUSTOM -> HIDE_MODS;
            case OFF -> false;
        };
    }
}
