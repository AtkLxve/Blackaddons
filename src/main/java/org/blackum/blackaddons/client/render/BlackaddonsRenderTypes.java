package org.blackum.blackaddons.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
//? if <26.2 {
import net.minecraft.client.renderer.MultiBufferSource;
import org.blackum.blackaddons.common.util.mc.McCompat;
//?}

public class BlackaddonsRenderTypes {
//? if <26.2 {
    public static VertexConsumer getWaypointBuffer(MultiBufferSource bufferSource) {
        return McCompat.getWaypointBuffer(bufferSource);
    }
//?}
}
