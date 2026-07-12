package org.blackum.blackaddons.common.util.mc;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.common.constants.Constants;
import java.util.HashSet;
import java.util.Set;

public final class KeybindManager {
    private static final Set<Integer> pressedMouseButtons = new HashSet<>();

    private KeybindManager() {
    }

    public static void setMouseButtonState(int button, boolean pressed) {
        if (pressed) {
            pressedMouseButtons.add(button);
        } else {
            pressedMouseButtons.remove(button);
        }
    }

    public static boolean isKeyPressed(int keyCode) {
        if (keyCode < 0) {
            return false;
        }
        if (keyCode >= Constants.MOUSE_BIND_OFFSET) {
            return pressedMouseButtons.contains(keyCode - Constants.MOUSE_BIND_OFFSET);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return false;
        }
        return InputConstants.isKeyDown(mc.getWindow(), keyCode);
    }
}
