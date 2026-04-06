package org.blackum.blackaddons.core.event;

import net.minecraft.core.BlockPos;

public class PlayerInteractEvent {
    public static class RIGHT_CLICK {
        public static class BLOCK {
            public final BlockPos pos;

            public BLOCK(BlockPos pos) {
                this.pos = pos;
            }
        }
    }
}
