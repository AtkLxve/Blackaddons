package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.Gui;
import org.blackum.blackaddons.feature.waypoint.WaypointActionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
/*
@Mixin(Gui.class)
public abstract class MinecraftMixin {

    @Shadow
    public Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onBeforeSetScreen(Screen screen, CallbackInfo ci) {
        if (this.screen != null && screen == null) {
            WaypointActionManager.getInstance().onGuiClosed();
        }
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        invokeResizeDisplay();
    }

    private void invokeResizeDisplay() {
        for (String name : new String[] { "resizeDisplay", "method_15993", "a" }) {
            try {
                var method = Minecraft.class.getDeclaredMethod(name);
                method.setAccessible(true);
                method.invoke(Minecraft.getInstance());
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
*/
//?} else {
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onBeforeSetScreen(Screen screen, CallbackInfo ci) {
        if (this.screen != null && screen == null) {
            WaypointActionManager.getInstance().onGuiClosed();
        }
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        invokeResizeDisplay();
    }

    private void invokeResizeDisplay() {
        for (String name : new String[] { "resizeDisplay", "method_15993", "a" }) {
            try {
                var method = Minecraft.class.getDeclaredMethod(name);
                method.setAccessible(true);
                method.invoke(Minecraft.getInstance());
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
//?}
