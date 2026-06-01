package dev.ithalo.teamping.ping;

import java.util.Locale;
import java.util.Optional;

public enum PingAckType {
    UNDERSTOOD("understood", "teamping.ack.understood", "understood", 0x7ED957, "OK"),
    ON_MY_WAY("on_my_way", "teamping.ack.on_my_way", "on_my_way", 0x45A3FF, "GO"),
    NEGATIVE("negative", "teamping.ack.negative", "negative", 0xFF5A5A, "NO"),
    CHECKING("checking", "teamping.ack.checking", "checking", 0xFFD35A, "CHK");

    private final String id;
    private final String translationKey;
    private final String iconName;
    private final int color;
    private final String shortLabel;

    PingAckType(String id, String translationKey, String iconName, int color, String shortLabel) {
        this.id = id;
        this.translationKey = translationKey;
        this.iconName = iconName;
        this.color = color;
        this.shortLabel = shortLabel;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public String iconName() {
        return iconName;
    }

    public int color() {
        return color;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public static Optional<PingAckType> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        String normalized = id.toLowerCase(Locale.ROOT);
        for (PingAckType type : values()) {
            if (type.id.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
