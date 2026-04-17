package org.blackum.blackaddons.feature.dungeon.solver.puzzle;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.event.DungeonEvent;
import org.blackum.blackaddons.common.event.PlayerInteractEvent;
import org.blackum.blackaddons.common.util.DataDownloader;
import org.blackum.blackaddons.common.util.MathUtils;
import org.blackum.blackaddons.common.util.NumbersUtils;
import org.blackum.blackaddons.common.util.ThreadUtils;
import org.blackum.blackaddons.common.util.WorldUtils;
import org.blackum.blackaddons.feature.dungeon.DungeonListener;
import org.blackum.blackaddons.feature.dungeon.ScanUtils;
import org.blackum.blackaddons.client.render.Render3D;
import org.blackum.blackaddons.client.render.RenderContext;

public class WaterBoardSolver {
    public static final WaterBoardSolver INSTANCE = new WaterBoardSolver();

    private WaterBoardSolver() {
    }

    private static Map<String, Map<String, List<Double>>> waterSolutions;
    private static final Map<LEVER, List<Double>> solution = new HashMap<>();
    private static int patternId = -1;
    private static long waterLeverTick = -1L;
    private static BlockPos anchorLever = null;
    private static int roomRotation = 0;
    private static BlockPos roomCenter = null;
    private static long lastInteractTime = -1L;

    public BlockPos getRoomCenter() {
        return roomCenter;
    }

    private static void loadSolutions(int patternId) {
        Type type = new TypeToken<Map<String, Map<String, Map<String, Map<String, List<Double>>>>>>() {
        }.getType();
        Map<String, Map<String, Map<String, Map<String, List<Double>>>>> allData = DataDownloader
                .loadJson("water_solutions.json", type);
        if (allData != null) {
            Map<String, Map<String, Map<String, List<Double>>>> optimizedData = allData.get("true");
            if (optimizedData != null) {
                waterSolutions = optimizedData.get(String.valueOf(patternId));
            }
        }
    }

    public static void onRoomEnter(DungeonEvent.RoomEvent.onEnter event) {
        if (patternId == -1) {
            anchorLever = event.leverPos;
            roomRotation = event.rotation;
            BlockPos relWater = LEVER.WATER.offset;
            BlockPos rotatedRel = ScanUtils.getRealCoord(relWater, new BlockPos(0, 0, 0), roomRotation);
            roomCenter = anchorLever.subtract(new BlockPos(rotatedRel.getX(), rotatedRel.getY(), rotatedRel.getZ()));
            ThreadUtils.loop(200, () -> patternId != -1, WaterBoardSolver::solve);
        }
    }

    public boolean isInactive() {
        return patternId == -1;
    }

    public static void renderHUD(GuiGraphics graphics) {
        if (patternId != -1 && !solution.isEmpty() && ConfigManager.data.waterBoardHudEnabled) {
            List<ClickInfo> clicks = getTicksToClicks();
            if (!clicks.isEmpty()) {
                int x = ConfigManager.data.waterBoardHudX < 0 ? graphics.guiWidth() / 2 + 15
                        : ConfigManager.data.waterBoardHudX;
                int y = ConfigManager.data.waterBoardHudY < 0 ? graphics.guiHeight() / 2 - 20
                        : ConfigManager.data.waterBoardHudY;
                float scale = ConfigManager.data.waterBoardHudScale <= 0.0f ? 1.0f : ConfigManager.data.waterBoardHudScale;

                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(scale, scale);

                graphics.drawString(Minecraft.getInstance().font, "§b§lWater Board", 0, -12, -1);

                for (int i = 0; i < Math.min(clicks.size(), 4); i++) {
                    ClickInfo click = clicks.get(i);
                    String name = click.lever.name();
                    double time = click.time;

                    String color = switch (click.lever.ordinal()) {
                        case 0 -> "§f";
                        case 1 -> "§6";
                        case 2 -> "§8";
                        case 3 -> "§b";
                        case 4 -> "§a";
                        case 5 -> "§c";
                        case 6 -> "§9";
                        default -> "§7";
                    };

                    String timeStr;
                    if (waterLeverTick == -1L) {
                        timeStr = time + "s";
                    } else {
                        double remaining = (waterLeverTick + (time * 20) - DungeonListener.currentTime) / 20.0;
                        timeStr = remaining <= 0 ? "§a§lCLICK" : NumbersUtils.toFixed(remaining, 1) + "s";
                    }

                    graphics.drawString(Minecraft.getInstance().font, color + name + ": §f" + timeStr, 0, i * 10, -1);
                }

                graphics.pose().popMatrix();
            }
        }
    }

    private static List<ClickInfo> getTicksToClicks() {
        return solution.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .skip(entry.getKey().clickCount)
                        .map(time -> new ClickInfo(entry.getKey(), time)))
                .sorted(Comparator.<ClickInfo, Boolean>comparing(c -> c.time != 0.0)
                        .thenComparingInt(c -> c.time == 0.0 ? c.lever.ordinal() : Integer.MAX_VALUE)
                        .thenComparingDouble(c -> c.time))
                .collect(Collectors.toList());
    }

    private static class ClickInfo {
        final LEVER lever;
        final double time;

        ClickInfo(LEVER lever, double time) {
            this.lever = lever;
            this.time = time;
        }
    }

    public static void onRenderWorld(RenderContext ctx) {
        if (anchorLever != null && (patternId == -1 || solution.isEmpty())) {
            for (Gate gate : Gate.values()) {
                Vec3 p = getRealPos(gate.defaultOffset);
                Render3D.renderBox(ctx, MathUtils.toPos(p), gate.getColor(), 1.5f, false);
            }
        }

        if (patternId != -1 && !solution.isEmpty()) {
            List<ClickInfo> clicks = getTicksToClicks();
            if (!clicks.isEmpty()) {
                LEVER nextClick = clicks.get(0).lever;
                Render3D.renderTracer(ctx, getLeverPos(nextClick).add(0.5, 0.5, 0.5), 0xFF00FF00, 0.3f);

                int maxLines = Math.min(2, clicks.size() - 1);
                for (int i = 0; i < maxLines; i++) {
                    LEVER from = clicks.get(i).lever;
                    LEVER to = clicks.get(i + 1).lever;
                    int color = i == 0 ? 0xFFFFFF00 : 0xFFFF5500;
                    if (from != to) {
                        Render3D.renderLine(ctx, getLeverPos(from).add(0.5, 0.5, 0.5),
                                getLeverPos(to).add(0.5, 0.5, 0.5), color, 0.2f);
                    }
                }

                solution.forEach((mech, times) -> {
                    for (int i = 0; i < times.size(); i++) {
                        if (i >= mech.clickCount) {
                            double timeSeconds = times.get(i);
                            double rem = waterLeverTick == -1L ? timeSeconds
                                    : (waterLeverTick + (timeSeconds * 20) - DungeonListener.currentTime) / 20.0;
                            String text;
                            if (rem <= 0.0) {
                                text = "§a§lCLICK";
                            } else if (rem > 7.0) {
                                text = "§a" + NumbersUtils.toFixed(rem, 1) + "s";
                            } else if (rem > 2.0) {
                                text = "§e" + NumbersUtils.toFixed(rem, 1) + "s";
                            } else {
                                text = "§c" + NumbersUtils.toFixed(rem, 1) + "s";
                            }

                            Vec3 timerPos = getLeverPos(mech).add(0.5, (i - mech.clickCount) * 0.4 + 1.5, 0.5);
                            float timerScale = ConfigManager.data.waterBoardTimerScale <= 0.0f ? 1.0f : ConfigManager.data.waterBoardTimerScale;
                            Render3D.renderString(ctx, text, timerPos, timerScale, true);
                        }
                    }
                });
            }
        }
    }

    private static Vec3 getLeverPos(LEVER lever) {
        return getRealPos(lever.offset);
    }

    private static Vec3 getRealPos(BlockPos offset) {
        if (roomCenter == null) {
            return Vec3.ZERO;
        }
        BlockPos p = ScanUtils.getRealCoord(offset, roomCenter, roomRotation);
        return MathUtils.toVec(p);
    }

    public static void onInteract(PlayerInteractEvent.RIGHT_CLICK.BLOCK event) {
        if (patternId != -1 && !solution.isEmpty() && roomCenter != null) {
            long now = System.currentTimeMillis();
            if (now - lastInteractTime >= 500L) {
                lastInteractTime = now;
                Minecraft mc = Minecraft.getInstance();
                BlockState state = mc.level.getBlockState(event.pos);
                if (!state.is(Blocks.LEVER)) {
                    if (state.getBlock() instanceof ChestBlock) {
                        for (Gate gate : Gate.values()) {
                            if (isGateExtended(gate)) return;
                        }
                        reset();
                    }
                    return;
                }

                boolean found = false;
                for (LEVER mech : LEVER.values()) {
                    if (MathUtils.toPos(mech.getPos()).equals(event.pos)) {
                        if (mech == LEVER.WATER && waterLeverTick == -1L) {
                            waterLeverTick = DungeonListener.currentTime;
                        }
                        mech.clickCount++;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(
                            "§c[BlackAddons Debug] §7Unknown lever clicked at: " + event.pos.getX() + ", "
                                    + event.pos.getY() + ", " + event.pos.getZ()));
                }
            }
        }
    }

    private static void solve() {
        if (anchorLever == null) return;

        StringBuilder gatesBuilder = new StringBuilder();
        for (Gate gate : Gate.values()) {
            if (isGateExtended(gate)) {
                gatesBuilder.append(gate.ordinal());
            }
        }
        String gates = gatesBuilder.toString();

        if (gates.length() == 3) {
            if (checkBlock(new BlockPos(-1, 77, 12), Blocks.TERRACOTTA))
                patternId = 0;
            else if (checkBlock(new BlockPos(1, 78, 12), Blocks.EMERALD_BLOCK))
                patternId = 1;
            else if (checkBlock(new BlockPos(-1, 78, 12), Blocks.DIAMOND_BLOCK))
                patternId = 2;
            else if (checkBlock(new BlockPos(-1, 78, 12), Blocks.QUARTZ_BLOCK))
                patternId = 3;

            if (patternId != -1) {
                loadSolutions(patternId);
                Map<String, List<Double>> gateSolution = waterSolutions.get(gates);
                if (gateSolution != null) {
                    gateSolution.forEach((k, v) -> {
                        LEVER l = LEVER.fromKey(k);
                        if (l != null) solution.put(l, v);
                    });
                } else {
                    Minecraft.getInstance().gui.getChat()
                            .addMessage(net.minecraft.network.chat.Component
                                    .literal("§c[BlackAddons Debug] §7Solution mapping not found for gates: " + gates));
                }
            }
        }
    }

    private static boolean checkBlock(BlockPos rel, Block expected) {
        if (roomCenter == null) return false;
        BlockPos realPos = ScanUtils.getRealCoord(rel, roomCenter, roomRotation);
        return WorldUtils.getBlockAt(realPos) == expected;
    }

    private static boolean isGateExtended(Gate gate) {
        Vec3 pos = getRealPos(gate.defaultOffset);
        BlockPos bp = MathUtils.toPos(pos);
        if (Minecraft.getInstance().level == null) return false;
        BlockState state = Minecraft.getInstance().level.getBlockState(bp);
        return !state.isAir();
    }

    public static void reset() {
        for (LEVER l : LEVER.values())
            l.clickCount = 0;
        patternId = -1;
        solution.clear();
        waterLeverTick = -1L;
        anchorLever = null;
    }

    private enum Gate {
        PURPLE(new BlockPos(0, 56, 4)),
        ORANGE(new BlockPos(0, 56, 3)),
        BLUE(new BlockPos(0, 56, 2)),
        GREEN(new BlockPos(0, 56, 1)),
        RED(new BlockPos(0, 56, 0));

        private final BlockPos defaultOffset;

        Gate(BlockPos o) {
            this.defaultOffset = o;
        }

        public int getColor() {
            return switch (this) {
                case PURPLE -> 0xFFAA00FF;
                case ORANGE -> 0xFFFFAA00;
                case BLUE -> 0xFF00AAFF;
                case GREEN -> 0xFF55FF55;
                case RED -> 0xFFFF5555;
            };
        }
    }

    private enum LEVER {
        QUARTZ(new BlockPos(5, 61, 5)),
        GOLD(new BlockPos(5, 61, 0)),
        COAL(new BlockPos(5, 61, -5)),
        DIAMOND(new BlockPos(-5, 61, 5)),
        EMERALD(new BlockPos(-5, 61, 0)),
        CLAY(new BlockPos(-5, 61, -5)),
        WATER(new BlockPos(0, 60, -10));

        private final BlockPos offset;
        public int clickCount = 0;

        LEVER(BlockPos o) {
            this.offset = o;
        }

        public Vec3 getPos() {
            if (roomCenter == null) return Vec3.ZERO;
            BlockPos realPos = ScanUtils.getRealCoord(offset, roomCenter, roomRotation);
            return MathUtils.toVec(realPos);
        }

        public static LEVER fromKey(String s) {
            return switch (s) {
                case "diamond_block" -> DIAMOND;
                case "emerald_block" -> EMERALD;
                case "hardened_clay" -> CLAY;
                case "quartz_block" -> QUARTZ;
                case "gold_block" -> GOLD;
                case "coal_block" -> COAL;
                case "water" -> WATER;
                default -> null;
            };
        }
    }
}
