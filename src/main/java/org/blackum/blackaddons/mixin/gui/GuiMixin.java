package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.manager.CustomNameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class GuiMixin {

    @ModifyVariable(method = "setTitle", at = @At("HEAD"), argsOnly = true)
    private Component onSetTitle(Component component) {
        if (component == null)
            return null;
        return CustomNameManager.getInstance().replaceNames(component);
    }

    @ModifyVariable(method = "setSubtitle", at = @At("HEAD"), argsOnly = true)
    private Component onSetSubtitle(Component component) {
        if (component == null)
            return null;
        return CustomNameManager.getInstance().replaceNames(component);
    }

    @ModifyVariable(method = "setOverlayMessage", at = @At("HEAD"), argsOnly = true)
    private Component onSetActionBar(Component component) {
        if (component == null)
            return null;
        return CustomNameManager.getInstance().replaceNames(component);
    }
}
