package com.akashic.ryoiki.fx;

/**
 * How a player in the sorcerer mode stands, runs and jumps: degrees for the bones of a player model
 * ({@link BlowPose#BONES}, same axes), from where the body is in its stride, how fast it goes, which way, and
 * whether it is in the air. No Minecraft classes.
 *
 * <p>
 * The game's walk is a run by any measure: over four blocks a second, on legs three quarters of a block long. So
 * both gaits here are runs, with both feet off the ground between steps, and they differ in what a runner changes
 * when it goes flat out. The <b>jog</b> (the game's walk) is upright and easy: the arms bent and swinging against
 * the legs, the knees coming up a little. The <b>sprint</b> (the game's run) leans into it: longer strides, the heel
 * kicking up behind and the knee driving high in front, and the arms laid back along the body, the way these
 * fighters run.
 *
 * <p>
 * A stride is one cycle of both legs. Each leg is on the ground for a part of it ({@code stance}), going back under
 * the hip at the speed of the ground, and in the air for the rest, folding to come forward. The two legs are half a
 * cycle apart and everything else follows from them: the hips turn with the leg that leads, the chest against the
 * hips, the arms against the legs, and the head against all of it, to stay level and on the way ahead. A foot on
 * the ground has its sole level, whatever the leg does above it.
 */
public final class Gait {

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

    /** Where the body is and what it does: filled by whoever follows the player, read by {@link #sample}. */
    public static final class State {

        /** Strides run so far: the whole number is forgotten, the fraction is where in the stride the body is. */
        public float phase;
        /** 0 standing, 1 moving at its gait's speed; in between while it gathers speed or loses it. */
        public float moving;
        /** 0 the jog, 1 the sprint. */
        public float sprint;
        /**
         * Which way it goes, against the way the body faces, in degrees: 0 ahead, 90 to its right, 180 backward.
         */
        public float heading;
        /** 0 on the ground, 1 in the air. */
        public float air;
        /** Blocks a tick, up: the jump leaves the ground at 0.42. */
        public float rising;
        /** Ticks since it came down from a jump or a fall; large when that was long ago. */
        public float landed = 100;
        /** A clock that never stops, in ticks: for breathing. */
        public float clock;
    }

    /** The numbers of a gait. Angles in degrees. */
    private static final class Style {

        /** The part of the stride a foot is on the ground. */
        float stance;
        /** How far forward the thigh is when the foot lands, how far back when it leaves, and its peak in front. */
        float reach;
        float push;
        float drive;
        /** The knee: when the foot lands, at the middle of its time on the ground, and folded in the air. */
        float kneeLands;
        float kneeBears;
        float kneeFolds;
        /** The lean of the whole body and of the chest. */
        float leanAll;
        float leanChest;
        /** The turn of the hips with the leading leg, and of the chest against them. */
        float hips;
        float chest;
        /** The upper arm: where its swing is centred (back is positive), how wide it is, and how far out. */
        float armAt;
        float armSwing;
        float armOut;
        /** The elbow, and how much it gives with the swing. */
        float elbow;
        float elbowSwing;
    }

    private static final Style JOG = new Style();
    private static final Style SPRINT = new Style();

    static {
        JOG.stance = 0.42F;
        JOG.reach = 24;
        JOG.push = 22;
        JOG.drive = 36;
        JOG.kneeLands = 8;
        JOG.kneeBears = 16;
        JOG.kneeFolds = 64;
        JOG.leanAll = 0;
        JOG.leanChest = 6;
        JOG.hips = 5;
        JOG.chest = 8;
        JOG.armAt = 4;
        JOG.armSwing = 28;
        JOG.armOut = 5;
        JOG.elbow = 62;
        JOG.elbowSwing = 12;

        SPRINT.stance = 0.3F;
        SPRINT.reach = 27;
        SPRINT.push = 31;
        SPRINT.drive = 60;
        SPRINT.kneeLands = 10;
        SPRINT.kneeBears = 20;
        SPRINT.kneeFolds = 108;
        SPRINT.leanAll = 8;
        SPRINT.leanChest = 13;
        SPRINT.hips = 7;
        SPRINT.chest = 6;
        // Laid back along the body, a little out from it, the elbow almost straight: they hardly swing.
        SPRINT.armAt = 40;
        SPRINT.armSwing = 5;
        SPRINT.armOut = 15;
        SPRINT.elbow = 22;
        SPRINT.elbowSwing = 4;
    }

    /** Blocks one stride covers, of each gait: the body's phase goes on by the distance run over this. */
    public static final float JOG_STRIDE = 2.3F;
    public static final float SPRINT_STRIDE = 2.7F;
    /** Ticks the body takes to settle after coming down. */
    public static final float LANDING = 7;

    private Gait() {}

    /** Blocks a stride covers at this much sprint. */
    public static float stride(float sprint) {
        return JOG_STRIDE + (SPRINT_STRIDE - JOG_STRIDE) * clamp(sprint);
    }

    public static float[] sample(State s) {
        float[] run = lerp(run(JOG, s), run(SPRINT, s), smooth(s.sprint));
        float[] pose = lerp(stand(s), run, smooth(s.moving));
        if (s.air > 0) {
            pose = lerp(pose, air(s), smooth(s.air));
        }
        float settle = s.landed / LANDING;
        if (settle < 1 && s.air < 1) {
            // Coming down: the chest gives forward and the arms come down with it, and it all springs back.
            float give = (float) (Math.sin(Math.PI * Math.sqrt(settle)) * (1 - settle)) * (1 - s.air);
            pose[CHEST * 3 + X] += 15 * give;
            pose[R_ARM * 3 + X] -= 22 * give;
            pose[L_ARM * 3 + X] -= 22 * give;
            pose[R_ARM * 3 + Z] += 10 * give;
            pose[L_ARM * 3 + Z] -= 10 * give;
        }
        return head(pose);
    }

    /** Standing, in the mode: ready, not at rest. One foot a little ahead, the hands low in front, breathing. */
    private static float[] stand(State s) {
        float[] p = new float[BlowPose.BONES.length * 3];
        float breath = (float) Math.sin(s.clock * 0.11F);
        p[ALL * 3 + Y] = 9;
        p[CHEST * 3 + X] = 3 + 0.9F * breath;
        p[CHEST * 3 + Y] = -5;
        // The left foot ahead, the right behind and turned out, as the body is turned.
        p[L_LEG * 3 + X] = -6;
        p[L_LEG * 3 + Z] = -3;
        p[R_LEG * 3 + X] = 6;
        p[R_LEG * 3 + Z] = 4;
        p[L_SHIN * 3 + X] = 4;
        p[R_SHIN * 3 + X] = 2;
        p[L_FOOT * 3 + Y] = -9;
        p[R_FOOT * 3 + Y] = 14;
        // The arms loose and a little forward, the elbows bent: hands at the hips, ready to come up.
        p[R_ARM * 3 + X] = -10 - 1.2F * breath;
        p[R_ARM * 3 + Z] = 7;
        p[R_FORE * 3 + X] = -42 + 1.5F * breath;
        p[L_ARM * 3 + X] = -16 - 1.2F * breath;
        p[L_ARM * 3 + Z] = -7;
        p[L_FORE * 3 + X] = -48 + 1.5F * breath;
        return level(p);
    }

    private static float[] run(Style g, State s) {
        float[] p = new float[BlowPose.BONES.length * 3];
        float phase = s.phase - (float) Math.floor(s.phase);
        double heading = Math.toRadians(s.heading);
        // Backward and sideways the stride is shorter, and sideways the legs open and close instead.
        float ahead = (float) Math.cos(heading);
        float aside = (float) Math.sin(heading);
        float along = ahead >= 0 ? ahead : 0.7F * ahead;
        float[] right = leg(g, phase);
        float[] left = leg(g, phase + 0.5F);
        limb(p, R_LEG, right, along, aside);
        limb(p, L_LEG, left, along, aside);

        // What the rest of the body does, by the cycle: +1 when the right leg is furthest forward, a moment before
        // its foot lands.
        float swing = (float) Math.cos(2 * Math.PI * (phase + 0.04F));
        float forward = Math.max(0, ahead);
        p[ALL * 3 + X] = g.leanAll * forward;
        p[CHEST * 3 + X] = g.leanChest * (0.4F + 0.6F * forward);
        // The hips go with the leg in front (to the model's left, when it is the right leg), the chest against.
        p[ALL * 3 + Y] = -g.hips * swing * ahead;
        p[CHEST * 3 + Y] = (g.hips + g.chest) * swing * ahead;
        // The body leans a little over the foot it stands on.
        p[CHEST * 3 + Z] = -2.5F * (float) Math.cos(2 * Math.PI * (phase - g.stance / 2)) * forward;
        // Each arm against its own leg: the right arm is back when the right leg is forward.
        float armSwing = g.armSwing * (0.35F + 0.65F * Math.abs(ahead));
        p[R_ARM * 3 + X] = g.armAt + armSwing * swing;
        p[L_ARM * 3 + X] = g.armAt - armSwing * swing;
        p[R_ARM * 3 + Z] = g.armOut;
        p[L_ARM * 3 + Z] = -g.armOut;
        // The elbow closes as its arm comes forward.
        p[R_FORE * 3 + X] = -(g.elbow - g.elbowSwing * swing);
        p[L_FORE * 3 + X] = -(g.elbow + g.elbowSwing * swing);
        // The legs were made for a body that stands straight: the lean of all of it is taken back at the hips.
        p[R_LEG * 3 + X] -= p[ALL * 3 + X];
        p[L_LEG * 3 + X] -= p[ALL * 3 + X];
        return p;
    }

    /**
     * One leg through the stride: the thigh (back is positive), the knee, and how much the foot is on the ground
     * (1) or free (0).
     *
     * @param phase the stride, from where this leg's foot lands
     */
    private static float[] leg(Style g, float phase) {
        phase -= (float) Math.floor(phase);
        float thigh;
        float knee;
        float planted;
        if (phase < g.stance) {
            // On the ground: back under the hip at the speed of the ground, the knee giving under the weight.
            float t = phase / g.stance;
            thigh = -g.reach + (g.reach + g.push) * t;
            knee = g.kneeLands + (g.kneeBears - g.kneeLands) * (float) Math.sin(Math.PI * t) - 3 * t;
            planted = Math.min(1, Math.min(t, 1 - t) * 8 + 0.35F);
        } else {
            // In the air: the knee folds, the thigh drives forward past where it will land, and comes down to it.
            float t = (phase - g.stance) / (1 - g.stance);
            float peak = 0.7F;
            thigh = t < peak ? g.push + (-g.drive - g.push) * smooth(t / peak)
                : -g.drive + (g.drive - g.reach) * smooth((t - peak) / (1 - peak));
            float fold = (float) Math.pow(Math.sin(Math.PI * Math.pow(t, 0.75)), 1.3);
            knee = (g.kneeLands - 3) + 3 * t + (g.kneeFolds - g.kneeLands) * fold;
            planted = Math.max(0, Math.max(0.35F - t * 3, 0.35F - (1 - t) * 3));
        }
        return new float[] { thigh, knee, planted };
    }

    /** Puts a leg's thigh, knee and foot into a pose, along the way the body goes. */
    private static void limb(float[] p, int at, float[] leg, float along, float aside) {
        p[at * 3 + X] = leg[0] * along;
        // Sideways, forward is to that side: about Z, positive is toward the model's right.
        p[at * 3 + Z] = -leg[0] * aside * 0.55F;
        float shin = leg[1] * (0.45F + 0.55F * Math.abs(along));
        p[(at + 1) * 3 + X] = shin;
        // On the ground the sole is level; free, the toes point down and back.
        float flat = -(p[at * 3 + X] + shin);
        float free = flat + 18 + 0.2F * shin;
        p[(at + 2) * 3 + X] = free + (flat - free) * leg[2];
    }

    /**
     * In the air. Going up, one knee is drawn high and the other leg trails, the arms thrown back; coming down, the
     * legs open under the body to meet the ground and the arms come out and up, for balance.
     */
    private static float[] air(State s) {
        float[] up = new float[BlowPose.BONES.length * 3];
        float[] down = new float[BlowPose.BONES.length * 3];
        // The leg that was coming forward when the body left the ground is the one that goes up.
        float phase = s.phase - (float) Math.floor(s.phase);
        boolean rightLeads = phase >= 0.5F + JOG.stance / 2 || phase < JOG.stance / 2;
        int lead = rightLeads ? R_LEG : L_LEG;
        int trail = rightLeads ? L_LEG : R_LEG;
        float side = rightLeads ? 1 : -1;

        up[CHEST * 3 + X] = 9;
        up[CHEST * 3 + Y] = 8 * side;
        up[lead * 3 + X] = -64;
        up[(lead + 1) * 3 + X] = 82;
        up[(lead + 2) * 3 + X] = 14;
        up[trail * 3 + X] = 20;
        up[(trail + 1) * 3 + X] = 22;
        up[(trail + 2) * 3 + X] = 30;
        up[R_ARM * 3 + X] = 38;
        up[L_ARM * 3 + X] = 38;
        up[R_ARM * 3 + Z] = 16;
        up[L_ARM * 3 + Z] = -16;
        up[R_FORE * 3 + X] = -24;
        up[L_FORE * 3 + X] = -24;

        down[CHEST * 3 + X] = -3;
        down[lead * 3 + X] = -24;
        down[(lead + 1) * 3 + X] = 34;
        down[(lead + 2) * 3 + X] = -8;
        down[trail * 3 + X] = -6;
        down[(trail + 1) * 3 + X] = 26;
        down[(trail + 2) * 3 + X] = -14;
        down[R_LEG * 3 + Z] = 5;
        down[L_LEG * 3 + Z] = -5;
        down[R_ARM * 3 + X] = -6;
        down[L_ARM * 3 + X] = -6;
        down[R_ARM * 3 + Z] = 32;
        down[L_ARM * 3 + Z] = -32;
        down[R_FORE * 3 + X] = -34;
        down[L_FORE * 3 + X] = -34;

        // From leaving the ground (0.42 up) through the top (0) to falling fast.
        return lerp(up, down, smooth((0.42F - s.rising) / 0.85F));
    }

    /** The soles of a standing body, level. */
    private static float[] level(float[] p) {
        p[R_FOOT * 3 + X] = -(p[ALL * 3 + X] + p[R_LEG * 3 + X] + p[R_SHIN * 3 + X]);
        p[L_FOOT * 3 + X] = -(p[ALL * 3 + X] + p[L_LEG * 3 + X] + p[L_SHIN * 3 + X]);
        return p;
    }

    /** The head takes back most of what the body under it does: level, and on the way ahead. */
    private static float[] head(float[] p) {
        p[HEAD * 3 + X] = -0.8F * (p[ALL * 3 + X] + p[CHEST * 3 + X]);
        p[HEAD * 3 + Y] = -0.92F * (p[ALL * 3 + Y] + p[CHEST * 3 + Y]);
        p[HEAD * 3 + Z] = -(p[ALL * 3 + Z] + p[CHEST * 3 + Z]);
        return p;
    }

    /** An angle of a pose, by the name of its bone and its axis (0 X, 1 Y, 2 Z): for tests. */
    public static float angle(float[] pose, String bone, int axis) {
        return BlowPose.angle(pose, bone, axis);
    }

    private static float[] lerp(float[] a, float[] b, float t) {
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = a[i] + (b[i] - a[i]) * t;
        }
        return out;
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    private static float smooth(float v) {
        v = clamp(v);
        return v * v * (3 - 2 * v);
    }
}
