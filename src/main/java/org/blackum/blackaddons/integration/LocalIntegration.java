package org.blackum.blackaddons.integration;

import org.blackum.blackaddons.core.util.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.manager.LocalTeammateManager;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.file.Path;

public class LocalIntegration {
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
            .build();

    private static Map<String, Double> priceCache = new ConcurrentHashMap<>();
    private static final AtomicLong priceCacheExpiry = new AtomicLong(0);
    private static volatile boolean isRefreshing = false;

    private static final Path CONFIG_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve(Constants.CONFIG_DIR_NAME);
    private static final File PRICES_FILE = CONFIG_DIR.resolve(Constants.PRICES_FILE_NAME).toFile();

    private static class PricesData {
        long timestamp;
        Map<String, Double> prices;
    }

    public static Double getPrice(String itemId) {
        if (System.currentTimeMillis() > priceCacheExpiry.get()) {
            checkAndRefreshPrices();
        }
        return priceCache.getOrDefault(itemId, 0.0);
    }

    public static CompletableFuture<JsonObject> getProfileStats(String player, String profileName, boolean force) {
        checkAndRefreshPrices();
        return getUuid(player)
                .thenCompose(uuid -> {
                    if (uuid == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return getProfileData(uuid).thenApply(profileData -> {
                        if (profileData == null)
                            return null;
                        return processProfileData(uuid, profileData, profileName);
                    });
                });
    }

    private static void checkAndRefreshPrices() {
        if (priceCache.isEmpty()) {
            loadPrices();
        }
        if (!isRefreshing && System.currentTimeMillis() > priceCacheExpiry.get()) {
            refreshPrices();
        }
    }

    private static void refreshPrices() {
        isRefreshing = true;
        Blackaddons.LOGGER.info("Refreshing local prices...");
        CompletableFuture<Map<String, Double>> bzFuture = getBazaarPrices();
        CompletableFuture<Map<String, Double>> ahFuture = getAhPrices();
        CompletableFuture<Map<String, Double>> specialFuture = getSpecialPrices();

        CompletableFuture.allOf(bzFuture, ahFuture, specialFuture).thenRun(() -> {
            Map<String, Double> newPrices = new HashMap<>();
            try {
                newPrices.putAll(bzFuture.get());
                newPrices.putAll(ahFuture.get());
                newPrices.putAll(specialFuture.get());

                if (newPrices.containsKey("SKELETON_MASTER_CHESTPLATE")) {
                    newPrices.put("SKELETON_MASTER_CHESTPLATE_50", newPrices.get("SKELETON_MASTER_CHESTPLATE"));
                }

                priceCache = new ConcurrentHashMap<>(newPrices);
                priceCacheExpiry.set(System.currentTimeMillis() + Constants.PRICE_CACHE_DURATION_MS);
                savePrices(newPrices);
                Blackaddons.LOGGER.info("Local prices refreshed. Total items: " + newPrices.size());
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to merge prices: " + e.getMessage());
            } finally {
                isRefreshing = false;
            }
        });
    }

    private static CompletableFuture<Map<String, Double>> getBazaarPrices() {
        return sendGetRequest(Constants.HYPIXEL_BAZAAR_API).thenApply(res -> {
            Map<String, Double> prices = new HashMap<>();
            if (res != null && res.statusCode() == 200) {
                try {
                    JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                    JsonObject products = json.getAsJsonObject("products");
                    for (String key : products.keySet()) {
                        JsonObject product = products.getAsJsonObject(key);
                        double sellPrice = product.getAsJsonObject("quick_status").get("sellPrice").getAsDouble();
                        prices.put(key, sellPrice);
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Error parsing Bazaar prices: " + e.getMessage());
                }
            }
            return prices;
        });
    }

    private static CompletableFuture<Map<String, Double>> getAhPrices() {
        return sendGetRequest(Constants.MOULBERRY_AH_API).thenApply(res -> {
            Map<String, Double> prices = new HashMap<>();
            if (res != null && res.statusCode() == 200) {
                try {
                    JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                    for (String key : json.keySet()) {
                        prices.put(key, json.get(key).getAsDouble());
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Error parsing AH prices: " + e.getMessage());
                }
            }
            return prices;
        });
    }

    private static CompletableFuture<Map<String, Double>> getSpecialPrices() {
        Map<String, String[]> specials = new HashMap<>();
        specials.put("SHINY_NECRON_HANDLE", new String[] { Constants.COFL_SHINY_NECRON_HANDLE });
        specials.put("SKELETON_MASTER_CHESTPLATE", new String[] {
                Constants.COFL_SKELETON_MASTER_CHESTPLATE_MAX,
                Constants.COFL_SKELETON_MASTER_CHESTPLATE_BASE // Fallback
        });

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Map<String, Double> results = new ConcurrentHashMap<>();

        for (Map.Entry<String, String[]> entry : specials.entrySet()) {
            CompletableFuture<Void> itemFuture = CompletableFuture.completedFuture(null);

            for (String url : entry.getValue()) {
                itemFuture = itemFuture.thenCompose(v -> {
                    if (results.containsKey(entry.getKey()) && results.get(entry.getKey()) > 0) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return sendGetRequest(url).thenAccept(res -> {
                        if (res != null && res.statusCode() == 200) {
                            try {
                                JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                                double price = json.has("median") ? json.get("median").getAsDouble()
                                        : (json.has("min") ? json.get("min").getAsDouble() : 0);
                                if (price > 0)
                                    results.put(entry.getKey(), price);
                            } catch (Exception e) {
                            }
                        }
                    });
                });
            }
            futures.add(itemFuture);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> new HashMap<>(results));
    }

    private static CompletableFuture<String> getUuid(String name) {
        String url = Constants.PLAYER_DB_API + name;
        return sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                    return json.get("data").getAsJsonObject().get("player").getAsJsonObject().get("raw_id")
                            .getAsString();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse UUID for " + name + ": " + e.getMessage());
                }
            }
            return null;
        });
    }

    private static CompletableFuture<JsonObject> getProfileData(String uuid) {
        String url = Constants.ADJECTILS_PROFILE_API + uuid;
        return sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse profile data: " + e.getMessage());
                }
            }
            return null;
        });
    }

    private static JsonObject processProfileData(String uuid, JsonObject profileData, String profileName) {
        try {
            if (!profileData.has("profiles") || profileData.get("profiles").isJsonNull())
                return null;

            JsonArray profiles = profileData.getAsJsonArray("profiles");
            if (profiles.isEmpty())
                return null;

            JsonArray profilesList = new JsonArray();
            for (JsonElement p : profiles) {
                JsonObject obj = p.getAsJsonObject();
                JsonObject pData = new JsonObject();
                pData.addProperty("name", obj.get("cute_name").getAsString());
                pData.addProperty("id", obj.get("profile_id").getAsString());
                pData.addProperty("selected", obj.has("selected") && obj.get("selected").getAsBoolean());
                profilesList.add(pData);
            }

            JsonObject selectedProfile = getSelectedProfile(profiles, profileName);
            if (selectedProfile == null)
                return null;

            JsonObject members = selectedProfile.getAsJsonObject("members");

            if (!members.has(uuid))
                return null;

            JsonObject member = members.getAsJsonObject(uuid);
            JsonObject dungeons = member.has("dungeons") ? member.getAsJsonObject("dungeons") : new JsonObject();

            JsonObject result = new JsonObject();
            result.addProperty("catacombs", extractCatacombsXp(dungeons));
            result.addProperty("secrets", extractSecrets(dungeons, member));
            result.addProperty("blood_mob_kills", extractBloodMobKills(member));
            result.add("classes", extractClassXp(dungeons));
            result.add("floors", extractFloorStats(dungeons));

            JsonArray recentRuns = extractRecentRuns(dungeons, uuid);
            result.add("recent_runs", recentRuns);
            result.add("teammates", LocalTeammateManager.getInstance().getTeammates(uuid));
            result.add("daily_stats", new JsonObject());
            result.add("monthly_stats", new JsonObject());
            result.add("profiles", profilesList);

            return result;
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Error processing profile data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static JsonObject getSelectedProfile(JsonArray profiles, String profileName) {
        JsonObject bestProfile = null;

        if (profileName != null) {
            for (JsonElement p : profiles) {
                JsonObject obj = p.getAsJsonObject();
                if (obj.has("cute_name") && obj.get("cute_name").getAsString().equalsIgnoreCase(profileName)) {
                    return obj;
                }
            }
        }

        for (JsonElement p : profiles) {
            JsonObject obj = p.getAsJsonObject();
            if (obj.has("selected") && obj.get("selected").getAsBoolean()) {
                bestProfile = obj;
                break;
            }
        }
        return (bestProfile != null) ? bestProfile : profiles.get(0).getAsJsonObject();
    }

    private static double extractCatacombsXp(JsonObject dungeons) {
        JsonObject dungeonTypes = dungeons.has("dungeon_types") ? dungeons.getAsJsonObject("dungeon_types")
                : new JsonObject();
        JsonObject catacombs = dungeonTypes.has("catacombs") ? dungeonTypes.getAsJsonObject("catacombs")
                : new JsonObject();
        return catacombs.has("experience") ? catacombs.get("experience").getAsDouble() : 0.0;
    }

    private static int extractSecrets(JsonObject dungeons, JsonObject member) {
        if (dungeons.has("secrets")) {
            return dungeons.get("secrets").getAsInt();
        } else if (member.has("achievements")) {
            JsonObject achievements = member.getAsJsonObject("achievements");
            if (achievements.has("skyblock_treasure_hunter")) {
                return achievements.get("skyblock_treasure_hunter").getAsInt();
            }
        }
        return 0;
    }

    private static int extractBloodMobKills(JsonObject member) {
        if (member.has("player_stats")) {
            JsonObject stats = member.getAsJsonObject("player_stats");
            if (stats.has("kills")) {
                JsonObject kills = stats.getAsJsonObject("kills");
                if (kills.has("watcher_summon_undead")) {
                    return kills.get("watcher_summon_undead").getAsInt();
                }
            }
        }
        return 0;
    }

    private static JsonObject extractClassXp(JsonObject dungeons) {
        JsonObject playerClasses = dungeons.has("player_classes") ? dungeons.getAsJsonObject("player_classes")
                : new JsonObject();
        JsonObject classXp = new JsonObject();
        String[] classes = { "archer", "berserk", "healer", "mage", "tank" };
        for (String cls : classes) {
            double xp = 0;
            if (playerClasses.has(cls)) {
                xp = playerClasses.getAsJsonObject(cls).get("experience").getAsDouble();
            }
            String capitalized = cls.substring(0, 1).toUpperCase() + cls.substring(1);
            classXp.addProperty(capitalized, xp);
        }
        return classXp;
    }

    private static JsonObject extractFloorStats(JsonObject dungeons) {
        JsonObject dungeonTypes = dungeons.has("dungeon_types") ? dungeons.getAsJsonObject("dungeon_types")
                : new JsonObject();
        JsonObject catacombs = dungeonTypes.has("catacombs") ? dungeonTypes.getAsJsonObject("catacombs")
                : new JsonObject();
        JsonObject masterCatacombs = dungeonTypes.has("master_catacombs")
                ? dungeonTypes.getAsJsonObject("master_catacombs")
                : new JsonObject();

        JsonObject floors = new JsonObject();
        processTier(catacombs, "F", floors);
        processTier(masterCatacombs, "M", floors);
        return floors;
    }

    private static JsonArray extractRecentRuns(JsonObject dungeons, String uuid) {
        JsonArray recentRuns = new JsonArray();
        if (dungeons.has("treasures")) {
            JsonObject treasures = dungeons.getAsJsonObject("treasures");
            if (treasures.has("runs")) {
                recentRuns = treasures.getAsJsonArray("runs");
                long minTs = Long.MAX_VALUE;
                for (JsonElement run : recentRuns) {
                    if (run.isJsonObject() && run.getAsJsonObject().has("completion_ts")) {
                        long ts = run.getAsJsonObject().get("completion_ts").getAsLong();
                        if (ts < minTs)
                            minTs = ts;
                    }
                }
                if (minTs != Long.MAX_VALUE) {
                    LocalTeammateManager.getInstance()
                            .setLocalRunWindowStart(minTs / 1000);
                }
                LocalTeammateManager.getInstance().processRuns(uuid, recentRuns);
            }
        }
        return recentRuns;
    }

    private static void processTier(JsonObject tierData, String prefix, JsonObject floors) {
        if (tierData == null)
            return;

        JsonObject timesSPlus = tierData.has("fastest_time_s_plus") ? tierData.getAsJsonObject("fastest_time_s_plus")
                : new JsonObject();
        JsonObject timesS = tierData.has("fastest_time_s") ? tierData.getAsJsonObject("fastest_time_s")
                : new JsonObject();
        JsonObject runs = tierData.has("tier_completions") ? tierData.getAsJsonObject("tier_completions")
                : new JsonObject();
        JsonObject bestScore = tierData.has("best_score") ? tierData.getAsJsonObject("best_score") : new JsonObject();

        for (String tier : runs.keySet()) {
            if ("total".equals(tier))
                continue;

            String floorName;
            if ("0".equals(tier)) {
                floorName = "F".equals(prefix) ? "Entrance" : "M0";
            } else {
                floorName = prefix + tier;
            }

            int runsVal = runs.has(tier) ? runs.get(tier).getAsInt() : 0;
            int bestScoreVal = bestScore.has(tier) ? bestScore.get(tier).getAsInt() : 0;
            int fastSPlus = timesSPlus.has(tier) ? timesSPlus.get(tier).getAsInt() : 0;
            int fastS = timesS.has(tier) ? timesS.get(tier).getAsInt() : 0;

            JsonObject floorObj = new JsonObject();
            floorObj.addProperty("runs", runsVal);
            floorObj.addProperty("best_score", bestScoreVal);
            floorObj.addProperty("fastest_s_plus", fastSPlus);
            floorObj.addProperty("fastest_s", fastS);

            floors.add(floorName, floorObj);
        }
    }

    private static CompletableFuture<HttpResponse<String>> sendGetRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .header("User-Agent", Constants.BROWSER_USER_AGENT)
                .header("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        Blackaddons.LOGGER.info("Successfully fetched: " + url);
                    } else {
                        Blackaddons.LOGGER.warn("Fetch failed. Status: " + res.statusCode() + " URL: " + url);
                    }
                    return res;
                })
                .exceptionally(e -> {
                    Blackaddons.LOGGER.error("Error fetching " + url + ": " + e.getMessage());
                    return null;
                });
    }

    private static void savePrices(Map<String, Double> prices) {
        if (!CONFIG_DIR.toFile().exists()) {
            CONFIG_DIR.toFile().mkdirs();
        }

        PricesData data = new PricesData();
        data.timestamp = System.currentTimeMillis();
        data.prices = prices;

        try (FileWriter writer = new FileWriter(PRICES_FILE)) {
            Constants.GSON.toJson(data, writer);
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Failed to save prices: " + e.getMessage());
        }
    }

    private static void loadPrices() {
        if (!PRICES_FILE.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(PRICES_FILE)) {
            PricesData data = Constants.GSON.fromJson(reader, PricesData.class);
            if (data != null && data.prices != null) {
                long age = System.currentTimeMillis() - data.timestamp;
                if (age < Constants.PRICE_CACHE_DURATION_MS) {
                    priceCache = new ConcurrentHashMap<>(data.prices);
                    priceCacheExpiry.set(data.timestamp + Constants.PRICE_CACHE_DURATION_MS);
                    Blackaddons.LOGGER.info("Loaded prices from local cache. Age: " + (age / 1000 / 60) + "m");
                } else {
                    Blackaddons.LOGGER.info("Local price cache expired.");
                }
            }
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Failed to load prices: " + e.getMessage());
        }
    }
}
