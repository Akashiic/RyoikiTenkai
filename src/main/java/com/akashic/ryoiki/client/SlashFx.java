package com.akashic.ryoiki.client;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.SlashField;

/**
 * A cut of the crimson shrine, drawn: the arc a blade sweeps ({@link SlashField}), as light.
 *
 * <p>
 * The blade crosses its arc in a little over two ticks, a white point at its head. Behind it the cut is a crescent:
 * thick in the middle and tapering to both ends, white-hot along the edge that cuts, through orange to crimson
 * toward the inside, over a dark shadow that gives it body against anything bright. Then it thins toward its edge
 * and goes out from the end it began at. Along the edge runs a hot line that always faces the eye, so that a cut
 * seen edge-on is still a cut. Splinters of light fly off along the blade's way as it passes, and where the arc
 * runs low it scores the floor: a scar that glows on after the cut is gone.
 */
final class SlashFx {

    private static final int WHITE = 0xFFFFFF;
    private static final int HOT = 0xFFB890;
    private static final int RED = 0xFF2A1E;
    private static final int DEEP = 0x8A0010;

    private static final int STEPS = 22;
    /** How thick the crescent is in its middle, for its radius. */
    private static final float THICK = 0.25F;
    private static final int SPLINTERS = 11;
    /** How high over the floor an arc still scores it. */
    private static final float SCAR_REACH = 3.2F;

    private static final SlashField.Slash SLASH = new SlashField.Slash();
    private static final float[] A = new float[3];
    private static final float[] B = new float[3];
    private static final float[] C = new float[3];
    private static final float[] D = new float[3];
    private static float near = 1;

    private SlashFx() {}

    /**
     * @param time ticks since the domain opened, with fractions
     * @param camX the camera, relative to the arena's centre, in the shrine's scene
     */
    static void draw(Tessellator t, int radius, float time, int seed, double camX, double camY, double camZ) {
        int latest = SlashField.index((int) Math.floor(time));
        int oldest = Math.max(0, latest - (SlashField.SCAR / SlashField.PERIOD + 2) * SlashField.FLURRY);
        GL11.glDepthMask(false);
        // The shadows first, laid over what is there; then the light, added.
        Draw.translucent();
        t.startDrawingQuads();
        for (int index = oldest; index <= latest; index++) {
            if (SlashField.at(seed, index, time, radius, SLASH) && SLASH.age < SlashField.LIFE) {
                near = near(camX, camY, camZ);
                crescent(t, true);
            }
        }
        t.draw();
        Draw.additive();
        t.startDrawingQuads();
        for (int index = oldest; index <= latest; index++) {
            if (!SlashField.at(seed, index, time, radius, SLASH)) {
                continue;
            }
            near = near(camX, camY, camZ);
            scar(t);
            if (SLASH.age < SlashField.LIFE) {
                crescent(t, false);
                edge(t, camX, camY, camZ);
                splinters(t, camX, camY, camZ);
            }
        }
        t.draw();
        t.startDrawing(GL11.GL_TRIANGLES);
        for (int index = oldest; index <= latest; index++) {
            if (SlashField.at(seed, index, time, radius, SLASH) && SLASH.age < SlashField.SWEEP + 1.2F) {
                near = near(camX, camY, camZ);
                head(t);
            }
        }
        t.draw();
        GL11.glDepthMask(true);
    }

    /** How far along its arc the cut has already gone out, from the end it began at. */
    private static float gone() {
        float t = (SLASH.age - 3) / (SlashField.LIFE - 3);
        return t <= 0 ? 0 : t * t;
    }

    /** How bright the cut still is, and is at all: one that passes right by the eye is dimmed, not a white screen. */
    private static float bright() {
        float t = Draw.clamp01((SLASH.age - 2.5F) / (SlashField.LIFE - 2.5F));
        return (1 - t) * (1 - t) * near;
    }

    /** How far the nearest part of the cut is from the eye, as a factor on its light: 1 from a few blocks off. */
    private static float near(double camX, double camY, double camZ) {
        double nearest = Double.MAX_VALUE;
        for (int i = 0; i <= 6; i++) {
            SlashField.point(SLASH, i / 6F, 0, A);
            double dx = A[0] - camX;
            double dy = A[1] - camY;
            double dz = A[2] - camZ;
            nearest = Math.min(nearest, dx * dx + dy * dy + dz * dz);
        }
        return 0.18F + 0.82F * Draw.clamp01(((float) Math.sqrt(nearest) - 1.2F) / 3.5F);
    }

    /** How thick the crescent is at a point of the arc, now: nothing at its ends, and thinning once the blade is by. */
    private static float thick(float along) {
        float shape = (float) Math.pow(Math.sin(Math.PI * along), 0.7);
        float left = 1 - 0.8F * Draw.clamp01((SLASH.age - 2.5F) / 5.5F);
        return SLASH.radius * THICK * shape * left;
    }

    /**
     * The crescent, between where it has gone out and where the blade has got to. Its shadow is a little wider
     * than its light, and dark toward the inside.
     */
    private static void crescent(Tessellator t, boolean shadow) {
        float from = gone();
        float to = SlashField.swept(SLASH.age);
        if (to - from < 1e-3F) {
            return;
        }
        float bright = bright();
        for (int i = 0; i < STEPS; i++) {
            float f0 = from + (to - from) * i / STEPS;
            float f1 = from + (to - from) * (i + 1) / STEPS;
            float w0 = thick(f0);
            float w1 = thick(f1);
            if (shadow) {
                band(t, f0, f1, -0.06F, -0.06F, w0 * 1.4F + 0.05F, w1 * 1.4F + 0.05F, 0, 0.62F * bright, 0, 0);
                continue;
            }
            // Across the crescent from the edge that cuts: white, hot, crimson, and out.
            band(t, f0, f1, 0, 0, w0 * 0.16F, w1 * 0.16F, WHITE, bright, HOT, 0.95F * bright);
            band(t, f0, f1, w0 * 0.16F, w1 * 0.16F, w0 * 0.5F, w1 * 0.5F, HOT, 0.95F * bright, RED, 0.7F * bright);
            band(t, f0, f1, w0 * 0.5F, w1 * 0.5F, w0, w1, RED, 0.7F * bright, DEEP, 0);
        }
    }

    /** A strip of the crescent between two places along the arc and two depths into it at each. */
    private static void band(Tessellator t, float f0, float f1, float outer0, float outer1, float inner0, float inner1,
        int outerColor, float outerAlpha, int innerColor, float innerAlpha) {
        SlashField.point(SLASH, f0, outer0, A);
        SlashField.point(SLASH, f1, outer1, B);
        SlashField.point(SLASH, f1, inner1, C);
        SlashField.point(SLASH, f0, inner0, D);
        Draw.color(t, outerColor, outerAlpha);
        t.addVertex(A[0], A[1], A[2]);
        t.addVertex(B[0], B[1], B[2]);
        Draw.color(t, innerColor, innerAlpha);
        t.addVertex(C[0], C[1], C[2]);
        t.addVertex(D[0], D[1], D[2]);
    }

    /** The hot line along the edge that cuts, and its glow: facing the eye wherever it is. */
    private static void edge(Tessellator t, double camX, double camY, double camZ) {
        float from = gone();
        float to = SlashField.swept(SLASH.age);
        float bright = bright();
        for (int i = 0; i < STEPS; i++) {
            float f0 = from + (to - from) * i / STEPS;
            float f1 = from + (to - from) * (i + 1) / STEPS;
            SlashField.point(SLASH, f0, 0, A);
            SlashField.point(SLASH, f1, 0, B);
            // Tapering to nothing at the ends of the arc.
            float taper = (float) Math.sqrt(Math.sin(Math.PI * (f0 + f1) / 2));
            facing(t, A, B, camX, camY, camZ, 0.38F * taper, RED, 0.3F * bright);
            facing(t, A, B, camX, camY, camZ, 0.07F * taper, WHITE, bright);
        }
    }

    /** A band of light from one point to another, turned to face the eye. */
    private static void facing(Tessellator t, float[] a, float[] b, double camX, double camY, double camZ, float width,
        int color, float alpha) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double dz = b[2] - a[2];
        double sx = (a[0] + b[0]) / 2 - camX;
        double sy = (a[1] + b[1]) / 2 - camY;
        double sz = (a[2] + b[2]) / 2 - camZ;
        double wx = dy * sz - dz * sy;
        double wy = dz * sx - dx * sz;
        double wz = dx * sy - dy * sx;
        double length = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (length < 1e-6) {
            return;
        }
        wx *= width / length;
        wy *= width / length;
        wz *= width / length;
        Draw.color(t, color, alpha);
        t.addVertex(a[0] - wx, a[1] - wy, a[2] - wz);
        t.addVertex(a[0] + wx, a[1] + wy, a[2] + wz);
        t.addVertex(b[0] + wx, b[1] + wy, b[2] + wz);
        t.addVertex(b[0] - wx, b[1] - wy, b[2] - wz);
    }

    /** The blade's head as it sweeps: a point of white light in a red one. */
    private static void head(Tessellator t) {
        float to = SlashField.swept(SLASH.age);
        float left = 1 - SLASH.age / (SlashField.SWEEP + 1.2F);
        SlashField.point(SLASH, to, 0, A);
        Draw.glow(t, A[0], A[1], A[2], 0.5F + 0.7F * left, WHITE, left * near);
        Draw.glow(t, A[0], A[1], A[2], 1.6F + 1.6F * left, RED, 0.5F * left * near);
    }

    /**
     * Splinters of light thrown off the arc as the blade passes each place: along its way and a little outward,
     * falling, each a short streak that cools from white to red.
     */
    private static void splinters(Tessellator t, double camX, double camY, double camZ) {
        for (int k = 0; k < SPLINTERS; k++) {
            float along = 0.08F + 0.84F * SlashField.chance(SLASH.index, 10 + k);
            // The tick the blade is at that place: the sweep, read backwards.
            float thrown = SlashField.SWEEP * (float) (1 - Math.cbrt(1 - along));
            float age = SLASH.age - thrown;
            float life = 5 + 4 * SlashField.chance(SLASH.index, 30 + k);
            if (age <= 0 || age >= life) {
                continue;
            }
            float speed = 0.3F + 0.5F * SlashField.chance(SLASH.index, 50 + k);
            float out = 0.02F + 0.14F * SlashField.chance(SLASH.index, 70 + k);
            float left = 1 - age / life;
            // It slows as it goes.
            float gone = speed * life * (1 - left * left) / 2;
            SlashField.point(SLASH, along, 0, A);
            SlashField.heading(SLASH, along, C);
            float fall = 0.012F * age * age;
            for (int i = 0; i < 3; i++) {
                float outward = i == 0 ? SLASH.vx : i == 1 ? SLASH.vy : SLASH.vz;
                B[i] = A[i] + C[i] * gone + outward * out * age;
                D[i] = B[i] - C[i] * speed * 1.8F * left;
            }
            B[1] -= fall;
            D[1] -= fall;
            facing(t, D, B, camX, camY, camZ, 0.05F, Draw.mix(RED, WHITE, left * left), 0.9F * left * near);
        }
    }

    /**
     * Where the arc runs low, the floor is scored under it: a line of light that is drawn with the blade and glows
     * on, red with a white heart, long after the cut is gone.
     */
    private static void scar(Tessellator t) {
        float to = SlashField.swept(SLASH.age);
        float left = 1 - Draw.clamp01((SLASH.age - 2) / (SlashField.SCAR - 2F));
        float glow = left * left;
        for (int i = 0; i < STEPS; i++) {
            float f0 = to * i / STEPS;
            float f1 = to * (i + 1) / STEPS;
            SlashField.point(SLASH, f0, 0, A);
            SlashField.point(SLASH, f1, 0, B);
            // The nearer the floor the arc is there, the deeper the score.
            float d0 = Draw.clamp01(1 - (A[1] - 0.3F) / SCAR_REACH);
            float d1 = Draw.clamp01(1 - (B[1] - 0.3F) / SCAR_REACH);
            if (d0 <= 0 && d1 <= 0) {
                continue;
            }
            flat(t, A, B, 0.26F, RED, 0.55F * glow * d0, 0.55F * glow * d1);
            flat(t, A, B, 0.07F, WHITE, 0.9F * glow * glow * d0, 0.9F * glow * glow * d1);
        }
    }

    /** A band of light lying on the floor under two points. */
    private static void flat(Tessellator t, float[] a, float[] b, float width, int color, float alphaA, float alphaB) {
        float dx = b[0] - a[0];
        float dz = b[2] - a[2];
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        if (length < 1e-4F) {
            return;
        }
        float sx = -dz / length * width;
        float sz = dx / length * width;
        Draw.color(t, color, alphaA);
        t.addVertex(a[0] - sx, 0.045, a[2] - sz);
        t.addVertex(a[0] + sx, 0.045, a[2] + sz);
        Draw.color(t, color, alphaB);
        t.addVertex(b[0] + sx, 0.045, b[2] + sz);
        t.addVertex(b[0] - sx, 0.045, b[2] - sz);
    }
}
