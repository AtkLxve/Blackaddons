package org.blackum.blackaddons.gui.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.gui.render.TextureSetup;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.common.util.io.HttpUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;

public class EmojiManager {

    public static final Identifier ARROW_LOCATION = Identifier.fromNamespaceAndPath("blackaddons",
            "textures/gui/arrow.png");
    private static TextureSetup arrowSetup;
    private static GpuTextureView arrowTextureView;

    public static TextureSetup getArrowSetup() {
        if (arrowSetup == null) {
            net.minecraft.client.renderer.texture.AbstractTexture tex = Minecraft.getInstance().getTextureManager()
                    .getTexture(ARROW_LOCATION);
            if (tex != null) {
                arrowSetup = TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler());
                arrowTextureView = tex.getTextureView();
            }
        }
        return arrowSetup;
    }

    public static GpuTextureView getArrowTextureView() {
        if (arrowTextureView == null) {
            getArrowSetup();
        }
        return arrowTextureView;
    }

    public static final float[][] ARROW_U_TABLE = {
            { 0.0f, 0.0f, 1.0f, 1.0f },
            { 0.0f, 1.0f, 1.0f, 0.0f },
            { 1.0f, 1.0f, 0.0f, 0.0f },
            { 1.0f, 0.0f, 0.0f, 1.0f }
    };

    public static final float[][] ARROW_V_TABLE = {
            { 0.0f, 1.0f, 1.0f, 0.0f },
            { 1.0f, 1.0f, 0.0f, 0.0f },
            { 1.0f, 0.0f, 0.0f, 1.0f },
            { 0.0f, 0.0f, 1.0f, 1.0f }
    };

    public static final float[][] PRECOMPUTED_ARROW_UVS = {
            { 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f },
            { 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f },
            { 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f },
            { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f }
    };

    public static float[] getArrowUvs(int direction) {
        return PRECOMPUTED_ARROW_UVS[direction];
    }

    public static int getArrowDirection(int cp) {
        if (cp == 0x25B6 || cp == 0x25BA || cp == 0x2192)
            return 0; // RIGHT: ▶, ►, →
        if (cp == 0x25BC || cp == 0x25BD || cp == 0x2193)
            return 1; // DOWN: ▼, ▽, ↓
        if (cp == 0x25C0 || cp == 0x25C4 || cp == 0x2190)
            return 2; // LEFT: ◀, ◄, ←
        if (cp == 0x25B2 || cp == 0x25B3 || cp == 0x2191)
            return 3; // UP: ▲, △, ↑
        return -1;
    }

    public static float getArrowSize() {
        float emojiSize = ConfigManager.data.customTextEnabled ? ConfigManager.data.customTextScale : 9.0f;
        return emojiSize * 0.65f;
    }

    public static float getArrowAdvance() {
        return getArrowSize() + 2.0f;
    }

    private static final String TWEMOJI_URL = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/";

    private static final Path CACHE_DIR = FabricLoader.getInstance().getConfigDir()
            .resolve("blackaddons").resolve("cache").resolve("emojis");

    private static final HttpClient HTTP = HttpUtils.client;

    public static class EmojiTexture {
        public final TextureSetup setup;
        public final Object location;
        public final GpuTextureView textureView;

        public EmojiTexture(TextureSetup setup, Object location, GpuTextureView textureView) {
            this.setup = setup;
            this.location = location;
            this.textureView = textureView;
        }
    }

    private static final Map<String, Integer> sequenceToPua = new ConcurrentHashMap<>();
    private static final Map<Integer, String> puaToSequence = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger nextPua = new java.util.concurrent.atomic.AtomicInteger(
            0xF0000);

    public static int getOrCreateSequenceCodepoint(String hexSeq) {
        return sequenceToPua.computeIfAbsent(hexSeq, key -> {
            int pua = nextPua.getAndIncrement();
            puaToSequence.put(pua, key);
            return pua;
        });
    }

    public static String getEmojiHexName(int codepoint) {
        if (codepoint >= 0xF0000 && codepoint <= 0xFFFFF) {
            String seq = puaToSequence.get(codepoint);
            if (seq != null)
                return seq;
        }
        return Integer.toHexString(codepoint);
    }

    public static boolean isEmoji(int cp) {
        if (cp >= 0xF0000 && cp <= 0xFFFFF) {
            return true;
        }
        return Character.isEmoji(cp) && cp > 0xFF;
    }

    public static String preprocessString(String text) {
        if (text == null || text.isEmpty())
            return text;
        StringBuilder sb = new StringBuilder();
        int len = text.length();
        for (int i = 0; i < len;) {
            int cp = text.codePointAt(i);
            int cpCount = Character.charCount(cp);

            if ((cp >= 0x30 && cp <= 0x39) || cp == 0x23 || cp == 0x2A) {
                if (i + cpCount < len) {
                    int next = text.codePointAt(i + cpCount);
                    int nextCount = Character.charCount(next);
                    if (next == 0xFE0F) {
                        if (i + cpCount + nextCount < len) {
                            int next2 = text.codePointAt(i + cpCount + nextCount);
                            if (next2 == 0x20E3) {
                                int pua = getOrCreateSequenceCodepoint(
                                        Integer.toHexString(cp) + "-20e3");
                                sb.appendCodePoint(pua);
                                i += cpCount + nextCount + Character.charCount(next2);
                                continue;
                            }
                        }
                    } else if (next == 0x20E3) {
                        int pua = getOrCreateSequenceCodepoint(
                                Integer.toHexString(cp) + "-20e3");
                        sb.appendCodePoint(pua);
                        i += cpCount + nextCount;
                        continue;
                    }
                }
            }

            if (cp >= 0x1F1E6 && cp <= 0x1F1FF) {
                if (i + cpCount < len) {
                    int next = text.codePointAt(i + cpCount);
                    if (next >= 0x1F1E6 && next <= 0x1F1FF) {
                        int pua = getOrCreateSequenceCodepoint(
                                Integer.toHexString(cp) + "-" + Integer.toHexString(next));
                        sb.appendCodePoint(pua);
                        i += cpCount + Character.charCount(next);
                        continue;
                    }
                }
            }

            if (Character.isEmoji(cp) && cp > 0xFF) {
                int currIdx = i + cpCount;
                StringBuilder seqHex = new StringBuilder(Integer.toHexString(cp));
                boolean hasSequence = false;
                while (currIdx < len) {
                    int next = text.codePointAt(currIdx);
                    int nextCount = Character.charCount(next);
                    if (next == 0x200D) {
                        if (currIdx + nextCount < len) {
                            int nextEmoji = text.codePointAt(currIdx + nextCount);
                            if (Character.isEmoji(nextEmoji) || (nextEmoji >= 0x2000 && nextEmoji <= 0x32FF)) {
                                seqHex.append("-200d-").append(Integer.toHexString(nextEmoji));
                                currIdx += nextCount + Character.charCount(nextEmoji);
                                hasSequence = true;
                                continue;
                            }
                        }
                    } else if (next >= 0x1F3FB && next <= 0x1F3FF) {
                        seqHex.append("-").append(Integer.toHexString(next));
                        currIdx += nextCount;
                        hasSequence = true;
                        continue;
                    } else if (next == 0xFE0F) {
                        currIdx += nextCount;
                        continue;
                    }
                    break;
                }
                if (hasSequence) {
                    int pua = getOrCreateSequenceCodepoint(seqHex.toString());
                    sb.appendCodePoint(pua);
                    i = currIdx;
                    continue;
                }
            }

            sb.appendCodePoint(cp);
            i += cpCount;
        }
        return sb.toString();
    }

    private static final Map<Integer, Object> textureCache = new ConcurrentHashMap<>();
    private static final Object LOADING_SENTINEL = new Object();

    public static EmojiTexture getEmojiTexture(int codepoint) {
        Object val = textureCache.get(codepoint);
        if (val != null) {
            return val == LOADING_SENTINEL ? null : (EmojiTexture) val;
        }

        textureCache.put(codepoint, LOADING_SENTINEL);
        String hex = getEmojiHexName(codepoint).toLowerCase();

        Thread t = new Thread(() -> {
            try {
                Files.createDirectories(CACHE_DIR);
                Path path = CACHE_DIR.resolve(hex + ".png");
                byte[] bytes;

                if (Files.exists(path)) {
                    bytes = Files.readAllBytes(path);
                } else {
                    String url = TWEMOJI_URL + hex + ".png";
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("User-Agent", "blackaddons-minecraft-mod")
                            .GET().timeout(Duration.ofSeconds(20)).build();
                    HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    if (resp.statusCode() == 200) {
                        bytes = resp.body();
                        Files.write(path, bytes);
                    } else {
                        textureCache.remove(codepoint);
                        return;
                    }
                }

                if (bytes != null && bytes.length > 0) {
                    final byte[] finalBytes = bytes;
                    Minecraft.getInstance().execute(() -> {
                        try (InputStream in = new ByteArrayInputStream(finalBytes)) {
                            NativeImage ni = NativeImage.read(in);
                            DynamicTexture tex = new DynamicTexture(() -> "emoji_" + hex, ni);
                            Object loc = McCompat.registerTexture(tex, "blackaddons", "emoji/" + hex);
                            TextureSetup setup = TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler());
                            textureCache.put(codepoint, new EmojiTexture(setup, loc, tex.getTextureView()));
                        } catch (Exception e) {
                            textureCache.remove(codepoint);
                        }
                    });
                }
            } catch (Exception e) {
                textureCache.remove(codepoint);
            }
        }, "Emoji-Downloader-" + hex);
        t.setDaemon(true);
        t.start();

        return null;
    }
}
