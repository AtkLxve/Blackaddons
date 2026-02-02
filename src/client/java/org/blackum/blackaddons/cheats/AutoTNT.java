package org.blackum.blackaddons.cheats;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.blackum.blackaddons.mixin.client.KeyBindingAccessor;
import org.blackum.blackaddons.mixin.client.InventoryAccessor; // IMPORT THIS

import java.util.List;

public class AutoTNT {

    private static final List<Block> TARGET_BLOCKS = List.of(Blocks.CRACKED_STONE_BRICKS, Blocks.STONE_SLAB);
    private static final List<String> TNT_NAMES = List.of("Infinityboom TNT", "Superboom TNT");
    private static final ModState STATE = new ModState();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoTNT::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (!client.options.keyAttack.isDown()) {
            unequipTnt(client.player);
            return;
        }

        if (isLookingAtTargetBlock(client)) {
            equipTnt(client.player);
            if (STATE.isTntEquipped && !STATE.hasClicked) {
                STATE.ticksSinceEquip++;
                if (STATE.ticksSinceEquip >= 2) {
                    triggerAttack(client);
                    STATE.hasClicked = true;
                }
            }
        } else {
            unequipTnt(client.player);
        }
    }

    private static void triggerAttack(Minecraft client) {
        if (client.options.keyAttack instanceof KeyBindingAccessor accessor) {
            KeyMapping.click(accessor.getBoundKey());
        }
        client.player.swing(InteractionHand.MAIN_HAND);
    }

    private static boolean isLookingAtTargetBlock(Minecraft client) {
        if (client.hitResult instanceof BlockHitResult blockHit) {
            Block block = client.level.getBlockState(blockHit.getBlockPos()).getBlock();
            return TARGET_BLOCKS.contains(block);
        }
        return false;
    }

    private static void equipTnt(Player player) {
        if (STATE.isTntEquipped) return;
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            String name = stack.getHoverName().getString().replaceAll("§.", "");
            if (TNT_NAMES.contains(name)) {
                slot = i;
                break;
            }
        }

        if (slot != -1) {
            InventoryAccessor inv = (InventoryAccessor) player.getInventory();
            STATE.previousSlot = inv.getSelectedSlot();
            inv.setSelectedSlot(slot);
            STATE.isTntEquipped = true;
        }
    }

    private static void unequipTnt(Player player) {
        if (!STATE.isTntEquipped) return;
        InventoryAccessor inv = (InventoryAccessor) player.getInventory();
        inv.setSelectedSlot(STATE.previousSlot);
        STATE.reset();
    }

    private static class ModState {
        boolean isTntEquipped = false;
        int previousSlot = -1;
        int ticksSinceEquip = 0;
        boolean hasClicked = false;
        void reset() { isTntEquipped = false; previousSlot = -1; ticksSinceEquip = 0; hasClicked = false; }
    }
}