package org.blackum.blackaddons.feature.dungeon;

import net.minecraft.core.BlockPos;

public class ScanUtils {
    public static BlockPos getRealCoord(BlockPos rel, BlockPos center, int rotation) {
        int x = rel.getX();
        int z = rel.getZ();
        switch (rotation) {
            case 90:
                return center.offset(-z, rel.getY(), x);
            case 180:
                return center.offset(-x, rel.getY(), -z);
            case 270:
                return center.offset(z, rel.getY(), -x);
            default:
                return center.offset(x, rel.getY(), z);
        }
    }
}
