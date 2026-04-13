package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.renderer.ShaderManager;
import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
public class ShaderManagerMixin {
    @Inject(method = "apply(Lnet/minecraft/client/renderer/ShaderManager$Configs;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void precompilePipelines(CallbackInfo ci) {
        BlackaddonsRenderPipelines.precompile();
    }
}
