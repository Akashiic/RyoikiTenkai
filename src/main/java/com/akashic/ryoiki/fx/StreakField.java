package com.akashic.ryoiki.fx;

/**
 * The rush of the star void: needles of light that pour out of one point (the armillary in its sky) and fly
 * outward from it through the whole arena, straight and fast, toward whoever looks at it and past them. Many at any
 * moment, while the domain is starting ({@link #OPENING} ticks); then they stop, and the void is still. No Minecraft
 * classes.
 *
 * <p>
 * Every streak runs along a ray from the focus. The rays are taken in lanes: a grid, {@link #LANE} blocks a side,
 * on the plane through the arena's centre that faces the focus. A lane may send a streak at each of {@link #SLOTS}
 * moments of every {@link #PERIOD} ticks; whether it does, how fast, how long, of which colour and where in the
 * lane come from the domain's number, the lane and the moment, so every client sees the same streaks without a
 * message. A streak keeps the speed it was sent with, and is seen from where its ray enters the arena to where it
 * leaves it or meets the floor.
 */
public final class StreakField {

    public static final float LANE = 3.2F;
    public static final int PERIOD = 18;
    public static final int SLOTS = 2;
    /** How far from the arena's centre the focus is, along the way the armillary hangs. */
    public static final float FOCUS = 150;
    /** Colours a streak may be: the drawing gives each number its colour. */
    public static final int COLORS = 4;

    /** Ticks the rush of the opening takes to settle. */
    public static final float OPENING = 50;

    /** How likely a slot sends a streak, at the height of a rush. */
    private static final float CHANCE = 0.9F;
    /** Blocks a tick at no rush at all, and how many times that at the height of one. */
    private static final float SPEED = 2.4F;
    private static final float RUSH_SPEED = 1.9F;
    private static final float MIN_LENGTH = 5;
    private static final float MAX_LENGTH = 13;
    private static final float RUSH_LENGTH = 1.8F;
    /** How far past the arena's wall the streaks are seen, and how near the floor. */
    private static final float BEYOND = 4;
    private static final float FLOOR = 0.15F;
    private static final float FADE = 4;

    /** One streak: a needle from its tail to its head, relative to the arena's centre. */
    public static final class Streak {

        public float headX;
        public float headY;
        public float headZ;
        public float tailX;
        public float tailY;
        public float tailZ;
        public float speed;
        /** Which of {@link #COLORS}. */
        public int color;
        /** How much of a rush it was sent in, 0 to 1: it is brighter for it. */
        public float rush;
        /** 0 to 1: it comes out of nothing where it enters the arena and goes into nothing where it leaves. */
        public float shown;
    }

    private StreakField() {}

    /**
     * How much of a rush there is at a time: 1 as the domain opens, and none once it has: the rush is the domain
     * starting. When it is complete the void is still.
     */
    public static float rush(float time) {
        if (time < 0 || time >= OPENING) {
            return 0;
        }
        // Whole for the first half of the opening, then dying away.
        float left = Math.min(1, (1 - time / OPENING) * 2);
        return left * left;
    }

    /**
     * The rush so far, summed: ticks' worth of full rush since the domain opened. What turns with the rush (the
     * void's sky) has turned by this much.
     */
    public static float rushed(float time) {
        float half = OPENING / 2;
        if (time <= 0) {
            return 0;
        }
        if (time <= half) {
            return time;
        }
        // Past the half the rush is the square of what is left, twice over: its sum is a cube.
        float left = Math.max(0, 1 - time / OPENING) * 2;
        return half + OPENING / 6 * (1 - left * left * left);
    }

    /** The number of the latest moment at which a lane may have sent a streak, at this time. */
    public static int moment(float time) {
        return (int) Math.floor(time * SLOTS / PERIOD);
    }

    /** Moments back from the latest whose streaks may still be crossing an arena of this radius. */
    public static int momentsAlive(int arenaRadius) {
        float longest = (2 * (arenaRadius + BEYOND) + MAX_LENGTH * RUSH_LENGTH) / (SPEED * 0.8F);
        return (int) Math.ceil(longest * SLOTS / PERIOD) + 1;
    }

    /** Lanes to either side of the middle one that reach an arena of this radius. */
    public static int lanes(int arenaRadius) {
        return (int) Math.ceil((arenaRadius + BEYOND) / LANE) + 1;
    }

    /**
     * Fills {@code out} with the streak a lane sent at a moment, as it is at a time, and says whether there is one
     * and it is still in the arena.
     *
     * @param laneA  the lane, counted across the plane that faces the focus
     * @param laneB  and counted up it
     * @param moment as {@link #moment} counts them
     * @param eyeX   the way the focus lies from the arena's centre: a unit direction
     */
    public static boolean at(int seed, int laneA, int laneB, int moment, float time, int arenaRadius, float eyeX,
        float eyeY, float eyeZ, Streak out) {
        if (moment < 0) {
            return false;
        }
        long lane = mix(seed * 0x9E3779B97F4A7C15L ^ laneA * 0xC2B2AE3D27D4EB4FL ^ laneB * 0x165667B19E3779F9L);
        // Each lane keeps its own time within the period, so they do not all send at once.
        float born = (moment + unit(lane)) * PERIOD / SLOTS;
        float age = time - born;
        if (age < 0) {
            return false;
        }
        long h = mix(lane + moment * 0x632BE59BD9B4E019L);
        float rush = rush(born);
        if (unit(h) >= CHANCE * rush) {
            return false;
        }
        // Two directions square to the way to the focus, and to each other: across, and up the plane.
        double flat = Math.sqrt(eyeX * eyeX + eyeZ * eyeZ);
        double ax = flat < 1e-6 ? 1 : eyeZ / flat;
        double az = flat < 1e-6 ? 0 : -eyeX / flat;
        double bx = eyeY * az;
        double by = eyeZ * ax - eyeX * az;
        double bz = -eyeY * ax;
        // Where the lane's ray crosses that plane.
        double a = (laneA + 0.15 + 0.7 * unit(h >>> 8)) * LANE;
        double b = (laneB + 0.15 + 0.7 * unit(h >>> 16)) * LANE;
        double px = ax * a + bx * b;
        double py = by * b;
        double pz = az * a + bz * b;
        // From the focus through it.
        double fx = eyeX * FOCUS;
        double fy = eyeY * FOCUS;
        double fz = eyeZ * FOCUS;
        double ux = px - fx;
        double uy = py - fy;
        double uz = pz - fz;
        double length = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= length;
        uy /= length;
        uz /= length;
        // Where that ray is inside the arena (a little beyond its wall) and above its floor.
        double reach = arenaRadius + BEYOND;
        double along = -(fx * ux + fy * uy + fz * uz);
        double inside = along * along - (fx * fx + fy * fy + fz * fz - reach * reach);
        if (inside <= 0) {
            return false;
        }
        double root = Math.sqrt(inside);
        double from = along - root;
        double to = along + root;
        if (uy < 0) {
            to = Math.min(to, (fy - FLOOR) / -uy);
        }
        if (to - from < 1) {
            return false;
        }
        float speed = SPEED * (0.8F + 0.4F * unit(h >>> 24)) * (1 + (RUSH_SPEED - 1) * rush);
        float needle = (MIN_LENGTH + (MAX_LENGTH - MIN_LENGTH) * unit(h >>> 32)) * (1 + (RUSH_LENGTH - 1) * rush);
        double head = from + speed * age;
        if (head - needle > to) {
            return false;
        }
        double shownHead = Math.min(head, to);
        double shownTail = Math.max(head - needle, from);
        if (shownHead <= shownTail) {
            return false;
        }
        out.headX = (float) (fx + ux * shownHead);
        out.headY = (float) (fy + uy * shownHead);
        out.headZ = (float) (fz + uz * shownHead);
        out.tailX = (float) (fx + ux * shownTail);
        out.tailY = (float) (fy + uy * shownTail);
        out.tailZ = (float) (fz + uz * shownTail);
        out.speed = speed;
        out.color = (int) (unit(h >>> 40) * COLORS);
        out.rush = rush;
        double in = Math.min(1, (shownHead - from) / FADE);
        double outAt = Math.min(1, (to - shownTail) / FADE);
        out.shown = (float) Math.max(0, Math.min(in, outAt));
        return true;
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
