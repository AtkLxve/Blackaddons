package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.KeyMapping;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.feature.TabListToggleHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
/*
import net.minecraft.client.gui.Hud;

@Mixin(Hud.class)
public class GuiMixin {

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigManager.data.hideStatusEffects) {
            ci.cancel();
        }
    }

    @Redirect(method = "extractTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean onPlayerListKeyIsDown(KeyMapping keyMapping) {
        return TabListToggleHandler.shouldShowTabList(keyMapping);
    }
}
*/
//?} else {
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigManager.data.hideStatusEffects) {
            ci.cancel();
        }
    }

    @Redirect(method = "extractTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean onPlayerListKeyIsDown(KeyMapping keyMapping) {
        return TabListToggleHandler.shouldShowTabList(keyMapping);
    }
}
//?}
