package org.blackum.blackaddons.common.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class WorldUtils {
    public static BlockState getBlockState(BlockPos pos) {
        return Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getBlockState(pos);
    }

    public static Block getBlockAt(BlockPos pos) {
        BlockState state = getBlockState(pos);
        return state != null ? state.getBlock() : null;
    }
}
