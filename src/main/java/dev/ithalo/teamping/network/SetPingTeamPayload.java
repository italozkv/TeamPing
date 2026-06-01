package dev.ithalo.teamping.network;

import dev.ithalo.teamping.TeamPing;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetPingTeamPayload(String teamId) implements CustomPacketPayload {
    public static final Type<SetPingTeamPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "set_ping_team"));

    public static final StreamCodec<ByteBuf, SetPingTeamPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SetPingTeamPayload::teamId,
            SetPingTeamPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
