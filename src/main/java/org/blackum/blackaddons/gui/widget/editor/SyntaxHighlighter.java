package org.blackum.blackaddons.gui.widget.editor;

import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;
import org.blackum.blackaddons.gui.widget.editor.*;

import net.minecraft.network.chat.Style;
import java.util.List;

public interface SyntaxHighlighter {
    List<Style> highlight(String line);
}
