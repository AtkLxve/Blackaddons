package org.blackum.blackaddons.gui.screen;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.*;

public class ModOrganizer {

    public static class ModGroup {
        public final String groupName;
        public final String parentId;
        public final List<ModInfo> mods = new ArrayList<>();
        public boolean allSelected = false;

        public ModGroup(String groupName, String parentId) {
            this.groupName = groupName;
            this.parentId = parentId;
        }
    }

    public static class ModInfo {
        public final ModContainer mod;
        public final String id;
        public final String name;
        public final boolean isLibrary;
        public String parentId;
        public final Set<String> dependencies = new HashSet<>();
        public final Set<String> dependents = new HashSet<>();

        public ModInfo(ModContainer mod) {
            this.mod = mod;
            this.id = mod.getMetadata().getId();
            this.name = mod.getMetadata().getName();

            String type = mod.getMetadata().getType();
            boolean basicLibCheck = "builtin".equals(type) ||
                    id.contains("library") ||
                    id.contains("api") ||
                    id.contains("lib") ||
                    name.toLowerCase().contains("library") ||
                    name.toLowerCase().contains("api") ||
                    id.contains("kotlin") ||
                    name.toLowerCase().contains("kotlin");

            this.isLibrary = basicLibCheck;

            this.parentId = findParent(mod);

            for (ModDependency dep : mod.getMetadata().getDependencies()) {
                if (dep.getKind() == ModDependency.Kind.DEPENDS ||
                        dep.getKind() == ModDependency.Kind.RECOMMENDS) {
                    dependencies.add(dep.getModId());
                }
            }
        }

        private String findParent(ModContainer mod) {
            ModMetadata metadata = mod.getMetadata();
            String id = metadata.getId();

            CustomValue modMenuValue = metadata.getCustomValue("modmenu");
            if (modMenuValue != null && modMenuValue.getType() == CustomValue.CvType.OBJECT) {
                CustomValue.CvObject modMenuObject = modMenuValue.getAsObject();
                CustomValue parentCv = modMenuObject.get("parent");
                if (parentCv != null) {
                    if (parentCv.getType() == CustomValue.CvType.STRING) {
                        return parentCv.getAsString();
                    } else if (parentCv.getType() == CustomValue.CvType.OBJECT) {
                        CustomValue.CvObject parentObj = parentCv.getAsObject();
                        CustomValue idCv = parentObj.get("id");
                        if (idCv != null && idCv.getType() == CustomValue.CvType.STRING) {
                            return idCv.getAsString();
                        }
                    }
                }
            }

            boolean isGenerated = metadata.containsCustomValue("fabric-loom:generated") &&
                    metadata.getCustomValue("fabric-loom:generated").getType() == CustomValue.CvType.BOOLEAN &&
                    metadata.getCustomValue("fabric-loom:generated").getAsBoolean();

            if (isGenerated && mod.getContainingMod().isPresent()) {
                return mod.getContainingMod().get().getMetadata().getId();
            }

            if (id.startsWith("fabric") && metadata.containsCustomValue("fabric-api:module-lifecycle")) {
                if (FabricLoader.getInstance().isModLoaded("fabric-api")) {
                    return "fabric-api";
                } else if (FabricLoader.getInstance().isModLoaded("fabric")) {
                    return "fabric";
                }
            }

            return null;
        }
    }

    public static class OrganizedMods {
        public final List<ModGroup> modGroups = new ArrayList<>();
        public final List<ModGroup> libraryGroups = new ArrayList<>();
        public final Map<String, ModInfo> allMods = new HashMap<>();
        public final Map<String, ModGroup> groupMap = new HashMap<>();
    }

    public static OrganizedMods organizeMods() {
        OrganizedMods result = new OrganizedMods();

        List<ModInfo> allModInfos = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if ("builtin".equals(mod.getMetadata().getType())) {
                continue;
            }
            ModInfo info = new ModInfo(mod);
            allModInfos.add(info);
            result.allMods.put(info.id, info);
        }

        for (ModInfo info : allModInfos) {
            for (String depId : info.dependencies) {
                ModInfo dep = result.allMods.get(depId);
                if (dep != null) {
                    dep.dependents.add(info.id);
                }
            }
        }

        boolean fabricApiLoaded = result.allMods.containsKey("fabric-api");

        for (ModInfo info : allModInfos) {
            String parentId = info.parentId;

            if (parentId != null && !result.allMods.containsKey(parentId)) {
                parentId = null;
            }

            String groupKey = (parentId == null) ? info.id : parentId;

            boolean isGroupLibrary = false;
            ModInfo parentInfo = result.allMods.get(groupKey);
            if (parentInfo != null) {
                isGroupLibrary = parentInfo.isLibrary;
            } else {
                isGroupLibrary = info.isLibrary;
            }

            if ("fabric-api".equals(groupKey) || "fabric".equals(groupKey)) {
                isGroupLibrary = true;
            }

            ModGroup group;
            if (result.groupMap.containsKey(groupKey)) {
                group = result.groupMap.get(groupKey);
            } else {
                String groupName = (parentInfo != null) ? parentInfo.name : info.name;
                if ("fabric-api".equals(groupKey) && parentInfo == null && fabricApiLoaded) {
                    groupName = result.allMods.get("fabric-api").name;
                }

                group = new ModGroup(groupName, groupKey);
                result.groupMap.put(groupKey, group);

                if (isGroupLibrary) {
                    result.libraryGroups.add(group);
                } else {
                    result.modGroups.add(group);
                }
            }

            group.mods.add(info);
        }

        result.modGroups.sort(Comparator.comparing(g -> g.groupName.toLowerCase()));
        result.libraryGroups.sort(Comparator.comparing(g -> g.groupName.toLowerCase()));

        for (ModGroup group : result.modGroups) {
            sortGroupMods(group);
        }
        for (ModGroup group : result.libraryGroups) {
            sortGroupMods(group);
        }

        return result;
    }

    private static void sortGroupMods(ModGroup group) {
        group.mods.sort((m1, m2) -> {
            if (m1.id.equals(group.parentId))
                return -1;
            if (m2.id.equals(group.parentId))
                return 1;
            return m1.name.compareToIgnoreCase(m2.name);
        });
    }
}
