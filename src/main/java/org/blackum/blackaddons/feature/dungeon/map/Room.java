package org.blackum.blackaddons.feature.dungeon.map;

import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Room {

    public enum Type {
        BLOOD, CHAMPION, ENTRANCE, FAIRY, NORMAL, PUZZLE, RARE, TRAP, UNKNOWN
    }

    public enum State {
        GREEN, CLEARED, FAILED, DISCOVERED, UNOPENED, UNDISCOVERED
    }

    public enum Shape {
        UNKNOWN("Unknown", 0),
        SL("L", 3),
        S1x1("1x1", 1),
        S2x1("1x2", 2),
        S3x1("1x3", 3),
        S4x1("1x4", 4),
        S2x2("2x2", 4);

        public final String str;
        public final int tileCount;

        Shape(String str, int tileCount) {
            this.str = str;
            this.tileCount = tileCount;
        }

        public static Shape fromStr(String str) {
            for (Shape s : values()) {
                if (s.str.equals(str))
                    return s;
            }
            return UNKNOWN;
        }
    }

    public enum Rotation {
        NORTH(new Vec2i(15, 15)),
        SOUTH(new Vec2i(-15, -15)),
        WEST(new Vec2i(15, -15)),
        EAST(new Vec2i(-15, 15)),
        NONE(new Vec2i(0, 0));

        public final Vec2i pos;

        Rotation(Vec2i pos) {
            this.pos = pos;
        }
    }

    public static class Tile {
        public final Room owner;
        public final Vec2i pos;
        public final Vec2i placement;

        public Tile(Room owner, Vec2i pos, int roomSize) {
            this.owner = owner;
            this.pos = pos;
            int cellSize = roomSize + 4;
            this.placement = new Vec2i(
                    ((pos.x + 185) >> 5) * cellSize,
                    ((pos.z + 185) >> 5) * cellSize);
        }

        public int listIndex() {
            return (pos.x + 185) / 32 * 6 + (pos.z + 185) / 32;
        }
    }

    public static class StateUpdated {
        public final Room room;
        public final State oldState;
        public final State newState;

        public StateUpdated(Room room, State oldState, State newState) {
            this.room = room;
            this.oldState = oldState;
            this.newState = newState;
        }
    }

    public Type type;
    public Shape shape;
    public RoomData data;
    public Integer height;
    public int[] clayPos;

    public List<Tile> tiles = new ArrayList<>();
    public List<Vec2i> places = new ArrayList<>();
    public Set<Door> doors = new LinkedHashSet<>();

    public State state = State.UNDISCOVERED;
    public Rotation rotation = Rotation.NONE;

    public Vec2i entryTile;
    public boolean specialTile;
    public boolean rushRoom;
    public boolean mimic;
    public int foundSecrets = -1;

    public Room(Type type, Shape shape) {
        this.type = type;
        this.shape = shape;
    }

    public Room(Type type, Shape shape, RoomData data, Integer height) {
        this.type = type;
        this.shape = shape;
        this.data = data;
        this.height = height;
    }

    public Room(RoomData data, int height) {
        this(data.type, data.shape, data, height);
    }

    public StateUpdated updateState(Vec2i placement, int color) {
        if (state == State.GREEN && data != null && "Golden Oasis".equals(data.name)) {
            return null;
        }

        State oldState = state;
        State newState;

        switch (color) {
            case 0:
                newState = State.UNDISCOVERED;
                break;
            case 18:
                if (type == Type.BLOOD) {
                    DungeonMap.setBloodRoom(this);
                    newState = State.DISCOVERED;
                } else if (type == Type.ENTRANCE) {
                    newState = State.FAILED;
                } else {
                    newState = state;
                }
                break;
            case 30:
                newState = (type == Type.ENTRANCE) ? State.DISCOVERED : State.GREEN;
                break;
            case 34:
                newState = State.CLEARED;
                break;
            case 85:
            case 119:
                this.entryTile = placement;
                this.specialTile = (placement.x == DungeonMap.getSpecialColumn());
                newState = State.UNOPENED;
                break;
            default:
                newState = State.DISCOVERED;
                break;
        }

        this.state = newState;
        return (newState == oldState) ? null : new StateUpdated(this, oldState, newState);
    }

    public BlockPos offset(BlockPos rel) {
        if (clayPos == null)
            return null;
        int rx = rel.getX(), ry = rel.getY(), rz = rel.getZ();
        int rotX, rotZ;
        switch (rotation) {
            case NORTH:
                rotX = -rx;
                rotZ = -rz;
                break;
            case WEST:
                rotX = -rz;
                rotZ = rx;
                break;
            case EAST:
                rotX = rz;
                rotZ = -rx;
                break;
            default:
                rotX = rx;
                rotZ = rz;
                break;
        }
        return new BlockPos(clayPos[0] + rotX, ry, clayPos[2] + rotZ);
    }

    public Tile roomTile(Vec2i pos, int roomSize) {
        for (Tile t : tiles) {
            if (t.pos.equals(pos))
                return null;
        }
        Tile tile = new Tile(this, pos, roomSize);
        tiles.add(tile);
        places.add(pos.add(185, 185).divide(32));
        DungeonMap.registerTile(tile);
        return tile;
    }

    public Vec2i topLeftTilePlacement() {
        Tile min = null;
        int minScore = Integer.MAX_VALUE;
        for (Tile t : tiles) {
            int score = t.pos.x * 1000 + t.pos.z;
            if (score < minScore) {
                minScore = score;
                min = t;
            }
        }
        return min != null ? min.placement : new Vec2i(0, 0);
    }

    public Vec2i bottomRightTilePlacement() {
        Tile max = null;
        int maxScore = Integer.MIN_VALUE;
        for (Tile t : tiles) {
            int score = t.pos.x * 1000 + t.pos.z;
            if (score > maxScore) {
                maxScore = score;
                max = t;
            }
        }
        return max != null ? max.placement : new Vec2i(0, 0);
    }
}
