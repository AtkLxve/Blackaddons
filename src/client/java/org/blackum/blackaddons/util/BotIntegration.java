package org.blackum.blackaddons.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.config.ConfigManager;
import java.util.Map;

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
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse profile stats: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getRtcaStats(String player, String floor,
            Map<String, Double> bonuses) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("floor", floor);

        if (bonuses != null && !bonuses.isEmpty()) {
            JsonObject bonusJson = new JsonObject();
            for (Map.Entry<String, Double> entry : bonuses.entrySet()) {
                bonusJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("bonuses", bonusJson);
        }

        return sendPostRequest("/v1/rtca", json.toString()).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
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
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RNG data: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<Integer> updateRngDrop(String player, String category, String item, String action,
            Integer count) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("action", action);
        json.addProperty("category", category);
        json.addProperty("item", item);
        if (count != null) {
            json.addProperty("count", count);
        }

        return sendPostRequest("/v1/rng", json.toString()).thenApply(res -> {
            if (res != null && res.statusCode() >= 200 && res.statusCode() < 300) {
                try {
                    JsonObject responseJson = JsonParser.parseString(res.body()).getAsJsonObject();
                    if (responseJson.has("count")) {
                        return responseJson.get("count").getAsInt();
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RNG update response: " + e.getMessage());
                }
                return -1;
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getLeaderboard(String period, String metric, int page) {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String endpoint = String.format("/v1/leaderboard?period=%s&metric=%s&page=%d", period, metric, page);
        return sendGetRequest(endpoint).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
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
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    private static CompletableFuture<HttpResponse<String>> sendPostRequest(String endpoint, String jsonBody) {
        return sendPostRequest(endpoint, jsonBody, true);
    }

    private static CompletableFuture<HttpResponse<String>> sendPostRequest(String endpoint, String jsonBody,
            boolean allowRetry) {
        String url = ConfigManager.botUrl + endpoint;

        String playerInit = "Unknown";
        String uuidInit = "Unknown";
        try {
            if (Minecraft.getInstance().getUser() != null) {
                playerInit = Minecraft.getInstance().getUser().getName();
                uuidInit = Minecraft.getInstance().getUser().getProfileId().toString();
            }
        } catch (Exception e) {
        }
        final String player = playerInit;
        final String uuid = uuidInit;

        String rawIdentity = uuid + ":" + player + ":" + System.currentTimeMillis();
        String encryptedIdentity = EncryptionUtils.encrypt(rawIdentity);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("User-Agent", "BlackAddons/1.0");

        if (encryptedIdentity != null) {
            builder.header("X-Encrypted-Identity", encryptedIdentity);
        }

        builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (ConfigManager.developerKey != null && !ConfigManager.developerKey.isEmpty()) {
            builder.header("X-Developer-Key", ConfigManager.developerKey);
        }

        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenCompose(res -> {
                    if (res.statusCode() == 403 && allowRetry) {
                        Blackaddons.LOGGER.info("Authentication failed. Refetching key and retrying...");
                        return fetchVerificationKey().thenCompose(v -> sendPostRequest(endpoint, jsonBody, false));
                    }

                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        Blackaddons.LOGGER.info("Successfully communicated with bot: " + endpoint);
                    } else {
                        Blackaddons.LOGGER
                                .warn("Bot communication failed. Status: " + res.statusCode() + " Body: " + res.body());
                    }
                    return CompletableFuture.completedFuture(res);
                })
                .exceptionally(e -> {
                    Blackaddons.LOGGER.error("Error communicating with bot: " + e.getMessage());
                    return null;
                });
    }

    private static CompletableFuture<HttpResponse<String>> sendGetRequest(String endpoint) {
        return sendGetRequest(endpoint, true);
    }

    private static CompletableFuture<HttpResponse<String>> sendGetRequest(String endpoint, boolean allowRetry) {
        String url = ConfigManager.botUrl + endpoint;

        String playerInit = "Unknown";
        String uuidInit = "Unknown";
        try {
            if (Minecraft.getInstance().getUser() != null) {
                playerInit = Minecraft.getInstance().getUser().getName();
                uuidInit = Minecraft.getInstance().getUser().getProfileId().toString();
            }
        } catch (Exception e) {
        }
        final String player = playerInit;
        final String uuid = uuidInit;

        String rawIdentity = uuid + ":" + player + ":" + System.currentTimeMillis();
        String encryptedIdentity = EncryptionUtils.encrypt(rawIdentity);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("User-Agent", "BlackAddons/1.0");

        if (encryptedIdentity != null) {
            builder.header("X-Encrypted-Identity", encryptedIdentity);
        }

        builder.GET();

        if (ConfigManager.developerKey != null && !ConfigManager.developerKey.isEmpty()) {
            builder.header("X-Developer-Key", ConfigManager.developerKey);
        }

        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenCompose(res -> {
                    if (res.statusCode() == 403 && allowRetry) {
                        Blackaddons.LOGGER.info("Authentication failed. Refetching key and retrying...");
                        return fetchVerificationKey().thenCompose(v -> sendGetRequest(endpoint, false));
                    }

                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        Blackaddons.LOGGER.info("Successfully communicated with bot: " + endpoint);
                    } else {
                        Blackaddons.LOGGER
                                .warn("Bot communication failed. Status: " + res.statusCode() + " Body: " + res.body());
                    }
                    return CompletableFuture.completedFuture(res);
                })
                .exceptionally(e -> {
                    Blackaddons.LOGGER.error("Error communicating with bot: " + e.getMessage());
                    return null;
                });
    }

    public static CompletableFuture<Void> fetchVerificationKey() {
        if (ConfigManager.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        return sendGetRequest("/v1/key").thenAccept(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                    if (json.has("key")) {
                        String key = json.get("key").getAsString();
                        EncryptionUtils.setKeyBase64(key);
                        Blackaddons.LOGGER.info("Successfully fetched verification key from bot.");
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse verification key: " + e.getMessage());
                }
            } else {
                Blackaddons.LOGGER
                        .warn("Failed to fetch verification key. Status: " + (res != null ? res.statusCode() : "null"));
            }
        });
    }

}
