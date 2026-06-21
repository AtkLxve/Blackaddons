package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.blackum.blackaddons.common.model.DungeonFloor;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.feature.dungeon.score.DungeonScore;
import org.blackum.blackaddons.feature.dungeon.solver.puzzle.tpmaze.TpMazeHandler;
import org.blackum.blackaddons.feature.ping.PingFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onHandleEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!LocationUtils.inDungeons()) return;
        
        DungeonFloor floor = LocationUtils.getCurrentFloor();
        if (floor == null) return;
        
        String floorName = floor.getDisplayName();
        if (!floorName.equals("F6") && !floorName.equals("M6") && !floorName.equals("F7") && !floorName.equals("M7")) return;
        if (LocationUtils.inBoss()) return;

        if (packet.getEventId() == 3) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;
            
            Entity entity = packet.getEntity(mc.level);
            if (entity instanceof Zombie zombie && zombie.isBaby()) {
                DungeonScore.onMimicKill();
            }
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onHandlePlayerPositionPre(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        TpMazeHandler.onServerTeleportPre(packet);
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void onHandlePlayerPositionPost(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        TpMazeHandler.onServerTeleportPost();
    }

    @Inject(method = "handlePongResponse", at = @At("HEAD"), cancellable = true)
    private void onHandlePongResponse(ClientboundPongResponsePacket packet, CallbackInfo ci) {
        if (PingFeature.onPongReceive(packet.time())) {
            ci.cancel();
        }
    }
}
