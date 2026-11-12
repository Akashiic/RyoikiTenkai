package com.akashic.ryoiki.client;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.SlashField;

/**
 * The crimson shrine: a black sky over still red water, a low blood moon, a ring of gates hung with lanterns, spires
 * on the horizon, mist, embers, and cuts of light that cross the arena without end.
 */
final class ShrineLook implements DomainLook {

    private static final int GATES = 8;
    private static final int SPIRES = 18;
    private static final int EMBERS = 90;
    private static final float MOON_X = 0.33F;
    private static final float MOON_Y = 0.21F;
    private static final float MOON_Z = -0.92F;

    private final SlashField.Slash slash = new SlashField.Slash();

    @Override
    public void sky(float radius, float time) {
        GL11.glDisable(GL11.GL_BLEND);
        Draw.sphere(radius, 0x000000, 0x4A050B, 0x000000, 0.42F);
        for (int pass = 0; pass < 2; pass++) {
            GL11.glPushMatrix();
            if (pass == 1) {
                GL11.glScalef(1, -1, 1);
                Draw.alpha = 0.5F;
            }
            Draw.additive();
            float breath = 0.9F + 0.1F * (float) Math.sin(time * 0.03F);
            Draw.skyDisc(MOON_X, MOON_Y, MOON_Z, radius * 0.95F, radius * 0.62F, 0xC8101C, 0.42F * breath, 0x400008, 0);
            Draw.skyDisc(MOON_X, MOON_Y, MOON_Z, radius * 0.94F, radius * 0.30F, 0xFF3020, 0.50F * breath, 0x8A0010, 0);
            Draw.translucent();
            Draw.skyDisc(MOON_X, MOON_Y, MOON_Z, radius * 0.93F, radius * 0.17F, 0xFF5A3C, 1, 0xB80A14, 1);
            // The moon's seas: darker patches on its face.
            Draw.skyDisc(
                MOON_X + 0.035F,
                MOON_Y + 0.03F,
                MOON_Z,
                radius * 0.925F,
                radius * 0.05F,
                0x8A0A12,
                0.5F,
                0xB80A14,
                0);
            Draw.skyDisc(
                MOON_X - 0.05F,
                MOON_Y - 0.02F,
                MOON_Z,
                radius * 0.925F,
                radius * 0.07F,
                0x8A0A12,
                0.4F,
                0xB80A14,
                0);
            Draw.skyDisc(
                MOON_X + 0.01F,
                MOON_Y - 0.07F,
                MOON_Z,
                radius * 0.925F,
                radius * 0.04F,
                0x8A0A12,
                0.45F,
                0xB80A14,
                0);
            GL11.glPopMatrix();
        }
        Draw.alpha = 1;
    }

    @Override
    public void floor(float far, int radius, float time) {
        Draw.translucent();
        Tessellator t = Tessellator.instance;
        // Red water: rings of light run out from the centre and die away; beyond, it is still and mirrors the sky.
        int rings = 64;
        int slices = 48;
        float reach = radius * 2.4F;
        t.startDrawingQuads();
        for (int i = 0; i < rings; i++) {
            float r0 = reach * i / rings;
            float r1 = reach * (i + 1) / rings;
            float w0 = wave(r0, time, reach);
            float w1 = wave(r1, time, reach);
            for (int j = 0; j < slices; j++) {
                double a0 = Math.PI * 2 * j / slices;
                double a1 = Math.PI * 2 * (j + 1) / slices;
                Draw.color(t, Draw.mix(0x1C0004, 0xA8101A, w0), 0.86F - 0.3F * r0 / reach);
                t.addVertex(Math.cos(a0) * r0, 0, Math.sin(a0) * r0);
                t.addVertex(Math.cos(a1) * r0, 0, Math.sin(a1) * r0);
                Draw.color(t, Draw.mix(0x1C0004, 0xA8101A, w1), 0.86F - 0.3F * r1 / reach);
                t.addVertex(Math.cos(a1) * r1, 0, Math.sin(a1) * r1);
                t.addVertex(Math.cos(a0) * r1, 0, Math.sin(a0) * r1);
            }
        }
        t.draw();
        Draw.ring(reach, far, 48, 0x1C0004, 0.56F, 0x30040A, 0.42F);
        Draw.additive();
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0.004F, 0);
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * 0.09F);
        Draw.ring(radius - 0.07F, radius + 0.07F, 96, 0xFF4030, 0.30F + 0.25F * pulse, 0xFF4030, 0.30F + 0.25F * pulse);
        Draw.ring(radius - 1.8F, radius, 96, 0xC00010, 0, 0xE01020, 0.14F);
        GL11.glPopMatrix();
    }

    private static float wave(float r, float time, float reach) {
        float s = (float) Math.sin(r * 1.15F - time * 0.16F);
        float crest = s > 0 ? s * s * s : 0;
        float fade = 1 - r / reach;
        return crest * fade * 0.75F + 0.04F;
    }

    @Override
    public void props(float far, int radius, float time, int seed, double camX, double camY, double camZ) {
        GL11.glDisable(GL11.GL_BLEND);
        Tessellator t = Tessellator.instance;
        // Spires on the horizon, black against the glow.
        t.startDrawingQuads();
        for (int i = 0; i < SPIRES; i++) {
            float a = (i + Draw.hash(i, 11) * 0.7F) / SPIRES * (float) (Math.PI * 2);
            float r = radius + 22 + Draw.hash(i, 12) * 26;
            if (r > far * 0.92F) {
                continue;
            }
            float h = 12 + Draw.hash(i, 13) * 26;
            float w = 0.7F + Draw.hash(i, 14) * 1.3F;
            float x = (float) Math.cos(a) * r;
            float z = (float) Math.sin(a) * r;
            Draw.box(t, x - w, 0, z - w, x + w, h * 0.7F, z + w, 0x0E0103, 0x030001);
            Draw.box(t, x - w * 0.55F, h * 0.7F, z - w * 0.55F, x + w * 0.55F, h, z + w * 0.55F, 0x120204, 0x040001);
            Draw.box(
                t,
                x - w * 1.5F,
                h * 0.7F - 0.5F,
                z - w * 1.5F,
                x + w * 1.5F,
                h * 0.7F,
                z + w * 1.5F,
                0x160305,
                0x050001);
        }
        t.draw();
        // The gates, in a ring around the arena, each facing the centre.
        for (int i = 0; i < GATES; i++) {
            GL11.glPushMatrix();
            float a = 360F * (i + 0.5F) / GATES;
            GL11.glRotatef(a, 0, 1, 0);
            GL11.glTranslatef(0, 0, radius + 5.5F);
            gate(t, time, i);
            GL11.glPopMatrix();
        }
        mist(radius, time);
        embers(t, radius, time);
        slashes(t, radius, time, seed, camX, camY, camZ);
    }

    /** Two pillars and two beams, with a lantern under each end of the lower beam. Drawn across the X axis. */
    private static void gate(Tessellator t, float time, int index) {
        int lit = 0x8A0E12;
        int dark = 0x1A0204;
        float half = 3.4F;
        float height = 8.6F;
        GL11.glDisable(GL11.GL_BLEND);
        t.startDrawingQuads();
        Draw.box(t, -half - 0.45F, 0, -0.45F, -half + 0.45F, height, 0.45F, lit, dark);
        Draw.box(t, half - 0.45F, 0, -0.45F, half + 0.45F, height, 0.45F, lit, dark);
        Draw.box(t, -half - 0.6F, 0, -0.6F, -half + 0.6F, 0.7F, 0.6F, 0x1E0406, 0x070102);
        Draw.box(t, half - 0.6F, 0, -0.6F, half + 0.6F, 0.7F, 0.6F, 0x1E0406, 0x070102);
        Draw.box(t, -half - 0.9F, height - 2.3F, -0.3F, half + 0.9F, height - 1.7F, 0.3F, lit, dark);
        Draw.box(t, -half - 1.9F, height, -0.55F, half + 1.9F, height + 0.75F, 0.55F, lit, dark);
        Draw.box(t, -half - 2.3F, height + 0.75F, -0.7F, half + 2.3F, height + 1.1F, 0.7F, 0x1E0406, 0x070102);
        Draw.box(t, -0.3F, height - 1.7F, -0.2F, 0.3F, height, 0.2F, lit, dark);
        // Lantern bodies.
        for (int side = -1; side <= 1; side += 2) {
            float x = side * (half - 1.3F);
            Draw.box(t, x - 0.03F, height - 3.2F, -0.03F, x + 0.03F, height - 2.3F, 0.03F, 0x070102, 0x070102);
            Draw.box(t, x - 0.28F, height - 4.0F, -0.28F, x + 0.28F, height - 3.2F, 0.28F, 0xFFC070, 0xD8501C);
        }
        t.draw();
        // Their light.
        Draw.additive();
        GL11.glDepthMask(false);
        t.startDrawing(GL11.GL_TRIANGLES);
        for (int side = -1; side <= 1; side += 2) {
            float x = side * (half - 1.3F);
            float flicker = 0.75F + 0.25F * (float) Math.sin(time * 0.31F + index * 2.3F + side);
            Draw.glow(t, x, height - 3.6F, 0, 1.7F, 0xFF8A30, 0.55F * flicker);
            Draw.glow(t, x, height - 3.6F, 0, 4.2F, 0xFF5A20, 0.22F * flicker);
        }
        t.draw();
        GL11.glDepthMask(true);
    }

    /** Low red mist in slow bands. */
    private static void mist(int radius, float time) {
        Draw.translucent();
        GL11.glDepthMask(false);
        Tessellator t = Tessellator.instance;
        int slices = 48;
        for (int band = 0; band < 5; band++) {
            float inner = radius * (0.35F + band * 0.42F);
            float outer = inner + radius * 0.65F;
            float y = 0.25F + band * 0.32F;
            float turn = time * 0.004F * (band % 2 == 0 ? 1 : -1) + band;
            t.startDrawingQuads();
            for (int j = 0; j < slices; j++) {
                double a0 = Math.PI * 2 * j / slices;
                double a1 = Math.PI * 2 * (j + 1) / slices;
                float d0 = 0.5F + 0.5F * (float) Math.sin(a0 * 3 + turn * 6 + band * 1.9F);
                float d1 = 0.5F + 0.5F * (float) Math.sin(a1 * 3 + turn * 6 + band * 1.9F);
                Draw.color(t, 0xB00818, 0);
                t.addVertex(Math.cos(a0) * inner, y, Math.sin(a0) * inner);
                t.addVertex(Math.cos(a1) * inner, y, Math.sin(a1) * inner);
                Draw.color(t, 0xB00818, 0.20F * d1);
                t.addVertex(Math.cos(a1) * (inner + outer) / 2, y, Math.sin(a1) * (inner + outer) / 2);
                Draw.color(t, 0xB00818, 0.20F * d0);
                t.addVertex(Math.cos(a0) * (inner + outer) / 2, y, Math.sin(a0) * (inner + outer) / 2);
                Draw.color(t, 0xB00818, 0.20F * d0);
                t.addVertex(Math.cos(a0) * (inner + outer) / 2, y, Math.sin(a0) * (inner + outer) / 2);
                Draw.color(t, 0xB00818, 0.20F * d1);
                t.addVertex(Math.cos(a1) * (inner + outer) / 2, y, Math.sin(a1) * (inner + outer) / 2);
                Draw.color(t, 0xB00818, 0);
                t.addVertex(Math.cos(a1) * outer, y, Math.sin(a1) * outer);
                t.addVertex(Math.cos(a0) * outer, y, Math.sin(a0) * outer);
            }
            t.draw();
        }
        GL11.glDepthMask(true);
    }

    private static void embers(Tessellator t, int radius, float time) {
        Draw.additive();
        GL11.glDepthMask(false);
        t.startDrawing(GL11.GL_TRIANGLES);
        for (int i = 0; i < EMBERS; i++) {
            float a = Draw.hash(i, 21) * (float) (Math.PI * 2);
            float r = (float) Math.sqrt(Draw.hash(i, 22)) * (radius + 8);
            float speed = 0.03F + Draw.hash(i, 23) * 0.05F;
            float height = 9;
            float y = (Draw.hash(i, 24) * height + time * speed) % height;
            float life = Draw.clamp01(y * 2) * Draw.clamp01((height - y) * 0.3F);
            double x = Math.cos(a) * r + Math.sin(time * 0.05F + i) * 0.5F;
            double z = Math.sin(a) * r + Math.cos(time * 0.04F + i * 1.3F) * 0.5F;
            float size = 0.02F + Draw.hash(i, 25) * 0.035F;
            Draw.glow(t, x, y, z, size * 1.6F, 0xFFB060, life);
            Draw.glow(t, x, y, z, size * 6, 0xFF3010, 0.28F * life);
        }
        t.draw();
        GL11.glDepthMask(true);
    }

    /** The cuts: a white line that runs across the arena in an instant and leaves a red wake that fades. */
    private void slashes(Tessellator t, int radius, float time, int seed, double camX, double camY, double camZ) {
        Draw.additive();
        GL11.glDepthMask(false);
        int latest = SlashField.index((int) time);
        t.startDrawingQuads();
        for (int index = latest - 2; index <= latest; index++) {
            if (!SlashField.at(seed, index, time, radius, slash)) {
                continue;
            }
            // The line draws itself from one end to the other in the first fifth of its life.
            float drawn = Draw.clamp01(slash.life * 5);
            float fade = 1 - slash.life;
            float half = slash.length / 2;
            float fromX = slash.x - slash.dirX * half;
            float fromY = slash.y - slash.dirY * half;
            float fromZ = slash.z - slash.dirZ * half;
            float toX = fromX + slash.dirX * slash.length * drawn;
            float toY = fromY + slash.dirY * slash.length * drawn;
            float toZ = fromZ + slash.dirZ * slash.length * drawn;
            // Across the line and across the line of sight: the ribbon always shows its face.
            double sx = slash.x - camX;
            double sy = slash.y - camY;
            double sz = slash.z - camZ;
            double wx = slash.dirY * sz - slash.dirZ * sy;
            double wy = slash.dirZ * sx - slash.dirX * sz;
            double wz = slash.dirX * sy - slash.dirY * sx;
            double wl = Math.sqrt(wx * wx + wy * wy + wz * wz);
            if (wl < 1.0e-5) {
                continue;
            }
            wx /= wl;
            wy /= wl;
            wz /= wl;
            ribbon(
                t,
                fromX,
                fromY,
                fromZ,
                toX,
                toY,
                toZ,
                wx,
                wy,
                wz,
                0.55F + slash.life * 0.5F,
                0xFF1020,
                0.42F * fade);
            ribbon(t, fromX, fromY, fromZ, toX, toY, toZ, wx, wy, wz, 0.16F, 0xFF6050, 0.7F * fade);
            ribbon(
                t,
                fromX,
                fromY,
                fromZ,
                toX,
                toY,
                toZ,
                wx,
                wy,
                wz,
                0.045F * (1 - slash.life * 0.6F),
                0xFFFFFF,
                fade * fade * 1.2F);
        }
        t.draw();
        GL11.glDepthMask(true);
    }

    /** A sliver from one point to another, brightest along its middle and tapering to both ends. */
    private static void ribbon(Tessellator t, float ax, float ay, float az, float bx, float by, float bz, double wx,
        double wy, double wz, float width, int rgb, float alpha) {
        float mx = (ax + bx) / 2;
        float my = (ay + by) / 2;
        float mz = (az + bz) / 2;
        double ox = wx * width;
        double oy = wy * width;
        double oz = wz * width;
        // A diamond: the quad is split along the line itself, so the light is on the line and dies to the sides.
        Draw.color(t, rgb, alpha);
        t.addVertex(ax, ay, az);
        Draw.color(t, rgb, 0);
        t.addVertex(mx + ox, my + oy, mz + oz);
        Draw.color(t, rgb, alpha);
        t.addVertex(bx, by, bz);
        Draw.color(t, rgb, 0);
        t.addVertex(mx - ox, my - oy, mz - oz);
    }

    @Override
    public int fogColor() {
        return 0x200206;
    }

    @Override
    public int shardColor() {
        return 0x0A0002;
    }

    @Override
    public int shardEdgeColor() {
        return 0xC8101C;
    }

    @Override
    public int bandFrom() {
        return 0x100002;
    }

    @Override
    public int bandTo() {
        return 0x7A0810;
    }

    @Override
    public int bandTrim() {
        return 0xFF5A40;
    }
}
