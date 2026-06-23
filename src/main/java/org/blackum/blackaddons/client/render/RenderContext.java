package org.blackum.blackaddons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
//?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import org.joml.Matrix4f;

public class RenderContext {
    private final PoseStack poseStack;
//? if >=26.2 {
/*    private final SubmitNodeCollector bufferSource;*/
//?} else {
    private final MultiBufferSource.BufferSource bufferSource;
//?}
    private final float partialTicks;

    public RenderContext(PoseStack poseStack,
//? if >=26.2 {
/*                         SubmitNodeCollector bufferSource,*/
//?} else {
                         MultiBufferSource.BufferSource bufferSource,
//?}
                         float partialTicks) {
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.partialTicks = partialTicks;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    public Matrix4f getMatrix() {
        return this.poseStack.last().pose();
    }

//? if >=26.2 {
/*    public SubmitNodeCollector getBufferSource() {*/
//?} else {
    public MultiBufferSource.BufferSource getBufferSource() {
//?}
        return this.bufferSource;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }
}
