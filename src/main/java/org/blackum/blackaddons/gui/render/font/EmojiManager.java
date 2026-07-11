package org.blackum.blackaddons.gui.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.gui.render.TextureSetup;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.blackum.blackaddons.common.util.mc.McCompat;

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

public class EmojiManager {

    private static final String TWEMOJI_URL = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/";

    private static final Path CACHE_DIR = FabricLoader.getInstance().getConfigDir()
            .resolve("blackaddons").resolve("cache").resolve("emojis");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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

    public static boolean isEmoji(int cp) {
        return Character.isEmoji(cp) && cp > 0xFF;
    }

    private static final Map<Integer, Object> textureCache = new ConcurrentHashMap<>();
    private static final Object LOADING_SENTINEL = new Object();

    public static EmojiTexture getEmojiTexture(int codepoint) {
        Object val = textureCache.get(codepoint);
        if (val != null) {
            return val == LOADING_SENTINEL ? null : (EmojiTexture) val;
        }

        textureCache.put(codepoint, LOADING_SENTINEL);
        String hex = Integer.toHexString(codepoint).toLowerCase();

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
