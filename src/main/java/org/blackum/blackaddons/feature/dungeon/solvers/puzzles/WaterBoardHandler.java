package org.blackum.blackaddons.feature.dungeon.solvers.puzzles;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.event.DungeonEvent;
import org.blackum.blackaddons.core.event.PlayerInteractEvent;

import org.blackum.blackaddons.core.util.ThreadUtils;
import org.blackum.blackaddons.feature.dungeon.ScanUtils;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMap;
import org.blackum.blackaddons.feature.dungeon.map.Room;
import org.blackum.blackaddons.gui.render.RenderContext;

public class WaterBoardHandler {

    public static void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> {
            if (!ConfigManager.data.waterBoardSolverEnabled) return;

            MultiBufferSource.BufferSource bufSource;
            if (context.consumers() instanceof MultiBufferSource.BufferSource bs) {
                bufSource = bs;
            } else {
                bufSource = Minecraft.getInstance().renderBuffers().bufferSource();
            }

            RenderContext ctx = new RenderContext(
                    context.matrices(),
                    bufSource,
                    0.0f
            );
            WaterBoardSolver.onRenderWorld(ctx);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!ConfigManager.data.waterBoardSolverEnabled) return InteractionResult.PASS;
            if (!world.isClientSide()) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            PlayerInteractEvent.RIGHT_CLICK.BLOCK event = new PlayerInteractEvent.RIGHT_CLICK.BLOCK(pos);
            WaterBoardSolver.onInteract(event);

            return InteractionResult.PASS;
        });

        ThreadUtils.loop(200, () -> !ConfigManager.data.waterBoardSolverEnabled, () -> {
            if (WaterBoardSolver.INSTANCE.isInactive()) {
                manualTrigger();
            }
        });
    }

    public static void manualTrigger() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        BlockPos playerPos = mc.player.blockPosition();
        int idx = (playerPos.getX() + 185) / 32 * 6 + (playerPos.getZ() + 185) / 32;
        if (idx < 0 || idx >= 36) return;

        Room.Tile tile = DungeonMap.getTileGrid()[idx];
        if (tile == null || tile.owner == null || tile.owner.data == null) return;
        if (!"Water Board".equals(tile.owner.data.name)) return;

        // If we are in Water Board, scan a slightly larger radius for the lever
        // because the lever might be across tile boundaries if the room is big.
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                for (int y = 56; y <= 75; y++) {
                    BlockPos pos = new BlockPos(playerPos.getX() + x, y, playerPos.getZ() + z);
                    if (mc.level.getBlockState(pos).is(Blocks.LEVER)) {
                        for (int rot : new int[]{0, 90, 180, 270}) {
                            int woolCount = 0;
                            int validCount = 0;
                            for (int i = 0; i < 5; i++) {
                                BlockPos gateOffset = new BlockPos(0, -4, 10 + i);
                                BlockPos gatePos = ScanUtils.getRealCoord(gateOffset, pos, rot);
                                BlockState state = mc.level.getBlockState(gatePos);
                                String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                        .getKey(state.getBlock()).getPath();
                                if (path.contains("wool")) {
                                    woolCount++;
                                    validCount++;
                                } else if (state.isAir() || path.contains("water") || path.contains("glass")) {
                                    validCount++;
                                }
                            }
                            if (validCount == 5 && woolCount >= 1) {
                                triggerRoomEntry(pos, rot, pos);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void triggerRoomEntry(BlockPos center, int rotation, BlockPos leverPos) {
        DungeonEvent.RoomEvent.onEnter event = new DungeonEvent.RoomEvent.onEnter(center, rotation, leverPos);
        WaterBoardSolver.onRoomEnter(event);
    }
}
