package org.blackum.blackaddons.gui.screen.main.tabs;


import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.feature.WaypointEditScreen;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.feature.waypoint.Waypoint;
import org.blackum.blackaddons.feature.waypoint.WaypointDragState;
import org.blackum.blackaddons.feature.waypoint.WaypointGroup;
import org.blackum.blackaddons.feature.waypoint.WaypointManager;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.gui.screen.feature.WaypointEditScreen;
import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;
import org.blackum.blackaddons.gui.widget.editor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WaypointsTabController extends SimpleTabController {

    private static int lastScrollOffset = 0;
    private final WaypointDragState dragState = new WaypointDragState();

    private ListView waypointList;

    public WaypointsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab waypointsTab) {
        int contentX = waypointsTab.getParent().getContentX();
        int contentY = waypointsTab.getParent().getContentY();
        int contentWidth = waypointsTab.getParent().getContentWidth();
        int contentHeight = waypointsTab.getParent().getContentHeight();

        waypointList = new ListView(contentX + Theme.PADDING, contentY + Theme.PADDING, contentWidth - Theme.PADDING * 2, contentHeight - Theme.PADDING * 2);
        waypointList.setScrollOffset(lastScrollOffset);
        waypointsTab.addWidget(waypointList);

        rebuildList();
    }

    private void updateDragPreview(double mouseY) {
        List<Widget> items = waypointList.getItems();
        Widget draggedWidget = null;

        for (Widget w : items) {
            if (w == dragState.draggedCard) {
                draggedWidget = w;
                break;
            }
            if (w instanceof ExpandableGroup && (((ExpandableGroup)w).getHeader() == dragState.draggedCard || ((ExpandableGroup)w).getChildren().contains(dragState.draggedCard))) {
                draggedWidget = w;
                break;
            }
        }
        
        if (draggedWidget == null) return;
        
        Widget target = findTargetWidget(items, mouseY, dragState.draggedCard);
        if (target == null) return;

        int targetIdx = -1;
        ExpandableGroup targetGroupContainer = null;
        for (int i = 0; i < items.size(); i++) {
            Widget w = items.get(i);
            if (w == target) {
                targetIdx = i;
                break;
            }
            if (w instanceof ExpandableGroup) {
                ExpandableGroup eg = (ExpandableGroup) w;
                if (eg.getHeader() == target || eg.getChildren().contains(target)) {
                    targetIdx = i;
                    targetGroupContainer = eg;
                    break;
                }
            }
        }
        
        if (targetIdx != -1) {
            int draggedIdx = items.indexOf(draggedWidget);
            if (draggedIdx == -1) return;

            double relativeY = (mouseY - target.getY()) / target.getHeight();

            boolean shouldSwap = false;
            if (targetIdx < draggedIdx) {
                if (relativeY < 0.3) shouldSwap = true;
            } else if (targetIdx > draggedIdx) {
                if (relativeY > 0.7) shouldSwap = true;
            }

            if (shouldSwap) {
                items.remove(draggedIdx);
                items.add(targetIdx, draggedWidget);
            }
        }
    }

    private void rebuildList() {
        if (waypointList == null) return;

        int itemWidth = waypointList.getWidth() - 16;
        lastScrollOffset = waypointList.getScrollOffset();
        waypointList.clearItems();

        Button addGroupBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "+ Add Group", () -> {
            WaypointGroup group = new WaypointGroup("New Group");
            WaypointManager.getInstance().addGroup(group);
            rebuildList();
        });
        waypointList.addItem(addGroupBtn);

        addGroupsRecursively(null, 0, null);

        WaypointManager mgr = WaypointManager.getInstance();
        List<Waypoint> ungrouped = mgr.getWaypointsForGroup(null);
        if (!ungrouped.isEmpty()) {
            SectionHeader ungroupedHeader = new SectionHeader(itemWidth, "Ungrouped");
            waypointList.addItem(ungroupedHeader);
            for (Waypoint wp : ungrouped) {
                WaypointCard card = new WaypointCard(wp, screen, this::rebuildList);
                card.setDragState(dragState, (mouseY) -> handleDrop(wp, mouseY));
                waypointList.addItem(card);
            }
        }

        Button addUngroupedBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "+ Add Waypoint (Ungrouped)", () -> {
            Minecraft mc = Minecraft.getInstance();
            double x = 0, y = 0, z = 0;
            String dim = null;
            if (mc.player != null && mc.level != null) {
                x = mc.player.getX();
                y = mc.player.getY();
                z = mc.player.getZ();
                dim = McCompat.dimensionId(mc.level.dimension());
            }
            final double fx = x, fy = y, fz = z;
            final String fdim = dim;
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new WaypointEditScreen(screen, createWaypoint(fx, fy, fz, fdim, null), wp -> {
                    mgr.addWaypoint(wp);
                    rebuildList();
                }));
            }
        });
        waypointList.addItem(addUngroupedBtn);

        waypointList.setScrollOffset(lastScrollOffset);
    }

    private void addGroupsRecursively(UUID parentId, int level, ExpandableGroup parentContainer) {
        if (level > 10) return;
        WaypointManager mgr = WaypointManager.getInstance();
        int itemWidth = waypointList.getWidth() - 16;
        Minecraft mc = Minecraft.getInstance();

        for (WaypointGroup group : mgr.getSubGroups(parentId)) {
            WaypointGroupCard groupCard = new WaypointGroupCard(group, screen, this::rebuildList);
            groupCard.setDragState(dragState, (mouseY) -> handleDrop(group, mouseY));
            groupCard.setIndent(level * 20);
            
            ExpandableGroup groupContainer = new ExpandableGroup(0, 0, itemWidth, groupCard, !group.collapsed);
            groupCard.setToggleCallback(() -> {
                groupContainer.setExpanded(!group.collapsed);
            });
            
            if (parentContainer != null) {
                parentContainer.addChild(groupContainer);
            } else {
                waypointList.addItem(groupContainer);
            }

            List<Waypoint> groupWaypoints = mgr.getWaypointsForGroup(group.id);
            for (Waypoint wp : groupWaypoints) {
                WaypointCard card = new WaypointCard(wp, screen, this::rebuildList);
                card.setDragState(dragState, (mouseY) -> handleDrop(wp, mouseY));
                card.setIndent(20);
                groupContainer.addChild(card);
            }

            Button addWpBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "+ Add Waypoint", () -> {
                double x = 0, y = 0, z = 0;
                String dim = null;
                if (mc.player != null && mc.level != null) {
                    x = mc.player.getX();
                    y = mc.player.getY();
                    z = mc.player.getZ();
                    dim = McCompat.dimensionId(mc.level.dimension());
                }
                final double fx = x, fy = y, fz = z;
                final String fdim = dim;
                final UUID groupId = group.id;
                if (Blackaddons.screenOpener != null) {
                    Blackaddons.screenOpener.accept(new WaypointEditScreen(screen, createWaypoint(fx, fy, fz, fdim, groupId), wp -> {
                        mgr.addWaypoint(wp);
                        rebuildList();
                    }));
                }
            });
            groupContainer.addChild(addWpBtn);

            addGroupsRecursively(group.id, level + 1, groupContainer);
        }
    }

    private void handleDrop(Object dragged, double mouseY) {
        dragState.reset();
        WaypointManager mgr = WaypointManager.getInstance();

        Widget targetWidget = findTargetWidget(waypointList.getItems(), mouseY, null);
        if (targetWidget == null || isOurOwnComponent(targetWidget)) {
            List<Widget> others = new ArrayList<>(waypointList.getItems());
            others.removeIf(this::isOurOwnComponent);
            targetWidget = findTargetWidget(others, mouseY, null);
        }
        
        if (dragged instanceof Waypoint) {
            Waypoint wp = (Waypoint) dragged;
            if (targetWidget instanceof WaypointCard) {
                Waypoint targetWp = ((WaypointCard) targetWidget).getWaypoint();
                if (wp == targetWp) return;
                
                wp.groupId = targetWp.groupId;
                mgr.getWaypoints().remove(wp);
                int idx = mgr.getWaypoints().indexOf(targetWp);
                if (idx == -1) {
                    mgr.getWaypoints().add(wp);
                } else {
                    boolean after = mouseY > targetWidget.getY() + targetWidget.getHeight() / 2;
                    mgr.getWaypoints().add(after ? idx + 1 : idx, wp);
                }
            } else if (targetWidget instanceof WaypointGroupCard) {
                wp.groupId = ((WaypointGroupCard) targetWidget).getGroup().id;
                mgr.getWaypoints().remove(wp);
                mgr.getWaypoints().add(0, wp);
            } else {
                wp.groupId = null;
                mgr.getWaypoints().remove(wp);
                mgr.getWaypoints().add(wp);
            }
        } else if (dragged instanceof WaypointGroup) {
            WaypointGroup grp = (WaypointGroup) dragged;
            if (targetWidget instanceof WaypointGroupCard) {
                WaypointGroup targetGrp = ((WaypointGroupCard) targetWidget).getGroup();
                if (grp == targetGrp) return;
                
                double relativeY = (mouseY - targetWidget.getY()) / targetWidget.getHeight();
                
                if (relativeY > 0.25 && relativeY < 0.75 && !isDescendant(grp.id, targetGrp.id)) {
                    grp.parentId = targetGrp.id;
                } else {
                    grp.parentId = targetGrp.parentId;
                    mgr.getGroups().remove(grp);
                    int idx = mgr.getGroups().indexOf(targetGrp);
                    if (idx == -1) {
                        mgr.getGroups().add(grp);
                    } else {
                        boolean after = relativeY >= 0.75;
                        mgr.getGroups().add(after ? idx + 1 : idx, grp);
                    }
                }
            } else if (targetWidget instanceof WaypointCard) {
                Waypoint targetWp = ((WaypointCard) targetWidget).getWaypoint();
                if (targetWp.groupId != null && isDescendant(grp.id, targetWp.groupId)) return;
                if (grp.id.equals(targetWp.groupId)) return;
                
                grp.parentId = targetWp.groupId;
                mgr.getGroups().remove(grp);
                mgr.getGroups().add(0, grp);
            } else {
                grp.parentId = null;
                mgr.getGroups().remove(grp);
                mgr.getGroups().add(grp);
            }
        }
        
        mgr.save();
        syncManagerWithList();
        rebuildList();
    }

    private void syncManagerWithList() {
        WaypointManager mgr = WaypointManager.getInstance();
        List<WaypointGroup> newGroups = new ArrayList<>();
        
        for (Widget w : waypointList.getItems()) {
            if (w instanceof ExpandableGroup) {
                Widget h = ((ExpandableGroup)w).getHeader();
                if (h instanceof WaypointGroupCard) {
                    WaypointGroup grp = ((WaypointGroupCard)h).getGroup();
                    newGroups.add(grp);
                }
            }
        }
        
        if (!newGroups.isEmpty()) {
            List<WaypointGroup> allGroups = mgr.getGroups();
            for (WaypointGroup grp : newGroups) {
                allGroups.remove(grp);
            }
            allGroups.addAll(newGroups);
        }
    }

    private Widget findTargetWidget(List<Widget> widgets, double mouseY, Widget exclude) {
        for (Widget widget : widgets) {
            if (widget == exclude) continue;
            if (mouseY >= widget.getY() && mouseY <= widget.getY() + widget.getHeight()) {
                if (widget instanceof ExpandableGroup) {
                    ExpandableGroup eg = (ExpandableGroup) widget;
                    Widget header = eg.getHeader();
                    if (header == exclude) {
                    } else if (mouseY >= header.getY() && mouseY <= header.getY() + header.getHeight()) {
                        return header;
                    }
                    Widget childTarget = findTargetWidget(eg.getChildren(), mouseY, exclude);
                    if (childTarget != null) return childTarget;
                    return header != exclude ? header : null;
                }
                return widget;
            }
        }
        return null;
    }

    private boolean isOurOwnComponent(Widget w) {
        if (w == dragState.draggedCard) return true;
        if (w instanceof ExpandableGroup) {
            return ((ExpandableGroup)w).getHeader() == dragState.draggedCard;
        }
        return false;
    }

    private boolean isDescendant(UUID potentialParent, UUID targetId) {
        return isDescendant(potentialParent, targetId, 0);
    }

    private boolean isDescendant(UUID potentialParent, UUID targetId, int depth) {
        if (depth > 10 || targetId == null) return false;
        if (targetId.equals(potentialParent)) return true;
        WaypointGroup target = WaypointManager.getInstance().getGroup(targetId);
        if (target == null) return false;
        return isDescendant(potentialParent, target.parentId, depth + 1);
    }

    private static Waypoint createWaypoint(double x, double y, double z, String dim, UUID groupId) {
        Waypoint wp = new Waypoint("", x, y, z, dim);
        wp.groupId = groupId;
        return wp;
    }

    @Override
    public void tick() {
        if (dragState.active) {
            Minecraft mc = Minecraft.getInstance();
            double scale = RenderHelper.getGuiScaleFactor();
            double windowHeight = mc.getWindow().getScreenHeight();
            double rawMouseY = (mc.mouseHandler.ypos() * (screen.height / windowHeight)) / scale;
            
            updateDragPreview(rawMouseY);
        }

        if (waypointList != null) {
            lastScrollOffset = waypointList.getScrollOffset();
        }
    }
}
