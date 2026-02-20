package org.blackum.blackaddons.core.util;

public class DungeonUtils {
    private static final double[] CATA_XP = {
            50, 125, 235, 395, 625, 955, 1425, 2095, 3045, 4385,
            6275, 8940, 12700, 17960, 25340, 35640, 50040, 70040, 97640, 135640,
            188140, 259640, 356640, 488640, 668640, 911640, 1239640, 1684640, 2284640, 3084640,
            4149640, 5559640, 7459640, 9959640, 13259640, 17559640, 23159640, 30359640, 39559640, 51559640,
            66559640, 85559640, 109559640, 139559640, 177559640, 225559640, 285559640, 360559640, 453559640, 569809640
    };

    public static double getCataLevel(double xp) {
        for (int i = 0; i < CATA_XP.length; i++) {
            if (xp < CATA_XP[i]) {
                double prev = i == 0 ? 0 : CATA_XP[i - 1];
                double next = CATA_XP[i];
                return i + (xp - prev) / (next - prev);
            }
        }
        return 50 + (xp - CATA_XP[49]) / 200000000.0;
    }
}
