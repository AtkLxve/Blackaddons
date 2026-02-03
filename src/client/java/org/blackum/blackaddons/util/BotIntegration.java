package org.blackum.blackaddons.util;

import com.google.gson.JsonObject;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.config.ConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class BotIntegration {
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void sendRngDrop(String player, String item, String rarity, String floor) {
        if (ConfigManager.botUrl.isEmpty())
            return;

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("item", item);
        json.addProperty("rarity", rarity);
        json.addProperty("floor", floor);
        json.addProperty("timestamp", System.currentTimeMillis() / 1000);

        sendPostRequest("/v1/rng", json.toString());
    }

    public static CompletableFuture<Boolean> sendDailySync(String player) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);

        return sendPostRequest("/v1/daily", json.toString()).thenApply(res -> {
            return res != null && res.statusCode() >= 200 && res.statusCode() < 300;
        });
    }

    public static CompletableFuture<JsonObject> getProfileStats(String player, boolean force) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = "/v1/profile?player=" + player;
        if (force) {
            url += "&force=true";
        }

        return sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse profile stats: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getRtcaStats(String player, String floor,
            java.util.Map<String, Double> bonuses) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("floor", floor);

        if (bonuses != null && !bonuses.isEmpty()) {
            JsonObject bonusJson = new JsonObject();
            for (java.util.Map.Entry<String, Double> entry : bonuses.entrySet()) {
                bonusJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("bonuses", bonusJson);
        }

        return sendPostRequest("/v1/rtca", json.toString()).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RTCA stats: " + res.body());
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getRngData(String player) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        return sendGetRequest("/v1/rng?player=" + player).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RNG data: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<Boolean> updateRngDrop(String player, String category, String item, String action,
            Integer count) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("action", action);
        json.addProperty("category", category);
        json.addProperty("item", item);
        if (count != null) {
            json.addProperty("count", count);
        }

        return sendPostRequest("/v1/rng", json.toString()).thenApply(res -> {
            return res != null && res.statusCode() >= 200 && res.statusCode() < 300;
        });
    }

    public static CompletableFuture<JsonObject> getLeaderboard(String period, String metric, int page) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String endpoint = String.format("/v1/leaderboard?period=%s&metric=%s&page=%d", period, metric, page);
        return sendGetRequest(endpoint).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse leaderboard stats: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getLeaderboardWithPlayer(String period, String metric, String player) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String endpoint = String.format("/v1/leaderboard?period=%s&metric=%s&find_player=%s", period, metric, player);
        return sendGetRequest(endpoint).thenApply(res -> {
            if (res != null && (res.statusCode() == 200 || res.statusCode() == 404)) {
                try {
                    return com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    private static CompletableFuture<HttpResponse<String>> sendPostRequest(String endpoint, String jsonBody) {
        String url = ConfigManager.botUrl + endpoint;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("User-Agent", "BlackAddons/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return sendRequest(request, endpoint);
    }

    private static CompletableFuture<HttpResponse<String>> sendGetRequest(String endpoint) {
        String url = ConfigManager.botUrl + endpoint;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("User-Agent", "BlackAddons/1.0")
                .GET()
                .build();

        return sendRequest(request, endpoint);
    }

    private static CompletableFuture<HttpResponse<String>> sendRequest(HttpRequest request, String endpoint) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        Blackaddons.LOGGER.info("Successfully communicated with bot: " + endpoint);
                    } else {
                        Blackaddons.LOGGER.warn(
                                "Bot communication failed. Status: " + res.statusCode() + " Body: " + res.body());
                    }
                    return res;
                })
                .exceptionally(e -> {
                    Blackaddons.LOGGER.error("Error communicating with bot: " + e.getMessage());
                    return null;
                });
    }
}
