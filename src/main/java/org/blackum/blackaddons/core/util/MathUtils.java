package org.blackum.blackaddons.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class MathUtils {
    public static BlockPos toPos(Vec3 vec) {
        return new BlockPos((int) Math.floor(vec.x), (int) Math.floor(vec.y), (int) Math.floor(vec.z));
    }

    public static Vec3 toVec(BlockPos pos) {
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
