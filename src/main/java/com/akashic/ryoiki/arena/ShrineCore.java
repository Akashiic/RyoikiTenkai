package com.akashic.ryoiki.arena;

/**
 * The crimson shrine stands in the middle of its arena, and is solid: this is the part of it players cannot walk
 * through, a square hall on its base, with a flat top, turned to face the way the caster looked. The client draws the
 * shrine
 * around these same measures. The caster stands in front of its door. No Minecraft classes.
 */
public final class ShrineCore {

    /** Half the hall's width, in blocks. */
    public static final float HALF = 5;
    /**
     * Half the width of what is solid: the hall and all it stands on. The hall is on a base of three stone tiers
     * wider than it, with steps before the door (the client's {@code ShrineLook}: the lowest tier is 8.6 wide and
     * the steps reach 8.7 where the hall is 5.4). People stop at the foot of that: any nearer, their legs would be
     * in the stone, and against the hall itself their eyes would be.
     */
    public static final float BASE = HALF * 8.7F / 5.4F;
    /** Its height above the floor. */
    public static final int HEIGHT = 7;
    /** How far in front of the shrine's centre the caster stands. */
    public static final float CASTER_DISTANCE = 13;
    /** How far from the centre, in blocks, a block may be part of the hall whatever way it is turned. */
    public static final int REACH = 12;

    /** How far from what is solid someone must be to be left where it is when a hall goes up. */
    public static final float CLEAR = 0.8F;

    private ShrineCore() {}

    /**
     * Whether the block at this offset from the arena's centre is part of the hall: its walls and its top, not its
     * inside.
     *
     * @param yaw where the caster looked, in degrees: the hall's door is toward that side
     */
    public static boolean solid(int dx, int dy, int dz, float yaw) {
        return solid(dx + 0.5, dy, dz + 0.5, yaw, false);
    }

    /**
     * The same for a hall that stands anywhere: {@code x} and {@code z} are where the middle of the block is from
     * the middle of the hall. A {@code filled} hall has no inside: for one that the arena's wall cuts short, which
     * would otherwise be open at the back.
     */
    public static boolean solid(double x, int dy, double z, float yaw, boolean filled) {
        if (dy < 0 || dy > HEIGHT) {
            return false;
        }
        double[] p = scene(x, z, yaw);
        double reach = Math.max(Math.abs(p[0]), Math.abs(p[1]));
        if (reach > BASE) {
            return false;
        }
        return filled || dy == HEIGHT || reach > HALF - 1.5;
    }

    /**
     * Where the hall of a shrine that answers another domain stands in a clash, and how it is turned: {x, z} from
     * the arena's centre, and the yaw of its door. The two domains meet on a line before the centre (at the centre
     * in a void, where the casters stand in a shrine: the client's {@code ClientDomain#sceneSeam}); the rival's hall
     * stands its own caster's distance beyond that line and faces it.
     *
     * @param hostShrine whether the domain it answers is a shrine too
     * @param yaw        where the host looked
     */
    public static double[] rival(boolean hostShrine, float yaw) {
        double[] at = world(0, (hostShrine ? CASTER_DISTANCE : 0) + CASTER_DISTANCE, yaw);
        return new double[] { at[0], at[1], yaw + 180 };
    }

    /**
     * Where someone standing at ({@code x}, {@code z}) from the middle of a hall goes when the hall is built: out
     * through the door side if that is inside it or against its walls, and where it is otherwise.
     */
    public static double[] clear(double x, double z, float yaw) {
        double[] p = scene(x, z, yaw);
        if (Math.max(Math.abs(p[0]), Math.abs(p[1])) < BASE + CLEAR) {
            return world(p[0], BASE + 2 * CLEAR, yaw);
        }
        return new double[] { x, z };
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
        return clear(x, z, yaw);
    }
}
