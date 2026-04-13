package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Shadow @Final GuiRenderState renderState;
}
