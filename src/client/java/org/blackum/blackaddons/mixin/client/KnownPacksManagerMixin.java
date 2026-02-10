package org.blackum.blackaddons.mixin.client;

import org.blackum.blackaddons.config.ConfigManager;
import org.blackum.blackaddons.modhider.ModHiderOptions;
import org.blackum.blackaddons.modhider.SpoofMode;

import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(KnownPacksManager.class)
public class KnownPacksManagerMixin {
    @Redirect(method = "trySelectingPacks", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <V> V redirectSelectPacks(Map<KnownPack, V> instance, Object object) {
        KnownPack pack = (KnownPack) object;
        if (!pack.namespace().equalsIgnoreCase("fabric") ||
                ConfigManager.data.modHiderConfig.SPOOF_MODE == SpoofMode.OFF) {
            return instance.get(pack);
        }
        if (ConfigManager.data.modHiderConfig.SPOOF_MODE == SpoofMode.VANILLA) {
            return null;
        }
        if (ConfigManager.data.modHiderConfig.SPOOF_MODE == SpoofMode.MODDED) {
            return instance.get(pack);
        }
        if (ConfigManager.data.modHiderConfig.SPOOF_MODE == SpoofMode.CUSTOM) {
            for (String mod : ConfigManager.data.modHiderConfig.ALLOWED_MODS) {
                if (pack.id().toLowerCase().startsWith(mod.toLowerCase())) {
                    return instance.get(pack);
                }
            }
        }
        return null;
    }
}
