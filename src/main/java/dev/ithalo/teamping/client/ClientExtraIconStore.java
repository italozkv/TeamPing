package dev.ithalo.teamping.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import dev.ithalo.teamping.ping.PingIconOption;
import dev.ithalo.teamping.ping.PingSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class ClientExtraIconStore {
    private static final String FILE_NAME = "teamping-client.properties";
    private static final String KEY_PREFIX = "slot.";

    private static final Map<PingSegment, String> ASSIGNED_ICON_IDS = new EnumMap<>(PingSegment.class);
    private static boolean loaded;

    private ClientExtraIconStore() {
    }

    public static synchronized String getAssignedIconId(PingSegment segment) {
        ensureLoaded();
        return ASSIGNED_ICON_IDS.get(segment);
    }

    public static synchronized String resolveIconId(PingSegment segment) {
        String iconId = getAssignedIconId(segment);
        return iconId == null || iconId.isBlank() ? "" : iconId;
    }

    public static synchronized ResourceLocation resolveIconTexture(PingSegment segment, ResourceLocation fallback) {
        String iconId = getAssignedIconId(segment);
        if (iconId == null || iconId.isBlank()) {
            return fallback;
        }

        return PingIconOption.byId(iconId).map(PingIconOption::texture).orElse(fallback);
    }

    public static synchronized void assignIcon(PingSegment segment, String iconId) {
        ensureLoaded();
        if (iconId == null || iconId.isBlank()) {
            ASSIGNED_ICON_IDS.remove(segment);
        } else {
            ASSIGNED_ICON_IDS.put(segment, PingIconOption.normalizeOrDefault(iconId));
        }
        save();
    }

    public static synchronized void clearIcon(PingSegment segment) {
        ensureLoaded();
        ASSIGNED_ICON_IDS.remove(segment);
        save();
    }

    public static synchronized void reload() {
        loaded = false;
        ASSIGNED_ICON_IDS.clear();
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        Properties properties = new Properties();
        Path file = configFile();
        if (Files.exists(file)) {
            try (InputStream inputStream = Files.newInputStream(file)) {
                properties.load(inputStream);
            } catch (IOException ignored) {
            }
        }

        for (PingSegment segment : PingSegment.values()) {
            String value = properties.getProperty(KEY_PREFIX + segment.id());
            if (value != null && !value.isBlank()) {
                ASSIGNED_ICON_IDS.put(segment, PingIconOption.normalizeOrDefault(value));
            }
        }

        loaded = true;
    }

    private static void save() {
        Properties properties = new Properties();
        for (Map.Entry<PingSegment, String> entry : ASSIGNED_ICON_IDS.entrySet()) {
            properties.setProperty(KEY_PREFIX + entry.getKey().id(), PingIconOption.normalizeOrDefault(entry.getValue()));
        }

        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream outputStream = Files.newOutputStream(file)) {
                properties.store(outputStream, "TeamPing client settings");
            }
        } catch (IOException ignored) {
        }
    }

    private static Path configFile() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }
}
