package org.blackum.blackaddons.mixin.core;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.feature.waypoint.AlignUtils;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.blackum.blackaddons.feature.cheat.Perspective;
import org.blackum.blackaddons.feature.waypoint.WaypointActionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getYRot()F", at = @At("HEAD"), cancellable = true)
    private void onGetYRot(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof LocalPlayer) {
            if (AlignUtils.isActive() && ConfigManager.data.alignSilent && AlignUtils.getSilentYaw() != null) {
                for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                    String methodName = element.getMethodName();
                    String className = element.getClassName();
                    if (methodName.equals("travel") 
                            || methodName.equals("moveRelative") 
                            || methodName.equals("sendPosition") 
                            || className.contains("ServerboundMovePlayerPacket") 
                            || className.contains("ClientPacketListener")) {
                        cir.setReturnValue(AlignUtils.getSilentYaw());
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void onTurn(double yRot, double xRot, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer) {
            if (Freecam.getInstance().isActive()) {
                Freecam.getInstance().changeLookDirection(yRot, xRot);
                ci.cancel();
            } else if (Perspective.getInstance().isActive()) {
                Perspective.getInstance().changeLookDirection(yRot, xRot);
                ci.cancel();
            } else if (AlignUtils.shouldBlockMovementInput() && !ConfigManager.data.alignSilent) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setPosRaw(DDD)V", at = @At("TAIL"))
    private void onSetPosRaw(double x, double y, double z, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer) {
            WaypointActionManager.getInstance().onPlayerPositionChanged();
        }
    }
}
