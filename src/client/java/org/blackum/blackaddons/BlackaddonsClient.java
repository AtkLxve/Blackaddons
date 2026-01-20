package org.blackum.blackaddons;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.gui.screen.DemoScreen;

public class BlackaddonsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Blackaddons.guiOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                client.setScreen(new DemoScreen());
            });
        };

        System.out.println("blackaddons: Client initialization completed");
    }
}
