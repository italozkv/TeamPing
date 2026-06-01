package dev.ithalo.teamping.server;

import dev.ithalo.teamping.network.SetPingTeamPayload;
import dev.ithalo.teamping.team.PingTeam;
import dev.ithalo.teamping.team.PlayerTeamData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerTeamManager {
    private ServerTeamManager() {
    }

    public static void handleSetTeam(SetPingTeamPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        PingTeam.byId(payload.teamId()).ifPresent(team -> PlayerTeamData.setTeam(player, team));
    }
}
