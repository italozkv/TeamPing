package dev.ithalo.teamping.network;

import dev.ithalo.teamping.server.ServerPingManager;
import dev.ithalo.teamping.server.ServerTeamManager;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TeamPingNetwork {
    private TeamPingNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CreatePingPayload.TYPE, CreatePingPayload.STREAM_CODEC, ServerPingManager::handleCreatePing);
        registrar.playToServer(C2SPingAckPayload.TYPE, C2SPingAckPayload.STREAM_CODEC, ServerPingManager::handlePingAck);
        registrar.playToServer(SetPingTeamPayload.TYPE, SetPingTeamPayload.STREAM_CODEC, ServerTeamManager::handleSetTeam);
        registrar.playToClient(ShowPingPayload.TYPE, ShowPingPayload.STREAM_CODEC, ClientPayloadBridge::handleShowPing);
        registrar.playToClient(S2CPingAckUpdatePayload.TYPE, S2CPingAckUpdatePayload.STREAM_CODEC, ClientPayloadBridge::handlePingAckUpdate);
    }
}
