package dev.ithalo.teamping.team;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.ChatFormatting;

public enum PingTeam {
    BLUE("azul", ChatFormatting.BLUE, 0x3366FF),
    RED("vermelho", ChatFormatting.RED, 0xFF3333),
    GREEN("verde", ChatFormatting.GREEN, 0x33CC66),
    YELLOW("amarelo", ChatFormatting.YELLOW, 0xFFD633);

    private final String id;
    private final ChatFormatting chatFormatting;
    private final int color;

    PingTeam(String id, ChatFormatting chatFormatting, int color) {
        this.id = id;
        this.chatFormatting = chatFormatting;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public ChatFormatting chatFormatting() {
        return chatFormatting;
    }

    public int color() {
        return color;
    }

    public static Optional<PingTeam> byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (PingTeam team : values()) {
            if (team.id.equals(normalized)) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }
}
