package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

@Mixin(ToastManager.class)
public class ToastManagerMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void onAdd(Toast toast, CallbackInfo ci) {
        if (ConfigManager.data.disableTutorialToasts && toast instanceof TutorialToast) {
            ci.cancel();
            return;
        }

        if (ConfigManager.data.disableUnsecureChatToast && toast instanceof SystemToast systemToast) {
            SystemToast.SystemToastId id = ((SystemToastAccessor) systemToast).getId();
            if (id == SystemToast.SystemToastId.UNSECURE_SERVER_WARNING) {
                ci.cancel();
                return;
            }

//? if >=26.2 {

            /*List<FormattedCharSequence> titleLines = ((SystemToastAccessor) systemToast).getTitleLines();
            if (titleLines != null) {
                for (FormattedCharSequence line : titleLines) {
                    StringBuilder sb = new StringBuilder();
                    line.accept((idx, style, cp) -> {
                        sb.appendCodePoint(cp);
                        return true;
                    });
                    String text = sb.toString();
                    if (text.contains("Chat messages can't be verified")
                            || text.contains("multiplayer.unsecureserver.toast.title")) {
                        ci.cancel();
                        break;
                    }
                }
            }

*///?} else {
            Component title = ((SystemToastAccessor) systemToast).getTitle();
            if (title != null) {
                String text = title.getString();
                if (text.contains("Chat messages can't be verified")
                        || text.equals("multiplayer.unsecureserver.toast.title")) {
                    ci.cancel();
                }
            }
//?}
        }
    }
}
