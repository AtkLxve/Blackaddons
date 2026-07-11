package org.blackum.blackaddons.gui.render.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

public class CustomTexturedRenderState implements GuiElementRenderState {

    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2f pose;
    private final float x0, y0, x1, y1;
    private final float u0, v0, u1, v1, u2, v2, u3, v3;
    private final int color;
    private final ScreenRectangle scissorArea;
    private final ScreenRectangle bounds;

    public CustomTexturedRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                                     float x0, float y0, float x1, float y1,
                                     float u0, float v0, float u1, float v1,
                                     float u2, float v2, float u3, float v3,
                                     int color, ScreenRectangle scissorArea) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.pose = pose;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        this.u3 = u3;
        this.v3 = v3;
        this.color = color;
        this.scissorArea = scissorArea;

        ScreenRectangle raw = new ScreenRectangle((int) x0, (int) y0,
                Math.max(1, (int) (x1 - x0 + 0.5f)),
                Math.max(1, (int) (y1 - y0 + 0.5f)));
        ScreenRectangle transformed = raw.transformMaxBounds(pose);
        this.bounds = (scissorArea != null && transformed != null) ? scissorArea.intersection(transformed) : transformed;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        float ax = pose.m00() * x0 + pose.m10() * y0 + pose.m20();
        float ay = pose.m01() * x0 + pose.m11() * y0 + pose.m21();
        float bx = pose.m00() * x0 + pose.m10() * y1 + pose.m20();
        float by = pose.m01() * x0 + pose.m11() * y1 + pose.m21();
        float cx = pose.m00() * x1 + pose.m10() * y1 + pose.m20();
        float cy = pose.m01() * x1 + pose.m11() * y1 + pose.m21();
        float dx = pose.m00() * x1 + pose.m10() * y0 + pose.m20();
        float dy = pose.m01() * x1 + pose.m11() * y0 + pose.m21();

        consumer.addVertex(ax, ay, 0.0f).setColor(color).setUv(u0, v0).setUv2(0, 240);
        consumer.addVertex(bx, by, 0.0f).setColor(color).setUv(u1, v1).setUv2(0, 240);
        consumer.addVertex(cx, cy, 0.0f).setColor(color).setUv(u2, v2).setUv2(0, 240);
        consumer.addVertex(dx, dy, 0.0f).setColor(color).setUv(u3, v3).setUv2(0, 240);
    }

    @Override public RenderPipeline pipeline() { return pipeline; }
    @Override public TextureSetup textureSetup() { return textureSetup; }
    @Override public ScreenRectangle scissorArea() { return scissorArea; }
    @Override public ScreenRectangle bounds() { return bounds; }
}
