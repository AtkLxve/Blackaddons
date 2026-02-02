package org.blackum.blackaddons.mixin.client;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyBindingAccessor {
    // In 1.21.1 Mojang mappings, the field is named 'key'
    @Accessor("key")
    InputConstants.Key getBoundKey();
}