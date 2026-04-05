package org.blackum.blackaddons.mixin.dungeon;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MapItemDataMixin {

    @Inject(method = "handleMapItemData", at = @At("TAIL"))
    private void onHandleMapItemData(ClientboundMapItemDataPacket packet, CallbackInfo ci) {
        DungeonMap.onMapPacket(packet);
    }
}
