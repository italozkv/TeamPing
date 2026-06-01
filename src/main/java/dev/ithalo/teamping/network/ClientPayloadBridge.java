package dev.ithalo.teamping.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadBridge {
    private ClientPayloadBridge() {
    }

    public static void handleShowPing(ShowPingPayload payload, IPayloadContext context) {
        dev.ithalo.teamping.client.ClientPingManager.addPing(payload);
    }

    public static void handlePingAckUpdate(S2CPingAckUpdatePayload payload, IPayloadContext context) {
        dev.ithalo.teamping.client.ClientPingManager.updateAcknowledgement(payload);
    }
}
