package dev.ithalo.teamping.ping;

import java.util.UUID;

public record PingAcknowledgement(
        UUID pingId,
        UUID playerId,
        String playerName,
        PingAckType type,
        long timestamp
) {
}
