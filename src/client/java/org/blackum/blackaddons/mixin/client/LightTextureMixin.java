package org.blackum.blackaddons.mixin.client;

import net.minecraft.client.renderer.LightTexture;
import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.legit.LegitOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), index = 0)
    private float forceGamma(float value) {
        if (ConfigManager.data.legitFullbrightEnabled) {
            return 100.0F;
        }
        return value;
    }
}
