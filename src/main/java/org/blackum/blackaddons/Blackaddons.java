package org.blackum.blackaddons;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Blackaddons implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("blackaddons");
    public static Runnable guiOpener;
    public static Runnable testMenuOpener;
    public static Runnable mainGuiOpener;

    @Override
    public void onInitialize() {
        LOGGER.info("Initialization completed");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<CommandSourceStack>literal("ba")
                            .executes(ctx -> executeOpenMainGui(ctx))
                            .then(LiteralArgumentBuilder.<CommandSourceStack>literal("DebugGui")
                                    .executes(ctx -> executeOpenGui(ctx)))
                            .then(LiteralArgumentBuilder.<CommandSourceStack>literal("TestMenu")
                                    .executes(ctx -> executeOpenTestMenu(ctx))));

            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("black")
                    .executes(ctx -> executeOpenMainGui(ctx))
                    .then(LiteralArgumentBuilder.<CommandSourceStack>literal("DebugGui")
                            .executes(ctx -> executeOpenGui(ctx)))
                    .then(LiteralArgumentBuilder.<CommandSourceStack>literal("TestMenu")
                            .executes(ctx -> executeOpenTestMenu(ctx))));

            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("blackaddons")
                    .executes(ctx -> executeOpenMainGui(ctx))
                    .then(LiteralArgumentBuilder.<CommandSourceStack>literal("DebugGui")
                            .executes(ctx -> executeOpenGui(ctx)))
                    .then(LiteralArgumentBuilder.<CommandSourceStack>literal("TestMenu")
                            .executes(ctx -> executeOpenTestMenu(ctx))));
        });
    }

    private int executeStatus(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§0Black§7Addons is §arunning!"));
        return 1;
    }

    private int executeOpenMainGui(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        if (mainGuiOpener != null) {
            mainGuiOpener.run();
        }
        return 1;
    }

    private int executeOpenGui(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        if (guiOpener != null) {
            guiOpener.run();
        }
        return 1;
    }

    private int executeOpenTestMenu(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        if (testMenuOpener != null) {
            testMenuOpener.run();
        }
        return 1;
    }
}
