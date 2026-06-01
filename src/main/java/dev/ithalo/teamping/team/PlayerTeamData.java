package dev.ithalo.teamping.team;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerTeamData {
    private static final String PERSISTED_ROOT = "PlayerPersisted";
    private static final String TEAM_KEY = "TeamPingTeam";

    private PlayerTeamData() {
    }

    public static Optional<PingTeam> getTeam(ServerPlayer player) {
        CompoundTag persisted = getPersistedTag(player);
        if (!persisted.contains(TEAM_KEY)) {
            return Optional.empty();
        }
        return PingTeam.byId(persisted.getString(TEAM_KEY));
    }

    public static void setTeam(ServerPlayer player, PingTeam team) {
        getPersistedTag(player).putString(TEAM_KEY, team.id());
    }

    public static void clearTeam(ServerPlayer player) {
        getPersistedTag(player).remove(TEAM_KEY);
    }

    private static CompoundTag getPersistedTag(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PERSISTED_ROOT)) {
            root.put(PERSISTED_ROOT, new CompoundTag());
        }
        return root.getCompound(PERSISTED_ROOT);
    }
}
