package org.blackum.blackaddons;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

import org.blackum.blackaddons.gui.screen.DemoScreen;
import org.blackum.blackaddons.gui.screen.TestMenuScreen;

public class BlackaddonsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("blackaddons: Initializing client...");
        org.blackum.blackaddons.config.ConfigManager.load();
        Blackaddons.guiOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                client.setScreen(new DemoScreen());
            });
        };

        Blackaddons.testMenuOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                client.setScreen(new TestMenuScreen());
            });
        };

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            if (org.blackum.blackaddons.gui.screen.BaseScreen.showDebugOverlay) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.options.hideGui)
                    return;

                int x = org.blackum.blackaddons.gui.screen.BaseScreen.overlayX;
                int y = org.blackum.blackaddons.gui.screen.BaseScreen.overlayY;
                float scale = org.blackum.blackaddons.gui.screen.BaseScreen.overlayScale;
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

                graphics.pose().pushMatrix();
                graphics.pose().translate((float) x, (float) y);
                graphics.pose().scale(scale, scale);

                int lineY = 0;
                for (String line : debugInfo) {
                    graphics.drawString(mc.font, line, 0, lineY, color);
                    lineY += 10;
                }
                graphics.pose().popMatrix();
            }
        });

        System.out.println("blackaddons: Client initialization completed");
    }
}
