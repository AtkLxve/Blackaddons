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
import net.minecraft.world.phys.HitResult;
import org.blackum.blackaddons.mixin.client.KeyBindingAccessor;
import org.blackum.blackaddons.mixin.client.InventoryAccessor;

import java.util.List;

public class AutoTNT {
    private static final List<Block> TARGET_BLOCKS = List.of(
            Blocks.CRACKED_STONE_BRICKS,
            Blocks.STONE_SLAB
    );

    private static final ModState STATE = new ModState();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoTNT::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        if (!CheatsOptions.AutoTNTEnabled || client.player == null || client.level == null) {
            if (STATE.isTntEquipped) unequipTnt(client.player);
            return;
        }

        if (isLookingAtTargetBlock(client)) {
            int tntSlot = findTntHotbarSlot(client.player);

            if (tntSlot != -1) {
                equipTnt(client.player, tntSlot);

                if (STATE.isTntEquipped && !STATE.hasClicked) {
                    STATE.ticksSinceEquip++;

                    if (STATE.ticksSinceEquip >= STATE.currentRandomDelay) {
                        triggerAttack(client);
                        STATE.hasClicked = true;
                    }
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
        if (client.player != null) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static boolean isLookingAtTargetBlock(Minecraft client) {
        if (client.hitResult instanceof BlockHitResult blockHit && client.hitResult.getType() == HitResult.Type.BLOCK) {
            // Get the distance to the block
            double distance = client.player.distanceToSqr(blockHit.getLocation());

            double limit = 3.3;
            if (distance > (limit * limit)) return false;

            Block block = client.level.getBlockState(blockHit.getBlockPos()).getBlock();
            return TARGET_BLOCKS.contains(block);
        }
        return false;
    }

    private static int findTntHotbarSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String name = stack.getHoverName().getString().replaceAll("(?i)§[0-9A-FK-OR]", "").toLowerCase();
            if (name.contains("superboom") || name.contains("infinityboom")) {
                return i;
            }
        }
        return -1;
    }

    private static void equipTnt(Player player, int slot) {
        if (STATE.isTntEquipped) return;
        InventoryAccessor inv = (InventoryAccessor) player.getInventory();
        STATE.previousSlot = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        STATE.isTntEquipped = true;
        STATE.ticksSinceEquip = 0;
        STATE.hasClicked = false;
    }

    private static void unequipTnt(Player player) {
        if (!STATE.isTntEquipped || player == null) return;
        InventoryAccessor inv = (InventoryAccessor) player.getInventory();
        inv.setSelectedSlot(STATE.previousSlot);
        STATE.reset();
    }

    private static class ModState {
        boolean isTntEquipped = false;
        int previousSlot = -1;
        int ticksSinceEquip = 0;
        int currentRandomDelay = 5;
        boolean hasClicked = false;

        void reset() {
            isTntEquipped = false;
            previousSlot = -1;
            ticksSinceEquip = 0;
            hasClicked = false;
            this.currentRandomDelay = CheatsOptions.AutoTNTDelay + (Math.random() > 0.5 ? 1 : 0);
        }
    }
}