package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class FullbrightMixin {

    @Inject(method = "extract", at = @At("TAIL"))
    private void onExtract(LightmapRenderState state, float tick, CallbackInfo ci) {
        if (ConfigManager.data.legitFullbrightEnabled) {
            state.nightVisionEffectIntensity = 1.0f;
            state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
            state.darknessEffectScale = 0.0f;
            state.bossOverlayWorldDarkening = 0.0f;
            state.brightness = 10.0f;
        }
    }
}