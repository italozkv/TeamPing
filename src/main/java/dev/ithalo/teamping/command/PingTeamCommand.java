package dev.ithalo.teamping.command;

import com.mojang.brigadier.CommandDispatcher;

import dev.ithalo.teamping.team.PingTeam;
import dev.ithalo.teamping.team.PlayerTeamData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class PingTeamCommand {
    private PingTeamCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("pingteam")
                .then(teamLiteral(PingTeam.BLUE))
                .then(teamLiteral(PingTeam.RED))
                .then(teamLiteral(PingTeam.GREEN))
                .then(teamLiteral(PingTeam.YELLOW))
                .then(Commands.literal("sair").executes(context -> leaveTeam(context.getSource()))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> teamLiteral(PingTeam team) {
        return Commands.literal(team.id()).executes(context -> joinTeam(context.getSource(), team));
    }

    private static int joinTeam(CommandSourceStack source, PingTeam team) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerTeamData.setTeam(player, team);
        source.sendSuccess(() -> Component.translatable("teamping.command.joined", team.id()).withStyle(team.chatFormatting()), false);
        return 1;
    }

    private static int leaveTeam(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerTeamData.clearTeam(player);
        source.sendSuccess(() -> Component.translatable("teamping.command.left"), false);
        return 1;
    }
}
