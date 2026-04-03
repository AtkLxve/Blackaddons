package org.blackum.blackaddons.integration;

import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.util.MinecraftInstance;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BotIntegration {
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
            .build();

    public static void sendRngDrop(String player, String item, String rarity, String floor) {
        if (ConfigManager.data.botUrl.isEmpty())
            return;

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("item", item);
        json.addProperty("rarity", rarity);
        json.addProperty("floor", floor);
        json.addProperty("timestamp", System.currentTimeMillis() / 1000);

        sendPostRequest(Constants.BOT_API_RNG, json.toString());
    }

    public static CompletableFuture<Boolean> sendDailySync(String player) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);

        return sendPostRequest(Constants.BOT_API_DAILY, json.toString()).thenApply(res -> {
            return res != null && res.statusCode() >= 200 && res.statusCode() < 300;
        });
    }

    public static CompletableFuture<JsonObject> getProfileStats(String player, String profileName, boolean force) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = Constants.BOT_API_PROFILE + "?player=" + player;
        if (profileName != null) {
            url += "&profile=" + profileName;
        }
        if (force) {
            url += "&force=true";
        }

        return sendGetRequest(url).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse profile stats response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getRtcaStats(String player, String profileName, String floor,
            Map<String, Double> bonuses) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        if (profileName != null) {
            json.addProperty("profile", profileName);
        }
        json.addProperty("floor", floor);

        if (bonuses != null && !bonuses.isEmpty()) {
            JsonObject bonusJson = new JsonObject();
            for (Map.Entry<String, Double> entry : bonuses.entrySet()) {
                bonusJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("bonuses", bonusJson);
        }

        return sendPostRequest(Constants.BOT_API_RTCA, json.toString()).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RTCA stats response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getRngData(String player) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        return sendGetRequest(Constants.BOT_API_RNG + "?player=" + player).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RNG data response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<Integer> updateRngDrop(String player, String category, String item, String action,
            Integer count) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("action", action);
        json.addProperty("category", category);
        json.addProperty("item", item);
        if (count != null) {
            json.addProperty("count", count);
        }

        return sendPostRequest(Constants.BOT_API_RNG, json.toString()).thenApply(res -> {
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
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String endpoint = String.format(Constants.BOT_API_LEADERBOARD + "?period=%s&metric=%s&page=%d", period, metric,
                page);
        return sendGetRequest(endpoint).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse leaderboard stats response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getLeaderboardWithPlayer(String period, String metric, String player) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String endpoint = String.format(Constants.BOT_API_LEADERBOARD + "?period=%s&metric=%s&find_player=%s", period,
                metric, player);
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

    public static CompletableFuture<JsonObject> createParty(String floor, String note, JsonObject reqs, int maxSize) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", MinecraftInstance.mc.getUser().getName());
        json.addProperty("floor", floor);
        json.addProperty("note", note);
        json.add("reqs", reqs);
        json.addProperty("max_size", maxSize);

        return sendPostRequest(Constants.BOT_API_PARTY_CREATE, json.toString()).thenApply(res -> {
            if (res != null && (res.statusCode() == 200 || res.statusCode() == 400)) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<Boolean> unqueueParty() {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", MinecraftInstance.mc.getUser().getName());

        return sendPostRequest(Constants.BOT_API_PARTY_UNQUEUE, json.toString()).thenApply(res -> {
            return res != null && res.statusCode() == 200;
        });
    }

    public static CompletableFuture<Boolean> updateParty(int memberCount, String note) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", MinecraftInstance.mc.getUser().getName());
        json.addProperty("member_count", memberCount);
        if (note != null) {
            json.addProperty("note", note);
        }

        return sendPostRequest(Constants.BOT_API_PARTY_UPDATE, json.toString()).thenApply(res -> {
            return res != null && res.statusCode() == 200;
        });
    }

    public static CompletableFuture<JsonObject> getParties(String floor) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = Constants.BOT_API_PARTY_LIST + (floor != null ? "?floor=" + floor : "");
        return sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getSoloLeaderboard(String floor) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = Constants.BOT_API_SOLO_LEADERBOARD + "?floor=" + floor;
        return sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse leaderboard response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> sendSoloClear(String player, String floor, String time,
            int secrets, List<String> puzzles, boolean prince, boolean mimic, boolean needsVerification) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("floor", floor);
        json.addProperty("time", time);
        json.addProperty("secrets", secrets);
        json.addProperty("prince", prince);
        json.addProperty("mimic", mimic);
        json.addProperty("needs_verification", needsVerification);

        JsonArray puzzleArray = new JsonArray();
        if (puzzles != null) {
            for (String p : puzzles) {
                puzzleArray.add(p);
            }
        }
        json.add("puzzles", puzzleArray);

        return sendPostRequest(Constants.BOT_API_SOLO_CLEAR, json.toString()).thenApply(res -> {
            if (res != null && res.statusCode() >= 200 && res.statusCode() < 300) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse solo clear response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    private static String activeServerId = null;

    public static CompletableFuture<String> getAuthKey() {
        if (activeServerId != null) {
            return CompletableFuture.completedFuture(activeServerId);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (MinecraftInstance.mc.getUser() != null) {
                    String serverId = UUID.randomUUID().toString().replace("-", "");
                    String accessToken = MinecraftInstance.mc.getUser().getAccessToken();
                    String profileId = MinecraftInstance.mc.getUser().getProfileId().toString().replace("-", "");

                    JsonObject payload = new JsonObject();
                    payload.addProperty("accessToken", accessToken);
                    payload.addProperty("selectedProfile", profileId);
                    payload.addProperty("serverId", serverId);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/join"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        activeServerId = serverId;
                        return serverId;
                    } else {
                        Blackaddons.LOGGER.warn("Mojang API join failed. Status: " + response.statusCode() + " Body: " + response.body());
                    }
                }
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed Mojang authentication: " + e.getMessage());
            }
            return null;
        });
    }

    public static CompletableFuture<Void> authenticateWithMojang() {
        return getAuthKey().thenAccept(key -> {
            if (key != null) {
                Blackaddons.LOGGER.info("Successfully authenticated with Mojang.");
            } else {
                Blackaddons.LOGGER.warn("Failed to get Mojang auth key.");
            }
        });
    }

    private static CompletableFuture<HttpResponse<String>> sendPostRequest(String endpoint, String jsonBody) {
        return sendRequest("POST", endpoint, jsonBody, true);
    }

    private static CompletableFuture<HttpResponse<String>> sendGetRequest(String endpoint) {
        return sendRequest("GET", endpoint, null, true);
    }

    private static CompletableFuture<HttpResponse<String>> sendRequest(String method, String endpoint, String jsonBody,
            boolean allowRetry) {
        return getAuthKey().thenCompose(key -> {
            String url = ConfigManager.data.botUrl + endpoint;

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", Constants.BOT_USER_AGENT);

            if (key != null) {
                builder.header(Constants.HEADER_PLAYER_KEY, key);
                try {
                    if (MinecraftInstance.mc.getUser() != null) {
                        builder.header(Constants.HEADER_PLAYER_NAME, MinecraftInstance.mc.getUser().getName());
                        builder.header(Constants.HEADER_PLAYER_UUID, MinecraftInstance.mc.getUser().getProfileId().toString());
                    }
                } catch (Exception ignored) {}
            }

            if (ConfigManager.data.developerKey != null && !ConfigManager.data.developerKey.isEmpty()) {
                builder.header(Constants.HEADER_DEVELOPER_KEY, ConfigManager.data.developerKey);
            }

            if (method.equalsIgnoreCase("POST") && jsonBody != null) {
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.GET();
            }

            return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenCompose(res -> {
                        if (res.statusCode() == 403 && allowRetry) {
                            Blackaddons.LOGGER.info("Authentication failed. Regenerating key and retrying...");
                            activeServerId = null; // force re-auth
                            return sendRequest(method, endpoint, jsonBody, false);
                        }

                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            Blackaddons.LOGGER.info("Successfully communicated with bot (" + method + "): " + endpoint);
                        } else {
                            Blackaddons.LOGGER.warn("Bot communication failed. Status: " + res.statusCode() + " Body: " + res.body());
                        }
                        return CompletableFuture.completedFuture(res);
                    })
                    .exceptionally(e -> {
                        Blackaddons.LOGGER.error("Error communicating with bot: " + e.getMessage());
                        return null;
                    });
        });
    }

}
