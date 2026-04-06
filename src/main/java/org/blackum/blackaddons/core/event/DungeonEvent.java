package org.blackum.blackaddons.core.event;

import net.minecraft.core.BlockPos;

public class DungeonEvent {
    public static class RoomEvent {
        public static class onEnter {
            public final BlockPos center;
            public final int rotation;
            public final BlockPos leverPos;

            public onEnter(BlockPos center, int rotation, BlockPos leverPos) {
                this.center = center;
                this.rotation = rotation;
                this.leverPos = leverPos;
            }
        }
    }
}
