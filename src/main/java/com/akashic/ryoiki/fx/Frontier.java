package com.akashic.ryoiki.fx;

/**
 * Where the two domains of a clash meet: not a line but a front, alive. Along the seam of the arena (x, in blocks
 * from its centre) it says how far the front stands before that seam, at a time. No Minecraft classes.
 *
 * <p>
 * The front is the sum of: long slow swells and short quick ones, running opposite ways; tongues, where one domain
 * reaches into the other and the other into it, side by side, growing and giving way; the push of whoever is ahead
 * in the fight, which bows the front into the other's ground; and the shoves of blows that landed, each a jolt of
 * the whole front and two humps running out along it from where it struck.
 */
public final class Frontier {

    /** Straight pieces the front is drawn with, across the arena. */
    public static final int SEGMENTS = 56;
    /** How far a full lead pushes the middle of the front into the other's ground, in blocks. */
    public static final float PUSH = 9;
    /** The furthest the front's own motion takes it from where the push leaves it. */
    public static final float REACH = 5.2F;
    /** Ticks a shove is felt for. */
    public static final float SHOVE_TICKS = 40;

    private static final float SWELL = 1.3F;
    private static final float CHOP = 0.7F;
    private static final float RIPPLE = 0.3F;
    private static final float TONGUE = 2.4F;
    private static final float SHOVE_HUMP = 2.6F;
    private static final float SHOVE_JOLT = 1.2F;
    private static final double TWO_PI = Math.PI * 2;

    private final double[] phase = new double[5];
    private final float halfWidth;

    /**
     * @param seed      the domain's number: the front is the same for everyone in it
     * @param halfWidth half the length of the seam, in blocks: the arena's radius
     */
    public Frontier(int seed, float halfWidth) {
        this.halfWidth = halfWidth;
        for (int i = 0; i < phase.length; i++) {
            int h = seed * 0x9E3779B1 + i * 0x85EBCA6B;
            h ^= h >>> 15;
            h *= 0x2C1B3C6D;
            h ^= h >>> 12;
            phase[i] = (h & 0xFFFF) / 65536.0 * TWO_PI;
        }
    }

    /**
     * How far before the seam the front stands.
     *
     * @param x      blocks along the seam from the arena's centre
     * @param time   ticks since the clash began, with fractions
     * @param push   who is ahead, -1 (the rival, wholly) to 1 (the host): the front bows away from the one ahead
     * @param shoves for each blow that landed, three numbers: where along the seam, how many ticks ago, and 1 if
     *               the host struck or -1 if the rival did
     */
    public float at(float x, float time, float push, float[] shoves) {
        double swell = SWELL * Math.sin(TWO_PI * (x / 23.0 + time / 190.0) + phase[0]);
        double chop = CHOP * Math.sin(TWO_PI * (x / 9.5 - time / 120.0) + phase[1]);
        double ripple = RIPPLE * Math.sin(TWO_PI * (x / 4.1 + time / 60.0) + phase[2]);
        // A tongue is a wave with its crests full and its flanks eased: round lobes of one domain in the other, not
        // teeth. They come and go, each at its pace.
        double wave = Math.sin(TWO_PI * (x / 7.3 + time / 400.0) + phase[3]);
        double breath = 0.6 + 0.4 * Math.sin(TWO_PI * time / 90.0 + x / 5.0 + phase[4]);
        double tongue = TONGUE * wave * Math.abs(wave) * breath;
        // The push is strongest in the middle, where the two stand, and less toward the walls.
        double bow = PUSH * push * (0.65 + 0.35 * Math.cos(Math.PI * x / (2 * halfWidth)));
        double shove = 0;
        if (shoves != null) {
            for (int i = 0; i + 2 < shoves.length; i += 3) {
                float age = shoves[i + 1];
                if (age < 0 || age >= SHOVE_TICKS) {
                    continue;
                }
                double out = 0.9 * age;
                double left = x - shoves[i] + out;
                double right = x - shoves[i] - out;
                double humps = Math.exp(-left * left / 6) + Math.exp(-right * right / 6);
                double fade = 1 - age / SHOVE_TICKS;
                shove += shoves[i + 2] * fade
                    * (SHOVE_HUMP * Math.exp(-age / 9.0) * humps
                        + SHOVE_JOLT * Math.exp(-age / 6.0) * Math.cos(age * 0.6));
            }
        }
        return (float) (bow + swell + chop + ripple + tongue + shove);
    }

    /**
     * The front as {@link #SEGMENTS} + 1 points across the arena: for each, x along the seam and how far before it.
     */
    public float[] line(float time, float push, float[] shoves) {
        float[] out = new float[(SEGMENTS + 1) * 2];
        for (int i = 0; i <= SEGMENTS; i++) {
            float x = -halfWidth + 2 * halfWidth * i / SEGMENTS;
            out[i * 2] = x;
            out[i * 2 + 1] = at(x, time, push, shoves);
        }
        return out;
    }

    /**
     * How much of a domain's own effects shows at a place that is {@code depth} blocks into the other's ground
     * (negative: on its own): whole on its own ground, and less and less over {@code reach} blocks of the other's.
     * Both domains' effects cross the front this way, so that each ground has its own at full strength and the
     * other's thinning out over it.
     */
    public static float crossing(float depth, float reach) {
        return depth <= 0 ? 1 : depth >= reach ? 0 : 1 - depth / reach;
    }

    /** How far before the drawn front a place is (negative: behind it, on the host's ground), by its line. */
    public static float before(float[] line, float x, float z) {
        int last = line.length / 2 - 1;
        if (x <= line[0]) {
            return z - line[1];
        }
        if (x >= line[last * 2]) {
            return z - line[last * 2 + 1];
        }
        int i = (int) ((x - line[0]) / (line[last * 2] - line[0]) * last);
        i = Math.max(0, Math.min(last - 1, i));
        float t = (x - line[i * 2]) / (line[i * 2 + 2] - line[i * 2]);
        return z - (line[i * 2 + 1] + (line[i * 2 + 3] - line[i * 2 + 1]) * t);
    }
}
