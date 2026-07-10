package org.blackum.blackaddons.common.util.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.DisplaySlot;

import net.minecraft.world.scores.Scoreboard;

import net.minecraft.world.scores.PlayerScoreEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

public class ScoreboardUtils {

    public static List<String> getSidebarLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return Collections.emptyList();

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

        if (objective == null)
            return Collections.emptyList();

        Collection<PlayerScoreEntry> scores;
        try {
            scores = scoreboard.listPlayerScores(objective);
        } catch (ConcurrentModificationException e) {
            return Collections.emptyList();
        }

        List<PlayerScoreEntry> safeScores;
        try {
            safeScores = new ArrayList<>(scores);
        } catch (ConcurrentModificationException e) {
            try {
                safeScores = new ArrayList<>(scores);
            } catch (ConcurrentModificationException e2) {
                return Collections.emptyList();
            }
        }

        return safeScores.stream()
                .sorted((s1, s2) -> Integer.compare(s2.value(), s1.value()))
                .limit(15)
                .map(score -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(score.owner());
                    Component ownerName = score.ownerName();
                    Component name = PlayerTeam.formatNameForTeam(team, ownerName);
                    return name.getString();
                })
                .toList();
    }

    public static List<String> getCleanSidebarLines() {
        return getSidebarLines().stream()
                .map(ScoreboardUtils::cleanScoreboard)
                .toList();
    }

    private static String cleanScoreboard(String text) {
        if (text == null)
            return "";
        String clean = text.replaceAll("§.", "");
        return clean.trim();
    }
}
