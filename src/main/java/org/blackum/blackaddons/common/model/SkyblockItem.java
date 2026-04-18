package org.blackum.blackaddons.common.model;

import net.minecraft.world.item.ItemStack;
import com.google.gson.JsonObject;

public record SkyblockItem(ItemStack itemStack, String skyblockId, String rarity, String customStackText,
        Integer customStackTextColor, JsonObject extraData) {
    public SkyblockItem(ItemStack itemStack, String skyblockId, String rarity) {
        this(itemStack, skyblockId, rarity, null, null, null);
    }
}
