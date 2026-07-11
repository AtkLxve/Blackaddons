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
import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class CustomEmojiBakedGlyph implements BakedGlyph {

    private static final ThreadLocal<Vector4f[]> TMP_VECTORS = ThreadLocal.withInitial(() -> new Vector4f[] {
            new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()
    });

    private final int codepoint;
    private final GlyphInfo glyphInfo;

    public CustomEmojiBakedGlyph(int codepoint) {
        this.codepoint = codepoint;
        this.glyphInfo = () -> {
            if (CustomFontRenderer.isVariationSelector(codepoint)) {
                return 0.0f;
            }
            float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
            return emojiSize + 1.0f;
        };
    }

    @Override
    public GlyphInfo info() {
        return glyphInfo;
    }

    @Override
    public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style,
            float boldOffset, float shadowOffset) {
        return new CustomEmojiRenderable(codepoint, x, y, style);
    }

    public static class CustomEmojiRenderable implements TextRenderable.Styled {
        private final int codepoint;
        private final float x;
        private final float y;
        private final Style style;

        public CustomEmojiRenderable(int codepoint, float x, float y, Style style) {
            this.codepoint = codepoint;
            this.x = x;
            this.y = y;
            this.style = style;
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            EmojiManager.EmojiTexture emojiTex = EmojiManager.getEmojiTexture(codepoint);
            if (emojiTex == null) {
                return null;
            }
            return (RenderType) McCompat.createTextRenderType("emoji_3d",
                    BlackaddonsRenderPipelines.PLAIN_TEXTURED, emojiTex.location);
        }

        @Override
        public GpuTextureView textureView() {
            EmojiManager.EmojiTexture emojiTex = EmojiManager.getEmojiTexture(codepoint);
            return emojiTex != null ? emojiTex.textureView : null;
        }

        @Override
        public RenderPipeline guiPipeline() {
            return BlackaddonsRenderPipelines.PLAIN_TEXTURED;
        }

        @Override
        public Style style() {
            return style;
        }

        @Override
        public void render(Matrix4fc matrix4f, VertexConsumer vertexConsumer, int light, boolean isGui) {
            if (CustomFontRenderer.isVariationSelector(codepoint)) {
                return;
            }
            EmojiManager.EmojiTexture emojiTex = EmojiManager.getEmojiTexture(codepoint);
            if (emojiTex == null) {
                return;
            }

            CustomFontRenderer renderer = CustomFontRenderer.getInstance();
            float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
            float baseline = ConfigManager.data.customTextEnabled ? renderer.getCachedBaseline() : 7.0f;
            float ey1 = y + baseline + emojiSize * 0.1f;
            float ey0 = ey1 - emojiSize;
            float ex0 = x;
            float ex1 = ex0 + emojiSize;

            Vector4f[] tmps = TMP_VECTORS.get();
            Vector4f v1p = tmps[0].set(ex0, ey0, 0, 1).mul(matrix4f);
            Vector4f v2p = tmps[1].set(ex0, ey1, 0, 1).mul(matrix4f);
            Vector4f v3p = tmps[2].set(ex1, ey1, 0, 1).mul(matrix4f);
            Vector4f v4p = tmps[3].set(ex1, ey0, 0, 1).mul(matrix4f);

            vertexConsumer.addVertex(v1p.x(), v1p.y(), v1p.z()).setColor(0xFFFFFFFF).setUv(0f, 0f).setUv2(0, 240);
            vertexConsumer.addVertex(v2p.x(), v2p.y(), v2p.z()).setColor(0xFFFFFFFF).setUv(0f, 1f).setUv2(0, 240);
            vertexConsumer.addVertex(v3p.x(), v3p.y(), v3p.z()).setColor(0xFFFFFFFF).setUv(1f, 1f).setUv2(0, 240);
            vertexConsumer.addVertex(v4p.x(), v4p.y(), v4p.z()).setColor(0xFFFFFFFF).setUv(1f, 0f).setUv2(0, 240);
        }

        @Override public float left() { return x; }
        @Override public float top() {
            float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
            float baseline = ConfigManager.data.customTextEnabled ? CustomFontRenderer.getInstance().getCachedBaseline() : 7.0f;
            return y + baseline - emojiSize;
        }
        @Override public float right() {
            float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
            return x + emojiSize;
        }
        @Override public float bottom() {
            float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
            float baseline = ConfigManager.data.customTextEnabled ? CustomFontRenderer.getInstance().getCachedBaseline() : 7.0f;
            return y + baseline + emojiSize * 0.1f;
        }
    }
}
