package org.blackum.blackaddons.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.features.LocalTeammateManager;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LocalIntegration {
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // Cloudflare bypass
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; x64; rv:136.0) Gecko/20100101 Firefox/136.0";

    private static Map<String, Double> priceCache = new HashMap<>();
    private static long priceCacheExpiry = 0;
    private static final long PRICE_CACHE_DURATION_MS = 24 * 60 * 60 * 1000L; // 24 hours
    private static volatile boolean isRefreshing = false;

    private static final java.nio.file.Path CONFIG_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve("blackaddons");
    private static final File PRICES_FILE = CONFIG_DIR.resolve("prices.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static class PricesData {
        long timestamp;
        Map<String, Double> prices;
    }

    public static Double getPrice(String itemId) {
        if (System.currentTimeMillis() > priceCacheExpiry) {
            checkAndRefreshPrices();
        }
        return priceCache.getOrDefault(itemId, 0.0);
    }

    public static CompletableFuture<JsonObject> getProfileStats(String player, boolean force) {
        checkAndRefreshPrices();
        return getUuid(player)
                .thenCompose(uuid -> {
                    if (uuid == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return getProfileData(uuid).thenApply(profileData -> {
                        if (profileData == null)
                            return null;
                        return processProfileData(uuid, profileData);
                    });
                });
    }

    private static void checkAndRefreshPrices() {
        if (priceCache.isEmpty()) {
            loadPrices();
        }
        if (!isRefreshing && System.currentTimeMillis() > priceCacheExpiry) {
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

                priceCache = newPrices;
                priceCacheExpiry = System.currentTimeMillis() + PRICE_CACHE_DURATION_MS;
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
        return sendGetRequest("https://api.hypixel.net/skyblock/bazaar").thenApply(res -> {
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
        return sendGetRequest("https://moulberry.codes/auction_averages_lbin/3day.json").thenApply(res -> {
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
        specials.put("SHINY_NECRON_HANDLE",
                new String[] { "https://sky.coflnet.com/api/item/price/NECRON_HANDLE?IsShiny=true" });
        specials.put("SKELETON_MASTER_CHESTPLATE", new String[] {
                "https://sky.coflnet.com/api/item/price/SKELETON_MASTER_CHESTPLATE?ItemTier=10-10&NoOtherValuableEnchants=true&BaseStatBoost=50",
                "https://sky.coflnet.com/api/item/price/SKELETON_MASTER_CHESTPLATE?BaseStatBoost=50" // Fallback
        });

        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        Map<String, Double> results = new java.util.concurrent.ConcurrentHashMap<>();

        for (Map.Entry<String, String[]> entry : specials.entrySet()) {
            CompletableFuture<Void> itemFuture = CompletableFuture.runAsync(() -> {
            });

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
        String url = "https://playerdb.co/api/player/minecraft/" + name;
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
        String url = "https://adjectilsbackend.adjectivenoun3215.workers.dev/v2/skyblock/profiles?uuid=" + uuid;
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

    private static JsonObject processProfileData(String uuid, JsonObject profileData) {
        try {
            if (!profileData.has("profiles") || profileData.get("profiles").isJsonNull())
                return null;

            var profiles = profileData.getAsJsonArray("profiles");
            if (profiles.isEmpty())
                return null;

            JsonObject bestProfile = null;
            for (JsonElement p : profiles) {
                JsonObject obj = p.getAsJsonObject();
                if (obj.has("selected") && obj.get("selected").getAsBoolean()) {
                    bestProfile = obj;
                    break;
                }
            }
            if (bestProfile == null)
                bestProfile = profiles.get(0).getAsJsonObject();

            JsonObject members = bestProfile.getAsJsonObject("members");
            if (!members.has(uuid))
                return null;

            JsonObject member = members.getAsJsonObject(uuid);
            JsonObject dungeons = member.has("dungeons") ? member.getAsJsonObject("dungeons") : new JsonObject();
            JsonObject dungeonTypes = dungeons.has("dungeon_types") ? dungeons.getAsJsonObject("dungeon_types")
                    : new JsonObject();

            JsonObject catacombs = dungeonTypes.has("catacombs") ? dungeonTypes.getAsJsonObject("catacombs")
                    : new JsonObject();
            JsonObject masterCatacombs = dungeonTypes.has("master_catacombs")
                    ? dungeonTypes.getAsJsonObject("master_catacombs")
                    : new JsonObject();

            double cataXp = catacombs.has("experience") ? catacombs.get("experience").getAsDouble() : 0.0;

            int secrets = 0;
            if (dungeons.has("secrets")) {
                secrets = dungeons.get("secrets").getAsInt();
            } else if (member.has("achievements")) {
                JsonObject achievements = member.getAsJsonObject("achievements");
                if (achievements.has("skyblock_treasure_hunter")) {
                    secrets = achievements.get("skyblock_treasure_hunter").getAsInt();
                }
            }

            int bloodMobKills = 0;
            if (member.has("player_stats")) {
                JsonObject stats = member.getAsJsonObject("player_stats");
                if (stats.has("kills")) {
                    JsonObject kills = stats.getAsJsonObject("kills");
                    if (kills.has("watcher_summon_undead")) {
                        bloodMobKills = kills.get("watcher_summon_undead").getAsInt();
                    }
                }
            }

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

            JsonObject floors = new JsonObject();
            processTier(catacombs, "F", floors);
            processTier(masterCatacombs, "M", floors);

            JsonObject result = new JsonObject();
            result.addProperty("catacombs", cataXp);
            result.addProperty("secrets", secrets);
            result.addProperty("blood_mob_kills", bloodMobKills);
            result.add("classes", classXp);
            result.add("floors", floors);

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

            result.add("recent_runs", recentRuns);
            result.add("teammates",
                    LocalTeammateManager.getInstance().getTeammates(uuid));
            result.add("daily_stats", new JsonObject());
            result.add("monthly_stats", new JsonObject());

            return result;
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Error processing profile data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", USER_AGENT)
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
            GSON.toJson(data, writer);
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Failed to save prices: " + e.getMessage());
        }
    }

    private static void loadPrices() {
        if (!PRICES_FILE.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(PRICES_FILE)) {
            PricesData data = GSON.fromJson(reader, PricesData.class);
            if (data != null && data.prices != null) {
                long age = System.currentTimeMillis() - data.timestamp;
                if (age < PRICE_CACHE_DURATION_MS) {
                    priceCache = data.prices;
                    priceCacheExpiry = data.timestamp + PRICE_CACHE_DURATION_MS;
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
