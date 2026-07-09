package org.blackum.blackaddons.common.util.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.regex.Pattern;

import org.blackum.blackaddons.mixin.core.PlayerTabOverlayAccessor;

public class TabListUtils {
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    public static List<String> getTabListLines() {
        return collectLines(true);
    }

    public static List<String> getRawTabListLines() {
        return collectLines(false);
    }

    private static List<String> collectLines(boolean stripColor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return Collections.emptyList();

        Collection<PlayerInfo> players = mc.getConnection().getOnlinePlayers();
        PlayerInfo[] playersArray;

        try {
            playersArray = players.toArray(new PlayerInfo[0]);
        } catch (ConcurrentModificationException e) {
            try {
                playersArray = players.toArray(new PlayerInfo[0]);
            } catch (ConcurrentModificationException e2) {
                return Collections.emptyList();
            }
        }

        List<String> lines = new ArrayList<>();
        for (PlayerInfo player : playersArray) {
            if (player == null) continue;
            Component name = player.getTabListDisplayName();
            String label = name != null ? name.getString() : player.getProfile().name();
            if (stripColor) {
                lines.add(STRIP_COLOR_PATTERN.matcher(label).replaceAll("").trim());
            } else {
                lines.add(label);
            }
        }
        return lines;
    }

    public static List<String> getFooterLines() {
        return collectFooterLines(true);
    }

    public static List<String> getRawFooterLines() {
        return collectFooterLines(false);
    }

    private static List<String> collectFooterLines(boolean stripColor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return Collections.emptyList();

        PlayerTabOverlay tabList = mc.gui.getTabList();
        if (tabList == null) return Collections.emptyList();

        Component footer = ((PlayerTabOverlayAccessor) tabList).getFooter();
        if (footer == null) return Collections.emptyList();

        String footerString = footer.getString();
        if (footerString == null || footerString.isEmpty()) return Collections.emptyList();

        String[] split = footerString.split("\n");
        List<String> lines = new ArrayList<>();
        for (String s : split) {
            if (stripColor) {
                lines.add(STRIP_COLOR_PATTERN.matcher(s).replaceAll("").trim());
            } else {
                lines.add(s);
            }
        }
        return lines;
    }
}
