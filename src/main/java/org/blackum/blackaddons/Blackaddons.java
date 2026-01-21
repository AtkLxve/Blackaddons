package org.blackum.blackaddons;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;

public class Blackaddons implements ModInitializer {
    public static Runnable guiOpener;
    public static Runnable testMenuOpener;

    @Override
    public void onInitialize() {
        System.out.println("blackaddons: Initialization completed");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<CommandSourceStack>literal("ba")
                            .executes(ctx -> executeStatus(ctx))
                            .then(LiteralArgumentBuilder.<CommandSourceStack>literal("DebugGui")
                                    .executes(ctx -> executeOpenGui(ctx)))
                            .then(LiteralArgumentBuilder.<CommandSourceStack>literal("TestMenu")
                                    .executes(ctx -> executeOpenTestMenu(ctx))));

            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("blackaddons")
                    .executes(ctx -> executeStatus(ctx))
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
