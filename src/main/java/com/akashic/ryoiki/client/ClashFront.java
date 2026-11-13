package com.akashic.ryoiki.client;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.Frontier;

/**
 * What happens where the two domains of a clash meet. Two things at once, all along the front: they push each other
 * away, and each gets into the other.
 *
 * <p>
 * Pushing away: a hot line runs the length of the front with a wall of light standing on it, discharges leap from it
 * into either ground, sparks are thrown off it both ways, and the floor cracks outward from it. Getting in: each
 * domain's light lies on the other's floor past the front, and motes of each drift across, taking the other's colour
 * as it consumes them. A blow that lands sends a ring out over the floor and heats the front where it struck. Tall
 * faint columns of light rise from the front into the band where the two skies mix.
 *
 * <p>
 * Everything is drawn from the front's line, the time and the domain's number, so everyone in the arena sees the
 * same. Drawn in the arena's scene, in light added to what is there.
 */
final class ClashFront {

    private static final int WHITE = 0xFFFFFF;

    /** How far each domain's light lies on the other's floor, in blocks. */
    private static final float BLEED = 4.2F;
    private static final float CORE = 0.16F;
    private static final float WALL = 4.5F;
    private static final int WALL_ROWS = 7;

    private static final int ARCS = 8;
    private static final float ARC_PERIOD = 9;
    private static final float ARC_LIFE = 5;
    private static final int ARC_NODES = 7;

    private static final int SPARKS = 64;
    private static final float SPARK_LIFE = 15;
    private static final float GRAVITY = 0.036F;

    private static final int CRACKS = 12;
    private static final float CRACK_PERIOD = 52;
    private static final int CRACK_NODES = 6;

    private static final int MOTES = 36;
    private static final float MOTE_LIFE = 46;

    private static final int PILLARS = 6;
    private static final float PILLAR_HEIGHT = 46;
    private static final int PILLAR_LIGHTS = 12;

    private static final float RING_TICKS = 14;

    private ClashFront() {}

    /**
     * @param line the front ({@link ClientDomain#frontLine}): x along the seam and how far before it, for each point
     * @param time ticks since the clash began, with fractions
     */
    static void draw(ClientDomain domain, float[] line, float time, float partialTicks) {
        int host = Scene.look(domain, 0)
            .bandTrim();
        int rival = Scene.look(domain, 1)
            .bandTrim();
        float seam = domain.sceneSeam();
        int seed = domain.session;
        int points = line.length / 2;
        // The front in the scene, and at each point the way that leads into the rival's ground.
        float[] px = new float[points];
        float[] pz = new float[points];
        float[] nx = new float[points];
        float[] nz = new float[points];
        float[] heat = new float[points];
        double[] cam = domain.toScene(RenderManager.renderPosX, RenderManager.renderPosZ);
        for (int i = 0; i < points; i++) {
            px[i] = line[i * 2];
            pz[i] = seam + line[i * 2 + 1];
        }
        for (int i = 0; i < points; i++) {
            int a = Math.max(0, i - 1);
            int b = Math.min(points - 1, i + 1);
            float tx = px[b] - px[a];
            float tz = pz[b] - pz[a];
            float len = (float) Math.sqrt(tx * tx + tz * tz);
            nx[i] = -tz / len;
            nz[i] = tx / len;
            // Hotter where a blow has just shoved it.
            for (float[] shove : domain.frontShoves) {
                float d = px[i] - shove[0];
                heat[i] += (float) (Math.exp(-d * d / 24) * Math.exp(-(shove[1] + partialTicks) / 7));
            }
            // The front ends at the arena's wall: so does its light. And right where the camera stands it is
            // nearly put out: light added a hand's breadth from the eye is a white screen, not a front.
            float edge = Draw.clamp01((domain.radius - Math.abs(px[i])) / 5F);
            double cx = px[i] - cam[0];
            double cz = pz[i] - cam[1];
            float near = 0.12F + 0.88F * Draw.clamp01(((float) Math.sqrt(cx * cx + cz * cz) - 1.2F) / 3.5F);
            heat[i] = edge * near * (1 + 1.6F * Math.min(1.5F, heat[i]));
        }
        Draw.camera(domain.yaw);
        Draw.additive();
        GL11.glDepthMask(false);
        Tessellator t = Tessellator.instance;

        t.startDrawingQuads();
        bleed(t, px, pz, nx, nz, heat, host, rival, time);
        wall(t, px, pz, heat, host, rival, time, seed);
        core(t, px, pz, nx, nz, heat, time, seed);
        cracks(t, domain, line, px, pz, nx, nz, host, rival, time, seed);
        arcs(t, px, pz, nx, nz, heat, host, rival, time, seed, true);
        rings(t, domain, line, seam, host, rival, partialTicks);
        t.draw();

        t.startDrawing(GL11.GL_TRIANGLES);
        arcs(t, px, pz, nx, nz, heat, host, rival, time, seed, false);
        pillars(t, px, pz, heat, host, rival, time, seed, cam[0], RenderManager.renderPosY - domain.arenaY, cam[1]);
        sparks(t, px, pz, nx, nz, heat, host, rival, time, seed);
        motes(t, px, pz, nx, nz, heat, host, rival, time, seed);
        t.draw();
        GL11.glDepthMask(true);
    }

    /** Each domain's light on the other's floor: strongest at the front, gone a few blocks past it. */
    private static void bleed(Tessellator t, float[] px, float[] pz, float[] nx, float[] nz, float[] heat, int host,
        int rival, float time) {
        for (int i = 0; i + 1 < px.length; i++) {
            for (int side = -1; side <= 1; side += 2) {
                // Past the front on the rival's side lies the host's light, and the other way about.
                int color = side > 0 ? host : rival;
                float reach = BLEED * side;
                float a0 = 0.34F * Math.min(1.6F, heat[i]) * breathe(px[i], time, side);
                float a1 = 0.34F * Math.min(1.6F, heat[i + 1]) * breathe(px[i + 1], time, side);
                Draw.color(t, color, a0);
                t.addVertex(px[i], 0.035, pz[i]);
                Draw.color(t, color, a1);
                t.addVertex(px[i + 1], 0.035, pz[i + 1]);
                Draw.color(t, color, 0);
                t.addVertex(px[i + 1] + nx[i + 1] * reach, 0.035, pz[i + 1] + nz[i + 1] * reach);
                t.addVertex(px[i] + nx[i] * reach, 0.035, pz[i] + nz[i] * reach);
            }
        }
    }

    /** The light of one domain on the other's floor swells and ebbs along the front, each side at its own pace. */
    private static float breathe(float x, float time, int side) {
        return 0.7F + 0.3F * (float) Math.sin(x * 0.42F + time * 0.11F * side + side);
    }

    /** The hot line itself, with pulses running along it. */
    private static void core(Tessellator t, float[] px, float[] pz, float[] nx, float[] nz, float[] heat, float time,
        int seed) {
        for (int i = 0; i + 1 < px.length; i++) {
            float a0 = Math.min(1, heat[i] * bead(px[i], time, seed));
            float a1 = Math.min(1, heat[i + 1] * bead(px[i + 1], time, seed));
            for (int side = -1; side <= 1; side += 2) {
                float w = CORE * side;
                Draw.color(t, WHITE, a0);
                t.addVertex(px[i], 0.05, pz[i]);
                Draw.color(t, WHITE, a1);
                t.addVertex(px[i + 1], 0.05, pz[i + 1]);
                Draw.color(t, WHITE, 0);
                t.addVertex(px[i + 1] + nx[i + 1] * w, 0.05, pz[i + 1] + nz[i + 1] * w);
                t.addVertex(px[i] + nx[i] * w, 0.05, pz[i] + nz[i] * w);
            }
        }
    }

    /**
     * How bright the hot line is at a place: not a stroke but beads of light running along it, two trains of them
     * at different speeds, dark between.
     */
    private static float bead(float x, float time, int seed) {
        float a = noise(x * 0.55F - time * 0.42F, seed);
        float b = noise(x * 1.3F + time * 0.27F, seed + 1);
        float both = a * b;
        return 0.08F + 2.6F * both * both;
    }

    /**
     * The wall of light on the front: white at the floor, toward the two domains' colours as it rises, streaked
     * and uneven in height, the streaks sliding along it.
     */
    private static void wall(Tessellator t, float[] px, float[] pz, float[] heat, int host, int rival, float time,
        int seed) {
        for (int i = 0; i + 1 < px.length; i++) {
            for (int row = 0; row < WALL_ROWS; row++) {
                float f0 = (float) row / WALL_ROWS;
                float f1 = (float) (row + 1) / WALL_ROWS;
                for (int end = 0; end < 2; end++) {
                    // Quads go round: bottom of one end, bottom of the other, top of the other, top of the one.
                    int a = end == 0 ? i : i + 1;
                    int b = end == 0 ? i + 1 : i;
                    float fa = end == 0 ? f0 : f1;
                    wallVertex(t, px[a], pz[a], fa, heat[a], host, rival, time, seed);
                    wallVertex(t, px[b], pz[b], fa, heat[b], host, rival, time, seed);
                }
            }
        }
    }

    private static void wallVertex(Tessellator t, float x, float z, float up, float heat, int host, int rival,
        float time, int seed) {
        // Its height swells and sinks along the front; its light is in soft bands that drift, wide ones and
        // narrower ones over them, so no two columns of it differ by much.
        float height = WALL * (0.4F + 0.9F * noise(x * 0.19F + time * 0.021F, seed + 3)) * Math.min(1.4F, heat);
        float streak = 0.35F
            + 0.65F * noise(x * 0.5F + time * 0.09F, seed + 5) * noise(x * 0.9F - time * 0.05F, seed + 6);
        // Which domain's colour the wall takes shifts along it and in time: neither holds it.
        int tint = Draw.mix(host, rival, noise(x * 0.11F - time * 0.03F, seed + 7));
        float fall = (1 - up) * (1 - up) * (1 - up);
        Draw.color(t, Draw.mix(WHITE, tint, Math.min(1, up * 2.2F)), 0.2F * fall * streak * Math.min(1.5F, heat));
        t.addVertex(x, 0.04 + up * height, z);
    }

    /**
     * Discharges: from a place on the front, a jagged leap into one ground or the other, a few blocks out and up
     * and back to the floor. Each lasts a quarter of a second and flickers as it lasts. The quads are its halo, in
     * the colour of the domain it comes from; the triangles, its white knots.
     */
    private static void arcs(Tessellator t, float[] px, float[] pz, float[] nx, float[] nz, float[] heat, int host,
        int rival, float time, int seed, boolean ribbons) {
        for (int k = 0; k < ARCS; k++) {
            float clock = time + k * ARC_PERIOD / ARCS * 1.37F;
            int born = (int) Math.floor(clock / ARC_PERIOD);
            float age = clock - born * ARC_PERIOD;
            if (age >= ARC_LIFE) {
                continue;
            }
            int id = seed * 31 + k * 977 + born * 7919;
            int at = 1 + (int) (Draw.hash(id, 1) * (px.length - 2));
            if (heat[at] <= 0.05F) {
                continue;
            }
            int side = Draw.hash(id, 2) < 0.5F ? -1 : 1;
            // Into the rival's ground it is the host's discharge.
            int color = side > 0 ? host : rival;
            float reach = (2 + 4 * Draw.hash(id, 3)) * side;
            float along = (Draw.hash(id, 4) - 0.5F) * 3;
            float crest = 1.2F + 2.6F * Draw.hash(id, 5);
            float life = 1 - age / ARC_LIFE;
            float shown = Math.min(1, heat[at]);
            // The jags change every other tick.
            int flicker = id + (int) (clock / 2) * 131;
            float lastX = px[at];
            float lastY = 0.05F;
            float lastZ = pz[at];
            for (int n = 1; n <= ARC_NODES; n++) {
                float f = (float) n / ARC_NODES;
                float jag = n == ARC_NODES ? 0 : 0.55F;
                float x = px[at] + nx[at] * reach * f - nz[at] * along * f + (Draw.hash(flicker, n * 3) - 0.5F) * jag;
                float z = pz[at] + nz[at] * reach * f
                    + nx[at] * along * f
                    + (Draw.hash(flicker, n * 3 + 1) - 0.5F) * jag;
                float y = 0.05F + crest * 4 * f * (1 - f) + (Draw.hash(flicker, n * 3 + 2) - 0.5F) * jag;
                if (ribbons) {
                    ribbon(t, lastX, lastY, lastZ, x, y, z, 0.16F, color, 0.5F * life * shown);
                    ribbon(t, lastX, lastY, lastZ, x, y, z, 0.05F, WHITE, 0.95F * life * shown);
                } else {
                    Draw.glow(t, x, y, z, 0.28F, Draw.mix(WHITE, color, 0.35F), 0.8F * life * shown);
                    if (n == ARC_NODES) {
                        // Where it strikes the floor.
                        Draw.glow(t, x, 0.1, z, 0.9F, color, 0.7F * life * shown);
                    }
                }
                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }

    /** A thin band of light from one point to another, seen from any side: two crossed quads. */
    private static void ribbon(Tessellator t, float x0, float y0, float z0, float x1, float y1, float z1, float width,
        int color, float alpha) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        float sx = len < 1e-3F ? width : -dz / len * width;
        float sz = len < 1e-3F ? 0 : dx / len * width;
        Draw.color(t, color, alpha);
        t.addVertex(x0 - sx, y0, z0 - sz);
        t.addVertex(x0 + sx, y0, z0 + sz);
        t.addVertex(x1 + sx, y1, z1 + sz);
        t.addVertex(x1 - sx, y1, z1 - sz);
        t.addVertex(x0, y0 - width, z0);
        t.addVertex(x0, y0 + width, z0);
        t.addVertex(x1, y1 + width, z1);
        t.addVertex(x1, y1 - width, z1);
    }

    /** Sparks thrown off the front to both sides: up and out, and down again, cooling from white as they go. */
    private static void sparks(Tessellator t, float[] px, float[] pz, float[] nx, float[] nz, float[] heat, int host,
        int rival, float time, int seed) {
        for (int k = 0; k < SPARKS; k++) {
            float clock = time + k * SPARK_LIFE / SPARKS * 3.11F;
            int born = (int) Math.floor(clock / SPARK_LIFE);
            float age = clock - born * SPARK_LIFE;
            int id = seed * 17 + k * 613 + born * 104729;
            int at = 1 + (int) (Draw.hash(id, 1) * (px.length - 2));
            if (heat[at] <= 0.05F) {
                continue;
            }
            int side = Draw.hash(id, 2) < 0.5F ? -1 : 1;
            float out = (0.05F + 0.2F * Draw.hash(id, 3)) * side;
            float along = (Draw.hash(id, 4) - 0.5F) * 0.14F;
            float up = 0.16F + 0.26F * Draw.hash(id, 5);
            float y = 0.1F + up * age - GRAVITY / 2 * age * age;
            if (y < 0.05F) {
                continue;
            }
            float x = px[at] + (nx[at] * out - nz[at] * along) * age;
            float z = pz[at] + (nz[at] * out + nx[at] * along) * age;
            float life = 1 - age / SPARK_LIFE;
            int color = Draw.mix(side > 0 ? host : rival, WHITE, life * life);
            Draw.glow(t, x, y, z, 0.1F + 0.12F * life, color, Math.min(1, heat[at]) * (0.4F + 0.6F * life));
        }
    }

    /**
     * Cracks in the floor, running out from the front into one ground or the other: quick to open, slow to close,
     * lit in the colour of the domain that drives them, with a branch.
     */
    private static void cracks(Tessellator t, ClientDomain domain, float[] line, float[] px, float[] pz, float[] nx,
        float[] nz, int host, int rival, float time, int seed) {
        for (int k = 0; k < CRACKS; k++) {
            float clock = time + k * CRACK_PERIOD / CRACKS;
            int born = (int) Math.floor(clock / CRACK_PERIOD);
            float age = clock - born * CRACK_PERIOD;
            int id = seed * 13 + k * 389 + born * 15485863;
            int at = 2 + (int) (Draw.hash(id, 1) * (px.length - 4));
            int side = k % 2 == 0 ? -1 : 1;
            int color = side > 0 ? host : rival;
            float open = Draw.easeOut(age / 7);
            float fade = Draw.clamp01((CRACK_PERIOD - age) / 18);
            float alpha = 0.75F * fade * Draw.clamp01((domain.radius - Math.abs(px[at])) / 5F);
            float length = (3 + 4.5F * Draw.hash(id, 2)) * side;
            float lastX = px[at];
            float lastZ = pz[at];
            for (int n = 1; n <= CRACK_NODES; n++) {
                float f = (float) n / CRACK_NODES;
                if (f > open + 1e-3F) {
                    break;
                }
                float wander = (Draw.hash(id, 10 + n) - 0.5F) * 1.3F * f;
                float x = px[at] + nx[at] * length * f - nz[at] * wander;
                float z = pz[at] + nz[at] * length * f + nx[at] * wander;
                float thin = 1 - f * 0.8F;
                flat(t, lastX, lastZ, x, z, 0.16F * thin, color, alpha * 0.55F * thin);
                flat(t, lastX, lastZ, x, z, 0.05F * thin, WHITE, alpha * thin);
                if (n == 3) {
                    // The branch, off to one side.
                    float turn = Draw.hash(id, 20) < 0.5F ? -1 : 1;
                    float bx = x + (nx[at] * side * 0.9F - nz[at] * turn * 1.4F) * open;
                    float bz = z + (nz[at] * side * 0.9F + nx[at] * turn * 1.4F) * open;
                    flat(t, x, z, bx, bz, 0.09F, color, alpha * 0.45F);
                    flat(t, x, z, bx, bz, 0.03F, WHITE, alpha * 0.7F);
                }
                lastX = x;
                lastZ = z;
            }
        }
    }

    /** A band of light lying on the floor from one place to another. */
    private static void flat(Tessellator t, float x0, float z0, float x1, float z1, float width, int color,
        float alpha) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-3F) {
            return;
        }
        float sx = -dz / len * width;
        float sz = dx / len * width;
        Draw.color(t, color, alpha);
        t.addVertex(x0 - sx, 0.045, z0 - sz);
        t.addVertex(x0 + sx, 0.045, z0 + sz);
        t.addVertex(x1 + sx, 0.045, z1 + sz);
        t.addVertex(x1 - sx, 0.045, z1 - sz);
    }

    /**
     * Motes of each domain drifting over the front into the other: slow, rising a little, wandering; they start in
     * their own domain's colour and end in the other's, smaller, as it takes them.
     */
    private static void motes(Tessellator t, float[] px, float[] pz, float[] nx, float[] nz, float[] heat, int host,
        int rival, float time, int seed) {
        for (int k = 0; k < MOTES; k++) {
            float clock = time + k * MOTE_LIFE / MOTES * 1.71F;
            int born = (int) Math.floor(clock / MOTE_LIFE);
            float age = clock - born * MOTE_LIFE;
            int id = seed * 29 + k * 211 + born * 32452843;
            int at = 1 + (int) (Draw.hash(id, 1) * (px.length - 2));
            if (heat[at] <= 0.05F) {
                continue;
            }
            int side = k % 2 == 0 ? -1 : 1;
            float f = age / MOTE_LIFE;
            // It starts a little behind the front, on its own ground, and crosses.
            float travel = (-1.2F + (5 + 5 * Draw.hash(id, 2)) * f) * side;
            float sway = (float) Math.sin(age * 0.21F + id) * 0.7F + (Draw.hash(id, 3) - 0.5F) * 3 * f;
            float x = px[at] + nx[at] * travel - nz[at] * sway;
            float z = pz[at] + nz[at] * travel + nx[at] * sway;
            float y = 0.3F + (0.6F + 1.6F * Draw.hash(id, 4)) * f + 0.15F * (float) Math.sin(age * 0.4F + id);
            int own = side > 0 ? host : rival;
            int other = side > 0 ? rival : host;
            // Past the front the other domain's colour takes it.
            float taken = Draw.clamp01((travel * side - 0.5F) / 4);
            float shown = Draw.clamp01(f * 6) * (1 - f) * (1 - f);
            Draw.glow(t, x, y, z, 0.16F + 0.22F * (1 - taken), Draw.mix(own, other, taken), 0.9F * shown);
            Draw.glow(t, x, y, z, 0.6F, Draw.mix(own, other, taken), 0.16F * shown);
        }
    }

    /**
     * Columns of faint light from the front up into the sky, where the two skies mix: a few, each leaning to one
     * domain's colour, slowly walking along the front. Soft round lights one above the other, larger and fainter
     * as they rise: from no side do they show an edge.
     */
    private static void pillars(Tessellator t, float[] px, float[] pz, float[] heat, int host, int rival, float time,
        int seed, double camX, double camY, double camZ) {
        for (int k = 0; k < PILLARS; k++) {
            float walk = (k + 0.5F) / PILLARS + 0.06F * (float) Math.sin(time * 0.013F + k * 2.1F);
            float where = Draw.clamp01(walk) * (px.length - 1);
            int i = Math.min(px.length - 2, (int) where);
            float f = where - i;
            float x = px[i] + (px[i + 1] - px[i]) * f;
            float z = pz[i] + (pz[i + 1] - pz[i]) * f;
            int color = Draw.mix(k % 2 == 0 ? host : rival, WHITE, 0.2F);
            float strength = Math.min(1.4F, heat[i]) * (0.7F + 0.3F * (float) Math.sin(time * 0.07F + k));
            for (int n = 0; n < PILLAR_LIGHTS; n++) {
                float up = (n + 0.5F) / PILLAR_LIGHTS;
                // It sways a little as it rises, like heat.
                float sway = (float) Math.sin(time * 0.05F + up * 5 + k) * 0.5F * up;
                // A light the eye is in, or nearly, is not drawn: looking along a column would stack them all on
                // the eye, a wash of colour over everything.
                double ex = x + sway - camX;
                double ey = 0.6F + up * up * PILLAR_HEIGHT - camY;
                double ez = z + sway * 0.6F - camZ;
                float clear = Draw.clamp01(((float) Math.sqrt(ex * ex + ey * ey + ez * ez) - 3) / 7);
                Draw.glow(
                    t,
                    x + sway,
                    0.6F + up * up * PILLAR_HEIGHT,
                    z + sway * 0.6F,
                    1.4F + 3.2F * up,
                    color,
                    0.13F * strength * (1 - up) * (1 - up) * clear);
            }
        }
    }

    /** A blow that lands: a ring running out over the floor from where it shoved the front. */
    private static void rings(Tessellator t, ClientDomain domain, float[] line, float seam, int host, int rival,
        float partialTicks) {
        for (float[] shove : domain.frontShoves) {
            float age = shove[1] + partialTicks;
            if (age >= RING_TICKS) {
                continue;
            }
            float x = shove[0];
            float z = seam + x(line, x);
            float f = age / RING_TICKS;
            float radius = 0.6F + 7.5F * Draw.easeOut(f);
            float width = 0.9F * (1 - f) + 0.15F;
            int color = Draw.mix(shove[2] > 0 ? host : rival, WHITE, 1 - f);
            int segments = 28;
            for (int s = 0; s < segments; s++) {
                double a0 = Math.PI * 2 * s / segments;
                double a1 = Math.PI * 2 * (s + 1) / segments;
                float c0 = (float) Math.cos(a0);
                float s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1);
                float s1 = (float) Math.sin(a1);
                Draw.color(t, color, 0.8F * (1 - f));
                t.addVertex(x + c0 * radius, 0.06, z + s0 * radius);
                t.addVertex(x + c1 * radius, 0.06, z + s1 * radius);
                Draw.color(t, color, 0);
                t.addVertex(x + c1 * (radius - width), 0.06, z + s1 * (radius - width));
                t.addVertex(x + c0 * (radius - width), 0.06, z + s0 * (radius - width));
            }
        }
    }

    /** How far before the seam the front stands at a place along it, by its line. */
    private static float x(float[] line, float x) {
        return -Frontier.before(line, x, 0);
    }

    /** A value that wanders smoothly between 0 and 1 as {@code x} goes. */
    private static float noise(float x, int salt) {
        int cell = (int) Math.floor(x);
        float f = x - cell;
        f = f * f * (3 - 2 * f);
        float a = Draw.hash(cell, salt);
        return a + (Draw.hash(cell + 1, salt) - a) * f;
    }
}
