package org.blackum.blackaddons.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JsonUtils {

    public static String getString(JsonObject json, String key, String defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : defaultValue;
    }

    public static int getInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : 0;
    }

    public static double getDouble(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : 0.0;
    }

    public static JsonObject getObject(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonObject() ? json.getAsJsonObject(key) : new JsonObject();
    }

    public static JsonObject getObject(JsonArray array, int index) {
        return index >= 0 && index < array.size() && array.get(index).isJsonObject()
                ? array.get(index).getAsJsonObject()
                : new JsonObject();
    }
}
