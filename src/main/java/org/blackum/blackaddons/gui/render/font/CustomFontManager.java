package org.blackum.blackaddons.gui.render.font;

import org.lwjgl.stb.STBTruetype;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomFontManager {
    private final STBTTFontinfo fontInfo;
    private final ByteBuffer fontBuffer;

    private final Map<Integer, GlyphData> glyphDataCache = new HashMap<>();
    private int cachedAscent = Integer.MIN_VALUE;

    public static class Curve {
        public float x0, y0;
        public float x1, y1;
        public float x2, y2;

        public Curve(float x0, float y0, float x1, float y1, float x2, float y2) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    public static class GlyphData {
        public final List<Curve> curves = new ArrayList<>();
        public int advance;
        public int leftSideBearing;
        public int x0, y0, x1, y1;

        public GlyphData() {}
    }

    public static class SdfGlyphData {
        public final byte[] pixels;
        public final int width;
        public final int height;
        public final int xoff;
        public final int yoff;

        public SdfGlyphData(byte[] pixels, int width, int height, int xoff, int yoff) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
            this.xoff = xoff;
            this.yoff = yoff;
        }
    }

    public CustomFontManager(ByteBuffer fontBuffer) {
        this.fontBuffer = fontBuffer;
        this.fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
            throw new RuntimeException("Failed to initialize font");
        }
    }

    public boolean hasGlyph(int codepoint) {
        return STBTruetype.stbtt_FindGlyphIndex(fontInfo, codepoint) != 0;
    }

    public int getGlyphAdvance(int codepoint) {
        int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, codepoint);
        if (glyphIndex == 0) {
            return 0;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advance = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advance, null);
            return advance.get(0);
        }
    }

    public float getScaleForPixelHeight(float pixels) {
        return STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, pixels);
    }

    public int getAscent() {
        if (cachedAscent == Integer.MIN_VALUE) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer ascent = stack.mallocInt(1);
                STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
                cachedAscent = ascent.get(0);
            }
        }
        return cachedAscent;
    }

    public GlyphData getGlyphData(int codepoint) {
        if (glyphDataCache.containsKey(codepoint)) {
            return glyphDataCache.get(codepoint);
        }
        GlyphData data = loadGlyphData(codepoint);
        glyphDataCache.put(codepoint, data);
        return data;
    }

    private GlyphData loadGlyphData(int codepoint) {
        int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, codepoint);
        if (glyphIndex == 0) return null;

        GlyphData data = new GlyphData();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advance = stack.mallocInt(1);
            IntBuffer lsb = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advance, lsb);
            data.advance = advance.get(0);
            data.leftSideBearing = lsb.get(0);

            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphBox(fontInfo, glyphIndex, x0, y0, x1, y1);
            data.x0 = x0.get(0);
            data.y0 = y0.get(0);
            data.x1 = x1.get(0);
            data.y1 = y1.get(0);

            STBTTVertex.Buffer vertices = STBTruetype.stbtt_GetGlyphShape(fontInfo, glyphIndex);
            if (vertices != null) {
                float lastX = 0;
                float lastY = 0;
                float startX = 0;
                float startY = 0;

                for (int i = 0; i < vertices.remaining(); i++) {
                    STBTTVertex v = vertices.get(i);
                    switch (v.type()) {
                        case STBTruetype.STBTT_vmove:
                            lastX = startX = v.x();
                            lastY = startY = v.y();
                            break;
                        case STBTruetype.STBTT_vline:
                            data.curves.add(new Curve(lastX, lastY, (lastX + v.x()) / 2f, (lastY + v.y()) / 2f, v.x(), v.y()));
                            lastX = v.x();
                            lastY = v.y();
                            break;
                        case STBTruetype.STBTT_vcurve:
                            data.curves.add(new Curve(lastX, lastY, v.cx(), v.cy(), v.x(), v.y()));
                            lastX = v.x();
                            lastY = v.y();
                            break;
                        case STBTruetype.STBTT_vcubic:
                            float midX = (lastX + 3 * v.cx() + 3 * v.cx1() + v.x()) / 8f;
                            float midY = (lastY + 3 * v.cy() + 3 * v.cy1() + v.y()) / 8f;
                            data.curves.add(new Curve(lastX, lastY, (lastX + 3 * v.cx()) / 4f, (lastY + 3 * v.cy()) / 4f, midX, midY));
                            data.curves.add(new Curve(midX, midY, (3 * v.cx1() + v.x()) / 4f, (3 * v.cy1() + v.y()) / 4f, v.x(), v.y()));
                            lastX = v.x();
                            lastY = v.y();
                            break;
                    }
                }
                STBTruetype.stbtt_FreeShape(fontInfo, vertices);
            }
        }
        return data;
    }

    public SdfGlyphData getSdfGlyphData(int codepoint, float pixelHeight, int padding, int onEdgeValue, float pixelDistScale) {
        float scale = getScaleForPixelHeight(pixelHeight);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer xoff = stack.mallocInt(1);
            IntBuffer yoff = stack.mallocInt(1);

            ByteBuffer sdfBuf = STBTruetype.stbtt_GetCodepointSDF(
                    fontInfo, scale, codepoint, padding, (byte) onEdgeValue, pixelDistScale,
                    width, height, xoff, yoff);
            if (sdfBuf == null) return null;

            int w = width.get(0);
            int h = height.get(0);
            if (w <= 0 || h <= 0) {
                STBTruetype.stbtt_FreeSDF(sdfBuf, 0L);
                return null;
            }

            byte[] pixels = new byte[w * h];
            sdfBuf.get(pixels);
            sdfBuf.rewind();
            STBTruetype.stbtt_FreeSDF(sdfBuf, 0L);
            return new SdfGlyphData(pixels, w, h, xoff.get(0), yoff.get(0));
        }
    }
}
