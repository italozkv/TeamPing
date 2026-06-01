package dev.ithalo.teamping.ping;

import java.util.Locale;
import java.util.Optional;

public enum PingSegment {
    DANGER,
    RESOURCE,
    COMBAT,
    LOOT,
    BASE,
    LOCATION;

    private static final double[] WHEEL_CENTERS = {
            270.0D,
            180.0D,
            0.0D,
            135.0D,
            45.0D
    };

    public static Optional<PingSegment> byId(String id) {
        for (PingSegment segment : values()) {
            if (segment.name().equalsIgnoreCase(id)) {
                return Optional.of(segment);
            }
        }
        return Optional.empty();
    }

    public static PingSegment fromAngle(double angleDegrees) {
        return fromWheelAngle(angleDegrees);
    }

    public static PingSegment fromWheelSelection(double angleDegrees, double distanceFromCenter, double centerRadius) {
        if (distanceFromCenter <= centerRadius) {
            return LOCATION;
        }
        return fromWheelAngle(angleDegrees);
    }

    public static PingSegment fromWheelAngle(double angleDegrees) {
        double normalized = normalize(angleDegrees);
        PingSegment best = DANGER;
        double bestDistance = Double.MAX_VALUE;

        PingSegment[] outerSegments = {
                DANGER,
                RESOURCE,
                COMBAT,
                LOOT,
                BASE
        };

        for (int i = 0; i < outerSegments.length; i++) {
            double distance = angularDistance(normalized, WHEEL_CENTERS[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = outerSegments[i];
            }
        }

        return best;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static double normalize(double angleDegrees) {
        double normalized = angleDegrees % 360.0D;
        if (normalized < 0.0D) {
            normalized += 360.0D;
        }
        return normalized;
    }

    private static double angularDistance(double a, double b) {
        double diff = Math.abs(normalize(a) - normalize(b));
        return Math.min(diff, 360.0D - diff);
    }
}
