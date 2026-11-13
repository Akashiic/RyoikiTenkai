package com.akashic.ryoiki.fx;

/**
 * The rush of the star void: needles of light that cross the arena in one straight line, all the same way, out of
 * the side the armillary hangs on. As the domain opens they come thick, long and very fast, and thin out as the sky
 * settles; after that a sparse flow goes on, and every few seconds a volley: the flow quickens and thickens for less
 * than a second. No Minecraft classes.
 *
 * <p>
 * Across the flow the arena is taken in lanes, {@link #LANE} blocks wide and {@link #LAYER} high. A lane may send a
 * streak at each of {@link #SLOTS} moments of every {@link #PERIOD} ticks; whether it does, how fast, how long and
 * where in the lane come from the domain's number, the lane and the moment, so every client sees the same streaks
 * without a message. A streak keeps the speed it was sent with.
 */
public final class StreakField {

    public static final float LANE = 3;
    public static final float LAYER = 2.2F;
    public static final int LAYERS = 4;
    public static final int PERIOD = 18;
    public static final int SLOTS = 3;

    /** Ticks the rush of the opening takes to die away. */
    public static final float OPENING = 40;
    /** The first volley, and how often after it. */
    public static final int FIRST_VOLLEY = 56;
    public static final int VOLLEY_PERIOD = 64;
    private static final float VOLLEY_RISE = 3;
    private static final float VOLLEY_HOLD = 6;
    private static final float VOLLEY_FALL = 11;
    private static final float VOLLEY_PEAK = 0.75F;

    /** How likely a slot sends a streak: in the calm, and at the height of a rush. */
    private static final float CALM = 0.12F;
    private static final float RUSH = 0.92F;
    /** Blocks a tick: in the calm, and how many times that at the height of a rush. */
    private static final float SPEED = 1.5F;
    private static final float RUSH_SPEED = 3.4F;
    private static final float MIN_LENGTH = 3;
    private static final float MAX_LENGTH = 9;
    private static final float RUSH_LENGTH = 2.4F;

    /** One streak: a needle along Z, its head leading, relative to the arena's centre. */
    public static final class Streak {

        public float x;
        public float y;
        /** Where its head is, and its tail behind it. */
        public float headZ;
        public float tailZ;
        public float speed;
        /** How much of a rush it was sent in, 0 to 1: it is brighter for it. */
        public float rush;
        /** 0 to 1: it comes out of nothing at the wall it starts from and goes into nothing at the other. */
        public float shown;
    }

    private StreakField() {}

    /**
     * How much of a rush there is at a time, 0 (the calm flow) to 1 (the opening at its first instant): the
     * opening, dying away, and the volleys after it.
     */
    public static float rush(float time) {
        float opening = 0;
        if (time >= 0 && time < OPENING) {
            float left = 1 - time / OPENING;
            opening = left * left;
        }
        float volley = 0;
        if (time >= FIRST_VOLLEY) {
            float into = (time - FIRST_VOLLEY) % VOLLEY_PERIOD;
            if (into < VOLLEY_RISE) {
                volley = into / VOLLEY_RISE;
            } else if (into < VOLLEY_RISE + VOLLEY_HOLD) {
                volley = 1;
            } else if (into < VOLLEY_RISE + VOLLEY_HOLD + VOLLEY_FALL) {
                float left = 1 - (into - VOLLEY_RISE - VOLLEY_HOLD) / VOLLEY_FALL;
                volley = left * left;
            }
        }
        return Math.max(opening, volley * VOLLEY_PEAK);
    }

    /** Whether a volley begins exactly at this tick. */
    public static boolean volleyStartsAt(int tick) {
        return tick >= FIRST_VOLLEY && (tick - FIRST_VOLLEY) % VOLLEY_PERIOD == 0;
    }

    /** The number of the latest moment at which a lane may have sent a streak, at this time. */
    public static int moment(float time) {
        return (int) Math.floor(time * SLOTS / PERIOD);
    }

    /** Moments back from the latest whose streaks may still be crossing an arena of this radius. */
    public static int momentsAlive(int arenaRadius) {
        float longest = (2 * arenaRadius + MAX_LENGTH * RUSH_LENGTH) / SPEED;
        return (int) Math.ceil(longest * SLOTS / PERIOD) + 1;
    }

    /**
     * Fills {@code out} with the streak a lane sent at a moment, as it is at a time, and says whether there is one
     * and it is still in the arena.
     *
     * @param laneX  the lane across the flow: the one that holds x is {@code floor(x / LANE)}
     * @param laneY  the layer, from 0 at the floor
     * @param moment as {@link #moment} counts them
     */
    public static boolean at(int seed, int laneX, int laneY, int moment, float time, int arenaRadius, Streak out) {
        if (laneY < 0 || laneY >= LAYERS || moment < 0) {
            return false;
        }
        long lane = mix(seed * 0x9E3779B97F4A7C15L ^ laneX * 0xC2B2AE3D27D4EB4FL ^ laneY * 0x165667B19E3779F9L);
        // Each lane keeps its own time within the period, so they do not all send at once.
        float born = (moment + unit(lane)) * PERIOD / SLOTS;
        float age = time - born;
        if (age < 0) {
            return false;
        }
        long h = mix(lane + moment * 0x632BE59BD9B4E019L);
        float rush = rush(born);
        if (unit(h) >= CALM + (RUSH - CALM) * rush) {
            return false;
        }
        float x = (laneX + 0.15F + 0.7F * unit(h >>> 8)) * LANE;
        // The arena is round: the flow crosses it from wall to wall, a shorter way toward its sides.
        float reach = arenaRadius - 1.5F;
        if (Math.abs(x) >= reach) {
            return false;
        }
        float half = (float) Math.sqrt(reach * reach - x * x);
        float speed = SPEED * (0.75F + 0.5F * unit(h >>> 16)) * (1 + (RUSH_SPEED - 1) * rush);
        float length = (MIN_LENGTH + (MAX_LENGTH - MIN_LENGTH) * unit(h >>> 24)) * (1 + (RUSH_LENGTH - 1) * rush);
        float head = -half + speed * age;
        if (head - length > half) {
            return false;
        }
        out.x = x;
        out.y = 0.5F + (laneY + 0.1F + 0.8F * unit(h >>> 32)) * LAYER;
        // Clipped to the arena at both ends: it grows out of one wall and sinks into the other.
        out.headZ = Math.min(head, half);
        out.tailZ = Math.max(head - length, -half);
        out.speed = speed;
        out.rush = rush;
        float in = Math.min(1, (out.headZ + half) / 3);
        float outAt = Math.min(1, (half - out.tailZ) / 3);
        out.shown = Math.max(0, Math.min(in, outAt));
        return out.headZ > out.tailZ;
    }

    private static long mix(long z) {
        z = (z ^ z >>> 30) * 0xBF58476D1CE4E5B9L;
        z = (z ^ z >>> 27) * 0x94D049BB133111EBL;
        return z ^ z >>> 31;
    }

    private static float unit(long bits) {
        return (bits & 0xFF) / 256F;
    }
}
