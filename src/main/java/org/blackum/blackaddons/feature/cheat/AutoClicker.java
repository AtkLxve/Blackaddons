package org.blackum.blackaddons.feature.cheat;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.lwjgl.glfw.GLFW;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.common.util.accessor.KeyBindingAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@AutoModule(order = 104)
public class AutoClicker {
    private static final String BUTTON_LEFT = "left";
    private static final String BUTTON_RIGHT = "right";
    private static final long MS_PER_SECOND = 1000L;
    private static final int MIN_CPS_LIMIT = 1;
    private static final int MAX_CPS_LIMIT = 20;

    private static long nextClickTime = 0L;
    private static boolean active = false;
    private static boolean lastKeybindHeld = false;

    private static boolean wasHolding = false;

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(AutoClicker::onClientTick);
    }

    private static void onClientTick(Minecraft mc) {
        FeatureConfig config = ConfigManager.data.autoClickerConfig;

        if (!config.enabled || mc.player == null || McCompat.getScreen(mc) != null) {
            deactivate(mc);
            return;
        }

        updateActiveState(config, mc);

        if (!active || !passesGroundCheck(config, mc) || !passesItemFilter(config, mc) || !passesLookAtFilter(config, mc)) {
            if (wasHolding) {
                releaseAllButtons(mc);
                wasHolding = false;
            }
            return;
        }

        if (config.holdClick) {
            setButtonsState(config, mc, true);
            wasHolding = true;
        } else {
            if (wasHolding) {
                releaseAllButtons(mc);
                wasHolding = false;
            }
            long now = System.currentTimeMillis();
            if (now >= nextClickTime) {
                fireClicks(config, mc);
                scheduleNextClick(config);
            }
        }
    }

    private static void deactivate(Minecraft mc) {
        if (wasHolding) {
            releaseAllButtons(mc);
            wasHolding = false;
        }
        active = false;
        lastKeybindHeld = false;
        nextClickTime = 0L;
    }

    private static void releaseAllButtons(Minecraft mc) {
        for (String button : List.of(BUTTON_LEFT, BUTTON_RIGHT)) {
            KeyMapping key = resolveKey(button, mc);
            if (key instanceof KeyBindingAccessor accessor) {
                boolean physicalDown = isPhysicalKeyDown(mc, accessor.getBoundKey());
                KeyMapping.set(accessor.getBoundKey(), physicalDown);
                accessor.setBlackaddonsIsDown(physicalDown);
            }
        }
    }

    private static boolean isPhysicalKeyDown(Minecraft mc, InputConstants.Key key) {
        if (mc == null || mc.getWindow() == null || key == null) return false;
        if (key.getType() == InputConstants.Type.MOUSE) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().handle(), key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } else if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(mc.getWindow(), key.getValue());
        }
        return false;
    }

    private static void setButtonsState(FeatureConfig config, Minecraft mc, boolean pressed) {
        List<String> buttons = config.buttons;
        if (buttons == null || buttons.isEmpty()) return;
        for (String button : buttons) {
            KeyMapping key = resolveKey(button, mc);
            if (key instanceof KeyBindingAccessor accessor) {
                KeyMapping.set(accessor.getBoundKey(), pressed);
                accessor.setBlackaddonsIsDown(pressed);
            }
        }
    }

    private static void updateActiveState(FeatureConfig config, Minecraft mc) {
        if (config.keybindKeyCode < 0) {
            active = true;
            return;
        }

        boolean currentlyHeld;
        if (config.keybindKeyCode >= 1000) {
            currentlyHeld = GLFW.glfwGetMouseButton(mc.getWindow().handle(), config.keybindKeyCode - 1000) == GLFW.GLFW_PRESS;
        } else {
            currentlyHeld = InputConstants.isKeyDown(mc.getWindow(), config.keybindKeyCode);
        }

        if (config.holdMode) {
            active = currentlyHeld;
            lastKeybindHeld = currentlyHeld;
        } else {
            if (currentlyHeld && !lastKeybindHeld) {
                active = !active;
                if (!active) nextClickTime = 0L;
            }
            lastKeybindHeld = currentlyHeld;
        }
    }

    private static boolean passesGroundCheck(FeatureConfig config, Minecraft mc) {
        return !config.groundOnly || mc.player.onGround();
    }

    private static boolean passesItemFilter(FeatureConfig config, Minecraft mc) {
        if (!config.itemFilterEnabled) return true;
        if (config.itemFilters == null || config.itemFilters.isEmpty()) return true;
        String heldName = mc.player.getMainHandItem().getHoverName().getString().toLowerCase();
        for (String filter : config.itemFilters) {
            if (heldName.contains(filter.toLowerCase())) return true;
        }
        return false;
    }

    private static boolean passesLookAtFilter(FeatureConfig config, Minecraft mc) {
        if (!config.lookAtFilterEnabled) return true;
        if (config.lookAtTargets == null || config.lookAtTargets.isEmpty()) return true;

        HitResult hit = getLookAtHitResult(config, mc);
        if (hit == null) return false;

        String keyStr = null;
        if (hit.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) hit).getEntity();
            Object entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (entityKey != null) {
                keyStr = entityKey.toString();
            }
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            Block block = mc.level.getBlockState(blockHit.getBlockPos()).getBlock();
            Object blockKey = BuiltInRegistries.BLOCK.getKey(block);
            if (blockKey != null) {
                keyStr = blockKey.toString();
            }
        }

        if (keyStr == null) return false;

        for (String t : config.lookAtTargets) {
            if (t.trim().equalsIgnoreCase(keyStr)) return true;
        }
        return false;
    }

    private static HitResult getLookAtHitResult(FeatureConfig config, Minecraft mc) {
        double dist = config.lookAtFilterDistance;
        
        HitResult blockHit = mc.player.pick(dist, 0.0f, false);
        double blockDistSq = dist * dist;
        if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
            blockDistSq = blockHit.getLocation().distanceToSqr(mc.player.getEyePosition(0.0f));
        }
        
        Vec3 eyePos = mc.player.getEyePosition(0.0f);
        Vec3 viewVec = mc.player.getViewVector(0.0f);
        Vec3 endPos = eyePos.add(viewVec.scale(dist));
        
        AABB searchBox = mc.player.getBoundingBox()
                .expandTowards(viewVec.x * dist, viewVec.y * dist, viewVec.z * dist)
                .inflate(1.0D, 1.0D, 1.0D);
                
        Entity closestEntity = null;
        Vec3 closestHitVec = null;
        double closestDistSq = blockDistSq;
        
        for (Entity entity : mc.level.getEntities(mc.player, searchBox, e -> e != null && !e.isSpectator() && e.isPickable())) {
            AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
            java.util.Optional<Vec3> clipResult = aabb.clip(eyePos, endPos);
            if (aabb.contains(eyePos)) {
                if (closestDistSq >= 0.0D) {
                    closestEntity = entity;
                    closestHitVec = clipResult.orElse(eyePos);
                    closestDistSq = 0.0D;
                }
            } else if (clipResult.isPresent()) {
                Vec3 hitVec = clipResult.get();
                double distSq = eyePos.distanceToSqr(hitVec);
                if (distSq < closestDistSq) {
                    closestEntity = entity;
                    closestHitVec = hitVec;
                    closestDistSq = distSq;
                }
            }
        }
        
        if (closestEntity != null) {
            return new EntityHitResult(closestEntity, closestHitVec);
        }
        
        return blockHit;
    }

    private static void fireClicks(FeatureConfig config, Minecraft mc) {
        List<String> buttons = config.buttons;
        if (buttons == null || buttons.isEmpty()) return;

        for (String button : buttons) {
            KeyMapping key = resolveKey(button, mc);
            if (key instanceof KeyBindingAccessor accessor) {
                KeyMapping.click(accessor.getBoundKey());
            }
        }
    }

    private static KeyMapping resolveKey(String button, Minecraft mc) {
        if (BUTTON_LEFT.equals(button)) return mc.options.keyAttack;
        if (BUTTON_RIGHT.equals(button)) return mc.options.keyUse;
        return null;
    }

    private static void scheduleNextClick(FeatureConfig config) {
        float minCps = Math.max(MIN_CPS_LIMIT, Math.min(config.minCps, config.maxCps));
        float maxCps = Math.max(minCps, Math.min(config.maxCps, MAX_CPS_LIMIT));
        float cps = ThreadLocalRandom.current().nextFloat(minCps, maxCps + 0.001f);
        long intervalMs = (long) (MS_PER_SECOND / cps);
        nextClickTime = System.currentTimeMillis() + intervalMs;
    }

    public static boolean isActive() {
        return active && ConfigManager.data.autoClickerConfig.enabled;
    }

    public static class FeatureConfig {
        public boolean enabled = false;
        public List<String> buttons = new ArrayList<>(List.of("left"));
        public float minCps = 5.0f;
        public float maxCps = 8.0f;
        public boolean holdMode = false;
        public boolean holdClick = false;
        public boolean groundOnly = false;
        public boolean lookAtFilterEnabled = false;
        public List<String> lookAtTargets = new ArrayList<>();
        public String lookAtMode = "mob";
        public float lookAtFilterDistance = 3.5f;
        public boolean itemFilterEnabled = false;
        public List<String> itemFilters = new ArrayList<>();
        public int keybindKeyCode = -1;
    }
}
