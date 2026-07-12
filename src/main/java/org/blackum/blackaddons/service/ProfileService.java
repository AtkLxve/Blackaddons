package org.blackum.blackaddons.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.feature.profile.LocalTeammateManager;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.util.io.HttpUtils;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.io.JsonUtils;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileService {
    private static String skyCryptBuildId = null;
    private static long buildIdExpiry = 0;
    private static String skyCryptRjsonHeader = "__skrao";
    private static long rjsonHeaderExpiry = 0;
    private static final Pattern BUILD_ID_PATTERN = Pattern.compile("([a-z0-9]+)/get[A-Za-z]+");
    private static final Pattern STATS_BUILD_ID_PATTERN = Pattern.compile("([a-z0-9]+)/get(?:ProfileStats|Combined|Inventories)");
    private static final Pattern RJSON_HEADER_PATTERN = Pattern.compile("\"(__[a-z0-9_]{4,10})\"");
    private static final long PRIMARY_SOURCE_FAILURE_NOTIFICATION_COOLDOWN_MS = 60_000L;
    private static long lastPrimarySourceFailureNotificationAt = 0L;

    public static CompletableFuture<JsonObject> getProfileStats(String player, String profileName, boolean force) {
        return getUuid(player)
                .thenCompose(uuid -> {
                    if (uuid == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return getProfileData(uuid, profileName).thenApply(profileData -> {
                        if (profileData == null)
                            return null;
                        return processProfileData(uuid, profileData, profileName);
                    });
                });
    }

    private static CompletableFuture<String> getUuid(String name) {
        String url = Constants.PLAYER_DB_API + name;
        return HttpUtils.sendGetRequest(url).thenApply(res -> {
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

    private static CompletableFuture<JsonObject> getProfileData(String uuid, String profileName) {
        CompletableFuture<JsonObject> future = CompletableFuture.completedFuture(null);
        if (ConfigManager.data.apiPriorityList.isEmpty()) {
            return future;
        }

        ConfigManager.ApiPriority primaryPriority = ConfigManager.data.apiPriorityList.get(0);
        AtomicBoolean primarySourceFailureNotified = new AtomicBoolean(false);

        for (ConfigManager.ApiPriority priority : ConfigManager.data.apiPriorityList) {
            future = future.thenCompose(data -> {
                if (data != null)
                    return CompletableFuture.completedFuture(data);
                return fetchProfileData(priority, uuid, profileName).thenApply(result -> {
                    if (result == null && priority == primaryPriority
                            && primarySourceFailureNotified.compareAndSet(false, true)) {
                        notifyPrimarySourceFailure(primaryPriority);
                    }
                    return result;
                });
            });
        }

        return future;
    }

    private static CompletableFuture<JsonObject> fetchProfileData(ConfigManager.ApiPriority priority, String uuid,
            String profileName) {
        return switch (priority) {
            case PLAIN_DAWN -> fetchStandardProfile(Constants.PLAIN_DAWN_PROFILE_API + uuid, "PlainDawn");
            case ADJECTILS -> fetchStandardProfile(Constants.ADJECTILS_PROFILE_API + uuid, "Adjectils",
                    "X-Timestamp", String.valueOf(System.currentTimeMillis()));
            case SOOPY -> fetchSoopyProfile(uuid);
            case SKYCRYPT -> fetchSkyCryptShiiyuProfile(uuid, profileName);
        };
    }

    private static void notifyPrimarySourceFailure(ConfigManager.ApiPriority priority) {
        long now = System.currentTimeMillis();
        synchronized (ProfileService.class) {
            if (now - lastPrimarySourceFailureNotificationAt < PRIMARY_SOURCE_FAILURE_NOTIFICATION_COOLDOWN_MS) {
                return;
            }
            lastPrimarySourceFailureNotificationAt = now;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> NotificationManager.addNotification(
                "Primary Source Failed",
                formatApiName(priority) + " failed. Change Primary Source in /b -> Settings tab.",
                NotificationType.WARNING));
    }

    private static String formatApiName(ConfigManager.ApiPriority priority) {
        return switch (priority) {
            case PLAIN_DAWN -> "PlainDawn";
            case ADJECTILS -> "Adjectils";
            case SKYCRYPT -> "SkyCrypt";
            case SOOPY -> "Soopy";
        };
    }

    private static CompletableFuture<JsonObject> fetchStandardProfile(String url, String sourceName, String... headers) {
        return HttpUtils.sendGetRequest(url, headers).thenApply(res -> {
            if (res == null || res.statusCode() != 200)
                return null;
            try {
                JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                if (!json.has("profiles") || !json.get("profiles").isJsonArray())
                    return null;
                normalizePetsData(json);
                return json;
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to parse " + sourceName + " profile data: " + e.getMessage());
                return null;
            }
        });
    }

    private static void normalizePetsData(JsonObject json) {
        for (JsonElement pEl : json.getAsJsonArray("profiles")) {
            if (!pEl.isJsonObject()) continue;
            JsonObject p = pEl.getAsJsonObject();
            if (p.has("members") && p.get("members").isJsonObject()) {
                JsonObject members = p.getAsJsonObject("members");
                for (String mUuid : members.keySet()) {
                    JsonElement mEl = members.get(mUuid);
                    if (mEl == null || !mEl.isJsonObject()) continue;
                    JsonObject m = mEl.getAsJsonObject();
                    if (m.has("pets_data") && m.get("pets_data").isJsonObject()) {
                        JsonObject petsData = m.getAsJsonObject("pets_data");
                        if (petsData.has("pets") && !m.has("pets")) {
                            m.add("pets", petsData.get("pets"));
                        }
                    }
                }
            }
            if (p.has("pets_data") && p.get("pets_data").isJsonObject()) {
                JsonObject petsData = p.getAsJsonObject("pets_data");
                if (petsData.has("pets") && p.has("members") && p.get("members").isJsonObject()) {
                    JsonObject members = p.getAsJsonObject("members");
                    for (String mUuid : members.keySet()) {
                        JsonElement mEl = members.get(mUuid);
                        if (mEl == null || !mEl.isJsonObject()) continue;
                        JsonObject m = mEl.getAsJsonObject();
                        if (!m.has("pets")) m.add("pets", petsData.get("pets"));
                    }
                }
            }
        }
    }

    private static CompletableFuture<JsonObject> fetchSkyCryptShiiyuProfile(String uuid, String profileName) {
        return getSkyCryptBuildId().thenCompose(buildId -> {
            if (buildId == null) {
                Blackaddons.LOGGER.warn("SkyCrypt Fetch: Failed to get buildId from sky.shiiyu.moe");
                return CompletableFuture.completedFuture(null);
            }
            return fetchSkyCryptRemote("getProfileStats", buildId, uuid, "").thenCompose(statsEl -> {
                if (statsEl == null || !statsEl.isJsonObject()) {
                    Blackaddons.LOGGER
                            .warn("SkyCrypt Fetch: Stats API returned no profiles or invalid format for " + uuid);
                    return CompletableFuture.completedFuture(null);
                }
                JsonObject stats = statsEl.getAsJsonObject();
                if (!stats.has("profiles") || !stats.get("profiles").isJsonArray()) {
                    Blackaddons.LOGGER
                            .warn("SkyCrypt Fetch: Stats API returned no profiles or invalid format for " + uuid);
                    return CompletableFuture.completedFuture(null);
                }

                String name = stats.has("username") ? stats.get("username").getAsString() : uuid;
                JsonArray profilesArr = stats.getAsJsonArray("profiles");
                JsonObject selectedProfile = null;

                if (profileName != null) {
                    for (JsonElement el : profilesArr) {
                        if (el.isJsonObject()) {
                            JsonObject p = el.getAsJsonObject();
                            if (p.has("cute_name")
                                    && p.get("cute_name").getAsString().equalsIgnoreCase(profileName)) {
                                selectedProfile = p;
                                break;
                            }
                        }
                    }
                }

                if (selectedProfile == null) {
                    for (JsonElement el : profilesArr) {
                        if (el.isJsonObject()) {
                            JsonObject p = el.getAsJsonObject();
                            if (p.has("selected") && !p.get("selected").isJsonNull()
                                    && p.get("selected").getAsBoolean()) {
                                selectedProfile = p;
                                break;
                            }
                        }
                    }
                }

                if (selectedProfile == null && !profilesArr.isEmpty()) {
                    selectedProfile = profilesArr.get(0).getAsJsonObject();
                    Blackaddons.LOGGER.info("SkyCrypt Fetch: No 'selected' profile found, using the first one");
                }
                if (selectedProfile == null) {
                    Blackaddons.LOGGER.warn("SkyCrypt Fetch: Profiles array is empty for " + name);
                    return CompletableFuture.completedFuture(null);
                }

                String finalSelectedId = selectedProfile.get("profile_id").getAsString();
                CompletableFuture<JsonElement> combinedFuture = fetchSkyCryptRemote("getCombined", buildId, uuid, finalSelectedId);
                CompletableFuture<JsonElement> inventoriesFuture = fetchSkyCryptRemote("getInventories", buildId, uuid, finalSelectedId);
                return combinedFuture.thenCombine(inventoriesFuture, (combined, inventories) -> {
                    if (combined == null) {
                        Blackaddons.LOGGER.warn("SkyCrypt Fetch: Failed to get combined data for " + name);
                        return null;
                    }
                    Blackaddons.LOGGER.info(
                            "SkyCrypt Fetch: Successfully scraped data and inventories for " + name + " (" + finalSelectedId
                                    + ")");
                    JsonObject normalized = normalizeSkyCryptScrapedData(stats, finalSelectedId, combined, uuid);
                    if (normalized != null) {
                        if (inventories != null && inventories.isJsonArray()) {
                            mergeSkyCryptInventories(normalized, inventories.getAsJsonArray(), uuid);
                        }
                    }
                    return normalized;
                });
            });
        });
    }

    private static CompletableFuture<JsonElement> fetchSkyCryptRemote(String endpoint, String buildId, String uuid,
            String profileId) {
        try {
            String uuidNoDashes = uuid.replace("-", "");
            JsonArray payloadArr = new JsonArray();
            JsonArray header = new JsonArray();
            header.add(skyCryptRjsonHeader);
            header.add(1);
            payloadArr.add(header);

            JsonObject mapping = new JsonObject();
            mapping.addProperty("profileId", 2);
            mapping.addProperty("uuid", 3);
            payloadArr.add(mapping);
            payloadArr.add(profileId);
            payloadArr.add(uuidNoDashes);

            String payloadJson = payloadArr.toString();
            String payloadB64 = Base64.getEncoder().encodeToString(payloadJson.getBytes());
            String url = Constants.SKYCRYPT_BASE_URL + "/_app/remote/" + buildId + "/" + endpoint + "?payload="
                    + payloadB64;

            return HttpUtils.sendGetRequest(url).thenApply(res -> {
                if (res != null && res.statusCode() == 200) {
                    try {
                        JsonObject envelope = JsonParser.parseString(res.body()).getAsJsonObject();
                        if (envelope.has("type") && envelope.get("type").getAsString().equals("result")) {
                            String resultStr = envelope.get("result").getAsString();
                            JsonElement raw = JsonParser.parseString(resultStr);
                            if (raw.isJsonArray()) {
                                JsonArray rawArr = raw.getAsJsonArray();
                                if (rawArr.size() > 0) {
                                    return resolveRjson(rawArr.get(0), rawArr, new HashMap<>());
                                }
                            }
                            return raw;
                        }
                    } catch (Exception e) {
                        Blackaddons.LOGGER.error("SkyCrypt Remote (" + endpoint + "): Failed to parse result: " + e.getMessage());
                    }
                }
                return null;
            });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static CompletableFuture<String> getSkyCryptBuildId() {
        if (skyCryptBuildId != null && buildIdExpiry > System.currentTimeMillis() &&
            skyCryptRjsonHeader != null && rjsonHeaderExpiry > System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(skyCryptBuildId);
        }

        return HttpUtils.sendGetRequest(Constants.SKYCRYPT_BASE_URL + "/stats/BLACKUM").thenCompose(res -> {
            if (res == null || res.statusCode() != 200) {
                Blackaddons.LOGGER.error("SkyCrypt Discovery: Failed to fetch profile page");
                return CompletableFuture.completedFuture(null);
            }

            String html = res.body();
            Matcher htmlIdMatcher = STATS_BUILD_ID_PATTERN.matcher(html);
            if (htmlIdMatcher.find()) {
                skyCryptBuildId = htmlIdMatcher.group(1);
                buildIdExpiry = System.currentTimeMillis() + 3600000;
                if (skyCryptRjsonHeader != null) {
                    Blackaddons.LOGGER.info("SkyCrypt Discovery: Found BuildID=" + skyCryptBuildId + ", Header=" + skyCryptRjsonHeader);
                    return CompletableFuture.completedFuture(skyCryptBuildId);
                }
            }
            Pattern appJsPattern = Pattern.compile("/_app/immutable/entry/app\\.([a-zA-Z0-9_-]+)\\.js");
            Matcher appMatcher = appJsPattern.matcher(html);
            if (!appMatcher.find()) {
                Blackaddons.LOGGER.error("SkyCrypt Discovery: Failed to find app entry script in HTML");
                return CompletableFuture.completedFuture(null);
            }

            String appJsUrl = Constants.SKYCRYPT_BASE_URL + appMatcher.group();
            return HttpUtils.sendGetRequest(appJsUrl).thenCompose(appRes -> {
                if (appRes == null || appRes.statusCode() != 200) {
                    Blackaddons.LOGGER.error("SkyCrypt Discovery: Failed to fetch app entry script");
                    return CompletableFuture.completedFuture(null);
                }

                String appJs = appRes.body();
                Pattern node4Pattern = Pattern.compile("nodes/4\\.([a-zA-Z0-9_-]+)\\.js");
                Matcher nodeMatcher = node4Pattern.matcher(appJs);
                if (!nodeMatcher.find()) {
                    Blackaddons.LOGGER.error("SkyCrypt Discovery: Failed to find node 4 script in app entry");
                    return CompletableFuture.completedFuture(null);
                }

                String nodeJsUrl = Constants.SKYCRYPT_BASE_URL + "/_app/immutable/" + nodeMatcher.group();
                return HttpUtils.sendGetRequest(nodeJsUrl).thenCompose(nodeRes -> {
                    if (nodeRes == null || nodeRes.statusCode() != 200) {
                        Blackaddons.LOGGER.error("SkyCrypt Discovery: Failed to fetch node 4 script");
                        return CompletableFuture.completedFuture(null);
                    }

                    String nodeJs = nodeRes.body();
                    Pattern chunkPattern = Pattern.compile("chunks/([a-zA-Z0-9_-]+)\\.js");
                    Matcher chunkMatcher = chunkPattern.matcher(nodeJs);

                    CompletableFuture<String> discoveryFuture = CompletableFuture.completedFuture(null);
                    while (chunkMatcher.find()) {
                        String chunkUrl = Constants.SKYCRYPT_BASE_URL + "/_app/immutable/" + chunkMatcher.group();
                        discoveryFuture = discoveryFuture.thenCompose(found -> {
                            if (skyCryptBuildId != null && skyCryptRjsonHeader != null)
                                return CompletableFuture.completedFuture(skyCryptBuildId);

                            return HttpUtils.sendGetRequest(chunkUrl).thenApply(chunkRes -> {
                                if (chunkRes != null && chunkRes.statusCode() == 200) {
                                    String body = chunkRes.body();

                                    if (skyCryptBuildId == null) {
                                        Matcher idMatcher = BUILD_ID_PATTERN.matcher(body);
                                        if (idMatcher.find()) {
                                            skyCryptBuildId = idMatcher.group(1);
                                            buildIdExpiry = System.currentTimeMillis() + 3600000;
                                        }
                                    }

                                    if (skyCryptRjsonHeader == null || skyCryptRjsonHeader.equals("__skrao")) {
                                        Matcher headerMatcher = RJSON_HEADER_PATTERN.matcher(body);
                                        List<String> candidates = new ArrayList<>();
                                        while (headerMatcher.find()) {
                                            candidates.add(headerMatcher.group(1));
                                        }
                                        for (int i = 0; i <= candidates.size() - 4; i++) {
                                            if (candidates.get(i).startsWith("__") &&
                                                candidates.get(i + 1).startsWith("__") &&
                                                candidates.get(i + 2).startsWith("__") &&
                                                candidates.get(i + 3).startsWith("__")) {
                                                skyCryptRjsonHeader = candidates.get(i);
                                                rjsonHeaderExpiry = System.currentTimeMillis() + 3600000;
                                                break;
                                            }
                                        }
                                    }
                                }
                                return skyCryptBuildId;
                            });
                        });
                    }

                    return discoveryFuture.thenApply(foundId -> {
                        if (skyCryptBuildId != null) {
                            Blackaddons.LOGGER.info("SkyCrypt Discovery: Found BuildID=" + skyCryptBuildId + ", Header=" + skyCryptRjsonHeader);
                            return skyCryptBuildId;
                        }
                        Blackaddons.LOGGER.error("SkyCrypt Discovery: Failed to find required identifiers in any imported chunks");
                        return null;
                    });
                });
            });
        });
    }

    private static void mergeSkyCryptInventories(JsonObject normalized, JsonArray inventories, String uuid) {
        try {
            if (normalized.has("profiles")) {
                JsonArray profiles = normalized.getAsJsonArray("profiles");
                for (JsonElement p : profiles) {
                    JsonObject prof = p.getAsJsonObject();
                    if (prof.has("members")) {
                        JsonObject members = prof.getAsJsonObject("members");
                        if (members.has(uuid)) {
                            JsonObject member = members.getAsJsonObject(uuid);
                            if (!member.has("inventory")) {
                                member.add("inventory", new JsonObject());
                            }
                            JsonObject inv = member.getAsJsonObject("inventory");
                            for (JsonElement containerEl : inventories) {
                                if (containerEl.isJsonObject()) {
                                    JsonObject container = containerEl.getAsJsonObject();
                                    if (container.has("name") && container.has("items")) {
                                        String rawName = container.get("name").getAsString();
                                        String lowerRaw = rawName.toLowerCase();
                                        boolean isBackpack = lowerRaw.contains("backpack");
                                        boolean isEnderChest = lowerRaw.contains("ender chest");

                                        String mappedName;
                                        if (isEnderChest) {
                                            mappedName = "ender_chest";
                                        } else if (isBackpack) {
                                            mappedName = "backpack";
                                        } else {
                                            mappedName = switch (rawName) {
                                                case "Inventory" -> "inv";
                                                case "Armor" -> "inv_armor";
                                                case "Equipment" -> "equipment";
                                                case "Wardrobe" -> "wardrobe";
                                                case "Personal Vault" -> "personal_vault";
                                                default -> lowerRaw.replace(" ", "_");
                                            };
                                        }

                                        JsonArray itemsSlice;
                                        if (container.has("items") && container.get("items").isJsonArray()) {
                                            itemsSlice = container.getAsJsonArray("items");
                                        } else if (container.has("containsItems") && container.get("containsItems").isJsonArray()) {
                                            itemsSlice = container.getAsJsonArray("containsItems");
                                        } else {
                                            itemsSlice = new JsonArray();
                                        }

                                        if (isBackpack && itemsSlice.size() > 0) {
                                            JsonObject first = itemsSlice.get(0).getAsJsonObject();
                                            String firstId = first.has("id") ? first.get("id").getAsString() : "";
                                            String firstDisplayName = first.has("display_name") ? first.get("display_name").getAsString() : "";
                                            if (firstId.toLowerCase().contains("backpack") || firstDisplayName.toLowerCase().contains("backpack") || firstDisplayName.equalsIgnoreCase(rawName)) {
                                                JsonArray contents = new JsonArray();
                                                for (int i = 1; i < itemsSlice.size(); i++) {
                                                    contents.add(itemsSlice.get(i));
                                                }
                                                itemsSlice = contents;
                                            }
                                        }

                                        JsonObject wrapper = new JsonObject();
                                        wrapper.add("skycrypt_items", itemsSlice);

                                        if (isBackpack) {
                                            if (!inv.has("backpack_contents")) {
                                                inv.add("backpack_contents", new JsonObject());
                                            }
                                            JsonObject bpContents = inv.getAsJsonObject("backpack_contents");
                                            int idx = 0;
                                            while (bpContents.has("backpack_" + idx)) idx++;
                                            bpContents.add("backpack_" + idx, wrapper);
                                        } else {
                                            String finalKey = mappedName.equals("inv_armor") ? "inv_armor" : mappedName + "_contents";
                                            if (inv.has(finalKey)) {
                                                JsonObject existing = inv.getAsJsonObject(finalKey);
                                                if (existing.has("skycrypt_items") && wrapper.has("skycrypt_items")) {
                                                    existing.getAsJsonArray("skycrypt_items").addAll(wrapper.getAsJsonArray("skycrypt_items"));
                                                }
                                            } else {
                                                inv.add(finalKey, wrapper);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Blackaddons.LOGGER.error("SkyCrypt Inventories: Error merging data: " + e.getMessage());
        }
    }



    private static JsonObject normalizeSkyCryptScrapedData(JsonObject stats, String profileId, JsonElement combinedEl,
            String uuid) {
        try {
            if (combinedEl == null || !combinedEl.isJsonObject()) return null;
            JsonObject combined = combinedEl.getAsJsonObject();

            JsonObject result = new JsonObject();
            JsonArray profilesArr = new JsonArray();
            JsonArray profilesRaw = stats.getAsJsonArray("profiles");
            JsonObject profileInfo = null;
            for (JsonElement el : profilesRaw) {
                if (el.isJsonObject() && el.getAsJsonObject().get("profile_id").getAsString().equals(profileId)) {
                    profileInfo = el.getAsJsonObject();
                    break;
                }
            }

            if (profileInfo == null)
                return null;

            JsonObject profile = new JsonObject();
            profile.addProperty("profile_id", profileId);
            profile.addProperty("cute_name",
                    profileInfo.has("cute_name") ? profileInfo.get("cute_name").getAsString() : "Unknown");
            profile.addProperty("selected", true);

            JsonObject members = new JsonObject();
            JsonObject memberData = new JsonObject();

            if (combined.has("dungeons") && combined.get("dungeons").isJsonObject()) {
                JsonObject dungeons = combined.getAsJsonObject("dungeons");
                JsonObject normalizedDungeons = new JsonObject();
                JsonObject catacombs = new JsonObject();
                double cataXp = 0.0;
                if (dungeons.has("level") && dungeons.get("level").isJsonObject()) {
                    JsonObject levelObj = dungeons.getAsJsonObject("level");
                    cataXp = levelObj.has("xp") && !levelObj.get("xp").isJsonNull()
                            ? levelObj.get("xp").getAsDouble()
                            : 0.0;
                }
                catacombs.addProperty("experience", cataXp);

                JsonObject masterCatacombs = new JsonObject();
                masterCatacombs.addProperty("experience", 0.0);

                JsonObject tierCompletions = new JsonObject();
                JsonObject masterTierCompletions = new JsonObject();
                JsonObject bestScore = new JsonObject();
                JsonObject masterBestScore = new JsonObject();
                JsonObject fastestS = new JsonObject();
                JsonObject masterFastestS = new JsonObject();
                JsonObject fastestSPlus = new JsonObject();
                JsonObject masterFastestSPlus = new JsonObject();

                if (dungeons.has("catacombs") && dungeons.get("catacombs").isJsonArray()) {
                    for (JsonElement floorEl : dungeons.getAsJsonArray("catacombs")) {
                        if (!floorEl.isJsonObject())
                            continue;
                        JsonObject floor = floorEl.getAsJsonObject();
                        String name = floor.has("name") ? floor.get("name").getAsString() : "";
                        if (name.isEmpty())
                            continue;

                        String tier = name.equalsIgnoreCase("Entrance") ? "0" : name.replace("Floor ", "");
                        JsonObject fStats = floor.has("stats") ? floor.getAsJsonObject("stats") : new JsonObject();

                        tierCompletions.addProperty(tier, JsonUtils.getInt(fStats, "tier_completions"));
                        bestScore.addProperty(tier, JsonUtils.getInt(fStats, "best_score"));
                        fastestS.addProperty(tier, JsonUtils.getInt(fStats, "fastest_time_s"));
                        fastestSPlus.addProperty(tier, JsonUtils.getInt(fStats, "fastest_time_s_plus"));
                    }
                }

                if (dungeons.has("master_catacombs") && dungeons.get("master_catacombs").isJsonArray()) {
                    for (JsonElement floorEl : dungeons.getAsJsonArray("master_catacombs")) {
                        if (!floorEl.isJsonObject())
                            continue;
                        JsonObject floor = floorEl.getAsJsonObject();
                        String name = floor.has("name") ? floor.get("name").getAsString() : "";
                        if (name.isEmpty())
                            continue;

                        String tier = name.equalsIgnoreCase("Entrance") ? "0" : name.replace("Floor ", "");
                        JsonObject fStats = floor.has("stats") ? floor.getAsJsonObject("stats") : new JsonObject();

                        masterTierCompletions.addProperty(tier, JsonUtils.getInt(fStats, "tier_completions"));
                        masterBestScore.addProperty(tier, JsonUtils.getInt(fStats, "best_score"));
                        masterFastestS.addProperty(tier, JsonUtils.getInt(fStats, "fastest_time_s"));
                        masterFastestSPlus.addProperty(tier, JsonUtils.getInt(fStats, "fastest_time_s_plus"));
                    }
                }

                catacombs.add("tier_completions", tierCompletions);
                catacombs.add("best_score", bestScore);
                catacombs.add("fastest_time_s", fastestS);
                catacombs.add("fastest_time_s_plus", fastestSPlus);

                masterCatacombs.add("tier_completions", masterTierCompletions);
                masterCatacombs.add("best_score", masterBestScore);
                masterCatacombs.add("fastest_time_s", masterFastestS);
                masterCatacombs.add("fastest_time_s_plus", masterFastestSPlus);

                JsonObject dungeonTypes = new JsonObject();
                dungeonTypes.add("catacombs", catacombs);
                dungeonTypes.add("master_catacombs", masterCatacombs);
                normalizedDungeons.add("dungeon_types", dungeonTypes);

                if (dungeons.has("classes") && dungeons.get("classes").isJsonObject()) {
                    JsonObject classesRoot = dungeons.getAsJsonObject("classes");
                    if (classesRoot.has("classes") && classesRoot.get("classes").isJsonObject()) {
                        JsonObject rawClasses = classesRoot.getAsJsonObject("classes");
                        JsonObject normalizedClasses = new JsonObject();
                        String[] dungeonClasses = { "archer", "berserk", "healer", "mage", "tank" };
                        for (String cls : dungeonClasses) {
                            if (rawClasses.has(cls)) {
                                JsonElement clsEl = rawClasses.get(cls);
                                if (clsEl.isJsonObject()) {
                                    JsonObject clsObj = clsEl.getAsJsonObject();
                                    JsonObject normCls = new JsonObject();
                                    normCls.addProperty("experience", JsonUtils.getDouble(clsObj, "xp"));
                                    normalizedClasses.add(cls, normCls);
                                }
                            }
                        }
                        normalizedDungeons.add("player_classes", normalizedClasses);
                    }
                }

                int watcherSummonUndeadKills = 0;
                if (combined.has("misc") && combined.getAsJsonObject("misc").has("kills")) {
                    JsonObject miscKillsRoot = combined.getAsJsonObject("misc").getAsJsonObject("kills");
                    if (miscKillsRoot.has("kills") && miscKillsRoot.get("kills").isJsonArray()) {
                        JsonArray killsArr = miscKillsRoot.getAsJsonArray("kills");
                        for (JsonElement el : killsArr) {
                            if (el.isJsonObject()) {
                                JsonObject killEntry = el.getAsJsonObject();
                                if (killEntry.has("name") && killEntry.get("name").getAsString().equalsIgnoreCase("Watcher Summon Undead")) {
                                    watcherSummonUndeadKills = JsonUtils.getInt(killEntry, "amount");
                                    break;
                                }
                            }
                        }
                    }
                }

                if (dungeons.has("stats") && dungeons.get("stats").isJsonObject()) {
                    JsonObject statsObj = dungeons.getAsJsonObject("stats");
                    if (statsObj.has("secrets") && statsObj.get("secrets").isJsonObject()) {
                        JsonObject secretsObj = statsObj.getAsJsonObject("secrets");
                        normalizedDungeons.addProperty("secrets", JsonUtils.getInt(secretsObj, "found"));
                    }
                    if (watcherSummonUndeadKills == 0 && statsObj.has("bloodMobKills") && !statsObj.get("bloodMobKills").isJsonNull()) {
                        watcherSummonUndeadKills = JsonUtils.getInt(statsObj, "bloodMobKills");
                    }
                }

                if (watcherSummonUndeadKills > 0) {
                    if (!memberData.has("player_stats")) {
                        memberData.add("player_stats", new JsonObject());
                    }
                    JsonObject playerStats = memberData.getAsJsonObject("player_stats");
                    JsonObject kills = playerStats.has("kills") ? playerStats.getAsJsonObject("kills")
                            : new JsonObject();
                    kills.addProperty("watcher_summon_undead", watcherSummonUndeadKills);
                    if (!playerStats.has("kills")) {
                        playerStats.add("kills", kills);
                    }
                }

                memberData.add("dungeons", normalizedDungeons);
            }

            if (combined.has("accessories") && combined.get("accessories").isJsonObject()) {
                JsonObject acc = combined.getAsJsonObject("accessories");
                if (acc.has("magicalPower") && acc.get("magicalPower").isJsonObject()) {
                    JsonObject mpObj = acc.getAsJsonObject("magicalPower");
                    if (mpObj.has("total")) {
                        JsonObject storage = new JsonObject();
                        storage.addProperty("highest_magical_power", mpObj.get("total").getAsInt());
                        memberData.add("accessory_bag_storage", storage);
                    }
                }
            }

            if (combined.has("pets") && combined.get("pets").isJsonObject()) {
                JsonObject petsObj = combined.getAsJsonObject("pets");
                if (petsObj.has("pets")) {
                    memberData.add("pets", petsObj.get("pets"));
                }
            } else if (combined.has("pets") && combined.get("pets").isJsonArray()) {
                memberData.add("pets", combined.get("pets"));
            }

            if (combined.has("gear") && combined.get("gear").isJsonObject()) {
                JsonObject gear = combined.getAsJsonObject("gear");
                if (!memberData.has("inventory")) {
                    memberData.add("inventory", new JsonObject());
                }
                JsonObject inv = memberData.getAsJsonObject("inventory");

                if (gear.has("wardrobe") && gear.get("wardrobe").isJsonArray()) {
                    JsonArray rawWardrobe = gear.getAsJsonArray("wardrobe");
                    JsonArray flatWardrobe = new JsonArray();
                    for (JsonElement setEl : rawWardrobe) {
                        if (setEl.isJsonArray()) {
                            for (JsonElement itemEl : setEl.getAsJsonArray()) {
                                flatWardrobe.add(itemEl);
                            }
                        } else {
                            flatWardrobe.add(setEl);
                        }
                    }
                    JsonObject wardrobe = new JsonObject();
                    wardrobe.add("skycrypt_items", flatWardrobe);
                    inv.add("wardrobe_contents", wardrobe);
                }
                if (gear.has("armor") && gear.getAsJsonObject("armor").has("armor")) {
                    JsonObject armor = new JsonObject();
                    armor.add("skycrypt_items", gear.getAsJsonObject("armor").get("armor"));
                    inv.add("inv_armor", armor);
                }
                if (gear.has("equipment") && gear.getAsJsonObject("equipment").has("equipment")) {
                    JsonObject equipment = new JsonObject();
                    equipment.add("skycrypt_items", gear.getAsJsonObject("equipment").get("equipment"));
                    inv.add("equipment_contents", equipment);
                }
            }

            members.add(uuid, memberData);
            profile.add("members", members);
            profilesArr.add(profile);

            result.add("profiles", profilesArr);
            return result;
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Error normalizing SkyCrypt scraped data: " + e.getMessage());
            return null;
        }
    }

    private static CompletableFuture<JsonObject> fetchSoopyProfile(String uuid) {
        String url = Constants.SOOPY_PROFILE_API + uuid;
        return HttpUtils.sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                    if (json.has("success") && json.get("success").getAsBoolean() && json.has("data")
                            && !json.get("data").isJsonNull()) {
                        return normalizeSoopyData(json.getAsJsonObject("data"), uuid);
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse Soopy profile data: " + e.getMessage());
                }
            }
            return null;
        });
    }

    private static JsonObject normalizeSoopyData(JsonObject data, String uuid) {
        try {
            if (data == null || data.isJsonNull()) {
                Blackaddons.LOGGER.error("Soopy Normalization: Input data is null");
                return null;
            }
            JsonObject result = new JsonObject();
            JsonArray profilesArr = new JsonArray();

            if (!data.has("profiles") || data.get("profiles").isJsonNull()) {
                Blackaddons.LOGGER.error("Soopy Normalization: Missing 'profiles' field");
                return null;
            }
            JsonObject profilesRaw = data.getAsJsonObject("profiles");

            String currentProfileId = "";
            if (data.has("stats") && !data.get("stats").isJsonNull()) {
                JsonObject statsObj = data.getAsJsonObject("stats");
                if (statsObj.has("currentProfileId") && !statsObj.get("currentProfileId").isJsonNull()) {
                    currentProfileId = statsObj.get("currentProfileId").getAsString();
                }
            }

            for (String profileId : profilesRaw.keySet()) {
                JsonElement profEl = profilesRaw.get(profileId);
                if (profEl == null || profEl.isJsonNull())
                    continue;
                JsonObject profileInfo = profEl.getAsJsonObject();
                if (!profileInfo.has("stats") || profileInfo.get("stats").isJsonNull())
                    continue;
                JsonObject stats = profileInfo.getAsJsonObject("stats");
                if (!profileInfo.has("members") || profileInfo.get("members").isJsonNull())
                    continue;
                JsonObject membersRaw = profileInfo.getAsJsonObject("members");

                JsonObject members = new JsonObject();
                for (String memberUuid : membersRaw.keySet()) {
                    JsonElement mEl = membersRaw.get(memberUuid);
                    if (mEl == null || mEl.isJsonNull())
                        continue;
                    JsonObject rawMember = mEl.getAsJsonObject();
                    JsonObject memberData = new JsonObject();

                    if (rawMember.has("dungeons") && !rawMember.get("dungeons").isJsonNull()) {
                        JsonObject soopyDungeons = rawMember.getAsJsonObject("dungeons");
                        JsonObject normalizedDungeons = new JsonObject();
                        JsonObject dungeonTypes = new JsonObject();

                        JsonObject catacombs = new JsonObject();
                        catacombs.addProperty("experience",
                                soopyDungeons.has("catacombs_xp") ? soopyDungeons.get("catacombs_xp").getAsDouble()
                                        : 0.0);

                        JsonObject masterCatacombs = new JsonObject();
                        masterCatacombs.addProperty("experience", 0.0);

                        JsonObject tierCompletions = new JsonObject();
                        JsonObject masterTierCompletions = new JsonObject();
                        JsonObject bestScore = new JsonObject();
                        JsonObject masterBestScore = new JsonObject();
                        JsonObject fastestS = new JsonObject();
                        JsonObject masterFastestS = new JsonObject();
                        JsonObject fastestSPlus = new JsonObject();
                        JsonObject masterFastestSPlus = new JsonObject();

                        if (soopyDungeons.has("floorStats") && !soopyDungeons.get("floorStats").isJsonNull()) {
                            JsonObject floorStats = soopyDungeons.getAsJsonObject("floorStats");
                            for (String key : floorStats.keySet()) {
                                JsonElement fEl = floorStats.get(key);
                                if (fEl == null || fEl.isJsonNull())
                                    continue;
                                JsonObject floorData = fEl.getAsJsonObject();
                                boolean isMaster = key.startsWith("m");
                                String tier = key.substring(1);
                                if (key.equals("e"))
                                    tier = "0";

                                if (isMaster) {
                                    masterTierCompletions.addProperty(tier, JsonUtils.getInt(floorData, "completions"));
                                    masterBestScore.addProperty(tier, JsonUtils.getInt(floorData, "best_score"));
                                    if (floorData.has("fastest_time_s")
                                            && !floorData.get("fastest_time_s").isJsonNull()) {
                                        JsonObject sVal = JsonUtils.getObject(floorData, "fastest_time_s");
                                        masterFastestS.addProperty(tier, JsonUtils.getInt(sVal, "raw"));
                                    }
                                    if (floorData.has("fastest_time_s_plus")
                                            && !floorData.get("fastest_time_s_plus").isJsonNull()) {
                                        JsonObject spVal = JsonUtils.getObject(floorData, "fastest_time_s_plus");
                                        masterFastestSPlus.addProperty(tier, JsonUtils.getInt(spVal, "raw"));
                                    }
                                } else {
                                    tierCompletions.addProperty(tier, JsonUtils.getInt(floorData, "completions"));
                                    bestScore.addProperty(tier, JsonUtils.getInt(floorData, "best_score"));
                                    if (floorData.has("fastest_time_s")
                                            && !floorData.get("fastest_time_s").isJsonNull()) {
                                        JsonObject sVal = JsonUtils.getObject(floorData, "fastest_time_s");
                                        fastestS.addProperty(tier, JsonUtils.getInt(sVal, "raw"));
                                    }
                                    if (floorData.has("fastest_time_s_plus")
                                            && !floorData.get("fastest_time_s_plus").isJsonNull()) {
                                        JsonObject spVal = JsonUtils.getObject(floorData, "fastest_time_s_plus");
                                        fastestSPlus.addProperty(tier, JsonUtils.getInt(spVal, "raw"));
                                    }
                                }
                            }
                        }

                        catacombs.add("tier_completions", tierCompletions);
                        catacombs.add("best_score", bestScore);
                        catacombs.add("fastest_time_s", fastestS);
                        catacombs.add("fastest_time_s_plus", fastestSPlus);

                        masterCatacombs.add("tier_completions", masterTierCompletions);
                        masterCatacombs.add("best_score", masterBestScore);
                        masterCatacombs.add("fastest_time_s", masterFastestS);
                        masterCatacombs.add("fastest_time_s_plus", masterFastestSPlus);

                        dungeonTypes.add("catacombs", catacombs);
                        dungeonTypes.add("master_catacombs", masterCatacombs);
                        normalizedDungeons.add("dungeon_types", dungeonTypes);

                        if (soopyDungeons.has("class_levels") && !soopyDungeons.get("class_levels").isJsonNull()) {
                            JsonObject soopyClasses = soopyDungeons.getAsJsonObject("class_levels");
                            JsonObject playerClasses = new JsonObject();
                            for (String cls : soopyClasses.keySet()) {
                                JsonElement cEl = soopyClasses.get(cls);
                                if (cEl == null || cEl.isJsonNull())
                                    continue;
                                JsonObject clsData = new JsonObject();
                                JsonObject rawCls = cEl.getAsJsonObject();
                                clsData.addProperty("experience", JsonUtils.getDouble(rawCls, "xp"));
                                playerClasses.add(cls, clsData);
                            }
                            normalizedDungeons.add("player_classes", playerClasses);
                        }

                        if (soopyDungeons.has("treasures") && !soopyDungeons.get("treasures").isJsonNull()) {
                            normalizedDungeons.add("treasures", soopyDungeons.get("treasures"));
                        }

                        memberData.add("dungeons", normalizedDungeons);
                    }

                    if (rawMember.has("achievements") && !rawMember.get("achievements").isJsonNull()) {
                        memberData.add("achievements", rawMember.get("achievements"));
                    }
                    JsonObject playerStats = new JsonObject();
                    if (rawMember.has("player_stats") && !rawMember.get("player_stats").isJsonNull()) {
                        playerStats = rawMember.getAsJsonObject("player_stats").deepCopy();
                    }
                    if (rawMember.has("kills") && !rawMember.get("kills").isJsonNull()) {
                        playerStats.add("kills", rawMember.get("kills"));
                    }
                    if (rawMember.has("deaths") && !rawMember.get("deaths").isJsonNull()) {
                        playerStats.add("deaths", rawMember.get("deaths"));
                    }
                    memberData.add("player_stats", playerStats);
                    if (rawMember.has("accessory_reforge") && !rawMember.get("accessory_reforge").isJsonNull()) {
                        JsonObject reforge = rawMember.getAsJsonObject("accessory_reforge");
                        if (reforge.has("highest_magical_power")
                                && !reforge.get("highest_magical_power").isJsonNull()) {
                            JsonObject storage = new JsonObject();
                            storage.addProperty("highest_magical_power",
                                    JsonUtils.getInt(reforge, "highest_magical_power"));
                            memberData.add("accessory_bag_storage", storage);
                        }
                    }

                    if (rawMember.has("inventory") && !rawMember.get("inventory").isJsonNull()) {
                        memberData.add("inventory", rawMember.get("inventory"));
                    }
                    if (rawMember.has("pets") && !rawMember.get("pets").isJsonNull()) {
                        memberData.add("pets", rawMember.get("pets"));
                    }

                    members.add(memberUuid, memberData);
                }

                JsonObject profile = new JsonObject();
                profile.addProperty("profile_id", profileId);
                String cuteName = "Unknown";
                if (stats.has("cute_name") && !stats.get("cute_name").isJsonNull()) {
                    cuteName = stats.get("cute_name").getAsString();
                }
                profile.addProperty("cute_name", cuteName);
                profile.addProperty("selected", profileId.equals(currentProfileId));
                profile.add("members", members);
                profilesArr.add(profile);
            }
            result.add("profiles", profilesArr);
            return result;
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Error normalizing Soopy data: " + e.getMessage());
            return null;
        }
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
            result.add("accessory_bag_storage",
                    member.has("accessory_bag_storage") ? member.getAsJsonObject("accessory_bag_storage")
                            : new JsonObject());
            result.add("inventory", member.has("inventory") ? member.get("inventory") : new JsonObject());
            result.add("pets", member.has("pets") ? member.get("pets") : new JsonArray());
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
            return JsonUtils.getInt(dungeons, "secrets");
        } else if (member.has("achievements")) {
            JsonObject achievements = member.getAsJsonObject("achievements");
            if (achievements.has("skyblock_treasure_hunter")) {
                return JsonUtils.getInt(achievements, "skyblock_treasure_hunter");
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
                    return JsonUtils.getInt(kills, "watcher_summon_undead");
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
                xp = JsonUtils.getDouble(playerClasses.getAsJsonObject(cls), "experience");
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
            JsonElement treasuresElement = dungeons.get("treasures");
            if (treasuresElement.isJsonObject()) {
                JsonObject treasures = treasuresElement.getAsJsonObject();
                if (treasures.has("runs") && treasures.get("runs").isJsonArray()) {
                    recentRuns = treasures.getAsJsonArray("runs");
                }
            } else if (treasuresElement.isJsonArray()) {
                recentRuns = treasuresElement.getAsJsonArray();
            }

            if (!recentRuns.isEmpty()) {
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

            int runsVal = JsonUtils.getInt(runs, tier);
            int bestScoreVal = JsonUtils.getInt(bestScore, tier);
            int fastSPlus = JsonUtils.getInt(timesSPlus, tier);
            int fastS = JsonUtils.getInt(timesS, tier);

            JsonObject floorObj = new JsonObject();
            floorObj.addProperty("runs", runsVal);
            floorObj.addProperty("best_score", bestScoreVal);
            floorObj.addProperty("fastest_s_plus", fastSPlus);
            floorObj.addProperty("fastest_s", fastS);

            floors.add(floorName, floorObj);
        }
    }

    private static JsonElement resolveRjson(JsonElement node, JsonArray raw, Map<Integer, JsonElement> cache) {
        if (node.isJsonPrimitive() && node.getAsJsonPrimitive().isNumber()) {
            int index = node.getAsInt();
            if (index >= 0 && index < raw.size()) {
                if (cache.containsKey(index)) {
                    return cache.get(index);
                }
                cache.put(index, null);
                JsonElement elementAt = raw.get(index);
                JsonElement resolved;
                if (elementAt.isJsonObject() || elementAt.isJsonArray()) {
                    resolved = resolveRjson(elementAt, raw, cache);
                } else {
                    resolved = elementAt;
                }
                cache.put(index, resolved);
                return resolved;
            }
        }
        if (node.isJsonObject()) {
            JsonObject obj = node.getAsJsonObject();
            JsonObject resolvedObj = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                resolvedObj.add(entry.getKey(), resolveRjson(entry.getValue(), raw, cache));
            }
            return resolvedObj;
        }
        if (node.isJsonArray()) {
            JsonArray arr = node.getAsJsonArray();
            JsonArray resolvedArr = new JsonArray();
            for (JsonElement el : arr) {
                resolvedArr.add(resolveRjson(el, raw, cache));
            }
            return resolvedArr;
        }
        return node;
    }
}
