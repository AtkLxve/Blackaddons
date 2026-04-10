package org.blackum.blackaddons.feature.dungeon.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import org.blackum.blackaddons.Blackaddons;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

public class RoomData {

    private static final JsonDeserializer<Room.Shape> SHAPE_DESERIALIZER =
            (json, typeOfT, ctx) -> Room.Shape.fromStr(json.getAsString());

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Room.Shape.class, SHAPE_DESERIALIZER)
            .create();

    private static Set<RoomData> roomList = Collections.emptySet();
    private static final Map<Integer, RoomData> roomMap = new HashMap<>();

    public static void loadRooms() {
        try {
            InputStream stream = RoomData.class.getResourceAsStream("/assets/blackaddons/rooms.json");
            if (stream == null) {
                Blackaddons.LOGGER.error("[DungeonMap] rooms.json not found in classpath");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                Type setType = new TypeToken<Set<RoomData>>() {}.getType();
                Set<RoomData> loaded = GSON.fromJson(reader, setType);
                if (loaded != null) {
                    roomList = loaded;
                    roomMap.clear();
                    for (RoomData rd : loaded) {
                        if (rd.cores != null) {
                            for (int core : rd.cores) {
                                roomMap.put(core, rd);
                            }
                        }
                    }
                    Blackaddons.LOGGER.info("[DungeonMap] Loaded " + loaded.size() + " rooms.");
                }
            }
        } catch (Exception e) {
            Blackaddons.LOGGER.error("[DungeonMap] Failed to load rooms.json", e);
        }
    }

    public static Set<RoomData> getRoomList() { return roomList; }

    public static RoomData getRoomData(int coreHash) {
        return roomMap.get(coreHash);
    }

    public String name = "Unknown";
    public Room.Type type = Room.Type.NORMAL;
    public List<Integer> cores = new ArrayList<>();
    public int crypts = 0;
    public int secrets = 0;
    public Map<String, List<String>> secretDetails = new HashMap<>();
    public List<String> trappedChests = new ArrayList<>();
    public Room.Shape shape = Room.Shape.UNKNOWN;
    public boolean prince = false;

    @Override
    public String toString() {
        return "RoomData{name='" + name + "', type=" + type + ", shape=" + shape + ", secrets=" + secrets + "}";
    }
}
