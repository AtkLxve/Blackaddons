package org.blackum.blackaddons.feature.dungeon.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.blackum.blackaddons.common.model.DungeonFloor;
import org.blackum.blackaddons.common.util.mc.LocationUtils;

import java.util.List;

public class DungeonMapSerializer {

    public static JsonObject serialize() {
        Integer roomSize = DungeonMap.getRoomSize();
        Vec2i mapSize = DungeonMap.getMapSize();
        if (roomSize == null || mapSize == null || DungeonMap.getRooms().isEmpty()) return null;

        JsonObject out = new JsonObject();
        DungeonFloor floor = LocationUtils.getCurrentFloor();
        out.addProperty("floor", floor != null ? floor.getDisplayName() : "");
        out.addProperty("room_size", roomSize);

        JsonObject ms = new JsonObject();
        ms.addProperty("x", mapSize.x);
        ms.addProperty("z", mapSize.z);
        out.add("map_size", ms);

        JsonArray roomsArr = new JsonArray();
        for (Room room : DungeonMap.getRooms()) {
            JsonObject r = new JsonObject();
            r.addProperty("type", room.type.name());
            r.addProperty("shape", room.shape.str);
            r.addProperty("state", room.state.name());
            r.addProperty("mimic", room.mimic);
            r.addProperty("found_secrets", room.foundSecrets);

            JsonArray places = new JsonArray();
            for (Vec2i p : room.places) {
                JsonArray pair = new JsonArray();
                pair.add(p.x);
                pair.add(p.z);
                places.add(pair);
            }
            r.add("places", places);

            if (room.data != null) {
                r.addProperty("name", room.data.name);
                r.addProperty("secrets", room.data.secrets);
                r.addProperty("crypts", room.data.crypts);
            }

            roomsArr.add(r);
        }
        out.add("rooms", roomsArr);

        JsonArray doorsArr = new JsonArray();
        for (Door door : DungeonMap.getDoors()) {
            JsonObject d = new JsonObject();
            d.addProperty("x", door.pos.x);
            d.addProperty("z", door.pos.z);
            d.addProperty("type", door.type.name());
            d.addProperty("locked", door.locked);

            if (!door.rooms.isEmpty()) {
                Vec2i near = nearestPlace(door.rooms.get(0).places, door.pos);
                if (near != null) {
                    JsonArray adj = new JsonArray();
                    adj.add(near.x);
                    adj.add(near.z);
                    d.add("adj_a", adj);
                }
            }
            if (door.rooms.size() >= 2) {
                Vec2i near = nearestPlace(door.rooms.get(1).places, door.pos);
                if (near != null) {
                    JsonArray adj = new JsonArray();
                    adj.add(near.x);
                    adj.add(near.z);
                    d.add("adj_b", adj);
                }
            }

            doorsArr.add(d);
        }
        out.add("doors", doorsArr);

        return out;
    }

    private static Vec2i nearestPlace(List<Vec2i> places, Vec2i worldPos) {
        Vec2i target = worldPos.add(185, 185).divide(32);
        Vec2i best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Vec2i p : places) {
            int dist = Math.abs(p.x - target.x) + Math.abs(p.z - target.z);
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }
}
