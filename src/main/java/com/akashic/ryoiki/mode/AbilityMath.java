package com.akashic.ryoiki.mode;

/**
 * The numbers of the techniques of the sorcerer mode, and the two pieces of geometry they turn on: how hard a place
 * pulls what is near it, and where a blade that flies straight meets a body. No Minecraft classes.
 *
 * <p>
 * <b>Atração</b> (the star void's first): a point where the caster aims, up to {@link #PULL_AIM} blocks away, pulls
 * to itself for {@link #PULL_TICKS} ticks whoever is within {@link #PULL_RADIUS} blocks of it.
 *
 * <p>
 * <b>Talho</b> (the crimson shrine's first): a blade nobody sees leaves the caster and flies straight, up to
 * {@link #CUT_RANGE} blocks at {@link #CUT_SPEED} blocks a tick, and cuts the first it meets.
 */
public final class AbilityMath {

    /** The kinds of effect a client is told of. */
    public static final int PULL = 1;
    public static final int CUT = 2;
    /** A flying cut met someone: it ends there. */
    public static final int CUT_HIT = 3;

    public static final double PULL_AIM = 8;
    public static final double PULL_RADIUS = 4;
    public static final int PULL_TICKS = 20;
    /** Ticks until it may be used again: 8 s. */
    public static final int PULL_RECHARGE = 160;
    /** Blocks a tick, at most, that the point draws someone in; and the part of the way left it covers each tick. */
    public static final double PULL_FASTEST = 0.5;
    public static final double PULL_GAIN = 0.3;
    /** This near the point there is nowhere left to go: whoever is there is held. */
    public static final double PULL_HELD = 0.35;

    public static final double CUT_RANGE = 12;
    public static final double CUT_SPEED = 1.5;
    /** 5 s. */
    public static final int CUT_RECHARGE = 100;
    /** Half the blade's width: a body this near its line is cut. */
    public static final double CUT_WIDTH = 0.4;
    /** Where the blade starts: this far before the caster's eyes, and this far under them. */
    public static final double CUT_AHEAD = 0.7;
    public static final double CUT_BELOW = 0.35;
    /** How hard a cut throws who it meets along its way, and up, in blocks a tick. */
    public static final double CUT_THROW = 0.25;
    public static final double CUT_LIFT = 0.2;

    private AbilityMath() {}

    /**
     * The velocity the point gives someone, for one tick.
     *
     * @param dx  from that someone to the point
     * @param out the velocity, in blocks a tick: straight at the point, faster from farther (never past the point in
     *            one tick, never faster than {@link #PULL_FASTEST}), and none at all when held at it
     */
    public static void pull(double dx, double dy, double dz, double[] out) {
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < PULL_HELD) {
            out[0] = 0;
            out[1] = 0;
            out[2] = 0;
            return;
        }
        double speed = Math.min(PULL_FASTEST, distance * PULL_GAIN);
        out[0] = dx / distance * speed;
        out[1] = dy / distance * speed;
        out[2] = dz / distance * speed;
    }

    /** How far a flying cut has gone, this many ticks after it left, in a flight of {@code range} blocks. */
    public static double cutGone(double age, double range) {
        return Math.max(0, Math.min(range, age * CUT_SPEED));
    }

    /** Ticks a flight of {@code range} blocks takes, rounded up. */
    public static int cutTicks(double range) {
        return (int) Math.ceil(range / CUT_SPEED);
    }

    /**
     * How far along a line a box begins: the distance from the line's start, along its direction (a unit vector), to
     * where it enters the box; 0 if it starts inside; -1 if it never meets it.
     */
    public static double enters(double ox, double oy, double oz, double dx, double dy, double dz, double minX,
        double minY, double minZ, double maxX, double maxY, double maxZ) {
        double near = 0;
        double far = Double.MAX_VALUE;
        double[] o = { ox, oy, oz };
        double[] d = { dx, dy, dz };
        double[] min = { minX, minY, minZ };
        double[] max = { maxX, maxY, maxZ };
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9) {
                if (o[i] < min[i] || o[i] > max[i]) {
                    return -1;
                }
                continue;
            }
            double a = (min[i] - o[i]) / d[i];
            double b = (max[i] - o[i]) / d[i];
            near = Math.max(near, Math.min(a, b));
            far = Math.min(far, Math.max(a, b));
            if (near > far) {
                return -1;
            }
        }
        return near;
    }
}
