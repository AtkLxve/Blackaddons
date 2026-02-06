package org.blackum.blackaddons.cheats;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.blackum.blackaddons.mixin.client.KeyBindingAccessor;
import org.blackum.blackaddons.mixin.client.InventoryAccessor;

import java.util.List;
import java.util.Random;

public class AutoTNT {
    private static final List<Block> TARGET_BLOCKS = List.of(
            Blocks.CRACKED_STONE_BRICKS,
            Blocks.SMOOTH_STONE_SLAB);

    private static final double BASE_DISTANCE_LIMIT = 3.3;
    private static final Random RANDOM = new Random();
    private static final ModState STATE = new ModState();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoTNT::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        if (!CheatsOptions.AutoTNTEnabled || client.player == null || client.level == null || client.screen != null) {
            return;
        }

        if (client.hitResult instanceof BlockHitResult blockHit && client.hitResult.getType() == HitResult.Type.BLOCK) {
            if (isTargetBlock(client, blockHit)) {
                if (!blockHit.getBlockPos().equals(STATE.lastTargetPos)) {
                    STATE.lastTargetPos = blockHit.getBlockPos();
                    if (true) {
                        STATE.ticksSinceEquip = 0;
                        STATE.updateDelays();
                    }
                }

                int tntSlot = findTntHotbarSlot(client.player);
                if (tntSlot != -1) {
                    if (!STATE.hasClicked) {
                        equipTnt(client.player, tntSlot);
                    }

                    if (STATE.isTntEquipped && !STATE.hasClicked) {
                        STATE.ticksSinceEquip++;
                        if (STATE.ticksSinceEquip >= STATE.currentRandomDelay) {
                            triggerAttack(client);
                            STATE.hasClicked = true;
                            if (CheatsOptions.SwapBack) {
                                unequipTnt(client.player, false);
                            }
                        }
                    }
                }
            } else {
                handleNotLooking(client.player);
            }
        } else {
            handleNotLooking(client.player);
        }
    }

    private static void handleNotLooking(Player player) {
        if (STATE.isTntEquipped || STATE.hasClicked) {
            STATE.ticksSinceStopLooking++;
            if (STATE.ticksSinceStopLooking >= STATE.unequipDelay) {
                if (STATE.isTntEquipped) {
                    unequipTnt(player, true);
                } else {
                    STATE.fullReset();
                }
            }
        }
    }

    private static void triggerAttack(Minecraft client) {
        if (client.options.keyAttack instanceof KeyBindingAccessor accessor) {
            KeyMapping.click(accessor.getBoundKey());
        }
    }

    private static boolean isTargetBlock(Minecraft client, BlockHitResult blockHit) {
        double distanceSq = client.player.distanceToSqr(blockHit.getLocation());
        double limit = STATE.currentDistanceLimit;

        if (distanceSq > (limit * limit))
            return false;

        Block block = client.level.getBlockState(blockHit.getBlockPos()).getBlock();
        return TARGET_BLOCKS.contains(block);
    }

    private static int findTntHotbarSlot(Player player) {
        if (STATE.lastKnownTntSlot != -1) {
            ItemStack stack = player.getInventory().getItem(STATE.lastKnownTntSlot);
            if (isTnt(stack))
                return STATE.lastKnownTntSlot;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isTnt(stack)) {
                STATE.lastKnownTntSlot = i;
                return i;
            }
        }
        STATE.lastKnownTntSlot = -1;
        return -1;
    }

    private static boolean isTnt(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        String name = stack.getHoverName().getString().replaceAll("(?i)§[0-9A-FK-OR]", "").toLowerCase();
        return name.contains("superboom") || name.contains("infinityboom");
    }

    private static void equipTnt(Player player, int slot) {
        if (STATE.isTntEquipped)
            return;
        InventoryAccessor inv = (InventoryAccessor) player.getInventory();
        STATE.originalItemSlot = inv.getBlackaddonsSelected();
        inv.setBlackaddonsSelected(slot);
        STATE.isTntEquipped = true;
        STATE.ticksSinceEquip = 0;
        STATE.ticksSinceStopLooking = 0;
    }

    private static void unequipTnt(Player player, boolean fullReset) {
        if (!STATE.isTntEquipped || player == null)
            return;

        InventoryAccessor inv = (InventoryAccessor) player.getInventory();
        if (CheatsOptions.SwapBack && STATE.originalItemSlot != -1) {
            if (STATE.originalItemSlot >= 0 && STATE.originalItemSlot < 9) {
                inv.setBlackaddonsSelected(STATE.originalItemSlot);
            }
        } else {
            int nonTnt = findNonTntHotbarSlot(player);
            if (nonTnt != -1) {
                inv.setBlackaddonsSelected(nonTnt);
            }
        }

        if (fullReset) {
            STATE.fullReset();
        } else {
            STATE.reset();
        }
    }

    private static int findNonTntHotbarSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            if (!isTnt(player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    public static List<String> getDebugInfo() {
        java.util.List<String> info = new java.util.ArrayList<>();
        if (!CheatsOptions.AutoTNTEnabled)
            return info;

        info.add("");
        info.add("§c[AutoTNT Debug]");

        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null && client.hitResult instanceof BlockHitResult blockHit
                && client.hitResult.getType() == HitResult.Type.BLOCK) {
            Block block = client.level.getBlockState(blockHit.getBlockPos()).getBlock();
            boolean isTarget = TARGET_BLOCKS.contains(block);
            double dist = client.player.distanceToSqr(blockHit.getLocation());
            info.add("Target: " + (isTarget ? "§aYES" : "§cNO") + " §r("
                    + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath() + ")");
            info.add("Distance: " + String.format("%.2f", dist) + " (Limit: "
                    + String.format("%.2f", STATE.currentDistanceLimit * STATE.currentDistanceLimit) + ")");
        } else {
            info.add("Target: None");
        }

        info.add("Equipped: " + STATE.isTntEquipped);
        info.add("Has Clicked: " + STATE.hasClicked);
        info.add("Ticks Eq: " + STATE.ticksSinceEquip + " / " + STATE.currentRandomDelay);
        info.add("Ticks Look: " + STATE.ticksSinceStopLooking + " / " + STATE.unequipDelay);

        return info;
    }

    private static class ModState {
        boolean isTntEquipped = false;
        boolean hasClicked = false;
        int ticksSinceEquip = 0;
        int ticksSinceStopLooking = 0;
        double currentRandomDelay = 0;
        double unequipDelay = 0;
        int originalItemSlot = -1;
        int lastKnownTntSlot = -1;
        net.minecraft.core.BlockPos lastTargetPos = null;
        double currentDistanceLimit = BASE_DISTANCE_LIMIT;

        ModState() {
            fullReset();
        }

        void reset() {
            isTntEquipped = false;
            ticksSinceEquip = 0;
            ticksSinceStopLooking = 0;
            originalItemSlot = -1;
            updateDelays();
        }

        void fullReset() {
            reset();
            hasClicked = false;
            lastKnownTntSlot = -1;
            lastTargetPos = null;
        }

        void updateDelays() {
            this.currentRandomDelay = CheatsOptions.AutoTNTDelay + RANDOM.nextInt(2);
            int baseUnequip = CheatsOptions.UnequipDelay;
            if (baseUnequip > 2) {
                this.unequipDelay = baseUnequip + RANDOM.nextInt(3) - 1;
            } else {
                this.unequipDelay = baseUnequip;
            }
            this.currentDistanceLimit = BASE_DISTANCE_LIMIT + (RANDOM.nextFloat() * 0.06 - 0.03);
        }
    }
}
