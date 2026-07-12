package org.blackum.blackaddons.gui.render.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Style;
import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import net.minecraft.client.renderer.rendertype.RenderType;

public class CustomBakedGlyph implements BakedGlyph {
    private static final ThreadLocal<Vector4f[]> TMP_VECTORS = ThreadLocal.withInitial(() -> new Vector4f[] {
            new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()
    });

    private final int codepoint;
    private final CustomFontManager.GlyphData glyphData;
    private final GlyphInfo glyphInfo;

    public CustomBakedGlyph(int codepoint, CustomFontManager.GlyphData glyphData) {
        this.codepoint = codepoint;
        this.glyphData = glyphData;
        this.glyphInfo = () -> CustomFontRenderer.getInstance().getCachedScale() * glyphData.advance + ConfigManager.data.customFontSpacing;
    }

    @Override
    public GlyphInfo info() {
        return glyphInfo;
    }

    @Override
    public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style,
            float boldOffset, float shadowOffset) {
        return new CustomTextRenderable(codepoint, glyphData, x, y, color, shadowColor, style, boldOffset,
                shadowOffset);
    }

    public static class CustomTextRenderable implements TextRenderable.Styled {
        private final int codepoint;
        private final CustomFontManager.GlyphData glyphData;
        private final float x;
        private final float y;
        private final int color;
        private final int shadowColor;
        private final Style style;
        private final float boldOffset;
        private final float shadowOffset;
        private Font.DisplayMode displayMode = Font.DisplayMode.NORMAL;

        public CustomTextRenderable(int codepoint, CustomFontManager.GlyphData glyphData, float x, float y, int color,
                int shadowColor, Style style,
                float boldOffset, float shadowOffset) {
            this.codepoint = codepoint;
            this.glyphData = glyphData;
            this.x = x;
            this.y = y;
            int argb = color;
            if (style != null && style.getColor() != null) {
                int styleRgb = style.getColor().getValue();
                argb = (argb & 0xFF000000) | (styleRgb & 0x00FFFFFF);
            }
            this.color = (argb & 0xFF000000) == 0 ? (argb | 0xFF000000) : argb;
            this.shadowColor = (shadowColor & 0xFF000000) == 0 ? (shadowColor | 0xFF000000) : shadowColor;
            this.style = style;
            this.boldOffset = boldOffset;
            this.shadowOffset = shadowOffset;
        }

        @Override
        public Style style() {
            return style;
        }

        @Override
        public void render(Matrix4fc matrix4f, VertexConsumer vertexConsumer, int light, boolean isGui) {
            CustomFontRenderer renderer = CustomFontRenderer.getInstance();
            CustomFontRenderer.SdfGlyph sdfGlyph = renderer.getSdfGlyph(codepoint);
            if (sdfGlyph == null) {
                return;
            }

            float glyphScale = renderer.getCachedSdfRenderScale();
            float baseline = renderer.getCachedBaseline() - 2.5f;
            float x0 = x + sdfGlyph.xoff * glyphScale;
            float y0 = y + baseline + sdfGlyph.yoff * glyphScale;
            float x1 = x0 + sdfGlyph.width * glyphScale;
            float y1 = y0 + sdfGlyph.height * glyphScale;

            var cfg = ConfigManager.data;
            boolean bold = cfg.customFontBold || (style != null && style.isBold());
            boolean italic = cfg.customFontItalic || (style != null && style.isItalic());
            float boldStrength = bold ? cfg.customFontBoldStrength : 0f;
            float skew = italic ? cfg.customFontItalicSlant * (y1 - y0) : 0f;
            boolean isSign = (displayMode != Font.DisplayMode.NORMAL) || CustomFontRenderer.inOutlinePass;

            float z = isGui ? 0f : 0.02f;
            float shadowZ = isGui ? 0f : 0.005f;
            float outlineZ = isGui ? 0f : 0.01f;

            if (!isSign) {
                if (cfg.customFontShadow) {
                    submitQuad(vertexConsumer, matrix4f, x0 + cfg.customFontShadowOffsetX + shadowOffset,
                            y0 + cfg.customFontShadowOffsetY + shadowOffset,
                            x1 + cfg.customFontShadowOffsetX + shadowOffset,
                            y1 + cfg.customFontShadowOffsetY + shadowOffset, shadowZ,
                            sdfGlyph.u0, sdfGlyph.v0, sdfGlyph.u1, sdfGlyph.v1,
                            skew, boldStrength, ensureOpaque(cfg.customFontShadowColor), light, false);
                } else if ((shadowColor >>> 24) != 0) {
                    submitQuad(vertexConsumer, matrix4f, x0 + shadowOffset, y0 + shadowOffset,
                            x1 + shadowOffset, y1 + shadowOffset, shadowZ,
                            sdfGlyph.u0, sdfGlyph.v0, sdfGlyph.u1, sdfGlyph.v1,
                            skew, boldStrength, shadowColor, light, false);
                }

                if (cfg.customFontOutline) {
                    submitQuad(vertexConsumer, matrix4f, x0, y0, x1, y1, outlineZ,
                            sdfGlyph.u0, sdfGlyph.v0, sdfGlyph.u1, sdfGlyph.v1,
                            skew, -(cfg.customFontOutlineWidth + boldStrength),
                            ensureOpaque(cfg.customFontOutlineColor), light, false);
                }
            }

            submitQuad(vertexConsumer, matrix4f, x0, y0, x1, y1, z,
                    sdfGlyph.u0, sdfGlyph.v0, sdfGlyph.u1, sdfGlyph.v1,
                    skew, boldStrength, color, light, false);
        }

        private void submitQuad(VertexConsumer vc, Matrix4fc m,
                float lx0, float ly0, float lx1, float ly1, float lz,
                float u0, float v0, float u1, float v1,
                float skew, float effectZ, int c, int light, boolean hardEdge) {
            Vector4f[] tmps = TMP_VECTORS.get();
            Vector4f v1p = tmps[0].set(lx0 + skew, ly0, lz, 1).mul(m);
            Vector4f v2p = tmps[1].set(lx0, ly1, lz, 1).mul(m);
            Vector4f v3p = tmps[2].set(lx1, ly1, lz, 1).mul(m);
            Vector4f v4p = tmps[3].set(lx1 + skew, ly0, lz, 1).mul(m);
            int effectBits = Float.floatToRawIntBits(CustomFontRenderer.packShaderEffect(effectZ, hardEdge));
            vc.addVertex(v1p.x(), v1p.y(), v1p.z()).setColor(c).setUv(u0, v0).setUv2(effectBits & 0xFFFF,
                    (effectBits >> 16) & 0xFFFF);
            vc.addVertex(v2p.x(), v2p.y(), v2p.z()).setColor(c).setUv(u0, v1).setUv2(effectBits & 0xFFFF,
                    (effectBits >> 16) & 0xFFFF);
            vc.addVertex(v3p.x(), v3p.y(), v3p.z()).setColor(c).setUv(u1, v1).setUv2(effectBits & 0xFFFF,
                    (effectBits >> 16) & 0xFFFF);
            vc.addVertex(v4p.x(), v4p.y(), v4p.z()).setColor(c).setUv(u1, v0).setUv2(effectBits & 0xFFFF,
                    (effectBits >> 16) & 0xFFFF);
        }

        private static int ensureOpaque(int argb) {
            return (argb & 0xFF000000) == 0 ? (argb | 0xFF000000) : argb;
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            this.displayMode = displayMode;
            CustomFontRenderer renderer = CustomFontRenderer.getInstance();
            if (displayMode == Font.DisplayMode.SEE_THROUGH) {
                return renderer.getLayer(codepoint, glyphData);
            } else {
                return renderer.getDepthLayer(codepoint, glyphData);
            }
        }

        @Override
        public GpuTextureView textureView() {
            DynamicTexture texture = CustomFontRenderer.getInstance().getTexture(codepoint, glyphData);
            return texture.getTextureView();
        }

        @Override
        public RenderPipeline guiPipeline() {
            return BlackaddonsRenderPipelines.CUSTOM_TEXT;
        }

        @Override
        public float left() {
            return getVisualBounds().left();
        }

        @Override
        public float top() {
            return getVisualBounds().top();
        }

        @Override
        public float right() {
            return getVisualBounds().right();
        }

        @Override
        public float bottom() {
            return getVisualBounds().bottom();
        }

        private VisualBounds getVisualBounds() {
            CustomFontRenderer renderer = CustomFontRenderer.getInstance();
            float glyphScale = renderer.getCachedScale();
            float baseline = renderer.getCachedBaseline() - 2.5f;

            float left = x + glyphData.x0 * glyphScale;
            float top = y + baseline - glyphData.y1 * glyphScale;
            float right = x + glyphData.x1 * glyphScale;
            float bottom = y + baseline - glyphData.y0 * glyphScale;

            var cfg = ConfigManager.data;
            float aa = cfg.customFontAntiAliasing ? cfg.customFontAntiAliasingWidth : 0.0f;
            float effectPad = aa;
            float boldPad = 0.0f;
            if (cfg.customFontBold || (style != null && style.isBold())) {
                boldPad = cfg.customFontBoldStrength
                        + (boldOffset / Math.max(renderer.getCachedSdfRenderScale(), 0.001f));
                effectPad += boldPad;
            }
            if (cfg.customFontOutline) {
                effectPad = Math.max(effectPad, aa + cfg.customFontOutlineWidth + boldPad);
            }

            left -= effectPad;
            top -= effectPad;
            right += effectPad;
            bottom += effectPad;

            if (cfg.customFontItalic || (style != null && style.isItalic())) {
                float skew = cfg.customFontItalicSlant * (bottom - top);
                left = Math.min(left, left + skew);
                right = Math.max(right, right + skew);
            }

            float baseLeft = left;
            float baseTop = top;
            float baseRight = right;
            float baseBottom = bottom;
            if (cfg.customFontShadow) {
                float shadowDx = cfg.customFontShadowOffsetX + shadowOffset;
                float shadowDy = cfg.customFontShadowOffsetY + shadowOffset;
                left = Math.min(baseLeft, baseLeft + shadowDx);
                top = Math.min(baseTop, baseTop + shadowDy);
                right = Math.max(baseRight, baseRight + shadowDx);
                bottom = Math.max(baseBottom, baseBottom + shadowDy);
            } else if ((shadowColor >>> 24) != 0) {
                left = Math.min(baseLeft, baseLeft + shadowOffset);
                top = Math.min(baseTop, baseTop + shadowOffset);
                right = Math.max(baseRight, baseRight + shadowOffset);
                bottom = Math.max(baseBottom, baseBottom + shadowOffset);
            }

            return new VisualBounds(left, top, right, bottom);
        }

        private record VisualBounds(float left, float top, float right, float bottom) {
        }
    }
}
