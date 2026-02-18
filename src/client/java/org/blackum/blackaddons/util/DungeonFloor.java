package org.blackum.blackaddons.util;

import java.util.ArrayList;
import java.util.List;

public enum DungeonFloor {
    ENTRANCE("normal_0", "Entrance", "Entrance"),
    F1("normal_1", "F1", "F1"),
    F2("normal_2", "F2", "F2"),
    F3("normal_3", "F3", "F3"),
    F4("normal_4", "F4", "F4"),
    F5("normal_5", "F5", "F5"),
    F6("normal_6", "F6", "F6"),
    F7("normal_7", "F7", "F7"),
    M1("master_1", "M1", "M1"),
    M2("master_2", "M2", "M2"),
    M3("master_3", "M3", "M3"),
    M4("master_4", "M4", "M4"),
    M5("master_5", "M5", "M5"),
    M6("master_6", "M6", "M6"),
    M7("master_7", "M7", "M7");

    private final String key;
    private final String displayName;
    private final String shortName;

    DungeonFloor(String key, String displayName, String shortName) {
        this.key = key;
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public static DungeonFloor fromKey(String key) {
        for (DungeonFloor floor : values()) {
            if (floor.key.equals(key))
                return floor;
        }
        return M7;
    }

    public static DungeonFloor fromDisplayName(String displayName) {
        if (displayName.equalsIgnoreCase("Entrance"))
            return ENTRANCE;
        for (DungeonFloor floor : values()) {
            if (floor.displayName.equalsIgnoreCase(displayName) || floor.shortName.equalsIgnoreCase(displayName))
                return floor;
        }
        return M7;
    }

    public static List<String> getDisplayNames() {
        List<String> names = new ArrayList<>();
        for (DungeonFloor floor : values()) {
            names.add(floor.displayName);
        }
        return names;
    }
}
