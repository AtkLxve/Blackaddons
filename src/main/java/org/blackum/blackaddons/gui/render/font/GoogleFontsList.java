package org.blackum.blackaddons.gui.render.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.util.io.HttpUtils;

public class GoogleFontsList {

    private static final String FONTS_API = Constants.DEFAULT_BOT_URL + "/v1/fonts";

    private static final Path CACHE_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("blackaddons").resolve("data").resolve("fontnames.txt");

    private static final HttpClient HTTP = HttpUtils.client;

    private static final List<String> nameList = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, String> urlByName = new ConcurrentHashMap<>();
    private static volatile String cachedHash = null;

    private static volatile boolean lazyStarted = false;
    private static volatile boolean lazyDone    = false;
    private static volatile boolean revalidateStarted = false;
    private static final List<Runnable> pendingCallbacks =
            Collections.synchronizedList(new ArrayList<>());

    public static List<String> get() { return nameList; }

    public static boolean isLazyDone() { return lazyDone; }

    public static String getUrl(String displayName) {
        return urlByName.get(displayName);
    }

    public static void startLazyLoad(Runnable onReady) {
        if (lazyDone) {
            if (onReady != null) Minecraft.getInstance().execute(onReady);
            return;
        }
        if (onReady != null) pendingCallbacks.add(onReady);
        if (lazyStarted) return;
        lazyStarted = true;

        Thread t = new Thread(() -> {
            try {
                Map<String, String> entries;
                if (Files.exists(CACHE_FILE)) {
                    entries = loadFromCache();
                } else {
                    ApiResponse apiResponse = fetchFromApi();
                    entries = apiResponse.fonts;
                    saveToCache(apiResponse.hash, entries);
                }

                applyEntries(entries);
                lazyDone = true;
            } catch (Exception ignored) {
            } finally {
                lazyStarted = false;
                if (lazyDone) lazyStarted = true;
                List<Runnable> cbs = new ArrayList<>(pendingCallbacks);
                pendingCallbacks.clear();
                Minecraft.getInstance().execute(() -> cbs.forEach(Runnable::run));
                revalidateInBackground();
            }
        }, "CustomFont-FontList");
        t.setDaemon(true);
        t.start();
    }

    private static void revalidateInBackground() {
        if (revalidateStarted) return;
        revalidateStarted = true;
        Thread t = new Thread(() -> {
            try {
                ApiResponse apiResponse = fetchFromApi();
                if (apiResponse.hash != null && apiResponse.hash.equals(cachedHash)) {
                    return;
                }
                Map<String, String> oldUrls = new HashMap<>(urlByName);
                applyEntries(apiResponse.fonts);
                saveToCache(apiResponse.hash, apiResponse.fonts);
                FontDownloader.evictStaleEntries(oldUrls, apiResponse.fonts);
            } catch (Exception ignored) {
            } finally {
                revalidateStarted = false;
            }
        }, "CustomFont-Revalidate");
        t.setDaemon(true);
        t.start();
    }

    private static void applyEntries(Map<String, String> entries) {
        List<String> newNames = new ArrayList<>(entries.keySet());
        newNames.sort(String.CASE_INSENSITIVE_ORDER);
        urlByName.clear();
        urlByName.putAll(entries);
        synchronized (nameList) {
            nameList.clear();
            nameList.addAll(newNames);
        }
    }

    public static void invalidateCache() {
        lazyDone         = false;
        lazyStarted      = false;
        revalidateStarted = false;
        cachedHash       = null;
        synchronized (nameList) { nameList.clear(); }
        urlByName.clear();
        try { Files.deleteIfExists(CACHE_FILE); } catch (Exception ignored) {}
    }

    private static ApiResponse fetchFromApi() throws Exception {
        String json = httpGet(FONTS_API);
        return parseApiResponse(json);
    }

    static ApiResponse parseApiResponse(String json) {
        Map<String, String> fonts = new LinkedHashMap<>();
        String hash = null;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("hash")) hash = root.get("hash").getAsString();
            JsonObject fontsObj = root.getAsJsonObject("fonts");
            if (fontsObj != null) {
                for (Map.Entry<String, JsonElement> entry : fontsObj.entrySet()) {
                    String name = entry.getKey();
                    String url = entry.getValue().getAsString();
                    if (name != null && !name.isBlank() && url != null && !url.isBlank()) {
                        fonts.put(name, url);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new ApiResponse(hash, fonts);
    }

    private static Map<String, String> loadFromCache() throws Exception {
        List<String> lines = Files.readAllLines(CACHE_FILE);
        if (lines.isEmpty()) {
            return fetchAndRefreshCache();
        }

        String firstLine = lines.get(0);
        if (firstLine.startsWith("hash=")) {
            cachedHash = firstLine.substring(5);
        } else {
            return fetchAndRefreshCache();
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split("\t", 2);
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                result.put(parts[0], parts[1]);
            }
        }

        if (result.isEmpty()) {
            return fetchAndRefreshCache();
        }
        return result;
    }

    private static Map<String, String> fetchAndRefreshCache() throws Exception {
        ApiResponse apiResponse = fetchFromApi();
        saveToCache(apiResponse.hash, apiResponse.fonts);
        return apiResponse.fonts;
    }

    private static void saveToCache(String hash, Map<String, String> entries) throws Exception {
        Files.createDirectories(CACHE_FILE.getParent());
        List<String> lines = new ArrayList<>(entries.size() + 1);
        lines.add("hash=" + (hash != null ? hash : ""));
        for (Map.Entry<String, String> e : entries.entrySet()) {
            lines.add(e.getKey() + "\t" + e.getValue());
        }
        Files.write(CACHE_FILE, lines);
        cachedHash = hash;
    }

    static String httpGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "blackaddons-minecraft-mod")
                .GET().timeout(Duration.ofSeconds(30)).build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + url);
        return resp.body();
    }

    record ApiResponse(String hash, Map<String, String> fonts) {}
}
