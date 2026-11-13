package com.akashic.ryoiki.fx;

/**
 * The blows of a clash as movement of the whole body, in degrees for the bones of a player model (the standard
 * names of the network's models), as a function of the time since the blow began. No Minecraft classes.
 *
 * <p>
 * What each axis does, measured on a model (docs/VISUAL.md has the table): about X, positive leans the body
 * forward, swings an arm or a leg back, bends a knee and points a foot's toes down; about Y, positive turns to the
 * model's own right; about Z, positive leans to its right and lifts the right arm or leg out sideways (the left ones
 * are mirrored).
 *
 * <p>
 * A blow is built from three poses of the same bones: the guard it starts from and returns to, a short gathering
 * before it, and the strike. The body does not take the strike all at once: the hips turn first, the chest follows,
 * the arm last, each on its own curve. Two things are computed, not posed, so they hold at every instant: the head
 * turns back by what the body turned, to stay on the target, and each foot's sole stays level with the ground,
 * whatever the leg does above it.
 */
public final class BlowPose {

    public static final String[] BONES = { "AllBody", "UpBody", "AllHead", "RightArm", "RightForeArm", "LeftArm",
        "LeftForeArm", "RightLeg", "RightLowerLeg", "RightFoot", "LeftLeg", "LeftLowerLeg", "LeftFoot" };

    private static final int ALL = 0;
    private static final int CHEST = 1;
    private static final int HEAD = 2;
    private static final int R_ARM = 3;
    private static final int R_FORE = 4;
    private static final int L_ARM = 5;
    private static final int L_FORE = 6;
    private static final int R_LEG = 7;
    private static final int R_SHIN = 8;
    private static final int R_FOOT = 9;
    private static final int L_LEG = 10;
    private static final int L_SHIN = 11;
    private static final int L_FOOT = 12;

    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    /** Ticks a blow is drawn for. */
    public static final float TICKS = 10;
    /** The tick the fist arrives at. */
    public static final float IMPACT = 3;

    /** The parts of the body, in the order the strike passes through them. */
    private static final int HIPS = 0;
    private static final int TORSO = 1;
    private static final int ARM = 2;
    /** For each part: when its strike begins and ends, and when its return begins and ends. */
    private static final float[][] TIMING = { { 0.9F, 2.4F, 4.6F, 9.2F }, { 1.2F, 2.7F, 4.3F, 8.6F },
        { 1.5F, IMPACT, 4.0F, 7.6F } };
    private static final int[] PART = new int[BONES.length];

    static {
        PART[ALL] = HIPS;
        PART[R_LEG] = HIPS;
        PART[R_SHIN] = HIPS;
        PART[R_FOOT] = HIPS;
        PART[L_LEG] = HIPS;
        PART[L_SHIN] = HIPS;
        PART[L_FOOT] = HIPS;
        PART[CHEST] = TORSO;
        PART[HEAD] = TORSO;
        PART[L_ARM] = TORSO;
        PART[L_FORE] = TORSO;
        PART[R_ARM] = ARM;
        PART[R_FORE] = ARM;
    }

    /** How much of the body's turn the head takes back, and of its lean. */
    private static final float HEAD_TURN = 0.92F;
    private static final float HEAD_LEAN = 0.6F;
    /** Which way the rear foot points, in the guard and at the strike: it turns on its ball as the hip comes. */
    private static final float REAR_FOOT_GUARD = 42;
    private static final float REAR_FOOT_STRIKE = 12;
    /** How far the rear heel is off the ground, in the guard and at the strike. */
    private static final float HEEL_GUARD = 8;
    private static final float HEEL_STRIKE = 26;

    /**
     * The guard: the left side leading, the body a fifth of a turn off the line, the weight on bent knees, the rear
     * fist at the chin and the lead one a little out.
     */
    private static final float[] GUARD = pose(
        new float[][] { { 0, 20, 0 }, { 5, 0, 0 }, { 0, 0, 0 }, { -34, -8, 0 }, { -122, 0, 0 }, { -56, 10, 0 },
            { -100, 0, 0 }, { 14, 0, 0 }, { 16, 0, 0 }, { 0, 0, 0 }, { -16, 0, 0 }, { 20, 0, 0 }, { 0, 0, 0 } });
    /** Gathering for the straight right: a little more turned, the weight on the rear leg, the fist drawn in. */
    private static final float[] GATHER_1 = pose(
        new float[][] { { 0, 25, 0 }, { 4, 6, 0 }, { 0, 0, 0 }, { -26, -8, 0 }, { -130, 0, 0 }, { -56, 10, 0 },
            { -102, 0, 0 }, { 12, 0, 0 }, { 22, 0, 0 }, { 0, 0, 0 }, { -12, 0, 0 }, { 14, 0, 0 }, { 0, 0, 0 } });
    /**
     * The straight right at its end: the hips turned through the line, the chest further, the right arm out along
     * the line at the height of the shoulder, the left fist tight at the chin, the weight on the lead leg and the
     * rear leg long behind.
     */
    private static final float[] STRIKE_1 = pose(
        new float[][] { { 0, -12, 0 }, { 12, -22, -3 }, { 0, 0, 0 }, { -84, 30, 0 }, { -5, 0, 0 }, { -48, 14, 0 },
            { -128, 0, 0 }, { 20, 0, 0 }, { 8, 0, 0 }, { 0, 0, 0 }, { -22, 0, 0 }, { 28, 0, 0 }, { 0, 0, 0 } });

    /**
     * The other three blows are not worked yet: one pose each, taken and left on one curve by the whole body at
     * once. They are here so the chain is whole while each is done in its turn, as the first was.
     */
    private static final float[][] ROUGH = {
        pose(
            new float[][] { { 0, 28, 0 }, { 8, 14, 0 }, { 0, 0, 0 }, { -40, -8, 0 }, { -126, 0, 0 }, { -98, -32, 0 },
                { -5, 0, 0 }, { 16, 0, 0 }, { 14, 0, 0 }, { 0, 0, 0 }, { -20, 0, 0 }, { 24, 0, 0 }, { 0, 0, 0 } }),
        pose(
            new float[][] { { 0, -10, 0 }, { 2, -30, 0 }, { 0, 0, 0 }, { -122, 12, 0 }, { -82, 0, 0 }, { -48, 14, 0 },
                { -128, 0, 0 }, { 18, 0, 0 }, { 10, 0, 0 }, { 0, 0, 0 }, { -20, 0, 0 }, { 26, 0, 0 }, { 0, 0, 0 } }),
        pose(
            new float[][] { { 0, 6, 0 }, { -12, 0, 0 }, { 0, 0, 0 }, { -20, -8, 0 }, { -110, 0, 0 }, { -44, 10, 0 },
                { -108, 0, 0 }, { -88, 0, 0 }, { 8, 0, 0 }, { 0, 0, 0 }, { 4, 0, 0 }, { 6, 0, 0 }, { 0, 0, 0 } }) };

    private BlowPose() {}

    private static float[] pose(float[][] bones) {
        float[] out = new float[BONES.length * 3];
        for (int i = 0; i < BONES.length; i++) {
            System.arraycopy(bones[i], 0, out, i * 3, 3);
        }
        return out;
    }

    /** Whether a blow has its movement here yet. */
    public static boolean has(int blow) {
        return blow == 1;
    }

    /** The guard, as a pose of {@link #BONES}. */
    public static float[] guard() {
        return solve(GUARD.clone(), 0);
    }

    /**
     * The pose of {@link #BONES} a blow is in.
     *
     * @param time ticks since the blow began, with fractions
     */
    public static float[] sample(int blow, float time) {
        float[] out = new float[BONES.length * 3];
        if (!has(blow)) {
            float[] rough = ROUGH[Math.max(2, Math.min(4, blow)) - 2];
            float reach = smooth(time / 2.5F) * (1 - smooth((time - 4) / 5));
            for (int i = 0; i < out.length; i++) {
                out[i] = GUARD[i] + (rough[i] - GUARD[i]) * reach;
            }
            // The kick is on the rear leg: its foot is in the air, and keeps the angle the pose gives it.
            return blow == 4 ? kick(solve(out, 0), reach) : solve(out, reach);
        }
        // The gathering comes and is gone before the strike is whole.
        float gather = smooth(time / 1.0F) * (1 - smooth((time - 1.0F) / 1.3F));
        for (int bone = 0; bone < BONES.length; bone++) {
            float[] t = TIMING[PART[bone]];
            float strike = strike(PART[bone], time, t);
            for (int axis = 0; axis < 3; axis++) {
                int i = bone * 3 + axis;
                out[i] = GUARD[i] + (GATHER_1[i] - GUARD[i]) * gather + (STRIKE_1[i] - GUARD[i]) * strike;
            }
        }
        // The fist goes from the chin to the target along a straight line: the shoulder and the elbow are solved
        // for it, not swung each on its own (which would carry the fist up in an arc).
        float reach = strike(ARM, time, TIMING[ARM]);
        float[] from = fist(GUARD[R_ARM * 3 + X], GUARD[R_FORE * 3 + X]);
        float[] to = fist(STRIKE_1[R_ARM * 3 + X], STRIKE_1[R_FORE * 3 + X]);
        float[] arm = reach(from[0] + (to[0] - from[0]) * reach, from[1] + (to[1] - from[1]) * reach);
        out[R_ARM * 3 + X] = arm[0] + (GATHER_1[R_ARM * 3 + X] - GUARD[R_ARM * 3 + X]) * gather;
        out[R_FORE * 3 + X] = arm[1] + (GATHER_1[R_FORE * 3 + X] - GUARD[R_FORE * 3 + X]) * gather;
        return solve(out, strike(HIPS, time, TIMING[HIPS]));
    }

    /**
     * Where the fist is, before and above the shoulder, for the angles of the upper arm and of the forearm about X:
     * the two as long as each other, their length the unit.
     */
    static float[] fist(float armX, float foreX) {
        double upper = Math.toRadians(-armX);
        double fore = Math.toRadians(-armX - foreX);
        return new float[] { (float) (Math.sin(upper) + Math.sin(fore)), (float) (-Math.cos(upper) - Math.cos(fore)) };
    }

    /** The angles of the upper arm and of the forearm about X that put the fist there, the elbow down. */
    static float[] reach(float forward, float up) {
        double d = Math.min(2, Math.max(1e-3, Math.sqrt(forward * forward + up * up)));
        double line = Math.atan2(forward, -up);
        double inner = Math.acos(d / 2);
        double elbow = Math.PI - Math.acos(1 - d * d / 2);
        return new float[] { (float) -Math.toDegrees(line - inner), (float) -Math.toDegrees(elbow) };
    }

    /** How far into its strike a part is, 0 to 1: out on its own curve, held, and back. */
    private static float strike(int part, float time, float[] t) {
        float out = (time - t[0]) / (t[1] - t[0]);
        // The arm snaps out and slows at the end; the hips and the chest gather speed and lose it.
        float reached = part == ARM ? 1 - cube(1 - clamp(out)) : smooth(out);
        return reached * (1 - smooth((time - t[2]) / (t[3] - t[2])));
    }

    /** What is computed from the rest: the feet on the ground and the head on the target. */
    private static float[] solve(float[] p, float hips) {
        float turn = p[ALL * 3 + Y];
        // The lead foot points down the line whatever the body does; the rear one turns on its ball.
        p[L_FOOT * 3 + Y] = -turn;
        p[R_FOOT * 3 + Y] = REAR_FOOT_GUARD + (REAR_FOOT_STRIKE - REAR_FOOT_GUARD) * hips - turn;
        // A sole is level when the turns above it cancel; the rear heel is off the ground by what is asked.
        p[L_FOOT * 3 + X] = -(p[L_LEG * 3 + X] + p[L_SHIN * 3 + X]);
        p[R_FOOT * 3 + X] = -(p[R_LEG * 3 + X] + p[R_SHIN * 3 + X]) + HEEL_GUARD + (HEEL_STRIKE - HEEL_GUARD) * hips;
        p[HEAD * 3 + Y] = -(turn + p[CHEST * 3 + Y]) * HEAD_TURN;
        p[HEAD * 3 + X] = -p[CHEST * 3 + X] * HEAD_LEAN;
        p[HEAD * 3 + Z] = -p[CHEST * 3 + Z];
        return p;
    }

    private static float[] kick(float[] p, float reach) {
        p[R_FOOT * 3 + X] = p[R_FOOT * 3 + X] * (1 - reach) - 30 * reach;
        return p;
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    /** 0 to 1 with no speed at either end. */
    private static float smooth(float v) {
        float c = clamp(v);
        return c * c * (3 - 2 * c);
    }

    private static float cube(float v) {
        return v * v * v;
    }

    /** One angle of a pose, by bone name and axis (0, 1, 2 for X, Y, Z): for tests and tools. */
    public static float angle(float[] pose, String bone, int axis) {
        for (int i = 0; i < BONES.length; i++) {
            if (BONES[i].equals(bone)) {
                return pose[i * 3 + axis];
            }
        }
        throw new IllegalArgumentException(bone);
    }
}
