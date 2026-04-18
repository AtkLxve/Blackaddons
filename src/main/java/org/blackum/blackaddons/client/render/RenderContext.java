package org.blackum.blackaddons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

public class RenderContext {
    private final PoseStack poseStack;
    private final MultiBufferSource.BufferSource bufferSource;
    private final float partialTicks;

    public RenderContext(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTicks) {
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

    public MultiBufferSource.BufferSource getBufferSource() {
        return this.bufferSource;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }
}
