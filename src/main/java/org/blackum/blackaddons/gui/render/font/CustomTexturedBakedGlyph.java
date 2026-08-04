package org.blackum.blackaddons.gui.render.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import java.util.function.Supplier;

public class CustomTexturedBakedGlyph implements BakedGlyph {

    private static final ThreadLocal<Vector4f[]> TMP_VECTORS = ThreadLocal.withInitial(() -> new Vector4f[] {
            new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()
    });

    public interface GeometrySupplier {
        float[] getBounds(float x, float y);
    }

    public interface UvSupplier {
        float[] getUvs();
    }

    private final GlyphInfo glyphInfo;
    private final GeometrySupplier geometrySupplier;
    private final UvSupplier uvSupplier;
    private final Supplier<RenderType> seeThroughRenderTypeSupplier;
    private final Supplier<RenderType> depthRenderTypeSupplier;
    private final Supplier<GpuTextureView> textureViewSupplier;
    private final RenderPipeline guiPipeline;

    public CustomTexturedBakedGlyph(GlyphInfo glyphInfo,
                                    GeometrySupplier geometrySupplier,
                                    UvSupplier uvSupplier,
                                    Supplier<RenderType> renderTypeSupplier,
                                    Supplier<GpuTextureView> textureViewSupplier,
                                    RenderPipeline guiPipeline) {
        this(glyphInfo, geometrySupplier, uvSupplier, renderTypeSupplier, renderTypeSupplier, textureViewSupplier, guiPipeline);
    }

    public CustomTexturedBakedGlyph(GlyphInfo glyphInfo,
                                    GeometrySupplier geometrySupplier,
                                    UvSupplier uvSupplier,
                                    Supplier<RenderType> seeThroughRenderTypeSupplier,
                                    Supplier<RenderType> depthRenderTypeSupplier,
                                    Supplier<GpuTextureView> textureViewSupplier,
                                    RenderPipeline guiPipeline) {
        this.glyphInfo = glyphInfo;
        this.geometrySupplier = geometrySupplier;
        this.uvSupplier = uvSupplier;
        this.seeThroughRenderTypeSupplier = seeThroughRenderTypeSupplier;
        this.depthRenderTypeSupplier = depthRenderTypeSupplier;
        this.textureViewSupplier = textureViewSupplier;
        this.guiPipeline = guiPipeline;
    }

    @Override
    public GlyphInfo info() {
        return glyphInfo;
    }

    @Override
    public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style,
            float boldOffset, float shadowOffset) {
        return new Renderable(x, y, style, geometrySupplier, uvSupplier,
                seeThroughRenderTypeSupplier, depthRenderTypeSupplier, textureViewSupplier, guiPipeline);
    }

    public static class Renderable implements TextRenderable.Styled {
        private final Style style;
        private final Supplier<RenderType> seeThroughRenderTypeSupplier;
        private final Supplier<RenderType> depthRenderTypeSupplier;
        private final Supplier<GpuTextureView> textureViewSupplier;
        private final RenderPipeline guiPipeline;
        private final float ex0, ey0, ex1, ey1;
        private final float[] uvs;

        public Renderable(float x, float y, Style style,
                          GeometrySupplier geometrySupplier,
                          UvSupplier uvSupplier,
                          Supplier<RenderType> seeThroughRenderTypeSupplier,
                          Supplier<RenderType> depthRenderTypeSupplier,
                          Supplier<GpuTextureView> textureViewSupplier,
                          RenderPipeline guiPipeline) {
            this.style = style;
            this.seeThroughRenderTypeSupplier = seeThroughRenderTypeSupplier;
            this.depthRenderTypeSupplier = depthRenderTypeSupplier;
            this.textureViewSupplier = textureViewSupplier;
            this.guiPipeline = guiPipeline;

            float[] bounds = geometrySupplier.getBounds(x, y);
            this.ex0 = bounds[0];
            this.ey0 = bounds[1];
            this.ex1 = bounds[2];
            this.ey1 = bounds[3];
            this.uvs = uvSupplier.getUvs();
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            if (displayMode == Font.DisplayMode.SEE_THROUGH) {
                return seeThroughRenderTypeSupplier.get();
            } else {
                return depthRenderTypeSupplier.get();
            }
        }

        @Override
        public GpuTextureView textureView() {
            return textureViewSupplier.get();
        }

        @Override
        public RenderPipeline guiPipeline() {
            return guiPipeline;
        }

        @Override
        public Style style() {
            return style;
        }

        @Override
        public void render(Matrix4fc matrix, VertexConsumer consumer, int light, boolean isGui) {
            Vector4f[] tmps = TMP_VECTORS.get();
            Vector4f v1p = tmps[0].set(ex0, ey0, 0, 1).mul(matrix);
            Vector4f v2p = tmps[1].set(ex0, ey1, 0, 1).mul(matrix);
            Vector4f v3p = tmps[2].set(ex1, ey1, 0, 1).mul(matrix);
            Vector4f v4p = tmps[3].set(ex1, ey0, 0, 1).mul(matrix);

            int packedLight = isGui ? 0xF000F0 : light;

            consumer.addVertex(v1p.x(), v1p.y(), v1p.z()).setColor(0xFFFFFFFF).setUv(uvs[0], uvs[1]).setUv2(packedLight & 0xFFFF, (packedLight >> 16) & 0xFFFF);
            consumer.addVertex(v2p.x(), v2p.y(), v2p.z()).setColor(0xFFFFFFFF).setUv(uvs[2], uvs[3]).setUv2(packedLight & 0xFFFF, (packedLight >> 16) & 0xFFFF);
            consumer.addVertex(v3p.x(), v3p.y(), v3p.z()).setColor(0xFFFFFFFF).setUv(uvs[4], uvs[5]).setUv2(packedLight & 0xFFFF, (packedLight >> 16) & 0xFFFF);
            consumer.addVertex(v4p.x(), v4p.y(), v4p.z()).setColor(0xFFFFFFFF).setUv(uvs[6], uvs[7]).setUv2(packedLight & 0xFFFF, (packedLight >> 16) & 0xFFFF);
        }

        @Override public float left() { return ex0; }
        @Override public float top()  { return ey0; }
        @Override public float right() { return ex1; }
        @Override public float bottom() { return ey1; }
    }
}
