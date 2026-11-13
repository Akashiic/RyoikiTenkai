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

    /** Ticks each gesture lasts. */
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

    public static float ticks(int kind) {
        return kind == CUT ? CUT_TICKS : PULL_TICKS;
    }

    /** How much of the gesture shows over whatever else the body does: it comes in fast and lets go slowly. */
    public static float layer(int kind, float age) {
        float ticks = ticks(kind);
        if (age <= 0 || age >= ticks) {
            return 0;
        }
        return Math.min(smooth(age / 2), smooth((ticks - age) / 5));
    }

    public static float[] sample(int kind, float age) {
        return kind == CUT ? cut(age) : pull(age);
    }

    /**
     * Atração. The open hand is thrown out at the point, the shoulder behind it; held there a moment; then the hand
     * closes and is drawn back toward the chest, and what was out there comes with it.
     */
    private static float[] pull(float age) {
        float[] p = new float[BONES.length * 3];
        // The reach: out by tick 3, a little past where it settles.
        float out = smooth(age / 3) + 0.14F * bump((age - 3) / 3);
        // The draw: from tick 8 to 13.
        float in = smooth((age - 8) / 5);
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
