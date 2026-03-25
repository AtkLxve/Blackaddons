package org.blackum.blackaddons.mixin.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.blackum.blackaddons.core.util.KeyBindingAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements KeyBindingAccessor {
    @Accessor("key")
    public abstract InputConstants.Key getBoundKey();

    @Accessor("isDown")
    public abstract void setBlackaddonsIsDown(boolean isDown);

    @Unique
    private boolean blackaddons$isForced;

    @Override
    public boolean blackaddons$isForced() {
        return blackaddons$isForced;
    }

    @Override
    public void blackaddons$setForced(boolean forced) {
        this.blackaddons$isForced = forced;
    }
}
