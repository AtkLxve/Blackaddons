package org.blackum.blackaddons.gui.hud;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.feature.cheat.AutoTNT;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.gui.screen.main.BaseScreen;

import java.util.ArrayList;
import java.util.List;
import org.blackum.blackaddons.feature.rotation.RotationManager;

@AutoModule(order = 401)
public class DebugHud implements HudElement {
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final int LINE_HEIGHT = 10;

    public static void register() {
        HudRegistry.register(new DebugHud());
    }

    @Override
    public String id() {
        return "debug";
    }

    @Override
    public String displayName() {
        return "Debug Overlay";
    }

    @Override
    public boolean enabled() {
        return BaseScreen.showDebugOverlay && !McCompat.isGuiHidden(Minecraft.getInstance());
    }

    @Override
    public int x() {
        return BaseScreen.overlayX;
    }

    @Override
    public int y() {
        return BaseScreen.overlayY;
    }

    @Override
    public void setPos(int x, int y) {
        BaseScreen.overlayX = x;
        BaseScreen.overlayY = y;
    }

    @Override
    public void reset() {
        BaseScreen.overlayX = 5;
        BaseScreen.overlayY = 5;
        BaseScreen.overlayScale = 1.0f;
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int w, int h) {
        float scale = Math.max(0.1f, Math.min(10.0f, w / 160.0f));
        BaseScreen.overlayScale = scale;
    }

    @Override
    public int width() {
        return (int) (160 * BaseScreen.overlayScale);
    }

    @Override
    public int height() {
        Minecraft mc = Minecraft.getInstance();
        return (int) (gatherDebugInfo(mc).size() * LINE_HEIGHT * BaseScreen.overlayScale);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        float scale = BaseScreen.overlayScale;
        List<String> debugInfo = gatherDebugInfo(mc);

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x(), (float) y());
        graphics.pose().scale(scale, scale);

        int lineY = 0;
        for (String line : debugInfo) {
            graphics.text(mc.font, line, 0, lineY, DEFAULT_COLOR);
            lineY += LINE_HEIGHT;
        }
        graphics.pose().popMatrix();
    }

    private static List<String> gatherDebugInfo(Minecraft mc) {
        List<String> debugInfo = new ArrayList<>();
        debugInfo.add(ChatFormatting.GOLD + "[BlackAddons Debug]");
        debugInfo.add("Nick: " + mc.getUser().getName());
        debugInfo.add("VSync: " + mc.options.enableVsync().get());

        double windowWidth = mc.getWindow().getScreenWidth();
        double windowHeight = mc.getWindow().getScreenHeight();
        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();

        int finalMouseX = (int) (mc.mouseHandler.xpos() * ((double) scaledWidth / windowWidth));
        int finalMouseY = (int) (mc.mouseHandler.ypos() * ((double) scaledHeight / windowHeight));
        debugInfo.add("Mouse: " + finalMouseX + ", " + finalMouseY);
        var currentScreen = McCompat.getScreen(mc);
        debugInfo.add("Screen: " + (currentScreen != null ? currentScreen.getClass().getSimpleName() : "None"));

        debugInfo.add("");
        debugInfo.add(ChatFormatting.GOLD + "[Mod Hider Real Info]");
        debugInfo.add("Real Brand: fabric");

        int modCount = 0;
        int libCount = 0;
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
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
        debugInfo.add(ChatFormatting.GOLD + "[Mod Hider Status]");
        debugInfo.add("Spoof Mode: " + ConfigManager.data.modHiderSpoofMode.name());
        debugInfo.add("Hide Mods: " + ConfigManager.data.hideMods());
        debugInfo.add("Custom Client: " + ConfigManager.data.modHiderCustomClient);
        debugInfo.add("Disable Payloads: " + ConfigManager.data.modHiderDisableCustomPayloads);

        addModHiderDetail(debugInfo);
        debugInfo.addAll(AutoTNT.getDebugInfo());
        debugInfo.addAll(RotationManager.getDebugInfo());
        return debugInfo;
    }

    private static void addModHiderDetail(List<String> debugInfo) {
        List<String> hiddenModIds = new ArrayList<>();
        List<String> hiddenLibIds = new ArrayList<>();
        int allowedModCount = 0;
        int allowedLibCount = 0;

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if ("builtin".equals(mod.getMetadata().getType()))
                continue;
            String modId = mod.getMetadata().getId();
            String type = mod.getMetadata().getType();
            boolean isLibrary = type.contains("library") || type.contains("api") ||
                    modId.contains("library") || modId.contains("api");
            boolean isAllowed = ConfigManager.data.modHiderAllowedMods.contains(modId);

            if (isLibrary) {
                if (isAllowed)
                    allowedLibCount++;
                else
                    hiddenLibIds.add(modId);
            } else {
                if (isAllowed)
                    allowedModCount++;
                else
                    hiddenModIds.add(modId);
            }
        }

        debugInfo.add("Allowed Mods: " + allowedModCount);
        debugInfo.add("Allowed Libraries: " + allowedLibCount);

        if (!hiddenModIds.isEmpty()) {
            debugInfo.add("");
            debugInfo.add(ChatFormatting.RED + "Hidden Mods:");
            for (int i = 0; i < Math.min(5, hiddenModIds.size()); i++) {
                debugInfo.add(" - " + hiddenModIds.get(i));
            }
            if (hiddenModIds.size() > 5) {
                debugInfo.add("  " + ChatFormatting.GRAY + "and " + (hiddenModIds.size() - 5) + " more mods");
            }
        }

        if (!hiddenLibIds.isEmpty()) {
            debugInfo.add("");
            debugInfo.add(ChatFormatting.RED + "Hidden Libraries:");
            for (int i = 0; i < Math.min(5, hiddenLibIds.size()); i++) {
                debugInfo.add("  " + hiddenLibIds.get(i));
            }
            if (hiddenLibIds.size() > 5) {
                debugInfo.add("  " + ChatFormatting.GRAY + "and " + (hiddenLibIds.size() - 5) + " more libraries");
            }
        }

        debugInfo.add("Allowed Channels: " + ConfigManager.data.modHiderAllowedCustomPayloadChannels.size());
    }
}
