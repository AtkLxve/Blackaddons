package org.blackum.blackaddons.gui.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GlyphRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.FormattedCharSequence;
import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.McCompat;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector4f;
//? if < 1.21.11 {
/*import net.minecraft.client.renderer.RenderType;*/
//?} else
import net.minecraft.client.renderer.rendertype.RenderType;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class VectorFontRenderer {
    private static VectorFontRenderer instance;
    private final Map<Integer, VectorFontManager.GlyphData> glyphCache = new HashMap<>();
    private final Map<Integer, DynamicTexture> textureCache = new HashMap<>();
    private final Map<Integer, Object> identifierCache = new HashMap<>();
    private final Map<Object, RenderType> layerCache = new HashMap<>();
    private boolean initialized = false;

    private float getScale() {
        return VectorFontManager.getInstance().getScaleForPixelHeight(
                org.blackum.blackaddons.common.config.ConfigManager.data.vectorTextScale);
    }

    private float getBaseline() {
        return VectorFontManager.getInstance().getAscent() * getScale();
    }

    public void init() {
        if (initialized) return;
        try {
            Optional<net.minecraft.server.packs.resources.Resource> resource =
                    McCompat.findResource(Minecraft.getInstance().getResourceManager(), "blackaddons", "font/vector_font.ttf");
            if (resource.isEmpty()) {
                org.blackum.blackaddons.Blackaddons.LOGGER.error("[VectorFont] Font not found at blackaddons:font/vector_font.ttf");
                return;
            }
            try (InputStream is = resource.get().open()) {
                byte[] bytes = is.readAllBytes();
                ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                buffer.put(bytes).flip();
                VectorFontManager.init(buffer);
            }
            initialized = true;
            org.blackum.blackaddons.Blackaddons.LOGGER.info("[VectorFont] Initialized.");
        } catch (Exception e) {
            org.blackum.blackaddons.Blackaddons.LOGGER.error("[VectorFont] Init failed", e);
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
        if (!initialized) init();
        if (!initialized) return;
        float[] curX = {x};
        text.accept((idx, style, cp) -> {
            int color = baseColor;
            if (style.getColor() != null) {
                int styleRgb = style.getColor().getValue();
                int alpha = (baseColor & 0xFF000000);
                color = alpha | (styleRgb & 0x00FFFFFF);
            }
            int argb = (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;

            VectorFontManager.GlyphData glyph = glyphCache.computeIfAbsent(cp,
                    c -> VectorFontManager.getInstance().getGlyphData(c));
            if (glyph != null) {
                if (!glyph.curves.isEmpty()) {
                    renderGlyphGui(pose, cp, curX[0], y, argb, scissor, renderState);
                }
                curX[0] += getAdvance(cp);
            } else {
                FormattedCharSequence singleChar = sink -> sink.accept(0, style, cp);
                //? if < 1.21.11 {
                /*font.prepareText(singleChar, curX[0], y, argb, false, 0)*/
                //?} else
                font.prepareText(singleChar, curX[0], y, argb, false, false, 0)
                        .visit(new Font.GlyphVisitor() {
                            //? if < 1.21.11 {
                            /*@Override
                            public void acceptEffect(TextRenderable effect) {
                            }*/
                            //?}

                            @Override
                            //? if < 1.21.11 {
                            /*public void acceptGlyph(TextRenderable styled) {*/
                            //?} else
                            public void acceptGlyph(TextRenderable.Styled styled) {
                                renderState.submitGlyphToCurrentLayer(
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
        if (!initialized) init();
        if (!initialized) return;
        float curX = x;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
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
        //? if < 1.21.11 {
        /*TextureSetup textureSetup = TextureSetup.singleTexture(texture.getTextureView());*/
        //?} else
        TextureSetup textureSetup = TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());

        float scale = getScale();
        float baseline = getBaseline();
        float sx0 = x + glyph.x0 * scale;
        float sy0 = y + baseline - glyph.y1 * scale;
        float sx1 = x + glyph.x1 * scale;
        float sy1 = y + baseline - glyph.y0 * scale;

        int argb = (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;
        renderState.submitGlyphToCurrentLayer(new VectorGlyphRenderState(
                BlackaddonsRenderPipelines.VECTOR_TEXT, textureSetup, pose, sx0, sy0, sx1, sy1, argb, scissor));
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

    public void drawString(PoseStack poseStack, String text, float x, float y, int color, MultiBufferSource bufferSource) {
        drawString(poseStack.last().pose(), text, x, y, color, bufferSource);
    }

    public void drawString(Matrix4f matrix, FormattedCharSequence text, float x, float y, int baseColor, MultiBufferSource bufferSource) {
        if (!ConfigManager.data.vectorTextEnabled) return;
        if (!initialized) init();
        if (!initialized) return;
        float[] curX = {x};
        text.accept((idx, style, cp) -> {
            int color = baseColor;
            if (style.getColor() != null) {
                int styleRgb = style.getColor().getValue();
                int alpha = (baseColor & 0xFF000000);
                color = alpha | (styleRgb & 0x00FFFFFF);
            }
            renderGlyph3d(matrix, cp, curX[0], y, color, bufferSource);
            curX[0] += getAdvance(cp);
            return true;
        });
    }

    public void drawString(Matrix4f matrix, String text, float x, float y, int color, MultiBufferSource bufferSource) {
        if (!ConfigManager.data.vectorTextEnabled) return;
        if (!initialized) init();
        if (!initialized) return;
        float curX = x;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            renderGlyph3d(matrix, cp, curX, y, color, bufferSource);
            curX += getAdvance(cp);
            i += Character.charCount(cp);
        }
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

        Vector4f v1 = new Vector4f(x0, y0, 0, 1).mul(matrix);
        Vector4f v2 = new Vector4f(x0, y1, 0, 1).mul(matrix);
        Vector4f v3 = new Vector4f(x1, y1, 0, 1).mul(matrix);
        Vector4f v4 = new Vector4f(x1, y0, 0, 1).mul(matrix);

        buffer.addVertex(v1.x(), v1.y(), v1.z()).setUv(0, 0).setColor(color);
        buffer.addVertex(v2.x(), v2.y(), v2.z()).setUv(0, 1).setColor(color);
        buffer.addVertex(v3.x(), v3.y(), v3.z()).setUv(1, 1).setColor(color);
        buffer.addVertex(v4.x(), v4.y(), v4.z()).setUv(1, 0).setColor(color);
    }

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
