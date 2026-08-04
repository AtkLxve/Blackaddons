package org.blackum.blackaddons.gui.render.font;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.blackum.blackaddons.common.config.ConfigManager;

public class EmojiSequenceCharSequence implements FormattedCharSequence {
    private final FormattedCharSequence original;

    private static final ThreadLocal<int[]> INDEX_BUFFER = ThreadLocal.withInitial(() -> new int[128]);
    private static final ThreadLocal<Style[]> STYLE_BUFFER = ThreadLocal.withInitial(() -> new Style[128]);
    private static final ThreadLocal<int[]> CODEPOINT_BUFFER = ThreadLocal.withInitial(() -> new int[128]);
    private static final ThreadLocal<int[]> SIZE_HOLDER = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<StringBuilder> STRING_BUILDER = ThreadLocal.withInitial(() -> new StringBuilder(32));

    public EmojiSequenceCharSequence(FormattedCharSequence original) {
        this.original = original;
    }

    private static void appendHex(StringBuilder sb, int val) {
        if (val == 0) {
            sb.append('0');
            return;
        }
        int shift = 28;
        boolean leadingZero = true;
        while (shift >= 0) {
            int digit = (val >>> shift) & 0xF;
            if (digit != 0 || !leadingZero) {
                leadingZero = false;
                if (digit < 10) {
                    sb.append((char) ('0' + digit));
                } else {
                    sb.append((char) ('a' + (digit - 10)));
                }
            }
            shift -= 4;
        }
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        if (!ConfigManager.data.customFontEmoji) {
            return original.accept(sink);
        }
        int[] size = SIZE_HOLDER.get();
        size[0] = 0;

        int[][] indicesRef = { INDEX_BUFFER.get() };
        Style[][] stylesRef = { STYLE_BUFFER.get() };
        int[][] cpsRef = { CODEPOINT_BUFFER.get() };

        original.accept((idx, style, cp) -> {
            int currentSize = size[0];
            int[] indices = indicesRef[0];
            Style[] styles = stylesRef[0];
            int[] cps = cpsRef[0];

            if (currentSize >= indices.length) {
                int newLen = indices.length * 2;
                int[] newIndices = new int[newLen];
                Style[] newStyles = new Style[newLen];
                int[] newCps = new int[newLen];

                System.arraycopy(indices, 0, newIndices, 0, indices.length);
                System.arraycopy(styles, 0, newStyles, 0, styles.length);
                System.arraycopy(cps, 0, newCps, 0, cps.length);

                INDEX_BUFFER.set(newIndices);
                STYLE_BUFFER.set(newStyles);
                CODEPOINT_BUFFER.set(newCps);

                indicesRef[0] = newIndices;
                stylesRef[0] = newStyles;
                cpsRef[0] = newCps;

                indices = newIndices;
                styles = newStyles;
                cps = newCps;
            }

            indices[currentSize] = idx;
            styles[currentSize] = style;
            cps[currentSize] = cp;
            size[0] = currentSize + 1;
            return true;
        });

        int len = size[0];
        int[] indices = indicesRef[0];
        Style[] styles = stylesRef[0];
        int[] cps = cpsRef[0];

        boolean result = true;
        try {
            for (int i = 0; i < len; ) {
                int cp = cps[i];
                int idx = indices[i];
                Style style = styles[i];

                if ((cp >= 0x30 && cp <= 0x39) || cp == 0x23 || cp == 0x2A) {
                    if (i + 1 < len) {
                        int nextCp = cps[i + 1];
                        if (nextCp == 0xFE0F) {
                            if (i + 2 < len) {
                                int next2Cp = cps[i + 2];
                                if (next2Cp == 0x20E3) {
                                    StringBuilder sb = STRING_BUILDER.get();
                                    sb.setLength(0);
                                    appendHex(sb, cp);
                                    sb.append("-20e3");
                                    int pua = EmojiManager.getOrCreateSequenceCodepoint(sb.toString());
                                    if (!sink.accept(idx, style, pua)) {
                                        result = false;
                                        break;
                                    }
                                    i += 3;
                                    continue;
                                }
                            }
                        } else if (nextCp == 0x20E3) {
                            StringBuilder sb = STRING_BUILDER.get();
                            sb.setLength(0);
                            appendHex(sb, cp);
                            sb.append("-20e3");
                            int pua = EmojiManager.getOrCreateSequenceCodepoint(sb.toString());
                            if (!sink.accept(idx, style, pua)) {
                                result = false;
                                break;
                            }
                            i += 2;
                            continue;
                        }
                    }
                }

                if (cp >= 0x1F1E6 && cp <= 0x1F1FF) {
                    if (i + 1 < len) {
                        int nextCp = cps[i + 1];
                        if (nextCp >= 0x1F1E6 && nextCp <= 0x1F1FF) {
                            StringBuilder sb = STRING_BUILDER.get();
                            sb.setLength(0);
                            appendHex(sb, cp);
                            sb.append('-');
                            appendHex(sb, nextCp);
                            int pua = EmojiManager.getOrCreateSequenceCodepoint(sb.toString());
                            if (!sink.accept(idx, style, pua)) {
                                result = false;
                                break;
                            }
                            i += 2;
                            continue;
                        }
                    }
                }

                if (Character.isEmoji(cp) && cp > 0xFF) {
                    int currIdx = i + 1;
                    StringBuilder sb = STRING_BUILDER.get();
                    sb.setLength(0);
                    appendHex(sb, cp);
                    boolean hasSequence = false;
                    while (currIdx < len) {
                        int nextCp = cps[currIdx];
                        if (nextCp == 0x200D) {
                            if (currIdx + 1 < len) {
                                int nextEmoji = cps[currIdx + 1];
                                if (Character.isEmoji(nextEmoji) || (nextEmoji >= 0x2000 && nextEmoji <= 0x32FF)) {
                                    sb.append("-200d-");
                                    appendHex(sb, nextEmoji);
                                    currIdx += 2;
                                    hasSequence = true;
                                    continue;
                                }
                            }
                        } else if (nextCp >= 0x1F3FB && nextCp <= 0x1F3FF) {
                            sb.append('-');
                            appendHex(sb, nextCp);
                            currIdx += 1;
                            hasSequence = true;
                            continue;
                        } else if (nextCp == 0xFE0F) {
                            currIdx += 1;
                            continue;
                        }
                        break;
                    }
                    if (hasSequence) {
                        int pua = EmojiManager.getOrCreateSequenceCodepoint(sb.toString());
                        if (!sink.accept(idx, style, pua)) {
                            result = false;
                            break;
                        }
                        i = currIdx;
                        continue;
                    }
                }

                if (!sink.accept(idx, style, cp)) {
                    result = false;
                    break;
                }
                i++;
            }
        } finally {
            for (int i = 0; i < len; i++) {
                styles[i] = null;
            }
        }
        return result;
    }
}
