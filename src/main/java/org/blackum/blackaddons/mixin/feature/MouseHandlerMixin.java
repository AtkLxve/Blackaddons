package org.blackum.blackaddons.mixin.feature;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.blackum.blackaddons.feature.cheat.Perspective;
import org.blackum.blackaddons.common.util.mc.KeybindManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("TAIL"))
    private void blackaddons$trackMouseButtons(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        KeybindManager.setMouseButtonState(button.button(), action != 0);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void blackaddons$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (Perspective.getInstance().isActive()) {
            Perspective.getInstance().onMouseScroll(yOffset);
            ci.cancel();
        }
    }
}
