package com.akashic.ryoiki.fx;

/**
 * What the upper body does while a player uses a technique of the sorcerer mode: degrees for the chest, the head
 * and the arms of a player model, by the time since the technique began. The legs are not here: they go on doing
 * what they were doing. No Minecraft classes. (The axes are {@link BlowPose}'s.)
 */
public final class CastPose {

    public static final String[] BONES = { "UpBody", "AllHead", "RightArm", "RightForeArm", "LeftArm", "LeftForeArm",
        "RightHand", "right_fg", "right_ffg", "left_dmz" };

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
    /** The right hand, its four fingers (as one, in two joints) and its thumb: the names of the network's models. */
    private static final int R_HAND = 18;
    private static final int R_THUMB = 27;
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
        return sample(kind, age, ticks, 0);
    }

    /**
     * @param pitch where the caster looks, in degrees, down positive: the arm that points, points there
     */
    public static float[] sample(int kind, float age, float ticks, float pitch) {
        return kind == CUT ? cut(age) : kind == RED ? red(age, ticks, pitch) : pull(age, ticks, pitch);
    }

    /**
     * Atração. The open hand is thrown out at the orb, the shoulder behind it, and held there for as long as the orb
     * is guided; then the hand closes and is drawn back toward the chest.
     */
    private static float[] pull(float age, float ticks, float pitch) {
        float[] p = new float[BONES.length * 3];
        // The reach: out by tick 3, a little past where it settles.
        float out = smooth(age / 3) + 0.14F * bump((age - 3) / 3);
        // The draw: over the last six ticks.
        float in = smooth((age - (ticks - 6)) / 5);
        // The arm is held out along the eyes: up when they look up.
        p[R_ARM + X] = (-84 + 0.85F * clampPitch(pitch)) * out + 26 * in;
        // The hand open, the thumb out, while it guides.
        p[R_THUMB + Z] = 30 * out * (1 - in);
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
     * Repulsão. The arm goes up overhead, the hand open and the fingers straight, and is held there while the orb
     * gathers over the fingertips: the body drawn up and a little back, that shoulder behind, the other arm loose
     * at the side. When it is let fly the arm comes down hard to point where the eyes look, the shoulder thrown
     * forward after it; it kicks up from the shot, settles on the line, and is held there a moment.
     *
     * @param ticks the gathering and {@link #RED_AFTER} ticks more
     */
    private static float[] red(float age, float ticks, float pitch) {
        float[] p = new float[BONES.length * 3];
        float fire = ticks - RED_AFTER;
        // Up in four ticks, a little past the top.
        float up = smooth(age / 4) + 0.06F * bump((age - 4) / 4);
        // The hand trembles more as the orb fills.
        float full = clamp(age / fire);
        float tremble = (float) Math.sin(age * 1.9F) * 1.3F * full * (age < fire ? 1 : 0);
        // The throw: two ticks down to the line, then the kick of the shot.
        float out = smooth((age - fire) / 2);
        float kick = bump((age - fire - 1.5F) / 5);
        float aim = -88 + 0.85F * clampPitch(pitch);
        p[R_ARM + X] = lerp((-162 + tremble) * up, aim - 11 * kick, out);
        p[R_ARM + Z] = lerp(9 * up, 2, out);
        p[R_FORE + X] = lerp(-20 * up, -3, out);
        p[CHEST + X] = lerp(-7 * up, 9 - 4 * kick, out);
        p[CHEST + Y] = lerp(11 * up, -19, out);
        p[HEAD + Y] = -0.9F * p[CHEST + Y];
        p[HEAD + X] = -0.6F * p[CHEST + X];
        // The other arm: loose, the hand by the hip; it goes back as the shot goes forward.
        p[L_ARM + X] = lerp(6 * up, 26, out);
        p[L_ARM + Z] = lerp(-3 * up, -8, out);
        p[L_FORE + X] = lerp(-34 * up, -18, out);
        // The wrist bent back so the palm is under the orb; straight along the arm when it points.
        p[R_HAND + X] = lerp(-18 * up, 0, out);
        p[R_THUMB + Z] = lerp(-35 * up, 20, out);
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

    private static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    /** As far up or down as an arm follows the eyes. */
    private static float clampPitch(float pitch) {
        return pitch < -70 ? -70 : pitch > 55 ? 55 : pitch;
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
