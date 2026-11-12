package com.akashic.ryoiki.arena;

/**
 * The crimson shrine stands in the middle of its arena, and is solid: this is the part of it players cannot walk
 * through, a square hall with a flat top, turned to face the way the caster looked. The client draws the shrine
 * around these same measures. The caster stands in front of its door. No Minecraft classes.
 */
public final class ShrineCore {

    /** Half the hall's width, in blocks. */
    public static final float HALF = 5;
    /** Its height above the floor. */
    public static final int HEIGHT = 7;
    /** How far in front of the shrine's centre the caster stands. */
    public static final float CASTER_DISTANCE = 13;
    /** How far from the centre, in blocks, a block may be part of the hall whatever way it is turned. */
    public static final int REACH = 8;

    private ShrineCore() {}

    /**
     * Whether the block at this offset from the arena's centre is part of the hall: its walls and its top, not its
     * inside.
     *
     * @param yaw where the caster looked, in degrees: the hall's door is toward that side
     */
    public static boolean solid(int dx, int dy, int dz, float yaw) {
        if (dy < 0 || dy > HEIGHT) {
            return false;
        }
        double[] p = scene(dx + 0.5, dz + 0.5, yaw);
        double reach = Math.max(Math.abs(p[0]), Math.abs(p[1]));
        if (reach > HALF) {
            return false;
        }
        return dy == HEIGHT || reach > HALF - 1.5;
    }

    /** A point of the arena (relative to its centre) in the shrine's own frame, where the door is toward +Z. */
    public static double[] scene(double x, double z, float yaw) {
        double a = Math.toRadians(yaw);
        return new double[] { x * Math.cos(a) + z * Math.sin(a), -x * Math.sin(a) + z * Math.cos(a) };
    }

    /** The other way: a point of the shrine's frame as an offset from the arena's centre. */
    public static double[] world(double sceneX, double sceneZ, float yaw) {
        double a = Math.toRadians(yaw);
        return new double[] { sceneX * Math.cos(a) - sceneZ * Math.sin(a),
            sceneX * Math.sin(a) + sceneZ * Math.cos(a) };
    }

    /**
     * Where a player whose offset from the caster is ({@code offsetX}, {@code offsetZ}) lands: the caster stands
     * before the door, the others keep their offsets from it; nobody lands inside the hall or beyond {@code limit}
     * blocks from the centre. Returns the offset from the arena's centre.
     */
    public static double[] place(double offsetX, double offsetZ, float yaw, double limit) {
        double[] caster = world(0, CASTER_DISTANCE, yaw);
        double x = caster[0] + offsetX;
        double z = caster[1] + offsetZ;
        double length = Math.sqrt(x * x + z * z);
        if (length > limit) {
            x *= limit / length;
            z *= limit / length;
        }
        double[] p = scene(x, z, yaw);
        if (Math.max(Math.abs(p[0]), Math.abs(p[1])) < HALF + 1.5) {
            // Out through the door side.
            return world(p[0], HALF + 2, yaw);
        }
        return new double[] { x, z };
    }
}
