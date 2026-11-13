package com.akashic.ryoiki.fx;

/**
 * What the body does while a player uses a technique of the sorcerer mode: degrees for the bones of a player model,
 * by the time since the technique began. The whole body, hands and fingers included: nothing of the model's own
 * resting pose is left to show through (a model may rest with a hand on its hip). No Minecraft classes. (The axes
 * are {@link BlowPose}'s; the first thirteen bones are its bones, in its order.)
 */
public final class CastPose {

    public static final String[] BONES = { "AllBody", "UpBody", "AllHead", "RightArm", "RightForeArm", "LeftArm",
        "LeftForeArm", "RightLeg", "RightLowerLeg", "RightFoot", "LeftLeg", "LeftLowerLeg", "LeftFoot", "RightHand",
        "right_fg", "right_ffg", "left_dmz", "LeftHand", "left_fg", "left_ffg", "right_dmz" };

    /** The kinds of gesture. */
    public static final int PULL = 1;
    public static final int CUT = 2;
    public static final int RED = 3;
    /** Squared up, the fist drawn back: waiting for what the red orb brings. */
    public static final int READY = 4;
    /** The black blow. */
    public static final int BLACK = 5;

    /** Ticks the cut's gesture lasts, and the least the pull's does. */
    public static final float PULL_TICKS = 16;
    public static final float CUT_TICKS = 13;
    /** Ticks after the red orb is let fly that the arm stays out and comes back. */
    public static final float RED_AFTER = 16;
    /** The black blow: ticks to the fist's arrival, and all of it. */
    public static final float BLACK_IMPACT = 3;
    public static final float BLACK_TICKS = 26;

    private static final int ALL = 0;
    private static final int CHEST = 3;
    private static final int HEAD = 6;
    private static final int R_ARM = 9;
    private static final int R_FORE = 12;
    private static final int L_ARM = 15;
    private static final int L_FORE = 18;
    private static final int R_LEG = 21;
    private static final int R_SHIN = 24;
    private static final int R_FOOT = 27;
    private static final int L_LEG = 30;
    private static final int L_SHIN = 33;
    private static final int L_FOOT = 36;
    /** The right hand, its four fingers (as one, in two joints) and its thumb: the names of the network's models. */
    private static final int R_FINGERS = 42;
    private static final int R_TIPS = 45;
    private static final int R_THUMB = 48;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    /** How far the fingers close into a fist, each joint: about the hand's own X, seen on the model. */
    private static final float FIST = 78;

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
        float[] p;
        switch (kind) {
            case CUT:
                p = cut(age);
                break;
            case RED:
                p = red(age, ticks, pitch);
                break;
            case READY:
                p = ready(age);
                break;
            case BLACK:
                p = black(age, pitch);
                break;
            default:
                p = pull(age, ticks, pitch);
        }
        return solve(p);
    }

    /**
     * Atração. The open hand is thrown out at the orb, the shoulder behind it, and held there for as long as the orb
     * is guided; then the hand closes and is drawn back toward the chest. The other arm hangs, a little behind.
     */
    private static float[] pull(float age, float ticks, float pitch) {
        float[] p = new float[BONES.length * 3];
        // The reach: out by tick 3, a little past where it settles.
        float out = smooth(age / 3) + 0.14F * bump((age - 3) / 3);
        // The draw: over the last six ticks.
        float in = smooth((age - (ticks - 6)) / 5);
        p[CHEST + Y] = -15 * out + 9 * in;
        p[CHEST + X] = 5 * out - 3 * in;
        // The arm is held out along the eyes: up when they look up.
        p[R_ARM + X] = (-84 + 0.85F * clampPitch(pitch) - p[CHEST + X]) * out + 26 * in;
        p[R_ARM + Z] = 6 * out;
        p[R_FORE + X] = -8 * out - 62 * in;
        // The hand open, the thumb out, while it guides; closed as it is drawn back.
        p[R_THUMB + Z] = 30 * out * (1 - in);
        fist(p, in);
        // The other arm hangs, a little behind and off the body: a counterweight, not a pose.
        p[L_ARM + X] = 16 * out - 6 * in;
        p[L_ARM + Z] = -7 * out;
        p[L_FORE + X] = -9 * out;
        // The feet a little apart along the line, the weight forward.
        p[L_LEG + X] = -8 * out;
        p[L_SHIN + X] = 9 * out;
        p[R_LEG + X] = 7 * out;
        p[R_SHIN + X] = 3 * out;
        return p;
    }

    /**
     * Repulsão. A throw. The arm goes up overhead, the hand open and the fingers straight, and the whole body is
     * drawn back under it and wound away from the target while the orb gathers over the fingertips: the weight on the
     * rear leg, the other arm hanging off the body. When it is let fly the body whips forward over the lead leg and
     * the arm comes down hard to point where the eyes look; it kicks up from the shot, settles on the line, and is
     * held there a moment before the body comes back up.
     *
     * @param ticks the gathering and {@link #RED_AFTER} ticks more
     */
    private static float[] red(float age, float ticks, float pitch) {
        float[] p = new float[BONES.length * 3];
        float fire = ticks - RED_AFTER;
        // Up and back in five ticks, a little past where it holds.
        float up = smooth(age / 5) + 0.06F * bump((age - 5) / 4);
        // The hand trembles more as the orb fills.
        float full = clamp(age / fire);
        float tremble = (float) Math.sin(age * 1.9F) * 1.3F * full * (age < fire ? 1 : 0);
        // The throw: the hips first, the chest after them, the arm last; then the kick of the shot.
        float hips = smooth((age - fire + 0.6F) / 2.2F);
        float chest = smooth((age - fire + 0.2F) / 2.2F);
        float out = smooth((age - fire) / 2);
        float kick = bump((age - fire - 1.5F) / 5);
        p[ALL + X] = lerp(-8 * up, 12, hips);
        p[ALL + Y] = lerp(9 * up, -8, hips);
        p[CHEST + X] = lerp(-13 * up, 22 - 4 * kick, chest);
        p[CHEST + Y] = lerp(13 * up, -22, chest);
        float lean = p[ALL + X] + p[CHEST + X];
        // Overhead while it gathers; on the line of the eyes when it is thrown, whatever the body leans.
        float aim = -88 + 0.85F * clampPitch(pitch);
        p[R_ARM + X] = lerp((-160 + tremble) * up, aim - lean - 11 * kick, out);
        p[R_ARM + Z] = lerp(8 * up, 2, out);
        p[R_FORE + X] = lerp(-16 * up, -3, out);
        // The other arm: hanging off the body, behind it; thrown back as the shot goes forward.
        p[L_ARM + X] = lerp(13 * up, 52, chest);
        p[L_ARM + Z] = lerp(-11 * up, -16, chest);
        p[L_FORE + X] = lerp(-7 * up, -14, chest);
        // The weight on the rear leg, and then over the lead one.
        p[R_LEG + X] = lerp(9 * up, 24, hips);
        p[R_SHIN + X] = lerp(12 * up, 8, hips);
        p[L_LEG + X] = lerp(-11 * up, -27, hips);
        p[L_SHIN + X] = lerp(5 * up, 31, hips);
        // The thumb folded while the fingers hold the orb up; out when the hand points.
        p[R_THUMB + Z] = lerp(-35 * up, 20, out);
        return p;
    }

    /**
     * Squared up for the black blow: the body turned to put the striking side behind, the weight low on bent knees,
     * that fist drawn back to the ribs and closed, the other hand out before the body, open, toward what is coming.
     */
    private static float[] ready(float age) {
        float[] p = new float[BONES.length * 3];
        float in = smooth(age / 5);
        // The fist shakes with what it holds.
        float tremble = (float) Math.sin(age * 2.3F) * 1.5F * in;
        wound(p, in, tremble);
        return p;
    }

    /** The wound-up stance, as much of it as {@code in}. */
    private static void wound(float[] p, float in, float tremble) {
        p[ALL + Y] = 27 * in;
        p[ALL + X] = 4 * in;
        p[CHEST + Y] = 21 * in;
        p[CHEST + X] = 9 * in;
        p[R_ARM + X] = (40 + tremble) * in;
        p[R_ARM + Z] = 15 * in;
        p[R_FORE + X] = -116 * in;
        p[L_ARM + X] = -64 * in;
        p[L_ARM + Z] = -6 * in;
        p[L_FORE + X] = -30 * in;
        p[L_LEG + X] = -23 * in;
        p[L_SHIN + X] = 31 * in;
        p[R_LEG + X] = 15 * in;
        p[R_SHIN + X] = 25 * in;
        fist(p, in);
    }

    /**
     * The black blow. From the wound-up stance the hips turn, the chest after them, and the fist goes out straight
     * down the line of the eyes with the whole body behind it, over a deep lead leg; the other arm is thrown back.
     * It arrives at {@link #BLACK_IMPACT}, drives a little through, and is held there, the body low and long, while
     * what it did plays out; then the body gathers itself and stands.
     */
    private static float[] black(float age, float pitch) {
        float[] p = new float[BONES.length * 3];
        wound(p, 1, 0);
        float hips = smooth(age / 2.2F);
        float chest = smooth((age - 0.5F) / 2.2F);
        // The arm snaps: slow to leave, there all at once.
        float arm = clamp((age - 1) / (BLACK_IMPACT - 1));
        arm = 1 - (1 - arm) * (1 - arm);
        // Through the target, and settling.
        float through = bump((age - BLACK_IMPACT) / 6);
        p[ALL + Y] = lerp(p[ALL + Y], -17 - 4 * through, hips);
        p[ALL + X] = lerp(p[ALL + X], 15, hips);
        p[CHEST + Y] = lerp(p[CHEST + Y], -33 - 5 * through, chest);
        p[CHEST + X] = lerp(p[CHEST + X], 20, chest);
        float lean = p[ALL + X] + p[CHEST + X];
        float aim = -90 + 0.85F * clampPitch(pitch);
        p[R_ARM + X] = lerp(p[R_ARM + X], aim - lean, arm);
        p[R_ARM + Z] = lerp(p[R_ARM + Z], 0, arm);
        p[R_FORE + X] = lerp(p[R_FORE + X], -4, arm);
        p[L_ARM + X] = lerp(p[L_ARM + X], 46, chest);
        p[L_ARM + Z] = lerp(p[L_ARM + Z], -14, chest);
        p[L_FORE + X] = lerp(p[L_FORE + X], -52, chest);
        p[L_LEG + X] = lerp(p[L_LEG + X], -40, hips);
        p[L_SHIN + X] = lerp(p[L_SHIN + X], 52, hips);
        p[R_LEG + X] = lerp(p[R_LEG + X], 33, hips);
        p[R_SHIN + X] = lerp(p[R_SHIN + X], 7, hips);
        // Standing again, over the last ticks: everything eases back to nothing.
        float back = smooth((age - (BLACK_TICKS - 11)) / 8);
        if (back > 0) {
            for (int i = 0; i < p.length; i++) {
                p[i] *= 1 - back;
            }
        }
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
        p[L_ARM + X] = lerp(lerp(0, -8, wound), 24, stroke);
        p[L_ARM + Z] = lerp(-5 * wound, -12, stroke);
        p[L_FORE + X] = lerp(lerp(0, -12, wound), -10, stroke);
        // A step into the stroke.
        p[L_LEG + X] = lerp(-6 * wound, -16, stroke);
        p[L_SHIN + X] = lerp(4 * wound, 18, stroke);
        p[R_LEG + X] = lerp(5 * wound, 14, stroke);
        p[R_SHIN + X] = lerp(6 * wound, 5, stroke);
        return p;
    }

    /** The fingers of the right hand closed, as much as {@code closed}. */
    private static void fist(float[] p, float closed) {
        p[R_FINGERS + X] = FIST * closed;
        p[R_TIPS + X] = FIST * closed;
        p[R_THUMB + Z] = lerp(p[R_THUMB + Z], -40, closed);
    }

    /**
     * What is computed from the rest, so that it holds at every instant: the head takes back what the body under it
     * turns and leans, to stay level and on the target; and each foot's sole stays level with the ground, the rear
     * heel lifting as that leg goes back.
     */
    private static float[] solve(float[] p) {
        p[HEAD + Y] = -0.92F * (p[ALL + Y] + p[CHEST + Y]);
        p[HEAD + X] = -0.8F * (p[ALL + X] + p[CHEST + X]);
        p[HEAD + Z] = -(p[ALL + Z] + p[CHEST + Z]);
        p[L_FOOT + X] = -(p[ALL + X] + p[L_LEG + X] + p[L_SHIN + X]);
        p[R_FOOT + X] = -(p[ALL + X] + p[R_LEG + X] + p[R_SHIN + X]) + Math.max(0, p[R_LEG + X]) * 0.8F;
        // The feet keep to the line of the target while the body turns over them.
        p[L_FOOT + Y] = -p[ALL + Y];
        p[R_FOOT + Y] = -p[ALL + Y] * 0.6F;
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
        v = clamp(v);
        return v * v * (3 - 2 * v);
    }

    /** 0 at 0 and at 1, 1 in between: a little more that comes and goes. */
    private static float bump(float v) {
        v = clamp(v);
        return (float) Math.sin(Math.PI * v);
    }
}
