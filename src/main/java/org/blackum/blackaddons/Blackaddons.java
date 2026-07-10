package org.blackum.blackaddons;

import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class Blackaddons implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("blackaddons");
    public static Runnable guiOpener;
    public static Runnable testMenuOpener;
    public static Runnable mainGuiOpener;
    public static Consumer<Screen> screenOpener;
    public static Consumer<String> notificationTrigger;

    @Override
    public void onInitialize() {
        LOGGER.info("Initialization completed");
    }





}
