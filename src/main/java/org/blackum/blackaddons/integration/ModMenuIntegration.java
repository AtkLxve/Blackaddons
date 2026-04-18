package org.blackum.blackaddons.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new BlackAddonsGUI(parent);
    }
}
