package com.akashic.ryoiki.fx;

/**
 * What the upper body does while a player uses a technique of the sorcerer mode: degrees for the chest, the head
 * and the arms of a player model, by the time since the technique began. The legs are not here: they go on doing
 * what they were doing. No Minecraft classes. (The axes are {@link BlowPose}'s.)
 */
public final class CastPose {

    public static final String[] BONES = { "UpBody", "AllHead", "RightArm", "RightForeArm", "LeftArm", "LeftForeArm" };

    /** The kinds of gesture. */
    public static final int PULL = 1;
    public static final int CUT = 2;
    public static final int RED = 3;
    /** Ticks after the red orb is let fly that the arm stays out and comes back. */
    public static final float RED_AFTER = 14;

    /** Ticks the cut's gesture lasts, and the least the pull's does. */
    public static final float PULL_TICKS = 16;
    public static final float CUT_TICKS = 13;

    private static final int CHEST = 0;
    private static final int HEAD = 3;
    private static final int R_ARM = 6;
    private static final int R_FORE = 9;
    private static final int L_ARM = 12;
    private static final int L_FORE = 15;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    private CastPose() {}

    /**
     * How much of a gesture that lasts {@code ticks} shows over whatever else the body does: it comes in fast and
     * lets go slowly.
     */
    public static float layer(float age, float ticks) {
        if (age <= 0 || age >= ticks) {
            return 0;
        }
        return Math.min(smooth(age / 2), smooth((ticks - age) / 5));
    }

    /**
     * @param ticks how long the gesture lasts: the cut's is always {@link #CUT_TICKS}; the pull's is as long as the
     *              orb is held out, and its last six ticks are the hand coming back
     */
    public static float[] sample(int kind, float age, float ticks) {
        return kind == CUT ? cut(age) : kind == RED ? red(age, ticks) : pull(age, ticks);
    }

    /**
     * Atração. The open hand is thrown out at the orb, the shoulder behind it, and held there for as long as the orb
     * is guided; then the hand closes and is drawn back toward the chest.
     */
    private static float[] pull(float age, float ticks) {
        float[] p = new float[BONES.length * 3];
        // The reach: out by tick 3, a little past where it settles.
        float out = smooth(age / 3) + 0.14F * bump((age - 3) / 3);
        // The draw: over the last six ticks.
        float in = smooth((age - (ticks - 6)) / 5);
        p[R_ARM + X] = -84 * out + 26 * in;
        p[R_ARM + Z] = 6 * out;
        p[R_FORE + X] = -8 * out - 62 * in;
        p[CHEST + Y] = -15 * out + 9 * in;
        p[CHEST + X] = 5 * out - 3 * in;
        // The eyes stay on the point: the head takes back the turn of the chest.
        p[HEAD + Y] = -0.9F * p[CHEST + Y];
        p[HEAD + X] = -0.6F * p[CHEST + X];
        // The other arm goes back, as a counterweight, elbow a little bent.
        p[L_ARM + X] = 20 * out - 6 * in;
        p[L_FORE + X] = -24 * out;
        return p;
    }

    /**
     * Repulsão. While it is gathered the hand is raised before the face, the elbow bent, the body drawn back a
     * little; when it is let fly the arm snaps straight out at what is ahead, the shoulder behind it, and stays
     * there a moment before it comes down.
     *
     * @param ticks the gathering and {@link #RED_AFTER} ticks more
     */
    private static float[] red(float age, float ticks) {
        float[] p = new float[BONES.length * 3];
        float fire = ticks - RED_AFTER;
        float raised = smooth(age / 5);
        // The snap: two ticks, a little past where it settles.
        float out = smooth((age - fire) / 2) + 0.12F * bump((age - fire - 2) / 4);
        p[R_ARM + X] = lerp(-128 * raised, -86, out);
        p[R_ARM + Z] = lerp(-6 * raised, 4, out);
        p[R_FORE + X] = lerp(-52 * raised, -4, out);
        p[CHEST + X] = lerp(-5 * raised, 7, out);
        p[CHEST + Y] = lerp(6 * raised, -16, out);
        p[HEAD + Y] = -0.9F * p[CHEST + Y];
        p[HEAD + X] = -0.6F * p[CHEST + X];
        p[L_ARM + X] = lerp(8 * raised, 22, out);
        p[L_FORE + X] = lerp(-20 * raised, -26, out);
        return p;
    }

    /**
     * Talho. The hand goes up across the body to the far shoulder, the chest wound that way; then it comes down
     * and across as a blade, the chest unwinding after it, and stays low at the end of the stroke before it lets go.
     */
    private static float[] cut(float age) {
        float[] p = new float[BONES.length * 3];
        float wound = smooth(age / 2.5F);
        // The stroke: from tick 2.5 to 5, fast in the middle.
        float stroke = smooth((age - 2.5F) / 2.5F);
        p[R_ARM + X] = lerp(lerp(0, -138, wound), -24, stroke);
        p[R_ARM + Z] = lerp(lerp(0, -32, wound), 36, stroke);
        p[R_FORE + X] = lerp(lerp(0, -58, wound), -6, stroke);
        p[CHEST + Y] = lerp(lerp(0, -24, wound), 26, stroke);
        p[CHEST + X] = lerp(0, 11, stroke);
        p[HEAD + Y] = -0.9F * p[CHEST + Y];
        p[HEAD + X] = -0.6F * p[CHEST + X];
        p[L_ARM + X] = lerp(lerp(0, -12, wound), 24, stroke);
        p[L_ARM + Z] = lerp(0, -10, stroke);
        p[L_FORE + X] = lerp(lerp(0, -30, wound), -14, stroke);
        return p;
    }

    /** An angle of the gesture, by the name of its bone and its axis (0 X, 1 Y, 2 Z): for tests. */
    public static float angle(float[] pose, String bone, int axis) {
        for (int i = 0; i < BONES.length; i++) {
            if (BONES[i].equals(bone)) {
                return pose[i * 3 + axis];
            }
        }
        throw new IllegalArgumentException(bone);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float smooth(float v) {
        v = v < 0 ? 0 : v > 1 ? 1 : v;
        return v * v * (3 - 2 * v);
    }

    /** 0 at 0 and at 1, 1 in between: a little more that comes and goes. */
    private static float bump(float v) {
        v = v < 0 ? 0 : v > 1 ? 1 : v;
        return (float) Math.sin(Math.PI * v);
    }
}
