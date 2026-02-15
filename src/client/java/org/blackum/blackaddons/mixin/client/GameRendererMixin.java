package org.blackum.blackaddons.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.blackum.blackaddons.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getNightVisionScale", at = @At("RETURN"), cancellable = true)
    private static void onGetNightVisionScale(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (ConfigManager.data.legitFullbrightEnabled) {
            cir.setReturnValue(0.0F);
        }
    }
}
