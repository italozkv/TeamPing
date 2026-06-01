package dev.ithalo.teamping.client;

import dev.ithalo.teamping.TeamPing;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = TeamPing.MOD_ID, dist = Dist.CLIENT)
public class TeamPingClient {
    private final PingWheelInputHandler pingWheelInputHandler = new PingWheelInputHandler();

    public TeamPingClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(pingWheelInputHandler::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(pingWheelInputHandler::onKeyInput);
        NeoForge.EVENT_BUS.addListener(pingWheelInputHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(pingWheelInputHandler::onMouseButton);
        NeoForge.EVENT_BUS.addListener(ClientPingRenderer::render);
        NeoForge.EVENT_BUS.addListener(ClientPingRenderer::renderHud);
        NeoForge.EVENT_BUS.addListener(PingWheelRenderer::render);
    }
}
