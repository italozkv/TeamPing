package dev.ithalo.teamping.ping;

import java.util.Locale;
import java.util.Optional;

public enum PingTargetKind {
    BLOCK,
    ITEM,
    HOSTILE_MOB,
    PASSIVE_MOB;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<PingTargetKind> byId(String id) {
        for (PingTargetKind kind : values()) {
            if (kind.id().equalsIgnoreCase(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
