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
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import org.blackum.blackaddons.client.render.BlackaddonsRenderPipelines;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector4f;
//? if < 1.21.11 {
/*import net.minecraft.client.renderer.RenderType;*/
//?} else
import net.minecraft.client.renderer.rendertype.RenderType;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomFontRenderer {
    private static final CustomFontRenderer INSTANCE = new CustomFontRenderer();
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFontRenderer.class);

    public static final float SDF_SOURCE_SIZE = 192.0f;
    public static final int SDF_PADDING = 24;
    public static final int SDF_ON_EDGE_VALUE = 128;
    public static final float SDF_PIXEL_DIST_SCALE = 12.0f;
    private static final float BASELINE_OFFSET = -2.5f;

    private static final int ATLAS_MAX_WIDTH = 1024;
    private static final int ATLAS_GAP = 2;

    private CustomFontManager manager;
    private boolean initialized = false;
    private volatile boolean loading = false;

    private DynamicTexture atlasTexture;
    private Object atlasId;
    public static boolean inOutlinePass = false;
    private RenderType atlasRenderType;
    private RenderType atlasDepthRenderType;
    private TextureSetup atlasTextureSetup;

    private final Map<Integer, SdfGlyph> sdfGlyphCache = new HashMap<>();
    private final Map<Integer, DynamicTexture> textureCache = new HashMap<>();
    private final Map<Integer, Object> identifierCache = new HashMap<>();
    private final Map<Object, RenderType> layerCache = new HashMap<>();
    private final Map<Object, RenderType> depthLayerCache = new HashMap<>();
    private final Map<Integer, TextureSetup> fallbackTextureSetupCache = new HashMap<>();
    private final Map<Integer, CustomBakedGlyph> bakedGlyphCache = new HashMap<>();

    private float lastScaleConfig = -1f;
    private float cachedScale = 1f;
    private float cachedBaseline = 0f;
    private float cachedSdfScale = -1f;

    private record PreparedAtlasEntry(int codepoint, int width, int height, int xoff, int yoff,
                                      float u0, float v0, float u1, float v1, byte[] pixels) {}

    private record PreparedAsciiAtlas(int width, int height, List<PreparedAtlasEntry> entries) {}

    public static final class SdfGlyph {
        public final int width;
        public final int height;
        public final int xoff;
        public final int yoff;
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;
        public final boolean atlasResident;

        private SdfGlyph(int width, int height, int xoff, int yoff, float u0, float v0, float u1, float v1, boolean atlasResident) {
            this.width = width;
            this.height = height;
            this.xoff = xoff;
            this.yoff = yoff;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.atlasResident = atlasResident;
        }
    }

    public boolean isLoading() {
        return loading;
    }

    public CustomFontManager getManager() {
        return manager;
    }

    public float getCachedScale() {
        float s = ConfigManager.data.customTextScale;
        if (s != lastScaleConfig) {
            if (manager == null) {
                return 1f;
            }
            cachedScale = manager.getScaleForPixelHeight(s);
            cachedBaseline = manager.getAscent() * cachedScale;
            lastScaleConfig = s;
        }
        return cachedScale;
    }

    public float getCachedBaseline() {
        getCachedScale();
        return cachedBaseline;
    }

    public float getCachedSdfRenderScale() {
        float sdfScale = getSdfScale();
        return sdfScale <= 0f ? 1f : getCachedScale() / sdfScale;
    }

    private float getScale() {
        return getCachedScale();
    }

    private float getBaseline() {
        return getCachedBaseline();
    }

    private float getSdfScale() {
        if (cachedSdfScale < 0f && manager != null) {
            cachedSdfScale = manager.getScaleForPixelHeight(SDF_SOURCE_SIZE);
        }
        return cachedSdfScale;
    }

    public void init() {
        if (initialized || loading) {
            return;
        }
        reloadAsync(null);
    }

    public void reload() {
        reloadAsync(null);
    }

    public void reloadAsync(Runnable onDone) {
        reloadAsync(onDone, null);
    }

    public void reloadAsync(Runnable onDone, Consumer<long[]> onProgress) {
        if (loading) {
            return;
        }
        loading = true;

        Thread t = new Thread(() -> {
            try {
                ByteBuffer buffer = loadFontBuffer(onProgress);
                if (buffer != null) {
                    CustomFontManager newManager = new CustomFontManager(buffer);
                    PreparedAsciiAtlas atlasData = prepareAsciiAtlas(newManager);
                    Minecraft.getInstance().execute(() -> {
                        try {
                            manager = newManager;
                            sdfGlyphCache.clear();
                            textureCache.clear();
                            identifierCache.clear();
                            layerCache.clear();
                            fallbackTextureSetupCache.clear();
                            bakedGlyphCache.clear();
                            lastScaleConfig = -1f;
                            cachedSdfScale = -1f;
                            atlasTexture = null;
                            atlasId = null;
                            atlasRenderType = null;
                            atlasDepthRenderType = null;
                            depthLayerCache.clear();
                            atlasTextureSetup = null;

                            applyPreparedAsciiAtlas(atlasData);
                            initialized = true;
                        } catch (Exception e) {
                            LOGGER.error("[CustomFont] Failed to apply font atlas on render thread", e);
                            initialized = false;
                        } finally {
                            loading = false;
                            if (onDone != null) {
                                onDone.run();
                            }
                        }
                    });
                } else {
                    loading = false;
                    if (onDone != null) {
                        onDone.run();
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[CustomFont] Failed to load font", e);
                loading = false;
                if (onDone != null) {
                    onDone.run();
                }
            }
        }, "CustomFont-Reload");
        t.setDaemon(true);
        t.start();
    }

    private ByteBuffer loadFontBuffer(Consumer<long[]> onProgress) throws Exception {
        String googleName = ConfigManager.data.customFontGoogleName;

        if (googleName != null && !googleName.isBlank()) {
            File localFile = new File(googleName);
            if (localFile.exists() && localFile.isFile()) {
                byte[] bytes = Files.readAllBytes(localFile.toPath());
                ByteBuffer buf = ByteBuffer.allocateDirect(bytes.length);
                buf.put(bytes).flip();
                return buf;
            }
            ByteBuffer downloaded = FontDownloader.download(googleName, onProgress);
            if (downloaded != null) {
                return downloaded;
            }
        }

        net.minecraft.server.packs.resources.ResourceManager rm = Minecraft.getInstance().getResourceManager();
        if (rm == null) {
            return null;
        }
        Optional<Resource> resource = McCompat.findResource(rm, "blackaddons", "font/custom_font.ttf");
        if (resource.isEmpty()) {
            return null;
        }

        try (InputStream is = resource.get().open()) {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        }
    }

    private PreparedAsciiAtlas prepareAsciiAtlas(CustomFontManager fontManager) {
        if (fontManager == null) {
            return null;
        }

        List<AtlasEntry> entries = new ArrayList<>();
        int atlasWidth = 0;
        int atlasHeight = ATLAS_GAP;
        int cursorX = ATLAS_GAP;
        int cursorY = ATLAS_GAP;
        int rowHeight = 0;

        for (int cp = 32; cp <= 126; cp++) {
            CustomFontManager.SdfGlyphData sdf = fontManager.getSdfGlyphData(cp, SDF_SOURCE_SIZE, SDF_PADDING, SDF_ON_EDGE_VALUE, SDF_PIXEL_DIST_SCALE);
            if (sdf == null || sdf.width <= 0 || sdf.height <= 0) {
                continue;
            }

            if (cursorX + sdf.width + ATLAS_GAP > ATLAS_MAX_WIDTH) {
                cursorX = ATLAS_GAP;
                cursorY += rowHeight + ATLAS_GAP;
                rowHeight = 0;
            }

            entries.add(new AtlasEntry(cp, sdf, cursorX, cursorY));
            cursorX += sdf.width + ATLAS_GAP;
            rowHeight = Math.max(rowHeight, sdf.height);
            atlasWidth = Math.max(atlasWidth, cursorX);
        }

        atlasHeight = cursorY + rowHeight + ATLAS_GAP;
        if (entries.isEmpty()) {
            return null;
        }

        atlasWidth = nextPow2(Math.max(32, atlasWidth));
        atlasHeight = nextPow2(Math.max(32, atlasHeight));

        List<PreparedAtlasEntry> preparedEntries = new ArrayList<>(entries.size());
        for (AtlasEntry entry : entries) {
            preparedEntries.add(new PreparedAtlasEntry(
                    entry.codepoint,
                    entry.sdf.width,
                    entry.sdf.height,
                    entry.sdf.xoff,
                    entry.sdf.yoff,
                    entry.x / (float) atlasWidth,
                    entry.y / (float) atlasHeight,
                    (entry.x + entry.sdf.width) / (float) atlasWidth,
                    (entry.y + entry.sdf.height) / (float) atlasHeight,
                    entry.sdf.pixels.clone()));
        }

        return new PreparedAsciiAtlas(atlasWidth, atlasHeight, preparedEntries);
    }

    private void applyPreparedAsciiAtlas(PreparedAsciiAtlas atlasData) {
        if (atlasData == null) {
            return;
        }

        atlasTexture = new DynamicTexture("custom_sdf_atlas", atlasData.width(), atlasData.height(), false);
        NativeImage pixels = atlasTexture.getPixels();

        for (PreparedAtlasEntry entry : atlasData.entries()) {
            copySdfBitmap(pixels, entry.pixels(), (int) (entry.u0() * atlasData.width()), (int) (entry.v0() * atlasData.height()), entry.width(), entry.height());
            sdfGlyphCache.put(entry.codepoint(), new SdfGlyph(
                    entry.width(),
                    entry.height(),
                    entry.xoff(),
                    entry.yoff(),
                    entry.u0(),
                    entry.v0(),
                    entry.u1(),
                    entry.v1(),
                    true));
        }

        McCompat.enableLinearFiltering(atlasTexture);
        atlasTexture.upload();
        atlasId = McCompat.registerTexture(atlasTexture, "blackaddons", "custom_sdf_atlas");
        atlasRenderType = (RenderType) McCompat.createTextRenderType("custom_text_atlas", BlackaddonsRenderPipelines.CUSTOM_TEXT, atlasId);
        atlasDepthRenderType = (RenderType) McCompat.createTextRenderType("custom_text_atlas_depth", BlackaddonsRenderPipelines.CUSTOM_TEXT_DEPTH, atlasId);
        //? if < 1.21.11 {
        /*atlasTextureSetup = TextureSetup.singleTexture(atlasTexture.getTextureView());*/
        //?} else
        atlasTextureSetup = TextureSetup.singleTexture(atlasTexture.getTextureView(), atlasTexture.getSampler());
    }

    public boolean isInitialized() {
        return initialized;
    }

    public static CustomFontRenderer getInstance() {
        return INSTANCE;
    }

    public SdfGlyph getSdfGlyph(int codepoint) {
        SdfGlyph cached = sdfGlyphCache.get(codepoint);
        if (cached != null) {
            return cached;
        }
        if (manager == null) {
            return null;
        }

        CustomFontManager.SdfGlyphData sdf = manager.getSdfGlyphData(codepoint, SDF_SOURCE_SIZE, SDF_PADDING, SDF_ON_EDGE_VALUE, SDF_PIXEL_DIST_SCALE);
        if (sdf == null || sdf.width <= 0 || sdf.height <= 0) {
            return null;
        }

        SdfGlyph glyph = new SdfGlyph(sdf.width, sdf.height, sdf.xoff, sdf.yoff, 0f, 0f, 1f, 1f, false);
        sdfGlyphCache.put(codepoint, glyph);
        return glyph;
    }

    public CustomBakedGlyph getOrCreateBakedGlyph(int codepoint) {
        CustomBakedGlyph cached = bakedGlyphCache.get(codepoint);
        if (cached != null) {
            return cached;
        }
        CustomFontManager.GlyphData data = manager != null ? manager.getGlyphData(codepoint) : null;
        if (data == null) {
            return null;
        }
        CustomBakedGlyph baked = new CustomBakedGlyph(codepoint, data);
        bakedGlyphCache.put(codepoint, baked);
        return baked;
    }

    public float getGlyphVisualLeft(CustomFontManager.GlyphData glyph, boolean bold, boolean italic, boolean shadowed, float shadowOffset) {
        VisualGlyphBounds bounds = getVisualGlyphBounds(glyph, bold, italic, shadowed, shadowOffset);
        return bounds.left();
    }

    public float getGlyphVisualRight(CustomFontManager.GlyphData glyph, boolean bold, boolean italic, boolean shadowed, float shadowOffset) {
        VisualGlyphBounds bounds = getVisualGlyphBounds(glyph, bold, italic, shadowed, shadowOffset);
        return bounds.right();
    }

    public void drawStringGui(Matrix3x2fc pose, FormattedCharSequence text, float x, float y,
                              int baseColor, ScreenRectangle scissor, GuiRenderState renderState,
                              Font font) {
        if (!initialized) {
            init();
        }
        if (!initialized || manager == null) {
            return;
        }

        float scale = getScale();
        float[] curX = {x};
        text.accept((idx, style, cp) -> {
            int color = baseColor;
            if (style.getColor() != null) {
                int styleRgb = style.getColor().getValue();
                color = (baseColor & 0xFF000000) | (styleRgb & 0x00FFFFFF);
            }
            int argb = (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;

            boolean bold = ConfigManager.data.customFontBold || style.isBold();
            boolean italic = ConfigManager.data.customFontItalic || style.isItalic();
            CustomFontManager.GlyphData glyph = manager.getGlyphData(cp);
            SdfGlyph sdfGlyph = getSdfGlyph(cp);
            if (glyph != null && sdfGlyph != null) {
                renderGlyphGui(pose, cp, glyph, sdfGlyph, curX[0], y, argb, scissor, renderState, bold, italic);
                curX[0] += glyph.advance * scale;
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
                                renderState.submitGlyphToCurrentLayer(new GlyphRenderState(new Matrix3x2f(pose), styled, scissor));
                            }
                        });
                curX[0] += font.width(singleChar);
            }
            return true;
        });
    }

    public void drawStringGui(Matrix3x2fc pose, String text, float x, float y,
                              int color, ScreenRectangle scissor, GuiRenderState renderState) {
        if (!initialized) {
            init();
        }
        if (!initialized || manager == null) {
            return;
        }

        float scale = getScale();
        boolean bold = ConfigManager.data.customFontBold;
        boolean italic = ConfigManager.data.customFontItalic;
        float curX = x;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            CustomFontManager.GlyphData glyph = manager.getGlyphData(cp);
            SdfGlyph sdfGlyph = getSdfGlyph(cp);
            if (glyph != null && sdfGlyph != null) {
                renderGlyphGui(pose, cp, glyph, sdfGlyph, curX, y, color, scissor, renderState, bold, italic);
            }
            curX += glyph != null ? glyph.advance * scale : 0f;
            i += Character.charCount(cp);
        }
    }

    private void renderGlyphGui(Matrix3x2fc pose, int codepoint, CustomFontManager.GlyphData glyph, SdfGlyph sdfGlyph,
                                float x, float y, int color, ScreenRectangle scissor,
                                GuiRenderState renderState, boolean bold, boolean italic) {
        GlyphQuad quad = getGlyphQuad(sdfGlyph, x, y);
        float slant = italic ? ConfigManager.data.customFontItalicSlant : 0f;
        float boldStrength = bold ? ConfigManager.data.customFontBoldStrength : 0f;
        var cfg = ConfigManager.data;

        if (cfg.customFontShadow) {
            submitGlyphGui(pose, codepoint, sdfGlyph,
                    quad.x0 + cfg.customFontShadowOffsetX, quad.y0 + cfg.customFontShadowOffsetY,
                    quad.x1 + cfg.customFontShadowOffsetX, quad.y1 + cfg.customFontShadowOffsetY,
                    slant, boldStrength, ensureOpaque(cfg.customFontShadowColor), scissor, renderState);
        }

        if (cfg.customFontOutline) {
            submitGlyphGui(pose, codepoint, sdfGlyph,
                    quad.x0, quad.y0, quad.x1, quad.y1,
                    slant, -(cfg.customFontOutlineWidth + boldStrength), ensureOpaque(cfg.customFontOutlineColor), scissor, renderState);
        }

        submitGlyphGui(pose, codepoint, sdfGlyph,
                quad.x0, quad.y0, quad.x1, quad.y1,
                slant, boldStrength, color, scissor, renderState);
    }

    private void submitGlyphGui(Matrix3x2fc pose, int codepoint, SdfGlyph sdfGlyph,
                                float sx0, float sy0, float sx1, float sy1,
                                float italicSlant, float effectZ, int color,
                                ScreenRectangle scissor, GuiRenderState renderState) {
        TextureSetup textureSetup;
        if (sdfGlyph.atlasResident) {
            textureSetup = atlasTextureSetup;
        } else {
            textureSetup = fallbackTextureSetupCache.computeIfAbsent(codepoint, cp -> {
                DynamicTexture tex = textureCache.computeIfAbsent(cp, this::createGlyphTexture);
                //? if < 1.21.11 {
                /*return TextureSetup.singleTexture(tex.getTextureView());*/
                //?} else
                return TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler());
            });
        }

        renderState.submitGlyphToCurrentLayer(new CustomGlyphRenderState(
                BlackaddonsRenderPipelines.CUSTOM_TEXT, textureSetup, new Matrix3x2f(pose),
                sx0, sy0, sx1, sy1,
                sdfGlyph.u0, sdfGlyph.v0, sdfGlyph.u1, sdfGlyph.v1,
                italicSlant, packShaderEffect(effectZ), color, scissor));
    }

    public static float packShaderEffect(float effect) {
        return packShaderEffect(effect, false);
    }

    public static float packShaderEffect(float effect, boolean hardEdge) {
        var cfg = ConfigManager.data;
        float aaWidth = (!hardEdge && cfg.customFontAntiAliasing) ? cfg.customFontAntiAliasingWidth : 0.0f;
        float packedAa = Math.round(aaWidth * 100.0f) + 1000.0f;
        return packedAa * 16.0f + (effect + 4.0f);
    }

    private static int ensureOpaque(int argb) {
        return (argb & 0xFF000000) == 0 ? (argb | 0xFF000000) : argb;
    }

    private VisualGlyphBounds getVisualGlyphBounds(CustomFontManager.GlyphData glyph, boolean bold, boolean italic,
                                                   boolean shadowed, float shadowOffset) {
        float glyphScale = getCachedScale();
        float left = glyph.x0 * glyphScale;
        float right = glyph.x1 * glyphScale;

        var cfg = ConfigManager.data;
        float aa = cfg.customFontAntiAliasing ? cfg.customFontAntiAliasingWidth : 0.0f;
        float effectPad = aa;
        float boldPad = 0.0f;
        if (bold) {
            boldPad = cfg.customFontBoldStrength;
            effectPad += boldPad;
        }
        if (cfg.customFontOutline) {
            effectPad = Math.max(effectPad, aa + cfg.customFontOutlineWidth + boldPad);
        }

        left -= effectPad;
        right += effectPad;

        if (italic) {
            float top = (getCachedBaseline() + BASELINE_OFFSET) - glyph.y1 * glyphScale - effectPad;
            float bottom = (getCachedBaseline() + BASELINE_OFFSET) - glyph.y0 * glyphScale + effectPad;
            float skew = cfg.customFontItalicSlant * (bottom - top);
            left = Math.min(left, left + skew);
            right = Math.max(right, right + skew);
        }

        if (cfg.customFontShadow || shadowed) {
            float shadowDx = (cfg.customFontShadow ? cfg.customFontShadowOffsetX : 0.0f) + shadowOffset;
            left = Math.min(left, left + shadowDx);
            right = Math.max(right, right + shadowDx);
        }

        return new VisualGlyphBounds(left, right);
    }

    public boolean supportsAllGlyphs(FormattedCharSequence text) {
        if (manager == null) {
            return false;
        }
        boolean[] result = {true};
        text.accept((idx, style, cp) -> {
            if (manager.getGlyphData(cp) == null || getSdfGlyph(cp) == null) {
                result[0] = false;
                return false;
            }
            return true;
        });
        return result[0];
    }

    public void drawString(PoseStack poseStack, String text, float x, float y, int color, MultiBufferSource bufferSource) {
        drawString(poseStack.last().pose(), text, x, y, color, bufferSource);
    }

    public void drawString(Matrix4f matrix, FormattedCharSequence text, float x, float y, int baseColor, MultiBufferSource bufferSource) {
        if (!ConfigManager.data.customTextEnabled) {
            return;
        }
        if (!initialized || manager == null) {
            if (!loading) {
                init();
            }
            Minecraft.getInstance().font.drawInBatch(text, x, y, baseColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            return;
        }

        float scale = getScale();
        float[] curX = {x};
        text.accept((idx, style, cp) -> {
            int color = baseColor;
            if (style.getColor() != null) {
                int styleRgb = style.getColor().getValue();
                color = (baseColor & 0xFF000000) | (styleRgb & 0x00FFFFFF);
            }
            CustomFontManager.GlyphData glyph = manager.getGlyphData(cp);
            SdfGlyph sdfGlyph = getSdfGlyph(cp);
            if (glyph != null && sdfGlyph != null) {
                renderGlyph3d(matrix, cp, sdfGlyph, curX[0], y, color, bufferSource);
            }
            curX[0] += glyph != null ? glyph.advance * scale : 0f;
            return true;
        });
    }

    public void drawString(Matrix4f matrix, String text, float x, float y, int color, MultiBufferSource bufferSource) {
        if (!ConfigManager.data.customTextEnabled) {
            return;
        }
        if (!initialized || manager == null) {
            if (!loading) {
                init();
            }
            Minecraft.getInstance().font.drawInBatch(text, x, y, color, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            return;
        }

        float scale = getScale();
        float curX = x;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            CustomFontManager.GlyphData glyph = manager.getGlyphData(cp);
            SdfGlyph sdfGlyph = getSdfGlyph(cp);
            if (glyph != null && sdfGlyph != null) {
                renderGlyph3d(matrix, cp, sdfGlyph, curX, y, color, bufferSource);
            }
            curX += glyph != null ? glyph.advance * scale : 0f;
            i += Character.charCount(cp);
        }
    }

    private void renderGlyph3d(Matrix4f matrix, int codepoint, SdfGlyph sdfGlyph,
                               float x, float y, int color, MultiBufferSource bufferSource) {
        GlyphQuad quad = getGlyphQuad(sdfGlyph, x, y);

        RenderType layer;
        float u0;
        float v0;
        float u1;
        float v1;
        if (sdfGlyph.atlasResident) {
            layer = atlasRenderType;
            u0 = sdfGlyph.u0;
            v0 = sdfGlyph.v0;
            u1 = sdfGlyph.u1;
            v1 = sdfGlyph.v1;
        } else {
            DynamicTexture texture = textureCache.computeIfAbsent(codepoint, this::createGlyphTexture);
            Object textureId = identifierCache.computeIfAbsent(codepoint, cp ->
                    McCompat.registerTexture(texture, "blackaddons", "custom_sdf/" + codepoint));
            layer = getPerGlyphLayer(textureId);
            u0 = 0f;
            v0 = 0f;
            u1 = 1f;
            v1 = 1f;
        }

        VertexConsumer buffer = bufferSource.getBuffer(layer);
        float effect = ConfigManager.data.customFontBold ? ConfigManager.data.customFontBoldStrength : 0f;
        int effectBits = Float.floatToRawIntBits(packShaderEffect(effect));

        Vector4f v1p = new Vector4f(quad.x0, quad.y0, 0, 1).mul(matrix);
        Vector4f v2p = new Vector4f(quad.x0, quad.y1, 0, 1).mul(matrix);
        Vector4f v3p = new Vector4f(quad.x1, quad.y1, 0, 1).mul(matrix);
        Vector4f v4p = new Vector4f(quad.x1, quad.y0, 0, 1).mul(matrix);

        buffer.addVertex(v1p.x(), v1p.y(), v1p.z()).setColor(color).setUv(u0, v0).setUv2(effectBits & 0xFFFF, (effectBits >> 16) & 0xFFFF);
        buffer.addVertex(v2p.x(), v2p.y(), v2p.z()).setColor(color).setUv(u0, v1).setUv2(effectBits & 0xFFFF, (effectBits >> 16) & 0xFFFF);
        buffer.addVertex(v3p.x(), v3p.y(), v3p.z()).setColor(color).setUv(u1, v1).setUv2(effectBits & 0xFFFF, (effectBits >> 16) & 0xFFFF);
        buffer.addVertex(v4p.x(), v4p.y(), v4p.z()).setColor(color).setUv(u1, v0).setUv2(effectBits & 0xFFFF, (effectBits >> 16) & 0xFFFF);
    }

    public DynamicTexture getTexture(int codepoint, CustomFontManager.GlyphData glyph) {
        SdfGlyph sdfGlyph = getSdfGlyph(codepoint);
        if (sdfGlyph != null && sdfGlyph.atlasResident) {
            return atlasTexture;
        }
        return textureCache.computeIfAbsent(codepoint, this::createGlyphTexture);
    }

    public RenderType getLayer(int codepoint, CustomFontManager.GlyphData glyph) {
        SdfGlyph sdfGlyph = getSdfGlyph(codepoint);
        if (sdfGlyph != null && sdfGlyph.atlasResident) {
            return atlasRenderType;
        }
        DynamicTexture texture = textureCache.computeIfAbsent(codepoint, this::createGlyphTexture);
        Object textureId = identifierCache.computeIfAbsent(codepoint, cp ->
                McCompat.registerTexture(texture, "blackaddons", "custom_sdf/" + codepoint));
        return getPerGlyphLayer(textureId);
    }

    public RenderType getDepthLayer(int codepoint, CustomFontManager.GlyphData glyph) {
        SdfGlyph sdfGlyph = getSdfGlyph(codepoint);
        if (sdfGlyph != null && sdfGlyph.atlasResident) {
            return atlasDepthRenderType;
        }
        DynamicTexture texture = textureCache.computeIfAbsent(codepoint, this::createGlyphTexture);
        Object textureId = identifierCache.computeIfAbsent(codepoint, cp ->
                McCompat.registerTexture(texture, "blackaddons", "custom_sdf/" + codepoint));
        return getPerGlyphDepthLayer(textureId);
    }

    private RenderType getPerGlyphLayer(Object textureId) {
        return layerCache.computeIfAbsent(textureId, loc ->
                (RenderType) McCompat.createTextRenderType("custom_text_" + loc.toString().hashCode(),
                        BlackaddonsRenderPipelines.CUSTOM_TEXT, loc));
    }

    private RenderType getPerGlyphDepthLayer(Object textureId) {
        return depthLayerCache.computeIfAbsent(textureId, loc ->
                (RenderType) McCompat.createTextRenderType("custom_text_depth_" + loc.toString().hashCode(),
                        BlackaddonsRenderPipelines.CUSTOM_TEXT_DEPTH, loc));
    }

    private DynamicTexture createGlyphTexture(int codepoint) {
        if (manager == null) {
            throw new IllegalStateException("Font manager not initialized");
        }

        CustomFontManager.SdfGlyphData sdf = manager.getSdfGlyphData(codepoint, SDF_SOURCE_SIZE, SDF_PADDING, SDF_ON_EDGE_VALUE, SDF_PIXEL_DIST_SCALE);
        if (sdf == null || sdf.width <= 0 || sdf.height <= 0) {
            DynamicTexture fallback = new DynamicTexture("custom_sdf_missing", 1, 1, false);
            fallback.getPixels().setPixelABGR(0, 0, 0);
            McCompat.enableLinearFiltering(fallback);
            fallback.upload();
            return fallback;
        }

        DynamicTexture texture = new DynamicTexture("custom_sdf", sdf.width, sdf.height, false);
        copySdfBitmap(texture.getPixels(), sdf.pixels, 0, 0, sdf.width, sdf.height);
        McCompat.enableLinearFiltering(texture);
        texture.upload();
        sdfGlyphCache.put(codepoint, new SdfGlyph(sdf.width, sdf.height, sdf.xoff, sdf.yoff, 0f, 0f, 1f, 1f, false));
        return texture;
    }

    private GlyphQuad getGlyphQuad(SdfGlyph sdfGlyph, float x, float y) {
        float glyphScale = getCachedSdfRenderScale();
        float baseline = getBaseline() + BASELINE_OFFSET;
        float x0 = x + sdfGlyph.xoff * glyphScale;
        float y0 = y + baseline + sdfGlyph.yoff * glyphScale;
        float x1 = x0 + sdfGlyph.width * glyphScale;
        float y1 = y0 + sdfGlyph.height * glyphScale;
        return new GlyphQuad(x0, y0, x1, y1);
    }

    private static void copySdfBitmap(NativeImage dest, byte[] src, int dstX, int dstY, int width, int height) {
        for (int y = 0; y < height; y++) {
            int srcRow = y * width;
            for (int x = 0; x < width; x++) {
                int v = src[srcRow + x] & 0xFF;
                int rgba = v | (v << 8) | (v << 16) | (v << 24);
                dest.setPixelABGR(dstX + x, dstY + y, rgba);
            }
        }
    }

    private static int nextPow2(int value) {
        int out = 1;
        while (out < value) {
            out <<= 1;
        }
        return out;
    }

    private record GlyphQuad(float x0, float y0, float x1, float y1) {}

    private record AtlasEntry(int codepoint, CustomFontManager.SdfGlyphData sdf, int x, int y) {}

    private record VisualGlyphBounds(float left, float right) {}
}
