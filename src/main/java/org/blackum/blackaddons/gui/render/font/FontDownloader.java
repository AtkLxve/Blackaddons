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
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FontDownloader {

    private static final String TREE_API = "https://api.github.com/repos/google/fonts/git/trees/";

    private static final Path CACHE_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("blackaddons").resolve("cache").resolve("fonts");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static ByteBuffer download(String fontName, Consumer<long[]> onProgress) {
        String safeName = fontName.replaceAll("[^A-Za-z0-9_\\-]", "_");
        Path cachePath = CACHE_DIR.resolve(safeName + ".ttf");

        if (Files.exists(cachePath)) {
            return loadFromDisk(cachePath);
        }

        try {
            String url = GoogleFontsList.getUrl(fontName);
            if (url == null) {
                url = resolveFromGitTree(fontName);
                if (url != null) GoogleFontsList.cacheUrl(fontName, url);
            }
            if (url == null) {
                return null;
            }

            byte[] bytes = fetchWithProgress(url, onProgress);
            Files.createDirectories(CACHE_DIR);
            Files.write(cachePath, bytes);
            return wrap(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    public static ByteBuffer download(String fontName) {
        return download(fontName, null);
    }

    public static void clearCache(String fontName) {
        String safeName = fontName.replaceAll("[^A-Za-z0-9_\\-]", "_");
        try { Files.deleteIfExists(CACHE_DIR.resolve(safeName + ".ttf")); }
        catch (IOException ignored) {}
    }

    private static String resolveFromGitTree(String fontName) {
        try {
            String dirName = GoogleFontsList.getDirName(fontName);
            String sha = GoogleFontsList.getSha(fontName);
            if (sha == null) {
                return null;
            }
            String treeUrl = TREE_API + sha;
            String json = GoogleFontsList.httpGet(treeUrl);
            Pattern p = Pattern.compile("\"path\":\\s*\"([^\"]+\\.ttf)\"");
            Matcher m = p.matcher(json);
            if (m.find()) {
                String filename = m.group(1);
                String url = new URI("https", "github.com", "/google/fonts/raw/refs/heads/main/ofl/" + dirName + "/" + filename, null).toASCIIString();
                return url;
            }
        } catch (Exception e) {
        }
        return null;
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
                    onProgress.accept(new long[]{dl, tot});
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
