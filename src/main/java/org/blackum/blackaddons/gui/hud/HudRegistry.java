package org.blackum.blackaddons.gui.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudRegistry {
    private static final Map<String, HudElement> ELEMENTS = new LinkedHashMap<>();
    private static boolean installed = false;

    private HudRegistry() {
    }

    public static void register(HudElement element) {
        ELEMENTS.put(element.id(), element);
    }

    public static HudElement get(String id) {
        return ELEMENTS.get(id);
    }

    public static Collection<HudElement> all() {
        return Collections.unmodifiableCollection(ELEMENTS.values());
    }

    public static void install() {
        if (installed) return;
        installed = true;
        for (HudElement e : ELEMENTS.values()) {
            Identifier id = Identifier.fromNamespaceAndPath("blackaddons", e.id().replace(':', '_').replace('/', '_'));
            HudElementRegistry.addLast(id,
                    (graphics, tracker) -> {
                        if (e.enabled()) {
                            e.render(graphics, tracker);
                        }
                    });
        }
    }
}
