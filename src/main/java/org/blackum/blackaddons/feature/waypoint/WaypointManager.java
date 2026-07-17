package org.blackum.blackaddons.feature.waypoint;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.config.ConfigManager.WaypointAction;
import org.blackum.blackaddons.common.config.ProfileManager;
import org.blackum.blackaddons.common.constants.Constants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.util.concurrent.Executors;

public class WaypointManager {
    private static WaypointManager instance;
    private static final Path OLD_CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME);
    private static final File OLD_WAYPOINTS_FILE = OLD_CONFIG_DIR.resolve(Constants.WAYPOINTS_FILE_NAME).toFile();
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Blackaddons-WaypointSave");
        thread.setDaemon(true);
        return thread;
    });

    public static class WaypointData {
        public List<Waypoint> waypoints = new ArrayList<>();
        public List<WaypointGroup> groups = new ArrayList<>();
    }

    private static File getWaypointsFile() {
        return ProfileManager.getActiveProfileFile(ProfileManager.Category.WAYPOINTS);
    }

    private static File getGroupsFile() {
        File waypointsFile = getWaypointsFile();
        String name = waypointsFile.getName().replace(".json", "_groups.json");
        return new File(waypointsFile.getParentFile(), name);
    }

    private final List<Waypoint> waypoints = new ArrayList<>();
    private final List<WaypointGroup> groups = new ArrayList<>();

    public void resetToDefaults() {
        waypoints.clear();
        groups.clear();
        save();
    }

    private WaypointManager() {
        load();
    }

    public static WaypointManager getInstance() {
        if (instance == null) {
            instance = new WaypointManager();
        }
        return instance;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public List<WaypointGroup> getGroups() {
        return groups;
    }

    public WaypointGroup getGroup(UUID id) {
        if (id == null) return null;
        for (WaypointGroup group : groups) {
            if (id.equals(group.id)) return group;
        }
        return null;
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
        save();
    }

    public void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint);
        save();
    }

    public void addGroup(WaypointGroup group) {
        groups.add(group);
        save();
    }

    public void removeGroup(WaypointGroup group) {
        for (Waypoint wp : waypoints) {
            if (group.id.equals(wp.groupId)) {
                wp.groupId = null;
            }
        }
        for (WaypointGroup g : groups) {
            if (group.id.equals(g.parentId)) {
                g.parentId = null;
            }
        }
        groups.remove(group);
        save();
    }

    public List<Waypoint> getWaypointsForGroup(UUID groupId) {
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint wp : waypoints) {
            if (groupId == null ? wp.groupId == null : groupId.equals(wp.groupId)) {
                result.add(wp);
            }
        }
        return result;
    }

    public List<WaypointGroup> getSubGroups(UUID parentId) {
        List<WaypointGroup> result = new ArrayList<>();
        for (WaypointGroup group : groups) {
            if (parentId == null ? group.parentId == null : parentId.equals(group.parentId)) {
                result.add(group);
            }
        }
        return result;
    }

    public void save() {
        WaypointData data = new WaypointData();
        data.waypoints.addAll(new ArrayList<>(waypoints));
        data.groups.addAll(new ArrayList<>(groups));
        
        SAVE_EXECUTOR.submit(() -> {
            try {
                File waypointsFile = getWaypointsFile();
                ensureParent(waypointsFile);
                try (FileWriter writer = new FileWriter(waypointsFile)) {
                    Constants.GSON.toJson(data, writer);
                }
            } catch (IOException e) {
                Blackaddons.LOGGER.error("Failed to save waypoints", e);
            }
        });
    }

    private static void ensureParent(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    public void load() {
        migrate();
        File waypointsFile = getWaypointsFile();
        if (!waypointsFile.exists()) return;

        try (FileReader reader = new FileReader(waypointsFile)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonArray()) {
                List<Waypoint> loaded = Constants.GSON.fromJson(element, new TypeToken<List<Waypoint>>() {}.getType());
                if (loaded != null) {
                    waypoints.clear();
                    waypoints.addAll(loaded);
                }
                loadLegacyGroups();
                save();
                File groupsFile = getGroupsFile();
                if (groupsFile.exists()) groupsFile.delete();
            } else if (element.isJsonObject()) {
                WaypointData data = Constants.GSON.fromJson(element, WaypointData.class);
                if (data != null) {
                    waypoints.clear();
                    waypoints.addAll(data.waypoints);
                    groups.clear();
                    groups.addAll(data.groups);
                    sanitizeGroups();
                }
            }

            for (Waypoint waypoint : waypoints) {
                if (waypoint.actions != null) {
                    for (WaypointAction action : waypoint.actions) {
                        ConfigManager.normalizeActionSteps(action.actions);
                    }
                }
            }
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Failed to load waypoints", e);
        }
    }

    private void sanitizeGroups() {
        boolean changed = false;
        for (WaypointGroup group : groups) {
            if (group.id.equals(group.parentId)) {
                group.parentId = null;
                changed = true;
            }
        }
        if (changed) save();
    }

    private void loadLegacyGroups() {
        File groupsFile = getGroupsFile();
        if (!groupsFile.exists()) return;
        try (FileReader reader = new FileReader(groupsFile)) {
            List<WaypointGroup> loaded = Constants.GSON.fromJson(reader, new TypeToken<List<WaypointGroup>>() {}.getType());
            if (loaded != null) {
                groups.clear();
                groups.addAll(loaded);
            }
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Failed to load legacy groups", e);
            try {
                File backupFile = new File(groupsFile.getParentFile(), groupsFile.getName() + ".corrupted");
                groupsFile.renameTo(backupFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void migrate() {
        File targetFile = getWaypointsFile();
        if (OLD_WAYPOINTS_FILE.exists() && !targetFile.exists()) {
            try {
                ensureParent(targetFile);
                if (OLD_WAYPOINTS_FILE.renameTo(targetFile)) {
                    Blackaddons.LOGGER.info("Successfully migrated waypoints.json to default profile");
                }
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to migrate waypoints.json", e);
            }
        }
    }

    public void mergeActions(Map<UUID, List<WaypointAction>> actionsMap) {
        boolean changed = false;
        for (Waypoint waypoint : waypoints) {
            List<WaypointAction> actions = actionsMap.get(waypoint.id);
            if (actions != null && !actions.isEmpty()) {
                for (WaypointAction action : actions) {
                    ConfigManager.normalizeActionSteps(action.actions);
                }
                if (waypoint.actions == null) waypoint.actions = new ArrayList<>();
                waypoint.actions.addAll(actions);
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }
}
