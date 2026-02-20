package org.blackum.blackaddons.gui.widget;

import net.minecraft.network.chat.Style;
import java.util.List;

public interface SyntaxHighlighter {
    List<Style> highlight(String line);
}
