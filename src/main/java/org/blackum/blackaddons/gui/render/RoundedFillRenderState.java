package org.blackum.blackaddons.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public class RoundedFillRenderState implements GuiElementRenderState {

    private final Matrix3x2f pose;
    private final float ex0, ey0, ex1, ey1;
    private final float u0, v0, u1, v1;
    private final int uv2x, uv2y;
    private final int color;
    private final ScreenRectangle scissorArea;
    private final ScreenRectangle bounds;

    public RoundedFillRenderState(Matrix3x2fc pose, float x0, float y0, float x1, float y1,
                                   float cornerRadius, int color, 
                                   boolean outerTop, boolean outerRight, boolean outerBottom, boolean outerLeft, 
                                   ScreenRectangle scissorArea) {
        this.pose = new Matrix3x2f(pose);
        this.color = color;
        this.scissorArea = scissorArea;

        float cx = (x0 + x1) * 0.5f;
        float cy = (y0 + y1) * 0.5f;
        float trueHalfW = (x1 - x0) * 0.5f;
        float trueHalfH = (y1 - y0) * 0.5f;

        this.ex0 = x0 - (outerLeft ? 1.0f : 0.0f);
        this.ex1 = x1 + (outerRight ? 1.0f : 0.0f);
        this.ey0 = y0 - (outerTop ? 1.0f : 0.0f);
        this.ey1 = y1 + (outerBottom ? 1.0f : 0.0f);

        this.u0 = this.ex0 - cx;
        this.u1 = this.ex1 - cx;
        this.v0 = this.ey0 - cy;
        this.v1 = this.ey1 - cy;

        int packedW = Math.round(trueHalfW * 4.0f) & 0x7FF;
        int packedH = Math.round(trueHalfH * 4.0f) & 0x7FF;
        
        float clampedRadius = Math.min(cornerRadius, Math.min(trueHalfW, trueHalfH));
        int packedR = Math.round(clampedRadius * 2.0f) & 0x1F;

        int edgeFlags = (outerTop ? 1 : 0) | (outerRight ? 2 : 0) | (outerBottom ? 4 : 0) | (outerLeft ? 8 : 0);

        this.uv2x = packedW | (edgeFlags << 11);
        this.uv2y = packedH | (packedR << 11);

        ScreenRectangle raw = new ScreenRectangle((int) x0, (int) y0,
                Math.max(1, (int) (x1 - x0 + 0.5f)),
                Math.max(1, (int) (y1 - y0 + 0.5f)));
        ScreenRectangle transformed = raw.transformMaxBounds(this.pose);
        this.bounds = (scissorArea != null && transformed != null)
                ? scissorArea.intersection(transformed)
                : transformed;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        float ax = pose.m00() * ex0 + pose.m10() * ey0 + pose.m20();
        float ay = pose.m01() * ex0 + pose.m11() * ey0 + pose.m21();
        float bx = pose.m00() * ex0 + pose.m10() * ey1 + pose.m20();
        float by = pose.m01() * ex0 + pose.m11() * ey1 + pose.m21();
        float cx2 = pose.m00() * ex1 + pose.m10() * ey1 + pose.m20();
        float cy2 = pose.m01() * ex1 + pose.m11() * ey1 + pose.m21();
        float dx = pose.m00() * ex1 + pose.m10() * ey0 + pose.m20();
        float dy = pose.m01() * ex1 + pose.m11() * ey0 + pose.m21();

        consumer.addVertex(ax, ay, 0.0f).setColor(color).setUv(u0, v0).setUv2(uv2x, uv2y);
        consumer.addVertex(bx, by, 0.0f).setColor(color).setUv(u0, v1).setUv2(uv2x, uv2y);
        consumer.addVertex(cx2, cy2, 0.0f).setColor(color).setUv(u1, v1).setUv2(uv2x, uv2y);
        consumer.addVertex(dx, dy, 0.0f).setColor(color).setUv(u1, v0).setUv2(uv2x, uv2y);
    }

    @Override
    public RenderPipeline pipeline() {
        return BlackaddonsRenderPipelines.ROUNDED_FILL;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }
}
