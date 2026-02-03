package org.blackum.blackaddons.util;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ProfileStateManager {
    private static ProfileStateManager instance;
    private final Map<String, CacheEntry<JsonObject>> profileCache = new HashMap<>();
    private final Map<String, CacheEntry<JsonObject>> rngCache = new HashMap<>();
    private final Map<String, LeaderboardCache> leaderboardCache = new HashMap<>();

    private ProfileStateManager() {
    }

    public static synchronized ProfileStateManager getInstance() {
        if (instance == null) {
            instance = new ProfileStateManager();
        }
        return instance;
    }

    public CompletableFuture<BotResult<JsonObject>> getProfile(String player, boolean force) {
        if (force) {
            clearCache(player);
        } else if (profileCache.containsKey(player.toLowerCase())) {
            CacheEntry<JsonObject> entry = profileCache.get(player.toLowerCase());
            if (!entry.isExpired()) {
                return CompletableFuture.completedFuture(BotResult.success(entry.data));
            }
        }

        return BotIntegration.getProfileStats(player, force).thenApply(json -> {
            if (json == null)
                return BotResult.error("API Unavailable");
            if (json.has("error"))
                return BotResult.error(json.get("error").getAsString());
            if (json.has("data")) {
                JsonObject data = json.getAsJsonObject("data");
                profileCache.put(player.toLowerCase(), new CacheEntry<>(data));
                return BotResult.success(data);
            }
            return BotResult.error("Invalid data");
        });
    }

    public CompletableFuture<BotResult<JsonObject>> getRngData(String player) {
        if (rngCache.containsKey(player.toLowerCase())) {
            CacheEntry<JsonObject> entry = rngCache.get(player.toLowerCase());
            if (!entry.isExpired()) {
                return CompletableFuture.completedFuture(BotResult.success(entry.data));
            }
        }

        return BotIntegration.getRngData(player).thenApply(json -> {
            if (json == null)
                return BotResult.error("API Unavailable");
            if (json.has("error"))
                return BotResult.error(json.get("error").getAsString());
            if (json.has("data")) {
                JsonObject data = json.getAsJsonObject("data");
                rngCache.put(player.toLowerCase(), new CacheEntry<>(data));
                return BotResult.success(data);
            }
            return BotResult.error("Invalid data");
        });
    }

    public void clearCache(String player) {
        profileCache.remove(player.toLowerCase());
        rngCache.remove(player.toLowerCase());
    }

    private static class CacheEntry<T> {
        final T data;
        final long timestamp;

        CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            long durationMs = org.blackum.blackaddons.general.GeneralOptions.CACHE_DURATION_MINUTES * 60 * 1000L;
            return System.currentTimeMillis() - timestamp > durationMs;
        }
    }

    private static class LeaderboardCache {
        Map<Integer, JsonObject> pages = new HashMap<>();
        long lastUpdated = 0;
    }
}
