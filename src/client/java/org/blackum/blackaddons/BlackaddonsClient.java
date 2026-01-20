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

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            if (org.blackum.blackaddons.gui.screen.BaseScreen.showDebugOverlay) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.options.hideGui)
                    return;

                int x = 5;
                int y = 5;
                int color = 0xFFFFFFFF;

                double windowWidth = mc.getWindow().getScreenWidth();
                double windowHeight = mc.getWindow().getScreenHeight();
                int scaledWidth = mc.getWindow().getGuiScaledWidth();
                int scaledHeight = mc.getWindow().getGuiScaledHeight();

                int finalMouseX = (int) (mc.mouseHandler.xpos() * ((double) scaledWidth / windowWidth));
                int finalMouseY = (int) (mc.mouseHandler.ypos() * ((double) scaledHeight / windowHeight));

                java.util.List<String> debugInfo = new java.util.ArrayList<>();
                debugInfo.add("§6[BlackAddons Debug]");
                debugInfo.add("VSync: " + mc.options.enableVsync().get());
                debugInfo.add("Mouse: " + finalMouseX + ", " + finalMouseY);
                debugInfo.add("Screen: " + (mc.screen != null ? mc.screen.getClass().getSimpleName() : "None"));

                for (String line : debugInfo) {
                    graphics.drawString(mc.font, line, x, y, color);
                    y += 10;
                }
            }
        });

        System.out.println("blackaddons: Client initialization completed");
    }
}
