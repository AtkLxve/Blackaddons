package org.blackum.blackaddons.gui.screen;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

import org.blackum.blackaddons.gui.widget.Label;
import org.blackum.blackaddons.gui.widget.TabPanel;
import org.blackum.blackaddons.util.BotIntegration;

public class ProfileViewerScreen extends BaseScreen {
    private final String player;
    private TabPanel tabPanel;
    private JsonObject profileData;
    private boolean isLoading = true;
    private String errorMessage = null;
    private static int lastTabIndex = 0;

    private org.blackum.blackaddons.gui.screen.tabs.DungeonsTabController dungeonsController;
    private org.blackum.blackaddons.gui.screen.tabs.TeammatesTabController teammatesController;
    private org.blackum.blackaddons.gui.screen.tabs.RngTabController rngController;
    private org.blackum.blackaddons.gui.screen.tabs.DailyTabController dailyController;
    private org.blackum.blackaddons.gui.screen.tabs.RtcaTabController rtcaController;

    @Override
    protected void initWidgets() {
        if (isLoading) {
            BotIntegration.getProfileStats(player).thenAccept(json -> {
                isLoading = false;
                if (json == null) {
                    errorMessage = "Failed to fetch data from bot.";
                } else if (json.has("error")) {
                    errorMessage = json.get("error").getAsString();
                } else if (json.has("data")) {
                    profileData = json.getAsJsonObject("data");
                } else {
                    errorMessage = "Invalid data received.";
                }
                net.minecraft.client.Minecraft.getInstance().execute(() -> this.init(this.width, this.height));
            });
        }

        if (isLoading) {
            addWidget(new Label(containerX + containerWidth / 2 - 30, containerY + containerHeight / 2, "Loading...",
                    Label.Style.TITLE));
            return;
        }

        if (errorMessage != null) {
            addWidget(new Label(containerX + containerWidth / 2 - 60, containerY + containerHeight / 2, errorMessage,
                    Label.Style.TITLE));
            return;
        }

        if (profileData == null)
            return;

        tabPanel = new TabPanel(containerX, containerY + 40, containerWidth, containerHeight - 40);
        tabPanel.setOnTabChange(index -> {
            lastTabIndex = index;
            stopConfetti();
        });
        addWidget(tabPanel);

        if (dungeonsController == null) {
            dungeonsController = new org.blackum.blackaddons.gui.screen.tabs.DungeonsTabController(this, profileData);
        }
        dungeonsController.init(tabPanel.addTab("Dungeons"));

        if (teammatesController == null) {
            teammatesController = new org.blackum.blackaddons.gui.screen.tabs.TeammatesTabController(this, profileData);
        }
        teammatesController.init(tabPanel.addTab("Recent Teammates"));

        if (rngController == null) {
            rngController = new org.blackum.blackaddons.gui.screen.tabs.RngTabController(this, profileData, player);
        }
        rngController.init(tabPanel.addTab("RNG"));

        if (dailyController == null) {
            dailyController = new org.blackum.blackaddons.gui.screen.tabs.DailyTabController(this, profileData);
        }
        dailyController.init(tabPanel.addTab("Daily"));

        if (rtcaController == null) {
            rtcaController = new org.blackum.blackaddons.gui.screen.tabs.RtcaTabController(this, profileData);
        }
        rtcaController.init(tabPanel.addTab("RTCA"));

        tabPanel.selectTab(lastTabIndex);
    }

    public ProfileViewerScreen(net.minecraft.client.gui.screens.Screen parent, String player) {
        this(parent, player, null);
    }

    public String getPlayer() {
        return player;
    }

    public ProfileViewerScreen(net.minecraft.client.gui.screens.Screen parent, String player, JsonObject data) {
        super(Component.literal("Profile: " + player), parent);
        this.player = player;
        if (data != null) {
            this.profileData = data;
            this.isLoading = false;
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderConfetti(graphics);
    }

    @Override
    public void tick() {
        super.tick();
        tickConfetti();

        if (dailyController != null) {
            dailyController.tick();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tabPanel != null && tabPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static class Confetti {
        double x, y;
        double speedX, speedY;
        int color;
        int life;
        int maxLife;
        float size;

        Confetti(double x, double y) {
            this.x = x;
            this.y = y;
            this.speedX = (Math.random() - 0.5) * 5;
            this.speedY = -(Math.random() * 3 + 2);
            java.awt.Color c = java.awt.Color.getHSBColor((float) Math.random(), 1f, 1f);
            this.color = c.getRGB();
            this.maxLife = 100 + (int) (Math.random() * 100);
            this.life = this.maxLife;
            this.size = (float) (Math.random() * 3 + 2);
        }
    }

    private java.util.List<Confetti> confettiParticles = new java.util.ArrayList<>();
    private boolean confettiActive = false;
    private int confettiTimer = 0;

    public void startConfetti() {
        if (confettiActive)
            return;
        stopConfetti();
        confettiActive = true;
        confettiTimer = 100;
        spawnConfettiBurst();
    }

    private void stopConfetti() {
        confettiActive = false;
        confettiTimer = 0;
        confettiParticles.clear();
    }

    private void spawnConfettiBurst() {
        if (width <= 0)
            return;
        for (int i = 0; i < 100; i++) {
            double startX = width / 2.0;
            double startY = height;
            double sx = (Math.random() - 0.5) * 10;
            double sy = -(Math.random() * 5 + 5);
            Confetti c = new Confetti(startX, startY);
            c.speedX = sx;
            c.speedY = sy;
            confettiParticles.add(c);
        }
    }

    private void tickConfetti() {
        if (confettiActive) {
            confettiTimer--;
            if (confettiTimer <= 0) {
                confettiActive = false;
            } else if (confettiTimer % 5 == 0 && confettiParticles.size() < 200) {
                for (int k = 0; k < 2; k++) {
                    double startX = width / 2.0 + (Math.random() - 0.5) * 100;
                    double startY = height;
                    Confetti c = new Confetti(startX, startY);
                    c.speedX = (Math.random() - 0.5) * 5;
                    c.speedY = -(Math.random() * 5 + 5);
                    confettiParticles.add(c);
                }
            }
        }

        if (confettiParticles.isEmpty())
            return;

        java.util.Iterator<Confetti> it = confettiParticles.iterator();
        while (it.hasNext()) {
            Confetti c = it.next();
            c.x += c.speedX;
            c.y += c.speedY;
            c.speedY += 0.2;
            c.life--;

            if (c.y > height + 20 || c.life <= 0) {
                it.remove();
            }
        }
    }

    private void renderConfetti(net.minecraft.client.gui.GuiGraphics graphics) {
        if (confettiParticles.isEmpty())
            return;

        for (Confetti c : confettiParticles) {
            graphics.fill((int) c.x, (int) c.y, (int) (c.x + c.size), (int) (c.y + c.size), c.color);
        }
    }
}
