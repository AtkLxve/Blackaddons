package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.renderer.ShaderLoader;
import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {

    @Inject(method = "apply(Lnet/minecraft/client/renderer/ShaderLoader$Definitions;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiler/ProfilerFiller;)V", at = @At("TAIL"))
    private void blackaddons$reloadPipelines(CallbackInfo info) {
        BlackaddonsRenderPipelines.precompile();
    }
}
