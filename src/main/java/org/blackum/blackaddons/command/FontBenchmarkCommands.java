package org.blackum.blackaddons.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.render.font.CustomFontRenderer;
import org.blackum.blackaddons.gui.render.font.EmojiManager;
import org.blackum.blackaddons.gui.render.font.EmojiSequenceCharSequence;
import net.minecraft.util.FormattedCharSequence;

public class FontBenchmarkCommands {

    private static final String BENCH_STRING_PLAIN =
            "Hello world! The quick brown fox jumps over the lazy dog. ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789";

    private static final String BENCH_STRING_EMOJI =
            "Hello \u2192 \u25b6 \ud83d\udd25 \u2764\ufe0f \ud83d\udca5 test 6\ufe0f\u20e3 \ud83c\uddee\ud83c\uddf3 mixed text here";

    private static final String BENCH_STRING_ZWJ =
            "Normal text with \ud83d\udc68\u200d\ud83d\udc69\u200d\ud83d\udc67 family \ud83c\udfc3\u200d\u2642\ufe0f runner and \ud83e\udd1a\ud83c\udffc pinch";

    private static final int WARMUP_ITERS = 500;
    private static final int BENCH_ITERS = 10_000;

    public static LiteralArgumentBuilder<FabricClientCommandSource> fontBenchmarkNode() {
        var node = ClientCommands.literal("fontbenchmark");

        node.executes(ctx -> {
            FabricClientCommandSource source = ctx.getSource();
            source.sendFeedback(Component.literal("§e[FontBench] Starting benchmark (" + BENCH_ITERS + " iters)..."));
            runBenchmarkAsync(source);
            return 1;
        });

        return node;
    }

    private static void runBenchmarkAsync(FabricClientCommandSource source) {
        Thread t = new Thread(() -> {
            try {
                long preprocessPlain = benchPreprocess(BENCH_STRING_PLAIN);
                long preprocessEmoji = benchPreprocess(BENCH_STRING_EMOJI);
                long preprocessZwj = benchPreprocess(BENCH_STRING_ZWJ);

                long seqPlain = benchEmojiSequence(BENCH_STRING_PLAIN);
                long seqEmoji = benchEmojiSequence(BENCH_STRING_EMOJI);
                long seqZwj = benchEmojiSequence(BENCH_STRING_ZWJ);

                long arrowDir = benchArrowDirection();
                long isEmoji = benchIsEmoji();

                Minecraft.getInstance().execute(() -> {
                    sendResult(source, "preprocessString (plain)",   preprocessPlain, BENCH_STRING_PLAIN.length());
                    sendResult(source, "preprocessString (emoji)",   preprocessEmoji, BENCH_STRING_EMOJI.length());
                    sendResult(source, "preprocessString (zwj)",     preprocessZwj,   BENCH_STRING_ZWJ.length());
                    sendResult(source, "EmojiSequenceCS (plain)",    seqPlain,         BENCH_STRING_PLAIN.length());
                    sendResult(source, "EmojiSequenceCS (emoji)",    seqEmoji,         BENCH_STRING_EMOJI.length());
                    sendResult(source, "EmojiSequenceCS (zwj)",      seqZwj,           BENCH_STRING_ZWJ.length());
                    sendResult(source, "getArrowDirection (per cp)", arrowDir,         1);
                    sendResult(source, "isEmoji (per cp)",           isEmoji,          1);
                    source.sendFeedback(Component.literal("§a[FontBench] Done."));
                });
            } catch (Exception e) {
                Minecraft.getInstance().execute(() ->
                        source.sendFeedback(Component.literal("§c[FontBench] Error: " + e.getMessage())));
            }
        }, "FontBenchmark");
        t.setDaemon(true);
        t.start();
    }

    private static long benchPreprocess(String input) {
        for (int i = 0; i < WARMUP_ITERS; i++) {
            EmojiManager.preprocessString(input);
        }
        long start = System.nanoTime();
        for (int i = 0; i < BENCH_ITERS; i++) {
            EmojiManager.preprocessString(input);
        }
        return System.nanoTime() - start;
    }

    private static long benchEmojiSequence(String input) {
        FormattedCharSequence seq = FormattedCharSequence.forward(input, net.minecraft.network.chat.Style.EMPTY);
        for (int i = 0; i < WARMUP_ITERS; i++) {
            EmojiSequenceCharSequence wrapped = new EmojiSequenceCharSequence(seq);
            wrapped.accept((idx, style, cp) -> true);
        }
        long start = System.nanoTime();
        for (int i = 0; i < BENCH_ITERS; i++) {
            EmojiSequenceCharSequence wrapped = new EmojiSequenceCharSequence(seq);
            wrapped.accept((idx, style, cp) -> true);
        }
        return System.nanoTime() - start;
    }

    private static long benchArrowDirection() {
        int[] testCps = { 0x2192, 0x2193, 0x2190, 0x2191, 0x25B6, 0x25BC, 0x0041, 0x0048, 0x0020, 0x1F525 };
        for (int i = 0; i < WARMUP_ITERS; i++) {
            for (int cp : testCps) EmojiManager.getArrowDirection(cp);
        }
        long start = System.nanoTime();
        int iters = BENCH_ITERS * testCps.length;
        for (int i = 0; i < BENCH_ITERS; i++) {
            for (int cp : testCps) EmojiManager.getArrowDirection(cp);
        }
        long elapsed = System.nanoTime() - start;
        return elapsed / testCps.length;
    }

    private static long benchIsEmoji() {
        int[] testCps = { 0x1F525, 0x2764, 0x1F1EE, 0x0041, 0x0048, 0x20E3, 0xFE0F, 0x200D, 0x1F3FB, 0x2705 };
        for (int i = 0; i < WARMUP_ITERS; i++) {
            for (int cp : testCps) EmojiManager.isEmoji(cp);
        }
        long start = System.nanoTime();
        for (int i = 0; i < BENCH_ITERS; i++) {
            for (int cp : testCps) EmojiManager.isEmoji(cp);
        }
        long elapsed = System.nanoTime() - start;
        return elapsed / testCps.length;
    }

    private static void sendResult(FabricClientCommandSource source, String label, long totalNs, int unitCount) {
        long totalUs = totalNs / 1_000;
        long perCallNs = totalNs / BENCH_ITERS;
        long perCharNs = unitCount > 0 ? totalNs / ((long) BENCH_ITERS * unitCount) : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("§7[FontBench] §f").append(label).append("§7: ")
          .append("§e").append(totalUs / 1000).append(".").append(String.format("%03d", totalUs % 1000)).append("ms total§7, ")
          .append("§b").append(perCallNs).append("ns/call§7, ")
          .append("§a").append(perCharNs).append("ns/char");

        source.sendFeedback(Component.literal(sb.toString()));
    }
}
