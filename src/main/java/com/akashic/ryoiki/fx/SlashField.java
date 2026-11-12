package com.akashic.ryoiki.fx;

/**
 * The cuts that cross the crimson shrine: where each one is and when, worked out from the domain's number and the
 * time alone, so every client draws the same cuts without a single message. A new cut starts every {@link #PERIOD}
 * ticks and lasts {@link #LIFE}. No Minecraft classes.
 */
public final class SlashField {

    public static final int PERIOD = 7;
    public static final int LIFE = 9;

    /** One cut: a segment through {@code (x, y, z)} (relative to the arena's centre) along a direction. */
    public static final class Slash {

        public float x;
        public float y;
        public float z;
        public float dirX;
        public float dirY;
        public float dirZ;
        public float length;
        /** 0 at its first instant, 1 at its last. */
        public float life;
    }

    private SlashField() {}

    /** The number of the cut that started last at this tick. */
    public static int index(int tick) {
        return Math.floorDiv(tick, PERIOD);
    }

    /** Whether a cut starts exactly at this tick. */
    public static boolean startsAt(int tick) {
        return tick >= 0 && tick % PERIOD == 0;
    }

    /**
     * Fills {@code out} with cut number {@code index} of the domain {@code seed} as it is at {@code time} (ticks,
     * with the fraction), and says whether it is still there.
     */
    public static boolean at(int seed, int index, float time, int radius, Slash out) {
        float age = time - index * PERIOD;
        if (index < 0 || age < 0 || age >= LIFE) {
            return false;
        }
        long h = mix(seed * 0x9E3779B97F4A7C15L + index);
        float angle = unit(h) * (float) (Math.PI * 2);
        float reach = unit(h >>> 12) * (radius - 3);
        float around = unit(h >>> 24) * (float) (Math.PI * 2);
        out.x = (float) Math.cos(around) * reach;
        out.z = (float) Math.sin(around) * reach;
        out.y = 0.5F + unit(h >>> 36) * 2.4F;
        float tilt = (unit(h >>> 48) - 0.5F) * 0.5F;
        float flat = (float) Math.sqrt(1 - tilt * tilt);
        out.dirX = (float) Math.cos(angle) * flat;
        out.dirY = tilt;
        out.dirZ = (float) Math.sin(angle) * flat;
        out.length = radius * 2.2F;
        out.life = age / LIFE;
        return true;
    }

    private static long mix(long z) {
        z = (z ^ z >>> 30) * 0xBF58476D1CE4E5B9L;
        z = (z ^ z >>> 27) * 0x94D049BB133111EBL;
        return z ^ z >>> 31;
    }

    private static float unit(long bits) {
        return (bits & 0xFFF) / 4096F;
    }
}
