package org.blackum.blackaddons.gui.render.font;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import java.util.ArrayList;
import java.util.List;

public class EmojiSequenceCharSequence implements FormattedCharSequence {
    private final FormattedCharSequence original;

    public EmojiSequenceCharSequence(FormattedCharSequence original) {
        this.original = original;
    }

    private static class CharInfo {
        final int index;
        final Style style;
        final int codepoint;

        CharInfo(int index, Style style, int codepoint) {
            this.index = index;
            this.style = style;
            this.codepoint = codepoint;
        }
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        List<CharInfo> list = new ArrayList<>();
        original.accept((idx, style, cp) -> {
            list.add(new CharInfo(idx, style, cp));
            return true;
        });

        int len = list.size();
        for (int i = 0; i < len; ) {
            CharInfo current = list.get(i);
            int cp = current.codepoint;

            if ((cp >= 0x30 && cp <= 0x39) || cp == 0x23 || cp == 0x2A) {
                if (i + 1 < len) {
                    CharInfo next = list.get(i + 1);
                    if (next.codepoint == 0xFE0F) {
                        if (i + 2 < len) {
                            CharInfo next2 = list.get(i + 2);
                            if (next2.codepoint == 0x20E3) {
                                int pua = EmojiManager.getOrCreateSequenceCodepoint(
                                        Integer.toHexString(cp) + "-20e3"
                                );
                                if (!sink.accept(current.index, current.style, pua)) return false;
                                i += 3;
                                continue;
                            }
                        }
                    } else if (next.codepoint == 0x20E3) {
                        int pua = EmojiManager.getOrCreateSequenceCodepoint(
                                Integer.toHexString(cp) + "-20e3"
                        );
                        if (!sink.accept(current.index, current.style, pua)) return false;
                        i += 2;
                        continue;
                    }
                }
            }

            if (cp >= 0x1F1E6 && cp <= 0x1F1FF) {
                if (i + 1 < len) {
                    CharInfo next = list.get(i + 1);
                    if (next.codepoint >= 0x1F1E6 && next.codepoint <= 0x1F1FF) {
                        int pua = EmojiManager.getOrCreateSequenceCodepoint(
                                Integer.toHexString(cp) + "-" + Integer.toHexString(next.codepoint)
                        );
                        if (!sink.accept(current.index, current.style, pua)) return false;
                        i += 2;
                        continue;
                    }
                }
            }

            if (Character.isEmoji(cp) && cp > 0xFF) {
                int currIdx = i + 1;
                StringBuilder seqHex = new StringBuilder(Integer.toHexString(cp));
                boolean hasSequence = false;
                while (currIdx < len) {
                    CharInfo next = list.get(currIdx);
                    if (next.codepoint == 0x200D) {
                        if (currIdx + 1 < len) {
                            CharInfo nextEmoji = list.get(currIdx + 1);
                            if (Character.isEmoji(nextEmoji.codepoint) || (nextEmoji.codepoint >= 0x2000 && nextEmoji.codepoint <= 0x32FF)) {
                                seqHex.append("-200d-").append(Integer.toHexString(nextEmoji.codepoint));
                                currIdx += 2;
                                hasSequence = true;
                                continue;
                            }
                        }
                    } else if (next.codepoint >= 0x1F3FB && next.codepoint <= 0x1F3FF) {
                        seqHex.append("-").append(Integer.toHexString(next.codepoint));
                        currIdx += 1;
                        hasSequence = true;
                        continue;
                    } else if (next.codepoint == 0xFE0F) {
                        currIdx += 1;
                        continue;
                    }
                    break;
                }
                if (hasSequence) {
                    int pua = EmojiManager.getOrCreateSequenceCodepoint(seqHex.toString());
                    if (!sink.accept(current.index, current.style, pua)) return false;
                    i = currIdx;
                    continue;
                }
            }

            if (!sink.accept(current.index, current.style, cp)) return false;
            i++;
        }
        return true;
    }
}
