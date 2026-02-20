package org.blackum.blackaddons.core.model;

import com.google.gson.JsonObject;
import org.blackum.blackaddons.core.util.JsonUtils;

public class Teammate {
    public final String ign;
    public final int count;
    public final String lastFloor;
    public final String lastClass;
    public final long lastTs;
    public final int lastClassLevel;

    public Teammate(String ign, JsonObject data) {
        this.ign = ign;
        this.count = JsonUtils.getInt(data, "count");
        this.lastFloor = JsonUtils.getString(data, "last_floor", "");
        this.lastClass = JsonUtils.getString(data, "last_class", "");
        this.lastTs = data.has("last_ts") && !data.get("last_ts").isJsonNull() ? data.get("last_ts").getAsLong() : 0;
        this.lastClassLevel = JsonUtils.getInt(data, "last_class_level");
    }
}
