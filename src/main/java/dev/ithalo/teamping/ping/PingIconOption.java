package dev.ithalo.teamping.ping;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import dev.ithalo.teamping.TeamPing;
import net.minecraft.resources.ResourceLocation;

public enum PingIconOption {
    QUESTION_MARK("question_mark", "Question", "textures/gui/pings/extras/question_mark.png"),
    FLAG("flag", "Flag", "textures/gui/pings/extras/flag.png"),
    FOOD("food", "Food", "textures/gui/pings/extras/food.png"),
    BED("bed", "Bed", "textures/gui/pings/extras/bed.png"),
    MINE("mine", "Mine", "textures/gui/pings/extras/mine.png"),
    SHIELD("shield", "Shield", "textures/gui/pings/extras/shield.png"),
    PLAYER("player", "Player", "textures/gui/pings/extras/player.png"),
    DEATH_LOCATION("death_location", "Death", "textures/gui/pings/extras/death_location.png"),
    EYE("eye", "Eye", "textures/gui/pings/extras/eye.png"),
    CHECK_MARK("check_mark", "Check", "textures/gui/pings/extras/check_mark.png"),
    USERS_GROUP("users_group", "Group", "textures/gui/pings/extras/users_group.png");

    public static final String DEFAULT_ID = QUESTION_MARK.id;

    private static final List<PingIconOption> DISPLAY_ORDER = List.copyOf(Arrays.asList(values()));

    private final String id;
    private final String label;
    private final ResourceLocation texture;

    PingIconOption(String id, String label, String texturePath) {
        this.id = id;
        this.label = label;
        this.texture = ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, texturePath);
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public static List<PingIconOption> displayOrder() {
        return DISPLAY_ORDER;
    }

    public static Optional<PingIconOption> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        for (PingIconOption option : values()) {
            if (option.id.equalsIgnoreCase(id)) {
                return Optional.of(option);
            }
        }
        return Optional.empty();
    }

    public static boolean isValidId(String id) {
        return byId(id).isPresent();
    }

    public static String normalizeOrDefault(String id) {
        return byId(id).map(PingIconOption::id).orElse(DEFAULT_ID);
    }

    public static String defaultId() {
        return DEFAULT_ID;
    }

    public static int size() {
        return DISPLAY_ORDER.size();
    }
}
