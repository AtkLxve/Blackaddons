package org.blackum.blackaddons.feature.dungeon.map;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.util.LocationUtils;

import java.util.Set;

public class DungeonWorldScanner {

    private static boolean shouldScan = false;

    private static final Block[] BLACKLISTED = {Blocks.CHEST, Blocks.TRAPPED_CHEST};

    public static void register() {
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (LocationUtils.inDungeons()) shouldScan = true;
        });

        ClientTickEvents.END_WORLD_TICK.register(world -> {
            if (shouldScan) {
                scan();
                shouldScan = false;
            }
        });
    }

    public static void reset() {
        shouldScan = false;
    }

    private static void scan() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Level level = mc.level;
        Set<Room> rooms = DungeonMap.getRooms();

        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 6; z++) {
                int wx = -185 + x * 32;
                int wz = -185 + z * 32;

                if (!level.hasChunk(wx >> 4, wz >> 4)) continue;

                int[] coreAndHeight = calculateCore(level, wx, wz);
                if (coreAndHeight == null) continue;

                int core = coreAndHeight[0];
                int height = coreAndHeight[1];

                if (core == 48696) continue;

                RoomData roomData = RoomData.getRoomData(core);
                if (roomData == null) {
                    continue;
                }

                Vec2i place = new Vec2i(x, z);
                int tileIdx = place.roomListIndex();
                Room.Tile[] tileGrid = DungeonMap.getTileGrid();

                synchronized (rooms) {
                    Room existing = (tileGrid[tileIdx] != null) ? tileGrid[tileIdx].owner : null;

                    if (existing != null) {
                        if (existing.data == null || !existing.data.name.equals(roomData.name)) {
                            existing.data = roomData;
                            existing.type = roomData.type;
                            existing.shape = roomData.shape;
                            existing.height = height;
                        }
                        updateRotationOffShape(existing);
                    } else {
                        Room found = null;
                        for (Room r : rooms) {
                            if (r.data != null && r.data.name.equals(roomData.name)) { found = r; break; }
                        }
                        if (found == null) {
                            found = new Room(roomData, height);
                            rooms.add(found);
                        }
                        Vec2i worldPos = new Vec2i(wx, wz);
                        Integer rs = DungeonMap.getRoomSize();
                        found.roomTile(worldPos, rs != null ? rs : 16);
                        updateRotationOffShape(found);
                    }
                }
            }
        }
    }

    private static int[] calculateCore(Level level, int worldX, int worldZ) {
        int height = 0;
        for (int y = 160; y > 10; y--) {
            Block block = level.getBlockState(new BlockPos(worldX, y, worldZ)).getBlock();
            if (block == Blocks.BEDROCK) return null;
            if (block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR) {
                height = y;
                break;
            }
        }
        if (height == 0) return null;

        int scanHeight = Math.max(11, Math.min(140, height));
        StringBuilder sb = new StringBuilder(150);
        sb.append(140 - scanHeight);

        int bedrock = 0;
        for (int y = scanHeight; y > 11; y--) {
            Block block = level.getBlockState(new BlockPos(worldX, y, worldZ)).getBlock();

            if (bedrock >= 2 && block == Blocks.AIR) {
                for (int i = 0; i < y - 11; i++) sb.append('a');
            }

            if (block == Blocks.BEDROCK) {
                bedrock++;
            } else {
                bedrock = 0;
                if (isBlacklisted(block)) continue;
            }

            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            sb.append(Character.toLowerCase(path.charAt(0)));
        }

        String coreStr = sb.toString();
        int hash = coreStr.hashCode();
        return new int[]{hash, height};
    }

    private static boolean isBlacklisted(Block block) {
        for (Block b : BLACKLISTED) if (b == block) return true;
        return false;
    }

    public static void updateRotationOffShape(Room room) {
        if (room.shape == Room.Shape.S1x1 || room.tiles.size() != room.shape.tileCount) return;

        Room.Tile topLeft = null, bottomRight = null;
        int minScore = Integer.MAX_VALUE, maxScore = Integer.MIN_VALUE;
        for (Room.Tile t : room.tiles) {
            int score = t.pos.x * 1000 + t.pos.z;
            if (score < minScore) { minScore = score; topLeft = t; }
            if (score > maxScore) { maxScore = score; bottomRight = t; }
        }
        if (topLeft == null || bottomRight == null) return;

        if (room.shape == Room.Shape.SL) {
            Room.Tile middle = null;
            for (Room.Tile t : room.tiles) {
                if (t != topLeft && t != bottomRight) { middle = t; break; }
            }
            if (middle == null) return;

            if (topLeft.pos.x == bottomRight.pos.x) {
                room.clayPos = new int[]{middle.pos.x - 15, 0, topLeft.pos.z + 15};
                room.rotation = Room.Rotation.EAST;
            } else if (topLeft.pos.z == bottomRight.pos.z) {
                room.clayPos = new int[]{bottomRight.pos.x + 15, 0, bottomRight.pos.z - 15};
                room.rotation = Room.Rotation.WEST;
            } else if (middle.pos.x == topLeft.pos.x) {
                room.clayPos = new int[]{topLeft.pos.x - 15, 0, topLeft.pos.z - 15};
                room.rotation = Room.Rotation.SOUTH;
            } else {
                room.clayPos = new int[]{bottomRight.pos.x + 15, 0, bottomRight.pos.z + 15};
                room.rotation = Room.Rotation.NORTH;
            }
        } else {
            if (topLeft.pos.x == bottomRight.pos.x) {
                room.clayPos = new int[]{topLeft.pos.x + 15, 0, topLeft.pos.z - 15};
                room.rotation = Room.Rotation.WEST;
            } else {
                room.clayPos = new int[]{topLeft.pos.x - 15, 0, topLeft.pos.z - 15};
                room.rotation = Room.Rotation.SOUTH;
            }
        }
    }
}
