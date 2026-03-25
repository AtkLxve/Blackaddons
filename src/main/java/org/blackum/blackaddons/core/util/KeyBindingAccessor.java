package org.blackum.blackaddons.core.util;

import com.mojang.blaze3d.platform.InputConstants;

public interface KeyBindingAccessor {
    InputConstants.Key getBoundKey();
    void setBlackaddonsIsDown(boolean isDown);

    void blackaddons$setForced(boolean forced);
    boolean blackaddons$isForced();
}
