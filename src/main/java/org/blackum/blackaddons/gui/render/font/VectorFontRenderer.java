package org.blackum.blackaddons.gui.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
//? if <26.2 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.FormattedCharSequence;
import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.blackum.blackaddons.Blackaddons;
import net.minecraft.server.packs.resources.Resource;

public class VectorFontRenderer {
    private static VectorFontRenderer instance;
    private final Map<Integer, VectorFontManager.GlyphData> glyphCache = new HashMap<>();
    private final Map<Integer, DynamicTexture> textureCache = new HashMap<>();
    private final Map<Integer, Object> identifierCache = new HashMap<>();
    private final Map<Object, RenderType> layerCache = new HashMap<>();
    private boolean initialized = false;

    private float getScale() {
        return VectorFontManager.getInstance().getScaleForPixelHeight(
                ConfigManager.data.vectorTextScale);
    }

    private float getBaseline() {
        return VectorFontManager.getInstance().getAscent() * getScale();
    }

    public void init() {
        if (initialized) return;
        try {
            Optional<Resource> resource =
                    McCompat.findResource(Minecraft.getInstance().getResourceManager(), "blackaddons", "font/vector_font.ttf");
            if (resource.isEmpty()) {
                Blackaddons.LOGGER.error("[VectorFont] Font not found at blackaddons:font/vector_font.ttf");
                return;
            }
            try (InputStream is = resource.get().open()) {
                byte[] bytes = is.readAllBytes();
                ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                buffer.put(bytes).flip();
                VectorFontManager.init(buffer);
            }
            initialized = true;
            Blackaddons.LOGGER.info("[VectorFont] Initialized.");
        } catch (Exception e) {
            Blackaddons.LOGGER.error("[VectorFont] Init failed", e);
        }
    }

    public boolean isInitialized() { return initialized; }

    public static VectorFontRenderer getInstance() {
        if (instance == null) instance = new VectorFontRenderer();
        return instance;
    }

    public void drawStringGui(Matrix3x2fc pose, FormattedCharSequence text, float x, float y,
                              int baseColor, ScreenRectangle scissor, GuiRenderState renderState,
                              Font font) {
        text = new EmojiSequenceCharSequence(text);
        if (!initialized) init();
        if (!initialized) return;
        float[] curX = {x};

        boolean textEnabled = ConfigManager.data.customTextEnabled;
        float textScale = ConfigManager.data.customTextScale;
        float arrowSize = EmojiManager.getArrowSize();
        float arrowAdvance = EmojiManager.getArrowAdvance();
        float baseline = getBaseline();
        float emojiSize = textEnabled ? textScale : 9.0f;
        float yCenter = y + baseline - emojiSize / 2.0f;
        float ey0Arrow = yCenter - arrowSize / 2.0f;
        float ey1Arrow = yCenter + arrowSize / 2.0f;
        float ey1Emoji = y + baseline + emojiSize * 0.1f;
        float ey0Emoji = ey1Emoji - emojiSize;
        float emojiAdvance = emojiSize + 1.0f;

        text.accept((idx, style, cp) -> {
            if (CustomFontRenderer.isVariationSelector(cp)) {
                return true;
            }
            int color = baseColor;
            if (style.getColor() != null) {
                int styleRgb = style.getColor().getValue();
                int alpha = (baseColor & 0xFF000000);
                color = alpha | (styleRgb & 0x00FFFFFF);
            }
            int argb = (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;

            if (ConfigManager.data.customFontEmoji) {
                int arrowDir = EmojiManager.getArrowDirection(cp);
                if (arrowDir != -1) {
                    float ex0 = curX[0] + (arrowAdvance - arrowSize) / 2.0f;
                    float ex1 = ex0 + arrowSize;

                    TextureSetup setup = EmojiManager.getArrowSetup();
                    if (setup != null) {
                        renderArrowGui(pose, setup, arrowDir, ex0, ey0Arrow, ex1, ey1Arrow, scissor, renderState);
                    }
                    curX[0] += arrowAdvance;
                    return true;
                }

                if (EmojiManager.isEmoji(cp)) {
                    float ex0 = curX[0];
                    float ex1 = ex0 + emojiSize;

                    EmojiManager.EmojiTexture emojiTex = EmojiManager.getEmojiTexture(cp);
                    if (emojiTex != null) {
                        renderEmojiGui(pose, emojiTex.setup, ex0, ey0Emoji, ex1, ey1Emoji, scissor, renderState);
                    }
                    curX[0] += emojiAdvance;
                    return true;
                }
            }

            VectorFontManager.GlyphData glyph = glyphCache.computeIfAbsent(cp,
                    c -> VectorFontManager.getInstance().getGlyphData(c));
            if (glyph != null) {
                if (!glyph.curves.isEmpty()) {
                    renderGlyphGui(pose, cp, curX[0], y, argb, scissor, renderState);
                }
                curX[0] += getAdvance(cp);
            } else {
                FormattedCharSequence singleChar = sink -> sink.accept(0, style, cp);
                font.prepareText(singleChar, curX[0], y, argb, false, false, 0)
                        .visit(new Font.GlyphVisitor() {
                            @Override
                            public void acceptGlyph(TextRenderable.Styled styled) {
                                renderState.addGlyphToCurrentLayer(
                                        new GlyphRenderState(new Matrix3x2f(pose), styled, scissor));
                            }
                        });
                curX[0] += font.width(singleChar);
            }
            return true;
        });
    }

    public void drawStringGui(Matrix3x2fc pose, String text, float x, float y,
                              int color, ScreenRectangle scissor, GuiRenderState renderState) {
        text = EmojiManager.preprocessString(text);
        if (!initialized) init();
        if (!initialized) return;
        float curX = x;

        boolean textEnabled = ConfigManager.data.customTextEnabled;
        float textScale = ConfigManager.data.customTextScale;
        float arrowSize = EmojiManager.getArrowSize();
        float arrowAdvance = EmojiManager.getArrowAdvance();
        float baseline = getBaseline();
        float emojiSize = textEnabled ? textScale : 9.0f;
        float yCenter = y + baseline - emojiSize / 2.0f;
        float ey0Arrow = yCenter - arrowSize / 2.0f;
        float ey1Arrow = yCenter + arrowSize / 2.0f;
        float ey1Emoji = y + baseline + emojiSize * 0.1f;
        float ey0Emoji = ey1Emoji - emojiSize;
        float emojiAdvance = emojiSize + 1.0f;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (CustomFontRenderer.isVariationSelector(cp)) {
                i += Character.charCount(cp);
                continue;
            }
            if (ConfigManager.data.customFontEmoji) {
                int arrowDir = EmojiManager.getArrowDirection(cp);
                if (arrowDir != -1) {
                    float ex0 = curX + (arrowAdvance - arrowSize) / 2.0f;
                    float ex1 = ex0 + arrowSize;

                    TextureSetup setup = EmojiManager.getArrowSetup();
                    if (setup != null) {
                        renderArrowGui(pose, setup, arrowDir, ex0, ey0Arrow, ex1, ey1Arrow, scissor, renderState);
                    }
                    curX += arrowAdvance;
                    i += Character.charCount(cp);
                    continue;
                }

                if (EmojiManager.isEmoji(cp)) {
                    float ex0 = curX;
                    float ex1 = ex0 + emojiSize;

                    EmojiManager.EmojiTexture emojiTex = EmojiManager.getEmojiTexture(cp);
                    if (emojiTex != null) {
                        renderEmojiGui(pose, emojiTex.setup, ex0, ey0Emoji, ex1, ey1Emoji, scissor, renderState);
                    }
                    curX += emojiAdvance;
                    i += Character.charCount(cp);
                    continue;
                }
            }

            renderGlyphGui(pose, cp, curX, y, color, scissor, renderState);
            curX += getAdvance(cp);
            i += Character.charCount(cp);
        }
    }

    private void renderGlyphGui(Matrix3x2fc pose, int codepoint, float x, float y,
                                int color, ScreenRectangle scissor, GuiRenderState renderState) {
        VectorFontManager.GlyphData glyph = glyphCache.computeIfAbsent(codepoint,
                cp -> VectorFontManager.getInstance().getGlyphData(cp));
        if (glyph == null || glyph.curves.isEmpty()) return;

        DynamicTexture texture = textureCache.computeIfAbsent(codepoint, cp -> createCurveTexture(glyph));
        TextureSetup textureSetup = TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());

        float scale = getScale();
        float baseline = getBaseline();
        float sx0 = x + glyph.x0 * scale;
        float sy0 = y + baseline - glyph.y1 * scale;
        float sx1 = x + glyph.x1 * scale;
        float sy1 = y + baseline - glyph.y0 * scale;

        int argb = (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;
        renderState.addGlyphToCurrentLayer(new VectorGlyphRenderState(
                BlackaddonsRenderPipelines.VECTOR_TEXT, textureSetup, pose, sx0, sy0, sx1, sy1, argb, scissor));
    }

    private void renderEmojiGui(Matrix3x2fc pose, TextureSetup textureSetup,
                                float x0, float y0, float x1, float y1,
                                ScreenRectangle scissor, GuiRenderState renderState) {
        renderState.addGlyphToCurrentLayer(new CustomGlyphRenderState(
                BlackaddonsRenderPipelines.PLAIN_TEXTURED, textureSetup, new Matrix3x2f(pose),
                x0, y0, x1, y1,
                0f, 0f, 1f, 1f,
                0f, 0f, 0xFFFFFFFF, scissor));
    }

    private void renderArrowGui(Matrix3x2fc pose, TextureSetup textureSetup, int direction,
                                float x0, float y0, float x1, float y1,
                                ScreenRectangle scissor, GuiRenderState renderState) {
        float[] uvs = EmojiManager.getArrowUvs(direction);
        renderState.addGlyphToCurrentLayer(new CustomTexturedRenderState(
                BlackaddonsRenderPipelines.PLAIN_TEXTURED, textureSetup, new Matrix3x2f(pose),
                x0, y0, x1, y1,
                uvs[0], uvs[1], uvs[2], uvs[3], uvs[4], uvs[5], uvs[6], uvs[7],
                0xFFFFFFFF, scissor));
    }

    public boolean supportsAllGlyphs(FormattedCharSequence text) {
        VectorFontManager mgr = VectorFontManager.getInstance();
        if (mgr == null) return false;
        boolean[] result = {true};
        text.accept((idx, style, cp) -> {
            if (!mgr.hasGlyph(cp)) { result[0] = false; return false; }
            return true;
        });
        return result[0];
    }

//? if <26.2 {
    private static final ThreadLocal<Vector4f[]> TMP_VECTORS = ThreadLocal.withInitial(() -> new Vector4f[] {
            new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()
    });

    public void drawString(PoseStack poseStack, String text, float x, float y, int color, MultiBufferSource bufferSource) {
        drawString(poseStack.last().pose(), text, x, y, color, bufferSource);
    }

    public void drawString(Matrix4f matrix, FormattedCharSequence text, float x, float y, int baseColor, MultiBufferSource bufferSource) {
        text = new EmojiSequenceCharSequence(text);
        if (!ConfigManager.data.vectorTextEnabled) return;
        if (!initialized) init();
        if (!initialized) return;
        float[] curX = {x};

        boolean textEnabled = ConfigManager.data.customTextEnabled;
        float textScale = ConfigManager.data.customTextScale;
        float arrowSize = EmojiManager.getArrowSize();
        float arrowAdvance = EmojiManager.getArrowAdvance();
        float baseline = getBaseline();
        float emojiSize = textEnabled ? textScale : 9.0f;
        float yCenter = y + baseline - emojiSize / 2.0f;
        float ey0Arrow = yCenter - arrowSize / 2.0f;
        float ey1Arrow = yCenter + arrowSize / 2.0f;
        float ey1Emoji = y + baseline + emojiSize * 0.1f;
        float ey0Emoji = ey1Emoji - emojiSize;
        float emojiAdvance = emojiSize + 1.0f;

        text.accept((idx, style, cp) -> {
            if (CustomFontRenderer.isVariationSelector(cp)) {
                return true;
            }
            int color = baseColor;
            if (style.getColor() != null) {
                int styleRgb = style.getColor().getValue();
                int alpha = (baseColor & 0xFF000000);
                color = alpha | (styleRgb & 0x00FFFFFF);
            }
            if (ConfigManager.data.customFontEmoji) {
                int arrowDir = EmojiManager.getArrowDirection(cp);
                if (arrowDir != -1) {
                    float ex0 = curX[0] + (arrowAdvance - arrowSize) / 2.0f;
                    float ex1 = ex0 + arrowSize;

                    renderArrow3d(matrix, arrowDir, ex0, ey0Arrow, ex1, ey1Arrow, bufferSource);
                    curX[0] += arrowAdvance;
                    return true;
                }

                if (EmojiManager.isEmoji(cp)) {
                    float ex0 = curX[0];
                    float ex1 = ex0 + emojiSize;

                    EmojiManager.EmojiTexture emojiTex = EmojiManager.getEmojiTexture(cp);
                    if (emojiTex != null) {
                        renderEmoji3d(matrix, emojiTex, ex0, ey0Emoji, ex1, ey1Emoji, bufferSource);
                    }
                    curX[0] += emojiAdvance;
                    return true;
                }
            }

            renderGlyph3d(matrix, cp, curX[0], y, color, bufferSource);
            curX[0] += getAdvance(cp);
            return true;
        });
    }

    public void drawString(Matrix4f matrix, String text, float x, float y, int color, MultiBufferSource bufferSource) {
        text = EmojiManager.preprocessString(text);
        if (!ConfigManager.data.vectorTextEnabled) return;
        if (!initialized) init();
        if (!initialized) return;
        float curX = x;

        boolean textEnabled = ConfigManager.data.customTextEnabled;
        float textScale = ConfigManager.data.customTextScale;
        float arrowSize = EmojiManager.getArrowSize();
        float arrowAdvance = EmojiManager.getArrowAdvance();
        float baseline = getBaseline();
        float emojiSize = textEnabled ? textScale : 9.0f;
        float yCenter = y + baseline - emojiSize / 2.0f;
        float ey0Arrow = yCenter - arrowSize / 2.0f;
        float ey1Arrow = yCenter + arrowSize / 2.0f;
        float ey1Emoji = y + baseline + emojiSize * 0.1f;
        float ey0Emoji = ey1Emoji - emojiSize;
        float emojiAdvance = emojiSize + 1.0f;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (CustomFontRenderer.isVariationSelector(cp)) {
                i += Character.charCount(cp);
                continue;
            }
            if (ConfigManager.data.customFontEmoji) {
                int arrowDir = EmojiManager.getArrowDirection(cp);
                if (arrowDir != -1) {
                    float ex0 = curX + (arrowAdvance - arrowSize) / 2.0f;
                    float ex1 = ex0 + arrowSize;

                    renderArrow3d(matrix, arrowDir, ex0, ey0Arrow, ex1, ey1Arrow, bufferSource);
                    curX += arrowAdvance;
                    i += Character.charCount(cp);
                    continue;
                }

                if (EmojiManager.isEmoji(cp)) {
                    float ex0 = curX;
                    float ex1 = ex0 + emojiSize;

                    EmojiManager.EmojiTexture emojiTex = EmojiManager.getEmojiTexture(cp);
                    if (emojiTex != null) {
                        renderEmoji3d(matrix, emojiTex, ex0, ey0Emoji, ex1, ey1Emoji, bufferSource);
                    }
                    curX += emojiAdvance;
                    i += Character.charCount(cp);
                    continue;
                }
            }

            renderGlyph3d(matrix, cp, curX, y, color, bufferSource);
            curX += getAdvance(cp);
            i += Character.charCount(cp);
        }
    }

    private void renderEmoji3d(Matrix4f matrix, EmojiManager.EmojiTexture emojiTex,
                               float x0, float y0, float x1, float y1,
                               MultiBufferSource bufferSource) {
        RenderType layer = (RenderType) McCompat.createTextRenderType("emoji_3d",
                BlackaddonsRenderPipelines.PLAIN_TEXTURED, emojiTex.location);
        VertexConsumer buffer = bufferSource.getBuffer(layer);

        Vector4f[] tmps = TMP_VECTORS.get();
        Vector4f v1p = tmps[0].set(x0, y0, 0, 1).mul(matrix);
        Vector4f v2p = tmps[1].set(x0, y1, 0, 1).mul(matrix);
        Vector4f v3p = tmps[2].set(x1, y1, 0, 1).mul(matrix);
        Vector4f v4p = tmps[3].set(x1, y0, 0, 1).mul(matrix);

        buffer.addVertex(v1p.x(), v1p.y(), v1p.z()).setColor(0xFFFFFFFF).setUv(0, 0).setUv2(0, 240);
        buffer.addVertex(v2p.x(), v2p.y(), v2p.z()).setColor(0xFFFFFFFF).setUv(0, 1).setUv2(0, 240);
        buffer.addVertex(v3p.x(), v3p.y(), v3p.z()).setColor(0xFFFFFFFF).setUv(1, 1).setUv2(0, 240);
        buffer.addVertex(v4p.x(), v4p.y(), v4p.z()).setColor(0xFFFFFFFF).setUv(1, 0).setUv2(0, 240);
    }

    private void renderArrow3d(Matrix4f matrix, int direction,
                               float x0, float y0, float x1, float y1,
                               MultiBufferSource bufferSource) {
        RenderType layer = (RenderType) McCompat.createTextRenderType("arrow_3d",
                BlackaddonsRenderPipelines.PLAIN_TEXTURED, EmojiManager.ARROW_LOCATION);
        VertexConsumer buffer = bufferSource.getBuffer(layer);

        Vector4f[] tmps = TMP_VECTORS.get();
        Vector4f v1p = tmps[0].set(x0, y0, 0, 1).mul(matrix);
        Vector4f v2p = tmps[1].set(x0, y1, 0, 1).mul(matrix);
        Vector4f v3p = tmps[2].set(x1, y1, 0, 1).mul(matrix);
        Vector4f v4p = tmps[3].set(x1, y0, 0, 1).mul(matrix);

        buffer.addVertex(v1p.x(), v1p.y(), v1p.z()).setColor(0xFFFFFFFF).setUv(EmojiManager.ARROW_U_TABLE[direction][0], EmojiManager.ARROW_V_TABLE[direction][0]).setUv2(0, 240);
        buffer.addVertex(v2p.x(), v2p.y(), v2p.z()).setColor(0xFFFFFFFF).setUv(EmojiManager.ARROW_U_TABLE[direction][1], EmojiManager.ARROW_V_TABLE[direction][1]).setUv2(0, 240);
        buffer.addVertex(v3p.x(), v3p.y(), v3p.z()).setColor(0xFFFFFFFF).setUv(EmojiManager.ARROW_U_TABLE[direction][2], EmojiManager.ARROW_V_TABLE[direction][2]).setUv2(0, 240);
        buffer.addVertex(v4p.x(), v4p.y(), v4p.z()).setColor(0xFFFFFFFF).setUv(EmojiManager.ARROW_U_TABLE[direction][3], EmojiManager.ARROW_V_TABLE[direction][3]).setUv2(0, 240);
    }

    private void renderGlyph3d(Matrix4f matrix, int codepoint, float x, float y, int color, MultiBufferSource bufferSource) {
        VectorFontManager.GlyphData glyph = glyphCache.computeIfAbsent(codepoint,
                cp -> VectorFontManager.getInstance().getGlyphData(cp));
        if (glyph == null || glyph.curves.isEmpty()) return;

        DynamicTexture texture = textureCache.computeIfAbsent(codepoint, cp -> createCurveTexture(glyph));
        Object curveTextureId = identifierCache.computeIfAbsent(codepoint, cp ->
                McCompat.registerTexture(texture, "blackaddons", "vector_curves/" + codepoint));
        RenderType layer = getLayer(curveTextureId);

        VertexConsumer buffer = bufferSource.getBuffer(layer);
        float scale = getScale();
        float baseline = getBaseline();
        float x0 = x + glyph.x0 * scale;
        float x1 = x + glyph.x1 * scale;
        float y0 = y + baseline - glyph.y1 * scale;
        float y1 = y + baseline - glyph.y0 * scale;

        Vector4f[] tmps = TMP_VECTORS.get();
        Vector4f v1 = tmps[0].set(x0, y0, 0, 1).mul(matrix);
        Vector4f v2 = tmps[1].set(x0, y1, 0, 1).mul(matrix);
        Vector4f v3 = tmps[2].set(x1, y1, 0, 1).mul(matrix);
        Vector4f v4 = tmps[3].set(x1, y0, 0, 1).mul(matrix);

        buffer.addVertex(v1.x(), v1.y(), v1.z()).setUv(0, 0).setColor(color);
        buffer.addVertex(v2.x(), v2.y(), v2.z()).setUv(0, 1).setColor(color);
        buffer.addVertex(v3.x(), v3.y(), v3.z()).setUv(1, 1).setColor(color);
        buffer.addVertex(v4.x(), v4.y(), v4.z()).setUv(1, 0).setColor(color);
    }

//?}

    public DynamicTexture getTexture(int codepoint, VectorFontManager.GlyphData glyph) {
        return textureCache.computeIfAbsent(codepoint, cp -> createCurveTexture(glyph));
    }

    public RenderType getLayer(int codepoint, VectorFontManager.GlyphData glyph) {
        DynamicTexture texture = getTexture(codepoint, glyph);
        Object curveTextureId = identifierCache.computeIfAbsent(codepoint, cp ->
                McCompat.registerTexture(texture, "blackaddons", "vector_curves/" + codepoint));
        return getLayer(curveTextureId);
    }

    private RenderType getLayer(Object curveTexture) {
        return layerCache.computeIfAbsent(curveTexture, loc ->
                (RenderType) McCompat.createTextRenderType("vector_text_" + loc.toString().hashCode(),
                        BlackaddonsRenderPipelines.VECTOR_TEXT, loc));
    }

    private DynamicTexture createCurveTexture(VectorFontManager.GlyphData glyph) {
        int width = 6;
        int height = glyph.curves.size() + 1;
        DynamicTexture texture = new DynamicTexture("vector_curves", width, height, false);
        NativeImage px = texture.getPixels();

        setPixel(px, 0, 0, Float.floatToRawIntBits((float) glyph.x0));
        setPixel(px, 1, 0, Float.floatToRawIntBits((float) glyph.y0));
        setPixel(px, 2, 0, Float.floatToRawIntBits((float) glyph.x1));
        setPixel(px, 3, 0, Float.floatToRawIntBits((float) glyph.y1));
        setPixel(px, 4, 0, Float.floatToRawIntBits((float) glyph.curves.size()));
        setPixel(px, 5, 0, 0);

        for (int i = 0; i < glyph.curves.size(); i++) {
            VectorFontManager.Curve c = glyph.curves.get(i);
            setPixel(px, 0, i + 1, Float.floatToRawIntBits(c.x0));
            setPixel(px, 1, i + 1, Float.floatToRawIntBits(c.y0));
            setPixel(px, 2, i + 1, Float.floatToRawIntBits(c.x1));
            setPixel(px, 3, i + 1, Float.floatToRawIntBits(c.y1));
            setPixel(px, 4, i + 1, Float.floatToRawIntBits(c.x2));
            setPixel(px, 5, i + 1, Float.floatToRawIntBits(c.y2));
        }

        texture.upload();
        return texture;
    }

    private float getAdvance(int codepoint) {
        VectorFontManager.GlyphData glyph = glyphCache.computeIfAbsent(codepoint,
                cp -> VectorFontManager.getInstance().getGlyphData(cp));
        return glyph != null ? glyph.advance * getScale() : 0f;
    }

    private void setPixel(NativeImage pixels, int x, int y, int abgr) {
        pixels.setPixelABGR(x, y, abgr);
    }
}
