package com.akashic.ryoiki.mode;

/**
 * The numbers of the techniques of the sorcerer mode, and the two pieces of geometry they turn on: how hard a place
 * pulls what is near it, and where a blade that flies straight meets a body. No Minecraft classes.
 *
 * <p>
 * <b>Atração</b> (the star void's first): an orb that goes where its caster looks. It appears before the caster, as
 * far as {@link #ORB_AIM} blocks, and for {@link #ORB_TICKS} ticks it follows the caster's eyes, at
 * {@link #ORB_SPEED} blocks a tick at most, drawing to itself whoever is within {@link #ORB_RADIUS} blocks of it and
 * carrying them along; when it ends, or when it is swung away faster than they can follow, they are let go with the
 * speed it had. It has two ways of being, by where its caster was when it began: on the <b>ground</b>, the caster
 * walks and the orb sweeps before it at about the height of a body; in the <b>air</b>, the caster hangs where it is
 * and the orb goes wherever the eyes go, the sky included.
 *
 * <p>
 * <b>Repulsão</b> (the star void's second): the opposite of the first. Its caster gathers it for
 * {@link #RED_CHARGE_TICKS} ticks and lets it fly: an orb that goes straight, {@link #RED_SPEED} blocks a tick, for
 * {@link #RED_RANGE} blocks or until something solid, through everyone in its way, throwing each one off along its
 * path. It harms less the farther it has flown. On the ground it flies nearly level; gathered in the air, its
 * caster hangs there and it flies wherever the eyes point. It has a second way: if its caster holds the mode's key
 * before it has flown its range, it <b>comes back</b> from the end of it. Whoever it meets on the way back it
 * brings along, to be set before its caster and struck with the <b>black blow</b>; if it comes back with nobody,
 * the caster takes it.
 *
 * <p>
 * <b>Talho</b> (the crimson shrine's first): a blade nobody sees leaves the caster and flies straight, up to
 * {@link #CUT_RANGE} blocks at {@link #CUT_SPEED} blocks a tick, and cuts the first it meets.
 */
public final class AbilityMath {

    /** The kinds of effect a client is told of. */
    public static final int CUT = 2;
    /** A flying cut met someone: it ends there. */
    public static final int CUT_HIT = 3;
    public static final int PULL_MOVE = 4;
    public static final int PULL_END = 5;
    /** The red orb: gathered by its caster, let fly, and each one it goes through. */
    public static final int RED_CHARGE = 6;
    public static final int RED_FIRE = 7;
    public static final int RED_HIT = 8;
    /** The red orb on its way back to who let it fly: where it is this tick. */
    public static final int RED_BACK = 9;
    /** The black blow lands on whoever the red orb brought back. */
    public static final int BLACK = 10;
    /** The red orb came back with nobody, and its caster takes it. */
    public static final int RED_SELF = 11;

    /** The orb's beginning, its move, and its end. */
    public static final int PULL = 1;

    public static final double ORB_AIM = 7;
    public static final double ORB_RADIUS = 3.5;
    /** 3.5 s. */
    public static final int ORB_TICKS = 70;
    /** Ticks until it may be used again: 12 s. */
    public static final int ORB_RECHARGE = 240;
    /** Blocks a tick the orb moves, at most, after its caster's eyes. */
    public static final double ORB_SPEED = 0.9;
    /** On the ground the orb keeps between these heights over its caster's feet. */
    public static final double ORB_LOW = 0.9;
    public static final double ORB_HIGH = 2.6;
    /** Ticks between two times it harms whoever it holds. */
    public static final int ORB_PULSE = 10;
    /** How much of the orb's speed those it lets go keep, the most they can leave with, and the lift they get. */
    public static final double ORB_THROW = 1.25;
    public static final double ORB_THROW_MOST = 1.7;
    public static final double ORB_THROW_LIFT = 0.18;
    /** Blocks a tick, at most, that the orb draws someone in; and the part of the way left it covers each tick. */
    public static final double PULL_FASTEST = 0.75;
    public static final double PULL_GAIN = 0.35;
    /** This near the orb there is nowhere left to go: whoever is there is held, and goes where it goes. */
    public static final double PULL_HELD = 0.5;

    /** A little over a second. */
    public static final int RED_CHARGE_TICKS = 24;
    public static final double RED_RANGE = 24;
    public static final double RED_SPEED = 2.4;
    /** 10 s. */
    public static final int RED_RECHARGE = 200;
    /** How near its line a body is hit. */
    public static final double RED_WIDTH = 0.6;
    /**
     * Where it leaves from: the hand that points, this far before the caster's eyes, to their right and under
     * them. It flies at what the eyes look at, not beside it.
     */
    public static final double RED_AHEAD = 0.9;
    public static final double RED_RIGHT = 0.32;
    public static final double RED_BELOW = 0.22;
    /** Blocks a tick on its way back, and how near its caster it has arrived. */
    public static final double RED_BACK_SPEED = 1.5;
    public static final double RED_ARRIVES = 2.2;
    /** Ticks it may take to come back before it gives up. */
    public static final int RED_BACK_TICKS = 60;
    /** Ticks from the one it brought being set before the caster to the blow: the fist's way. */
    public static final int BLACK_WIND = 3;
    /** How far before the caster the one it brought is set, and how the black blow throws. */
    public static final double BLACK_AT = 1.7;
    /** The step its caster takes into the blow, in blocks a tick. */
    public static final double BLACK_STEP = 0.32;
    public static final double BLACK_THROW = 1.9;
    public static final double BLACK_LIFT = 0.42;
    /** The part of its harm it still does at the end of its range. */
    public static final double RED_FAR = 7.0 / 30;
    /** How hard it throws who it goes through, near and at the end of its range, and the lift they get. */
    public static final double RED_THROW = 1.5;
    public static final double RED_THROW_FAR = 0.6;
    public static final double RED_LIFT = 0.38;
    /** On the ground it flies no steeper than this: the sine of twelve degrees. */
    public static final double RED_LEVEL = 0.208;

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
     * @param out the velocity, in blocks a tick: straight at the orb, faster from farther (never past it in one
     *            tick, never faster than {@link #PULL_FASTEST}), and none at all when held at it
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

    /**
     * One tick of the orb after its caster's eyes: from where it is toward where the eyes put it, no farther than
     * {@link #ORB_SPEED}.
     *
     * @param out the step it takes, which is also its velocity
     */
    public static void follow(double fromX, double fromY, double fromZ, double toX, double toY, double toZ,
        double[] out) {
        step(fromX, fromY, fromZ, toX, toY, toZ, ORB_SPEED, out);
    }

    /** One step from a place toward another, no longer than {@code speed}: all the way if it is nearer. */
    public static void step(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double speed,
        double[] out) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double scale = distance > speed ? speed / distance : 1;
        out[0] = dx * scale;
        out[1] = dy * scale;
        out[2] = dz * scale;
    }

    /**
     * The hand a technique leaves from, for a caster whose eyes are at a place and who looks along a unit vector:
     * {@link #RED_AHEAD} before the eyes, {@link #RED_RIGHT} to the caster's right, {@link #RED_BELOW} under them.
     */
    public static void hand(double eyeX, double eyeY, double eyeZ, double lookX, double lookY, double lookZ,
        double[] out) {
        double flat = Math.sqrt(lookX * lookX + lookZ * lookZ);
        // The caster's right, level: the way it looks, turned a quarter to the right. Looking straight up or
        // down there is none, and the hand is taken to be before the eyes.
        double rx = flat < 1e-6 ? 0 : -lookZ / flat;
        double rz = flat < 1e-6 ? 0 : lookX / flat;
        out[0] = eyeX + lookX * RED_AHEAD + rx * RED_RIGHT;
        out[1] = eyeY + lookY * RED_AHEAD - RED_BELOW;
        out[2] = eyeZ + lookZ * RED_AHEAD + rz * RED_RIGHT;
    }

    /** The unit vector from a place to another; the fallback if they are the same place. */
    public static void toward(double fromX, double fromY, double fromZ, double toX, double toY, double toZ,
        double[] fallback, double[] out) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 1e-6) {
            out[0] = fallback[0];
            out[1] = fallback[1];
            out[2] = fallback[2];
            return;
        }
        out[0] = dx / distance;
        out[1] = dy / distance;
        out[2] = dz / distance;
    }

    /**
     * What those the orb lets go leave with: its own velocity and a little more, never more than
     * {@link #ORB_THROW_MOST}, and a lift off the ground.
     */
    public static void thrown(double vx, double vy, double vz, double[] out) {
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz) * ORB_THROW;
        double scale = speed > ORB_THROW_MOST ? ORB_THROW_MOST / speed : 1;
        out[0] = vx * ORB_THROW * scale;
        out[1] = vy * ORB_THROW * scale + ORB_THROW_LIFT;
        out[2] = vz * ORB_THROW * scale;
    }

    /** The height the orb is put at on the ground: where the eyes look, kept about the height of a body. */
    public static double grounded(double aimedY, double feetY) {
        return Math.max(feetY + ORB_LOW, Math.min(feetY + ORB_HIGH, aimedY));
    }

    /** How far the red orb has flown, this many ticks after it was let fly, in a flight of {@code range} blocks. */
    public static double redGone(double age, double range) {
        return Math.max(0, Math.min(range, age * RED_SPEED));
    }

    /** What the red orb does to someone it meets this far along its way: all of {@code most} near, less far. */
    public static float redDamage(float most, double distance) {
        double far = Math.max(0, Math.min(1, distance / RED_RANGE));
        return (float) (most * (1 - (1 - RED_FAR) * far));
    }

    /** How hard it throws them, along its way. */
    public static double redThrow(double distance) {
        double far = Math.max(0, Math.min(1, distance / RED_RANGE));
        return RED_THROW + (RED_THROW_FAR - RED_THROW) * far;
    }

    /**
     * The way the red orb flies from the ground: where the eyes point, but nearly level.
     *
     * @param out a unit vector no steeper than {@link #RED_LEVEL}
     */
    public static void level(double dx, double dy, double dz, double[] out) {
        double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat < 1e-6) {
            // Straight up or down: there is no way ahead to keep. It goes where it is told.
            out[0] = dx;
            out[1] = dy;
            out[2] = dz;
            return;
        }
        double y = Math.max(-RED_LEVEL, Math.min(RED_LEVEL, dy));
        double scale = Math.sqrt(1 - y * y) / flat;
        out[0] = dx * scale;
        out[1] = y;
        out[2] = dz * scale;
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
