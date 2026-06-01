package dev.ithalo.teamping;

import dev.ithalo.teamping.command.PingTeamCommand;
import dev.ithalo.teamping.config.TeamPingConfig;
import dev.ithalo.teamping.network.TeamPingNetwork;
import dev.ithalo.teamping.server.ServerPingManager;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(TeamPing.MOD_ID)
public class TeamPing {
    public static final String MOD_ID = "teamping";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TeamPing(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, TeamPingConfig.SPEC);
        modEventBus.addListener(TeamPingNetwork::register);

        NeoForge.EVENT_BUS.addListener(PingTeamCommand::register);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
    }

    private void onServerTick(ServerTickEvent.Post event) {
        ServerPingManager.cleanup(event.getServer());
    }
}
