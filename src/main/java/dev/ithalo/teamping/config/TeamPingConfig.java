package dev.ithalo.teamping.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TeamPingConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue PING_DURATION_SECONDS = BUILDER
            .comment("How long a ping remains visible, in seconds.")
            .translation("teamping.configuration.pingDurationSeconds")
            .defineInRange("pingDurationSeconds", 10, 1, 60);

    public static final ModConfigSpec.IntValue PING_COOLDOWN_SECONDS = BUILDER
            .comment("Minimum time between pings from the same player, in seconds.")
            .translation("teamping.configuration.pingCooldownSeconds")
            .defineInRange("pingCooldownSeconds", 2, 0, 60);

    public static final ModConfigSpec.IntValue MAX_PING_DISTANCE = BUILDER
            .comment("Maximum distance, in blocks, from the player to the ping target.")
            .translation("teamping.configuration.maxPingDistance")
            .defineInRange("maxPingDistance", 216, 1, 2048);

    public static final ModConfigSpec.IntValue MAX_ACTIVE_PINGS_PER_PLAYER = BUILDER
            .comment("Maximum number of non-expired pings each player may have active.")
            .translation("teamping.configuration.maxActivePingsPerPlayer")
            .defineInRange("maxActivePingsPerPlayer", 3, 1, 32);

    public static final ModConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION_PINGS = BUILDER
            .comment("Whether teammates in other dimensions receive pings.")
            .translation("teamping.configuration.allowCrossDimensionPings")
            .define("allowCrossDimensionPings", false);

    public static final ModConfigSpec.BooleanValue ENABLE_ACKNOWLEDGEMENTS = BUILDER
            .comment("Whether players can respond to pings with quick acknowledgements.")
            .translation("teamping.configuration.enableAcknowledgements")
            .define("enableAcknowledgements", true);

    public static final ModConfigSpec.IntValue ACK_INTERACTION_DISTANCE = BUILDER
            .comment("Maximum distance, in blocks, for responding to a visible ping.")
            .translation("teamping.configuration.ackInteractionDistance")
            .defineInRange("ackInteractionDistance", 96, 1, 512);

    public static final ModConfigSpec.IntValue ACK_HUD_DETAILS_DISTANCE = BUILDER
            .comment("Maximum distance, in blocks, for showing detailed acknowledgement names.")
            .translation("teamping.configuration.ackHudDetailsDistance")
            .defineInRange("ackHudDetailsDistance", 48, 1, 512);

    public static final ModConfigSpec.IntValue ACK_RESPONSE_COOLDOWN_MS = BUILDER
            .comment("Minimum time between acknowledgement responses from the same player, in milliseconds.")
            .translation("teamping.configuration.ackResponseCooldownMs")
            .defineInRange("ackResponseCooldownMs", 500, 0, 10000);

    public static final ModConfigSpec.BooleanValue SHOW_ACK_NOTIFICATIONS = BUILDER
            .comment("Whether acknowledgement updates show a small HUD notification.")
            .translation("teamping.configuration.showAckNotifications")
            .define("showAckNotifications", true);

    public static final ModConfigSpec.BooleanValue PLAY_ACK_SOUND = BUILDER
            .comment("Whether acknowledgement updates play a short UI sound.")
            .translation("teamping.configuration.playAckSound")
            .define("playAckSound", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private TeamPingConfig() {
    }
}
