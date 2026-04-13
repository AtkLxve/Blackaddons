package org.blackum.blackaddons.gui.render.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public class VectorGlyphRenderState implements GuiElementRenderState {

    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2fc pose;
    private final float x0, y0, x1, y1;
    private final int color;
    private final @Nullable ScreenRectangle scissorArea;
    private final @Nullable ScreenRectangle bounds;

    public VectorGlyphRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
                                   float x0, float y0, float x1, float y1, int color,
                                   @Nullable ScreenRectangle scissorArea) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.pose = pose;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.color = color;
        this.scissorArea = scissorArea;

        ScreenRectangle raw = new ScreenRectangle((int) x0, (int) y0, Math.max(1, (int) (x1 - x0 + 0.5f)), Math.max(1, (int) (y1 - y0 + 0.5f)));
        ScreenRectangle transformed = raw.transformMaxBounds(pose);
        this.bounds = (scissorArea != null && transformed != null) ? scissorArea.intersection(transformed) : transformed;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        consumer.addVertexWith2DPose(pose, x0, y0).setUv(0f, 0f).setColor(color);
        consumer.addVertexWith2DPose(pose, x0, y1).setUv(0f, 1f).setColor(color);
        consumer.addVertexWith2DPose(pose, x1, y1).setUv(1f, 1f).setColor(color);
        consumer.addVertexWith2DPose(pose, x1, y0).setUv(1f, 0f).setColor(color);
    }

    @Override public RenderPipeline pipeline() { return pipeline; }
    @Override public TextureSetup textureSetup() { return textureSetup; }
    @Override public @Nullable ScreenRectangle scissorArea() { return scissorArea; }
    @Override public @Nullable ScreenRectangle bounds() { return bounds; }
}
