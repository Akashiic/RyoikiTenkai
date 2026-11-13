package com.akashic.ryoiki.fx;

/**
 * The cuts that cross the crimson shrine: where each one is and when, worked out from the domain's number and the
 * time alone, so every client draws the same cuts without a single message. No Minecraft classes.
 *
 * <p>
 * A cut is the arc a blade sweeps through the air: part of a circle, in a plane of its own, drawn from one end to
 * the other. They come in flurries: {@link #FLURRY} cuts {@link #STEP} ticks apart, crossing each other at one
 * place, and a new flurry every {@link #PERIOD} ticks somewhere else. A cut is seen for {@link #LIFE} ticks; the
 * scar it leaves on the floor under it, for {@link #SCAR}.
 */
public final class SlashField {

    public static final int FLURRY = 3;
    public static final int STEP = 2;
    public static final int PERIOD = 5;
    public static final int LIFE = 10;
    public static final int SCAR = 44;
    /** Ticks the blade takes from one end of its arc to the other. */
    public static final float SWEEP = 2.2F;

    private static final float MIN_RADIUS = 3.5F;
    private static final float MAX_RADIUS = 7.5F;
    private static final float MIN_SPAN = (float) Math.toRadians(70);
    private static final float MAX_SPAN = (float) Math.toRadians(130);
    /** How near the floor an arc may come. */
    private static final float CLEAR = 0.3F;
    /** How far from the arena's centre a flurry keeps: the hall in the middle would hide it. */
    private static final float HALL = 10;

    /**
     * One cut. Its arc is the part of a circle of {@link #radius} about a centre, {@link #span} radians wide, in the
     * plane of the two unit directions {@code u} (along it, the way the blade goes) and {@code v} (the way it
     * bulges); {@code mid} is the middle of the arc, relative to the arena's centre.
     */
    public static final class Slash {

        public float midX;
        public float midY;
        public float midZ;
        public float ux;
        public float uy;
        public float uz;
        public float vx;
        public float vy;
        public float vz;
        public float radius;
        public float span;
        /** Ticks since it began. */
        public float age;
        /** Its number: what is random about how it is drawn comes from this. */
        public int index;
    }

    private SlashField() {}

    /** The number of the flurry going on at a tick. */
    public static int flurry(int tick) {
        return Math.floorDiv(tick, PERIOD);
    }

    /** Whether a flurry starts exactly at this tick. */
    public static boolean flurryStartsAt(int tick) {
        return tick >= 0 && tick % PERIOD == 0;
    }

    /** The number of the cut that began last at this tick. */
    public static int index(int tick) {
        int flurry = flurry(tick);
        return flurry * FLURRY + Math.min(FLURRY - 1, (tick - flurry * PERIOD) / STEP);
    }

    /** The tick a cut begins at. */
    public static int born(int index) {
        return Math.floorDiv(index, FLURRY) * PERIOD + Math.floorMod(index, FLURRY) * STEP;
    }

    /**
     * Fills {@code out} with cut number {@code index} of the domain {@code seed} as it is at {@code time} (ticks,
     * with the fraction), and says whether it, or its scar, is still there.
     */
    public static boolean at(int seed, int index, float time, int arenaRadius, Slash out) {
        float age = time - born(index);
        if (index < 0 || age < 0 || age >= SCAR) {
            return false;
        }
        int flurry = Math.floorDiv(index, FLURRY);
        int which = Math.floorMod(index, FLURRY);
        long place = mix(seed * 0x9E3779B97F4A7C15L + flurry);
        long own = mix(place + 0x632BE59BD9B4E019L * (which + 1));
        // The flurry's place: anywhere on the floor but the middle, where the shrine's hall stands, and as likely in
        // one part of it as in another.
        double outer = arenaRadius - MAX_RADIUS - 2;
        double reach = Math.sqrt(HALL * HALL + unit(place) * (outer * outer - HALL * HALL));
        double around = unit(place >>> 12) * Math.PI * 2;
        // Each cut of the flurry a little off that place and turned a good way from the one before: they cross.
        double turn = unit(place >>> 24) * Math.PI * 2 + which * Math.toRadians(58 + 30 * unit(own));
        out.midX = (float) (Math.cos(around) * reach + (unit(own >>> 12) - 0.5) * 2.4);
        out.midZ = (float) (Math.sin(around) * reach + (unit(own >>> 24) - 0.5) * 2.4);
        out.radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * unit(own >>> 36);
        out.span = MIN_SPAN + (MAX_SPAN - MIN_SPAN) * unit(own >>> 48);
        // The blade goes level or nearly, either way round; the arc bulges up, sideways, or anything between.
        double pitch = (unit(own >>> 6) - 0.5) * Math.toRadians(36);
        double way = (own >>> 60 & 1) == 0 ? 1 : -1;
        double dx = Math.cos(turn) * way;
        double dz = Math.sin(turn) * way;
        out.ux = (float) (dx * Math.cos(pitch));
        out.uy = (float) Math.sin(pitch);
        out.uz = (float) (dz * Math.cos(pitch));
        // Square to the blade's way: up (leaned back by the pitch), and level across it.
        double upX = -dx * Math.sin(pitch);
        double upY = Math.cos(pitch);
        double upZ = -dz * Math.sin(pitch);
        double sideX = -dz;
        double sideZ = dx;
        double lean = (unit(own >>> 18) - 0.5) * Math.toRadians(170);
        out.vx = (float) (upX * Math.cos(lean) + sideX * Math.sin(lean));
        out.vy = (float) (upY * Math.cos(lean));
        out.vz = (float) (upZ * Math.cos(lean) + sideZ * Math.sin(lean));
        out.age = age;
        out.index = index;
        // As high as it likes, but its whole arc clear of the floor.
        out.midY = 0;
        float[] p = new float[3];
        float lowest = 0;
        for (int i = 0; i <= 8; i++) {
            point(out, i / 8F, 0, p);
            lowest = Math.min(lowest, p[1]);
        }
        out.midY = CLEAR - lowest + unit(own >>> 30) * 2.2F;
        return true;
    }

    /**
     * A point of a cut's arc.
     *
     * @param along 0 at the end the blade starts from, 1 at the other
     * @param inset blocks toward the circle's centre: 0 is the arc itself, the edge the blade cuts with
     */
    public static void point(Slash s, float along, float inset, float[] out) {
        double angle = (along - 0.5) * s.span;
        double r = s.radius - inset;
        // About the circle's centre, which lies a radius behind the middle of the arc.
        double a = Math.sin(angle) * r;
        double b = Math.cos(angle) * r - s.radius;
        out[0] = (float) (s.midX + s.ux * a + s.vx * b);
        out[1] = (float) (s.midY + s.uy * a + s.vy * b);
        out[2] = (float) (s.midZ + s.uz * a + s.vz * b);
    }

    /** The way the blade is going at a point of its arc: a unit direction. */
    public static void heading(Slash s, float along, float[] out) {
        double angle = (along - 0.5) * s.span;
        out[0] = (float) (s.ux * Math.cos(angle) - s.vx * Math.sin(angle));
        out[1] = (float) (s.uy * Math.cos(angle) - s.vy * Math.sin(angle));
        out[2] = (float) (s.uz * Math.cos(angle) - s.vz * Math.sin(angle));
    }

    /** How far along its arc the blade has got, 0 to 1: fast at first, easing to the end. */
    public static float swept(float age) {
        float t = age / SWEEP;
        if (t <= 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        float left = 1 - t;
        return 1 - left * left * left;
    }

    /** A number from 0 to 1 that belongs to one cut and one purpose: the same on every client. */
    public static float chance(int index, int purpose) {
        return unit(mix(index * 0x9E3779B97F4A7C15L + purpose * 0xD6E8FEB86659FD93L) >>> 20);
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
