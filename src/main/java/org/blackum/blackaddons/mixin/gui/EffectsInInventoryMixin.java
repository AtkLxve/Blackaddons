package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EffectsInInventory.class)
public class EffectsInInventoryMixin {

//? if <1.21.11 {
    /*@Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (ConfigManager.data.hideStatusEffects) {
            ci.cancel();
        }
    }
*///? } else {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (ConfigManager.data.hideStatusEffects) {
            ci.cancel();
        }
    }
//? }
}
