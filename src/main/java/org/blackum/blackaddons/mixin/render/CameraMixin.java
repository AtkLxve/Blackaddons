package org.blackum.blackaddons.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.blackum.blackaddons.feature.cheat.Perspective;
import net.minecraft.client.player.LocalPlayer;
import org.blackum.blackaddons.feature.waypoint.AlignUtils;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.phys.Vec3;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow private Entity entity;
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void move(float x, float y, float z);
    @Shadow protected abstract float getMaxZoom(float startingDistance);
    @Shadow public abstract float getCameraEntityPartialTicks(DeltaTracker deltaTracker);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = At.Shift.AFTER))
    private void onUpdateAfterAlign(DeltaTracker deltaTracker, CallbackInfo ci) {
        Entity entity = this.entity;
        if (entity == null) {
            return;
        }

        if (AlignUtils.isActive() && ConfigManager.data.alignSilent && AlignUtils.getSilentYaw() != null) {
            if (entity instanceof LocalPlayer player) {
                float silentYaw = AlignUtils.getSilentYaw();
                player.setYHeadRot(silentYaw);
                player.yHeadRotO = silentYaw;
                player.setYBodyRot(silentYaw);
                player.yBodyRotO = silentYaw;
            }
        }

        float tickDelta = getCameraEntityPartialTicks(deltaTracker);
        if (Freecam.getInstance().isActive()) {
            this.detached = true;
            setPosition(
                Freecam.getInstance().getX(tickDelta),
                Freecam.getInstance().getY(tickDelta),
                Freecam.getInstance().getZ(tickDelta)
            );
            setRotation(
                Freecam.getInstance().getYaw(tickDelta),
                Freecam.getInstance().getPitch(tickDelta)
            );
        } else if (Perspective.getInstance().isActive()) {
            this.detached = true;
            Vec3 eyePos = entity.getEyePosition(tickDelta);
            setPosition(eyePos.x, eyePos.y, eyePos.z);
            
            float pYaw = Perspective.getInstance().getYaw(tickDelta);
            float pPitch = Perspective.getInstance().getPitch(tickDelta);
            setRotation(pYaw, pPitch);

            float dist = Perspective.getInstance().getDistance();
            move(-getMaxZoom(dist), 0.0F, 0.0F);
        }
    }

    @Inject(method = "getMaxZoom(F)F", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float startingDistance, CallbackInfoReturnable<Float> cir) {
        if (Freecam.getInstance().isActive() || Perspective.getInstance().isActive()) {
            cir.setReturnValue(startingDistance);
        }
    }
}
