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
    private final Supplier<RenderType> renderTypeSupplier;
    private final Supplier<GpuTextureView> textureViewSupplier;
    private final RenderPipeline guiPipeline;

    public CustomTexturedBakedGlyph(GlyphInfo glyphInfo,
                                    GeometrySupplier geometrySupplier,
                                    UvSupplier uvSupplier,
                                    Supplier<RenderType> renderTypeSupplier,
                                    Supplier<GpuTextureView> textureViewSupplier,
                                    RenderPipeline guiPipeline) {
        this.glyphInfo = glyphInfo;
        this.geometrySupplier = geometrySupplier;
        this.uvSupplier = uvSupplier;
        this.renderTypeSupplier = renderTypeSupplier;
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
                renderTypeSupplier, textureViewSupplier, guiPipeline);
    }

    public static class Renderable implements TextRenderable.Styled {
        private final float x;
        private final float y;
        private final Style style;
        private final GeometrySupplier geometrySupplier;
        private final UvSupplier uvSupplier;
        private final Supplier<RenderType> renderTypeSupplier;
        private final Supplier<GpuTextureView> textureViewSupplier;
        private final RenderPipeline guiPipeline;

        public Renderable(float x, float y, Style style,
                          GeometrySupplier geometrySupplier,
                          UvSupplier uvSupplier,
                          Supplier<RenderType> renderTypeSupplier,
                          Supplier<GpuTextureView> textureViewSupplier,
                          RenderPipeline guiPipeline) {
            this.x = x;
            this.y = y;
            this.style = style;
            this.geometrySupplier = geometrySupplier;
            this.uvSupplier = uvSupplier;
            this.renderTypeSupplier = renderTypeSupplier;
            this.textureViewSupplier = textureViewSupplier;
            this.guiPipeline = guiPipeline;
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            return renderTypeSupplier.get();
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
            float[] bounds = geometrySupplier.getBounds(x, y);
            float[] uvs = uvSupplier.getUvs();
            float ex0 = bounds[0];
            float ey0 = bounds[1];
            float ex1 = bounds[2];
            float ey1 = bounds[3];

            Vector4f[] tmps = TMP_VECTORS.get();
            Vector4f v1p = tmps[0].set(ex0, ey0, 0, 1).mul(matrix);
            Vector4f v2p = tmps[1].set(ex0, ey1, 0, 1).mul(matrix);
            Vector4f v3p = tmps[2].set(ex1, ey1, 0, 1).mul(matrix);
            Vector4f v4p = tmps[3].set(ex1, ey0, 0, 1).mul(matrix);

            consumer.addVertex(v1p.x(), v1p.y(), v1p.z()).setColor(0xFFFFFFFF).setUv(uvs[0], uvs[1]).setUv2(0, 240);
            consumer.addVertex(v2p.x(), v2p.y(), v2p.z()).setColor(0xFFFFFFFF).setUv(uvs[2], uvs[3]).setUv2(0, 240);
            consumer.addVertex(v3p.x(), v3p.y(), v3p.z()).setColor(0xFFFFFFFF).setUv(uvs[4], uvs[5]).setUv2(0, 240);
            consumer.addVertex(v4p.x(), v4p.y(), v4p.z()).setColor(0xFFFFFFFF).setUv(uvs[6], uvs[7]).setUv2(0, 240);
        }

        @Override public float left() { return geometrySupplier.getBounds(x, y)[0]; }
        @Override public float top()  { return geometrySupplier.getBounds(x, y)[1]; }
        @Override public float right() { return geometrySupplier.getBounds(x, y)[2]; }
        @Override public float bottom() { return geometrySupplier.getBounds(x, y)[3]; }
    }
}
