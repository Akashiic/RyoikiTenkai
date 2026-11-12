package com.akashic.ryoiki.client;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

/** The few shapes the domains are drawn with: flat colours on the tessellator, no textures. */
final class Draw {

    /** Everything drawn while this is below 1 is that much fainter (reflections, fades). */
    static float alpha = 1;

    private Draw() {}

    static int mix(int from, int to, float blend) {
        blend = blend < 0 ? 0 : blend > 1 ? 1 : blend;
        int r = (int) ((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * blend);
        int g = (int) ((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * blend);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * blend);
        return r << 16 | g << 8 | b;
    }

    static void color(Tessellator t, int rgb, float a) {
        a *= alpha;
        t.setColorRGBA_F(
            (rgb >> 16 & 255) / 255F,
            (rgb >> 8 & 255) / 255F,
            (rgb & 255) / 255F,
            a < 0 ? 0 : a > 1 ? 1 : a);
    }

    static void additive() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    static void translucent() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    static float clamp01(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    static float easeOut(float t) {
        t = 1 - clamp01(t);
        return 1 - t * t * t;
    }

    /** A value in 0..1 that is always the same for the same two numbers. */
    static float hash(int a, int b) {
        int h = a * 0x9E3779B1 ^ b * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /**
     * A sphere around the origin whose colour runs, by height, from {@code below} at the nadir through
     * {@code horizon} to {@code top} at the zenith.
     */
    static void sphere(float radius, int top, int horizon, int below, float bandPower) {
        Tessellator t = Tessellator.instance;
        int stacks = 24;
        int slices = 32;
        t.startDrawingQuads();
        for (int i = 0; i < stacks; i++) {
            double p0 = Math.PI * i / stacks - Math.PI / 2;
            double p1 = Math.PI * (i + 1) / stacks - Math.PI / 2;
            for (int j = 0; j < slices; j++) {
                double a0 = Math.PI * 2 * j / slices;
                double a1 = Math.PI * 2 * (j + 1) / slices;
                sphereVertex(t, radius, p0, a0, top, horizon, below, bandPower);
                sphereVertex(t, radius, p0, a1, top, horizon, below, bandPower);
                sphereVertex(t, radius, p1, a1, top, horizon, below, bandPower);
                sphereVertex(t, radius, p1, a0, top, horizon, below, bandPower);
            }
        }
        t.draw();
    }

    private static void sphereVertex(Tessellator t, float radius, double pitch, double yaw, int top, int horizon,
        int below, float bandPower) {
        double y = Math.sin(pitch);
        double r = Math.cos(pitch);
        float blend = (float) Math.pow(Math.abs(y), bandPower);
        color(t, mix(horizon, y >= 0 ? top : below, blend), 1);
        t.addVertex(Math.cos(yaw) * r * radius, y * radius, Math.sin(yaw) * r * radius);
    }

    /** A flat ring on the ground (y = 0) between two radii, each with its colour and opacity. */
    static void ring(float inner, float outer, int segments, int innerRgb, float innerAlpha, int outerRgb,
        float outerAlpha) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        for (int j = 0; j < segments; j++) {
            double a0 = Math.PI * 2 * j / segments;
            double a1 = Math.PI * 2 * (j + 1) / segments;
            color(t, innerRgb, innerAlpha);
            t.addVertex(Math.cos(a0) * inner, 0, Math.sin(a0) * inner);
            t.addVertex(Math.cos(a1) * inner, 0, Math.sin(a1) * inner);
            color(t, outerRgb, outerAlpha);
            t.addVertex(Math.cos(a1) * outer, 0, Math.sin(a1) * outer);
            t.addVertex(Math.cos(a0) * outer, 0, Math.sin(a0) * outer);
        }
        t.draw();
    }

    /**
     * A disc facing the origin, at {@code distance} along a direction, coloured from its centre to its rim. For
     * things in the sky: moons, glows.
     */
    static void skyDisc(float dirX, float dirY, float dirZ, float distance, float radius, int centerRgb,
        float centerAlpha, int rimRgb, float rimAlpha) {
        // Two directions across the line of sight.
        float len = (float) Math.sqrt(dirX * dirX + dirZ * dirZ);
        float ux = len < 1.0e-4F ? 1 : -dirZ / len;
        float uz = len < 1.0e-4F ? 0 : dirX / len;
        float vx = dirY * uz;
        float vy = dirZ * ux - dirX * uz;
        float vz = -dirY * ux;
        float cx = dirX * distance;
        float cy = dirY * distance;
        float cz = dirZ * distance;
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLE_FAN);
        color(t, centerRgb, centerAlpha);
        t.addVertex(cx, cy, cz);
        color(t, rimRgb, rimAlpha);
        int segments = 40;
        for (int j = 0; j <= segments; j++) {
            double a = Math.PI * 2 * j / segments;
            float c = (float) Math.cos(a) * radius;
            float s = (float) Math.sin(a) * radius;
            t.addVertex(cx + ux * c + vx * s, cy + vy * s, cz + uz * c + vz * s);
        }
        t.draw();
    }

    /**
     * A soft round light that faces the camera, added to a batch started with {@code GL_TRIANGLES}: full at its
     * centre, nothing at its rim.
     */
    static void glow(Tessellator t, double x, double y, double z, float size, int rgb, float a) {
        float rx = ActiveRenderInfo.rotationX * size;
        float rz = ActiveRenderInfo.rotationZ * size;
        float ux = ActiveRenderInfo.rotationYZ * size;
        float uy = ActiveRenderInfo.rotationXZ * size;
        float uz = ActiveRenderInfo.rotationXY * size;
        int segments = 8;
        for (int j = 0; j < segments; j++) {
            double a0 = Math.PI * 2 * j / segments;
            double a1 = Math.PI * 2 * (j + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            color(t, rgb, a);
            t.addVertex(x, y, z);
            color(t, rgb, 0);
            t.addVertex(x + rx * c0 + ux * s0, y + uy * s0, z + rz * c0 + uz * s0);
            t.addVertex(x + rx * c1 + ux * s1, y + uy * s1, z + rz * c1 + uz * s1);
        }
    }

    /** A box with shaded faces, added to a batch of quads: lit on top, darker on the sides away from the light. */
    static void box(Tessellator t, float x0, float y0, float z0, float x1, float y1, float z1, int lit, int dark) {
        color(t, lit, 1);
        t.addVertex(x0, y1, z0);
        t.addVertex(x0, y1, z1);
        t.addVertex(x1, y1, z1);
        t.addVertex(x1, y1, z0);
        color(t, dark, 1);
        t.addVertex(x0, y0, z0);
        t.addVertex(x1, y0, z0);
        t.addVertex(x1, y0, z1);
        t.addVertex(x0, y0, z1);
        side(t, x0, z0, x1, z0, y0, y1, mix(dark, lit, 0.55F), dark);
        side(t, x1, z1, x0, z1, y0, y1, mix(dark, lit, 0.25F), dark);
        side(t, x0, z1, x0, z0, y0, y1, mix(dark, lit, 0.4F), dark);
        side(t, x1, z0, x1, z1, y0, y1, mix(dark, lit, 0.4F), dark);
    }

    private static void side(Tessellator t, float xa, float za, float xb, float zb, float y0, float y1, int top,
        int bottom) {
        color(t, bottom, 1);
        t.addVertex(xa, y0, za);
        t.addVertex(xb, y0, zb);
        color(t, top, 1);
        t.addVertex(xb, y1, zb);
        t.addVertex(xa, y1, za);
    }
}
