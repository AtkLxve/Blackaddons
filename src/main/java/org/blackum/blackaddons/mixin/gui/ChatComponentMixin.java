package org.blackum.blackaddons.mixin.gui;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.feature.chat.ChatImageHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"), argsOnly = true)
    private Component onAddMessage(Component message) {
        return ChatImageHandler.handleMessage(message);
    }
}
