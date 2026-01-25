package org.blackum.blackaddons.gui.notification;

public enum NotificationType {
    INFO(0xFF50C8FF),
    SUCCESS(0xFF55FF55),
    WARNING(0xFFFFAA00),
    ERROR(0xFFFF5555);

    private final int color;

    NotificationType(int color) {
        this.color = color;
    }

    public int getColor() {
        if (this == INFO) {
            return org.blackum.blackaddons.gui.theme.Theme.ACCENT;
        }
        return color;
    }
}
