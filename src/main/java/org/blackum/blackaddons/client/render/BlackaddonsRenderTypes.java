package org.blackum.blackaddons.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.blackum.blackaddons.common.util.McCompat;

public class BlackaddonsRenderTypes {
    public static VertexConsumer getWaypointBuffer(MultiBufferSource bufferSource) {
        return McCompat.getWaypointBuffer(bufferSource);
    }
}
