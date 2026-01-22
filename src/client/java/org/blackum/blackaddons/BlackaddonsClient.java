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

        Blackaddons.mainGuiOpener = () -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                client.setScreen(new org.blackum.blackaddons.gui.screen.BlackAddonsGUI());
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

                debugInfo.add("");
                debugInfo.add("§6[Mod Hider Real Info]");
                String realBrand = "fabric";
                debugInfo.add("Real Brand: " + realBrand);

                int modCount = 0;
                int libCount = 0;
                for (net.fabricmc.loader.api.ModContainer mod : net.fabricmc.loader.api.FabricLoader.getInstance()
                        .getAllMods()) {
                    if ("builtin".equals(mod.getMetadata().getType()))
                        continue;
                    String type = mod.getMetadata().getType();
                    if (type.contains("library") || type.contains("api") ||
                            mod.getMetadata().getId().contains("library") ||
                            mod.getMetadata().getId().contains("api")) {
                        libCount++;
                    } else {
                        modCount++;
                    }
                }
                debugInfo.add("Real Mods: " + modCount);
                debugInfo.add("Real Libraries: " + libCount);

                debugInfo.add("");
                debugInfo.add("§6[Mod Hider Status]");
                debugInfo.add("Spoof Mode: " + org.blackum.blackaddons.modhider.ModHiderOptions.SPOOF_MODE.name());
                debugInfo.add("Hide Mods: " + org.blackum.blackaddons.modhider.ModHiderOptions.hideMods());
                debugInfo.add("Custom Client: " + org.blackum.blackaddons.modhider.ModHiderOptions.CUSTOM_CLIENT);
                debugInfo.add("Disable Payloads: "
                        + org.blackum.blackaddons.modhider.ModHiderOptions.DISABLE_CUSTOM_PAYLOADS);

                java.util.List<String> allowedModIds = new java.util.ArrayList<>();
                java.util.List<String> allowedLibIds = new java.util.ArrayList<>();
                java.util.List<String> hiddenModIds = new java.util.ArrayList<>();
                java.util.List<String> hiddenLibIds = new java.util.ArrayList<>();

                for (net.fabricmc.loader.api.ModContainer mod : net.fabricmc.loader.api.FabricLoader.getInstance()
                        .getAllMods()) {
                    if ("builtin".equals(mod.getMetadata().getType()))
                        continue;
                    String modId = mod.getMetadata().getId();
                    String type = mod.getMetadata().getType();
                    boolean isLibrary = type.contains("library") || type.contains("api") ||
                            modId.contains("library") || modId.contains("api");
                    boolean isAllowed = org.blackum.blackaddons.modhider.ModHiderOptions.ALLOWED_MODS.contains(modId);

                    if (isLibrary) {
                        if (isAllowed) {
                            allowedLibIds.add(modId);
                        } else {
                            hiddenLibIds.add(modId);
                        }
                    } else {
                        if (isAllowed) {
                            allowedModIds.add(modId);
                        } else {
                            hiddenModIds.add(modId);
                        }
                    }
                }

                debugInfo.add("Allowed Mods: " + allowedModIds.size());
                debugInfo.add("Allowed Libraries: " + allowedLibIds.size());

                if (!hiddenModIds.isEmpty()) {
                    debugInfo.add("");
                    debugInfo.add("§cHidden Mods:");
                    int count = 0;
                    for (String modId : hiddenModIds) {
                        if (count >= 5)
                            break;
                        debugInfo.add("  " + modId);
                        count++;
                    }
                    if (hiddenModIds.size() > 5) {
                        debugInfo.add("  §7and " + (hiddenModIds.size() - 5) + " more mods");
                    }
                }

                if (!hiddenLibIds.isEmpty()) {
                    debugInfo.add("");
                    debugInfo.add("§cHidden Libraries:");
                    int count = 0;
                    for (String libId : hiddenLibIds) {
                        if (count >= 5)
                            break;
                        debugInfo.add("  " + libId);
                        count++;
                    }
                    if (hiddenLibIds.size() > 5) {
                        debugInfo.add("  §7and " + (hiddenLibIds.size() - 5) + " more libraries");
                    }
                }

                debugInfo.add("Allowed Channels: "
                        + org.blackum.blackaddons.modhider.ModHiderOptions.ALLOWED_CUSTOM_PAYLOAD_CHANNELS.size());

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
