package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.components.toasts.SystemToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

@Mixin(SystemToast.class)
public interface SystemToastAccessor {
    @Accessor("id")
    SystemToast.SystemToastId getId();

//? if >=26.2 {

    /*@Accessor("titleLines")
    List<FormattedCharSequence> getTitleLines();

*///?} else {
    @Accessor("title")
    Component getTitle();
//?}
}
