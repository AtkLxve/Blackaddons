package org.blackum.blackaddons.gui.screen.tabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;

public class TeammatesTabController extends ProfileTabController {

    private List<Teammate> allTeammates = new ArrayList<>();
    private String filterClass = "All";
    private String filterTime = "Any";
    private String filterSort = "Runs";

    private TextField searchField;
    private ListView teammatesList;
    private String lastSearchText = "";

    private static final int COL_IGN = 100;
    private static final int COL_RUNS = 40;
    private static final int COL_CLASS = 80;
    private static final int COL_FLOOR = 35;

    public TeammatesTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int controlsHeight = 20;
        int listMarginTop = 30;

        int startY = tab.getParent().getContentY();
        int contentWidth = tab.getParent().getContentWidth();

        searchField = new TextField(tab.getParent().getContentX(), startY, 120, controlsHeight, "Search IGN...");
        searchField.setCharFilter(c -> Character.isLetterOrDigit(c) || c == '_');
        tab.addWidget(searchField);

        int btnY = startY;
        int btnH = controlsHeight;
        int btnW = 75;
        int btnGap = 5;

        int currentBtnX = tab.getParent().getContentX() + 120 + btnGap;

        Button classBtn = new Button(currentBtnX, btnY, btnW, btnH, "Class: All", () -> {
            cycleClassFilter();
            updateTeammatesList();
        });
        tab.addWidget(classBtn);
        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                classBtn.setText("Class: " + filterClass);
            }

            @Override
            public void render(GuiGraphics g, int x, int y, float p) {
            }
        });
        currentBtnX += btnW + btnGap;

        Button timeBtn = new Button(currentBtnX, btnY, btnW, btnH, "Time: Any", () -> {
            cycleTimeFilter();
            updateTeammatesList();
        });
        tab.addWidget(timeBtn);
        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                timeBtn.setText("Time: " + filterTime);
            }

            @Override
            public void render(GuiGraphics g, int x, int y, float p) {
            }
        });
        currentBtnX += btnW + btnGap;

        Button sortBtn = new Button(currentBtnX, btnY, btnW, btnH, "Sort: Runs", () -> {
            cycleSortFilter();
            updateTeammatesList();
        });
        tab.addWidget(sortBtn);
        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                sortBtn.setText("Sort: " + filterSort);
            }

            @Override
            public void render(GuiGraphics g, int x, int y, float p) {
            }
        });

        int headerY = startY + controlsHeight + 10;
        int headerX = tab.getParent().getContentX();
        tab.addWidget(new Widget(headerX, headerY, contentWidth - 20, 15) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY,
                    float partialTick) {
                int x = this.x + 2;
                graphics.drawString(Minecraft.getInstance().font, "§7IGN", x, y + 4, 0xFFFFFFFF);
                x += COL_IGN;
                graphics.drawString(Minecraft.getInstance().font, "§7Runs", x, y + 4, 0xFFFFFFFF);
                x += COL_RUNS;
                graphics.drawString(Minecraft.getInstance().font, "§7Class", x, y + 4, 0xFFFFFFFF);
                x += COL_CLASS;
                graphics.drawString(Minecraft.getInstance().font, "§7Floor", x, y + 4, 0xFFFFFFFF);
                x += COL_FLOOR;
                graphics.drawString(Minecraft.getInstance().font, "§7Last Seen", x, y + 4, 0xFFFFFFFF);

                graphics.fill(this.x, this.y + 14, this.x + width, this.y + 15, 0x40FFFFFF);
            }
        });
        teammatesList = new ListView(tab.getParent().getContentX(), headerY + 15,
                contentWidth - 20, tab.getParent().getContentHeight() - controlsHeight - 10 - 15);
        teammatesList.setItemSpacing(0);
        tab.addWidget(teammatesList);

        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                if (searchField != null) {
                    String currentText = searchField.getText();
                    if (!currentText.equals(lastSearchText)) {
                        lastSearchText = currentText;
                        updateTeammatesList();
                    }
                }
            }

            @Override
            public void render(GuiGraphics g, int x, int y, float p) {
            }
        });

        if (profileData.has("teammates")) {
            JsonArray tmArray = profileData.getAsJsonArray("teammates");
            allTeammates.clear();
            for (JsonElement tmElem : tmArray) {
                if (tmElem.isJsonArray()) {
                    JsonArray tuple = tmElem.getAsJsonArray();
                    if (tuple.size() >= 2) {
                        String ign = tuple.get(0).getAsString();
                        JsonObject data = tuple.get(1).getAsJsonObject();
                        allTeammates.add(new Teammate(ign, data));
                    }
                }
            }
            updateTeammatesList();
        } else {
            addInfoRow(teammatesList, "No teammate data available.", "");
        }
    }

    private void cycleClassFilter() {
        switch (filterClass) {
            case "All" -> filterClass = "Archer";
            case "Archer" -> filterClass = "Berserk";
            case "Berserk" -> filterClass = "Healer";
            case "Healer" -> filterClass = "Mage";
            case "Mage" -> filterClass = "Tank";
            default -> filterClass = "All";
        }
    }

    private void cycleTimeFilter() {
        switch (filterTime) {
            case "Any" -> filterTime = "24h";
            case "24h" -> filterTime = "7d";
            case "7d" -> filterTime = "30d";
            default -> filterTime = "Any";
        }
    }

    private void cycleSortFilter() {
        filterSort = filterSort.equals("Runs") ? "Recent" : "Runs";
    }

    private void updateTeammatesList() {
        if (teammatesList == null)
            return;
        teammatesList.clearItems();

        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        long now = System.currentTimeMillis() / 1000;
        long timeThreshold = 0;

        if (filterTime.equals("24h"))
            timeThreshold = now - 86400;
        else if (filterTime.equals("7d"))
            timeThreshold = now - 604800;
        else if (filterTime.equals("30d"))
            timeThreshold = now - 2592000;

        List<Teammate> filtered = new ArrayList<>();
        for (Teammate tm : allTeammates) {
            if (!search.isEmpty() && !tm.ign.toLowerCase().contains(search))
                continue;

            if (!filterClass.equals("All") && !filterClass.equalsIgnoreCase(tm.lastClass))
                continue;

            if (timeThreshold > 0 && tm.lastTs < timeThreshold)
                continue;

            filtered.add(tm);
        }

        if (filterSort.equals("Runs")) {
            filtered.sort((t1, t2) -> Integer.compare(t2.count, t1.count));
        } else {
            filtered.sort((t1, t2) -> Long.compare(t2.lastTs, t1.lastTs));
        }

        if (filtered.isEmpty()) {
            teammatesList.addItem(new Widget(0, 0, 0, 5) {
                @Override
                public void render(GuiGraphics g, int x, int y, float p) {
                }
            });
            addInfoRow(teammatesList, "No teammates found.", "");
        } else {
            for (Teammate tm : filtered) {
                teammatesList.addItem(new TeammateRow(teammatesList.getWidth(), tm));
            }
        }
    }

    private static class Teammate {
        String ign;
        int count;
        String lastFloor;
        String lastClass;
        long lastTs;
        int lastClassLevel;

        Teammate(String ign, JsonObject data) {
            this.ign = ign;
            this.count = data.has("count") ? data.get("count").getAsInt() : 0;
            this.lastFloor = data.has("last_floor") && !data.get("last_floor").isJsonNull()
                    ? data.get("last_floor").getAsString()
                    : "";
            this.lastClass = data.has("last_class") && !data.get("last_class").isJsonNull()
                    ? data.get("last_class").getAsString()
                    : "";
            this.lastTs = data.has("last_ts") && !data.get("last_ts").isJsonNull() ? data.get("last_ts").getAsLong()
                    : 0;
            this.lastClassLevel = data.has("last_class_level") && !data.get("last_class_level").isJsonNull()
                    ? data.get("last_class_level").getAsInt()
                    : 0;
        }
    }

    private class TeammateRow extends Widget {
        private final Teammate tm;
        private Animation hoverAnimation;
        private final Button inviteBtn;

        public TeammateRow(int width, Teammate tm) {
            super(0, 0, width, 18);
            this.tm = tm;
            this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
            this.inviteBtn = new Button(0, 0, 40, 12, "Invite", () -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.connection.sendCommand("party " + tm.ign);
                }
            });
        }

        @Override
        public void updateHoverState(int mouseX, int mouseY) {
            super.updateHoverState(mouseX, mouseY);
            inviteBtn.setX(this.x + this.width - 45);
            inviteBtn.setY(this.y + 3);
            inviteBtn.updateHoverState(mouseX, mouseY);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float hover = hoverAnimation.getValue();
            if (hover > 0) {
                int color = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hover * 0.2f);
                graphics.fill(x, y, x + width, y + height, color);
            }

            int cx = x + 2;
            int cy = y + 5;

            String ignText = tm.ign;
            graphics.drawString(Minecraft.getInstance().font, ignText, cx, cy, Theme.ACCENT);
            cx += COL_IGN;
            graphics.drawString(Minecraft.getInstance().font, "§f" + tm.count, cx, cy, 0xFFFFFFFF);
            cx += COL_RUNS;

            String classText = String.format("§f%s %d", tm.lastClass, tm.lastClassLevel);
            graphics.drawString(Minecraft.getInstance().font, classText, cx, cy, 0xFFFFFFFF);
            cx += COL_CLASS;

            graphics.drawString(Minecraft.getInstance().font, "§f" + tm.lastFloor, cx, cy, 0xFFFFFFFF);
            cx += COL_FLOOR;

            String timeAgo = formatRelativeTime(tm.lastTs);
            graphics.drawString(Minecraft.getInstance().font, "§7" + timeAgo, cx, cy, 0xFFFFFFFF);

            inviteBtn.setX(this.x + this.width - 45);
            inviteBtn.setY(this.y + 3);
            inviteBtn.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void tick() {
            inviteBtn.tick();
            if (hovered && hoverAnimation.getProgress() < 1
                    && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            } else if (!hovered && hoverAnimation.getProgress() > 0
                    && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (inviteBtn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (visible && isMouseOver(mouseX, mouseY) && button == 0) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.connection.sendCommand("ba pv " + tm.ign);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return inviteBtn.mouseReleased(mouseX, mouseY, button);
        }
    }
}
