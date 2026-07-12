package org.blackum.blackaddons.gui.render.font;

import net.fabricmc.loader.api.FabricLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.blackum.blackaddons.common.util.io.HttpUtils;

public class FontDownloader {

    private static final Path CACHE_DIR = FabricLoader.getInstance().getConfigDir().resolve("blackaddons")
            .resolve("cache").resolve("fonts");

    private static final HttpClient HTTP = HttpUtils.client;

    private static final ConcurrentHashMap<String, ByteBuffer> memoryCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> downloadLocks = new ConcurrentHashMap<>();

    public static ByteBuffer download(String fontName, Consumer<long[]> onProgress) {
        ByteBuffer cached = memoryCache.get(fontName);
        if (cached != null) {
            return cached.duplicate();
        }

        Object lock = downloadLocks.computeIfAbsent(fontName, k -> new Object());
        synchronized (lock) {
            ByteBuffer afterLock = memoryCache.get(fontName);
            if (afterLock != null) {
                return afterLock.duplicate();
            }

            String safeName = fontName.replaceAll("[^A-Za-z0-9_\\-]", "_");
            Path cachePath = CACHE_DIR.resolve(safeName + ".ttf");

            if (Files.exists(cachePath)) {
                ByteBuffer buf = loadFromDisk(cachePath);
                if (buf != null) {
                    memoryCache.put(fontName, buf);
                    return buf.duplicate();
                }
            }

            try {
                String url = GoogleFontsList.getUrl(fontName);
                if (url == null) {
                    return null;
                }

                byte[] bytes = fetchWithProgress(url, onProgress);
                Files.createDirectories(CACHE_DIR);
                Files.write(cachePath, bytes);
                ByteBuffer buf = wrap(bytes);
                memoryCache.put(fontName, buf);
                return buf.duplicate();
            } catch (Exception e) {
                return null;
            } finally {
                downloadLocks.remove(fontName);
            }
        }
    }

    public static ByteBuffer download(String fontName) {
        return download(fontName, null);
    }

    public static void clearCache(String fontName) {
        memoryCache.remove(fontName);
        String safeName = fontName.replaceAll("[^A-Za-z0-9_\\-]", "_");
        try {
            Files.deleteIfExists(CACHE_DIR.resolve(safeName + ".ttf"));
        } catch (IOException ignored) {
        }
    }

    public static void evictStaleEntries(Map<String, String> oldUrls, Map<String, String> newUrls) {
        for (Map.Entry<String, String> old : oldUrls.entrySet()) {
            String newUrl = newUrls.get(old.getKey());
            if (newUrl == null || !newUrl.equals(old.getValue())) {
                clearCache(old.getKey());
            }
        }
    }

    private static byte[] fetchWithProgress(String url, Consumer<long[]> onProgress)
            throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "blackaddons-minecraft-mod")
                .GET()
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + ": " + url);
        }

        long total = resp.headers().firstValueAsLong("content-length").orElse(-1L);
        try (InputStream is = resp.body()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long downloaded = 0;
            int n;
            while ((n = is.read(chunk)) != -1) {
                out.write(chunk, 0, n);
                downloaded += n;
                if (onProgress != null) {
                    long dl = downloaded, tot = total;
                    onProgress.accept(new long[] { dl, tot });
                }
            }
            return out.toByteArray();
        }
    }

    private static ByteBuffer loadFromDisk(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return wrap(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    private static ByteBuffer wrap(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.allocateDirect(bytes.length);
        buf.put(bytes).flip();
        return buf;
    }
}
