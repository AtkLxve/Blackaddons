package org.blackum.blackaddons.core.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.feature.chat.ChatUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CustomNameManager {
    private static CustomNameManager instance;
    private final Map<String, CustomName> customNames = new ConcurrentHashMap<>();
    private final HttpClient httpClient;

    private CustomNameManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .build();
    }

    public static CustomNameManager getInstance() {
        if (instance == null) {
            instance = new CustomNameManager();
        }
        return instance;
    }

    public void fetch() {
        if (ConfigManager.data.botUrl.isEmpty()) {
            return;
        }

        String url = ConfigManager.data.botUrl + Constants.BOT_API_NAMES;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", Constants.BOT_USER_AGENT)
                .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    }
                    return null;
                })
                .thenAccept(body -> {
                    if (body != null) {
                        try {
                            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                            if (json.has("names")) {
                                JsonObject namesObj = json.getAsJsonObject("names");
                                customNames.clear();
                                for (Map.Entry<String, JsonElement> entry : namesObj.entrySet()) {
                                    JsonObject data = entry.getValue().getAsJsonObject();
                                    String displayName = data.get("display").getAsString();
                                    String color = data.has("color") ? data.get("color").getAsString() : "";

                                    String gradientStart = "";
                                    String gradientEnd = "";
                                    if (data.has("gradient")) {
                                        JsonArray gradient = data.getAsJsonArray("gradient");
                                        if (gradient.size() >= 2) {
                                            gradientStart = gradient.get(0).getAsString();
                                            gradientEnd = gradient.get(1).getAsString();
                                        }
                                    }

                                    customNames.put(entry.getKey().toLowerCase(),
                                            new CustomName(displayName, color, gradientStart, gradientEnd));
                                }
                                Blackaddons.LOGGER
                                        .info("Successfully fetched " + customNames.size() + " custom names.");
                            }
                        } catch (Exception e) {
                            Blackaddons.LOGGER.error("Failed to parse custom names: " + e.getMessage());
                        }
                    }
                })
                .exceptionally(ex -> {
                    Blackaddons.LOGGER.error("Error fetching custom names: " + ex.getMessage());
                    return null;
                });
    }

    public Component replaceNames(Component component) {
        if (customNames.isEmpty()) {
            return component;
        }

        String text = component.getString();
        String lowerText = text.toLowerCase();
        boolean found = false;

        for (String ign : customNames.keySet()) {
            if (lowerText.contains(ign)) {
                found = true;
                break;
            }
        }

        if (!found) {
            return component;
        }

        MutableComponent result = Component.empty();
        int currentPos = 0;

        while (currentPos < text.length()) {
            String earliestIgn = null;
            int earliestIdx = -1;

            for (String ign : customNames.keySet()) {
                int idx = lowerText.indexOf(ign, currentPos);
                if (idx != -1 && (earliestIdx == -1 || idx < earliestIdx)) {
                    earliestIdx = idx;
                    earliestIgn = ign;
                }
            }

            if (earliestIgn == null) {
                result.append(Component.literal(text.substring(currentPos)));
                break;
            }

            if (earliestIdx > currentPos) {
                result.append(Component.literal(text.substring(currentPos, earliestIdx)));
            }

            result.append(applyCustomName(earliestIgn, Component.literal(earliestIgn)));
            currentPos = earliestIdx + earliestIgn.length();
        }

        return result;
    }

    public Component applyCustomName(String username, Component originalComponent) {
        CustomName custom = customNames.get(username.toLowerCase());
        if (custom == null) {
            return originalComponent;
        }

        if (custom.gradientStart() != null && !custom.gradientStart().isEmpty() &&
                custom.gradientEnd() != null && !custom.gradientEnd().isEmpty()) {
            try {
                int start = Integer.parseInt(custom.gradientStart().replace("#", ""), 16);
                int end = Integer.parseInt(custom.gradientEnd().replace("#", ""), 16);
                return ChatUtils.BuildGradient(custom.display(), start, end);
            } catch (Exception e) {
            }
        }

        if (custom.display().contains("§")) {
            return Component.literal(custom.display());
        }

        if (custom.color() != null && !custom.color().isEmpty()) {
            try {
                TextColor color = TextColor.parseColor(custom.color()).getOrThrow();
                return Component.literal(custom.display()).withStyle(Style.EMPTY.withColor(color));
            } catch (Exception e) {
            }
        }

        return Component.literal(custom.display());
    }

    public CustomName getCustomName(String username) {
        return customNames.get(username.toLowerCase());
    }

    public record CustomName(String display, String color, String gradientStart, String gradientEnd) {
    }
}
