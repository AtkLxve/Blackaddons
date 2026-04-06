package org.blackum.blackaddons.feature.dungeon.map;

import java.util.ArrayList;
import java.util.List;

public class Door {

    public enum Type {
        BLOOD, NORMAL, WITHER
    }

    public final Vec2i pos;
    public Type type;
    public final List<Room> rooms;
    public boolean locked;
    public boolean worldScanned;

    public Door(Vec2i pos, Type type, List<Room> rooms) {
        this.pos = pos;
        this.type = type;
        this.rooms = new ArrayList<>(rooms);
        this.locked = (type == Type.WITHER || type == Type.BLOOD);
    }

    public boolean isSeen() {
        for (Room r : rooms) {
            if (r.state != Room.State.UNDISCOVERED && r.state != Room.State.UNOPENED) return true;
        }
        return false;
    }

    public float[] placement(float doorThickness, int roomSize) {
        int x = (pos.x + 185) >> 4;
        int z = (pos.z + 185) >> 4;
        int xEven = x % 2;
        int zEven = z % 2;
        float cellSize = roomSize + 4f;
        float thicknessOffset = (roomSize - doorThickness) / 2f;
        float px = (x >> 1) * cellSize + xEven * roomSize + (xEven ^ 1) * thicknessOffset;
        float pz = (z >> 1) * cellSize + zEven * roomSize + (zEven ^ 1) * thicknessOffset;
        return new float[]{px, pz};
    }

    public Vec2i size(float doorThickness, int roomSize) {
        int xOffset = ((pos.x + 185) >> 4) % 2;
        int zOffset = ((pos.z + 185) >> 4) % 2;
        return new Vec2i(
                (int) ((xOffset ^ 1) * doorThickness + xOffset * 4),
                (int) ((zOffset ^ 1) * doorThickness + zOffset * 4)
        );
    }
}
