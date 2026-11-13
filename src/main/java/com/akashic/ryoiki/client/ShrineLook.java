package com.akashic.ryoiki.client;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.arena.ShrineCore;

/**
 * The crimson shrine: a black sky over still red water and a blood moon; in the middle, behind the caster, the
 * shrine itself (solid: the server builds its hall of invisible blocks), a
 * tower of three roofs on a stepped base with a lit doorway, a rope across its front, banners and a path of stone
 * lanterns; around the arena a ring of gates hung with lanterns, spires on the horizon, mist, embers, and cuts of
 * light that cross the arena without end.
 */
final class ShrineLook implements DomainLook {

    private static final int GATES = 12;
    private static final int SPIRES = 18;
    private static final int EMBERS = 90;
    /** The moon hangs behind the shrine, which stands to the north of the scene. */
    private static final float MOON_X = 0.13F;
    private static final float MOON_Y = 0.33F;
    private static final float MOON_Z = -0.935F;
    /**
     * The shrine is drawn this much smaller than its measures below: its hall is then as wide as the solid one the
     * server builds in the middle of the arena ({@code ShrineCore}).
     */
    private static final float SHRINE_SCALE = ShrineCore.HALF / 5.4F;

    @Override
    public void sky(float radius, float time) {
        GL11.glDisable(GL11.GL_BLEND);
        Draw.sphere(radius, 0x000000, 0x4A050B, 0x000000, 0.42F);
        skyFeatures(radius, time);
    }

    @Override
    public float[] backdrop() {
        return new float[] { 0x000000, 0x4A050B, 0x000000, 0.42F };
    }

    @Override
    public void skyFeatures(float radius, float time) {
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
        GL11.glPushMatrix();
        GL11.glScalef(SHRINE_SCALE, SHRINE_SCALE, SHRINE_SCALE);
        shrine(t, time);
        GL11.glPopMatrix();
        lights(t, radius, time);
        mist(radius, time);
        embers(t, radius, time);
        SlashFx.draw(t, radius, time, seed, camX, camY, camZ);
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
    }

    /**
     * The shrine: a stepped base, a hall of four pillars with a lit doorway, three roofs each smaller than the one
     * below, a spire with rings of light. A thick rope hangs across its front with paper streamers, a banner stands
     * on either side and a path of stone lanterns runs up to it. Drawn facing +Z, its centre at the origin.
     */
    private static void shrine(Tessellator t, float time) {
        int stoneLit = 0x3A1418;
        int stoneDark = 0x0A0304;
        int woodLit = 0xA81418;
        int woodDark = 0x220406;
        int roofLit = 0x3E0C12;
        int roofDark = 0x070102;
        GL11.glDisable(GL11.GL_BLEND);
        t.startDrawingQuads();
        // The base and its steps.
        Draw.frustum(t, 0, 0, 0, 8.6F, 8.6F, 0.7F, 8.2F, 8.2F, stoneLit, stoneDark);
        Draw.frustum(t, 0, 0, 0.7F, 7.4F, 7.4F, 1.4F, 7.0F, 7.0F, stoneLit, stoneDark);
        Draw.frustum(t, 0, 0, 1.4F, 6.2F, 6.2F, 2.1F, 5.9F, 5.9F, stoneLit, stoneDark);
        for (int step = 0; step < 4; step++) {
            Draw.box(
                t,
                -2.2F,
                0,
                5.9F + step * 0.7F,
                2.2F,
                1.9F - step * 0.5F,
                6.6F + step * 0.7F,
                stoneLit,
                stoneDark);
        }
        // The hall: back and side walls in shadow, pillars at the corners and beside the door, beams above.
        Draw.box(t, -4.6F, 2.1F, -4.6F, 4.6F, 7.4F, -4.2F, 0x12030A, 0x050102);
        Draw.box(t, -4.6F, 2.1F, -4.6F, -4.2F, 7.4F, 4.2F, 0x16040A, 0x050102);
        Draw.box(t, 4.2F, 2.1F, -4.6F, 4.6F, 7.4F, 4.2F, 0x16040A, 0x050102);
        for (int sx = -1; sx <= 1; sx += 2) {
            Draw.box(t, sx * 4.9F - 0.5F, 2.1F, 4.4F, sx * 4.9F + 0.5F, 7.6F, 5.4F, woodLit, woodDark);
            Draw.box(t, sx * 4.9F - 0.5F, 2.1F, -5.4F, sx * 4.9F + 0.5F, 7.6F, -4.4F, woodLit, woodDark);
            Draw.box(t, sx * 2.1F - 0.35F, 2.1F, 4.5F, sx * 2.1F + 0.35F, 7.2F, 5.2F, woodLit, woodDark);
            Draw.box(t, sx * 3.5F - 1.1F, 2.1F, 4.5F, sx * 3.5F + 1.1F, 6.6F, 4.8F, 0x1C0408, 0x070102);
        }
        Draw.box(t, -5.8F, 7.2F, 4.3F, 5.8F, 7.9F, 5.5F, woodLit, woodDark);
        Draw.box(t, -5.8F, 7.2F, -5.5F, 5.8F, 7.9F, -4.3F, woodLit, woodDark);
        Draw.box(t, -5.5F, 7.2F, -5.8F, -4.3F, 7.9F, 5.8F, woodLit, woodDark);
        Draw.box(t, 4.3F, 7.2F, -5.8F, 5.5F, 7.9F, 5.8F, woodLit, woodDark);
        Draw.box(t, -1.9F, 6.3F, 4.5F, 1.9F, 7.2F, 5.0F, woodLit, woodDark);
        // Three roofs: each flares at the eaves and climbs to the storey above.
        roof(t, 7.9F, 8.6F, 5.9F, 3.3F, 2.5F, roofLit, roofDark, woodLit, woodDark);
        Draw.box(t, -3.0F, 10.2F, -3.0F, 3.0F, 12.6F, 3.0F, 0x5A0A0E, 0x160305);
        roof(t, 12.6F, 5.6F, 3.8F, 2.1F, 2.0F, roofLit, roofDark, woodLit, woodDark);
        Draw.box(t, -1.9F, 14.4F, -1.9F, 1.9F, 16.2F, 1.9F, 0x5A0A0E, 0x160305);
        roof(t, 16.2F, 3.7F, 2.4F, 0.5F, 1.9F, roofLit, roofDark, woodLit, woodDark);
        // The spire.
        Draw.box(t, -0.22F, 18.0F, -0.22F, 0.22F, 23.5F, 0.22F, 0x2A0A0C, 0x0A0203);
        Draw.frustum(t, 0, 0, 23.5F, 0.4F, 0.4F, 24.6F, 0.02F, 0.02F, 0xC89040, 0x3A1408);
        // Banner poles and the stone lanterns of the path.
        for (int sx = -1; sx <= 1; sx += 2) {
            Draw.box(t, sx * 11F - 0.18F, 0, 6.8F, sx * 11F + 0.18F, 13.5F, 7.16F, 0x241012, 0x080203);
            Draw.box(t, sx * 11F - 0.18F, 13.0F, 6.8F, sx * 11F - 0.18F - sx * 2.6F, 13.3F, 7.1F, 0x241012, 0x080203);
            for (int k = 0; k < 2; k++) {
                float z = 9.6F + k * 2.6F;
                float x = sx * 3.3F;
                Draw.frustum(t, x, z, 0, 0.5F, 0.5F, 0.35F, 0.4F, 0.4F, stoneLit, stoneDark);
                Draw.box(t, x - 0.17F, 0.35F, z - 0.17F, x + 0.17F, 1.45F, z + 0.17F, stoneLit, stoneDark);
                Draw.box(t, x - 0.36F, 1.45F, z - 0.36F, x + 0.36F, 1.6F, z + 0.36F, stoneLit, stoneDark);
                Draw.box(t, x - 0.26F, 1.6F, z - 0.26F, x + 0.26F, 2.15F, z + 0.26F, 0xFFB868, 0xC8401A);
                Draw.frustum(t, x, z, 2.15F, 0.52F, 0.52F, 2.6F, 0.08F, 0.08F, stoneLit, stoneDark);
            }
        }
        // The rope across the front: a sagging run of thick knots, and paper streamers under it that sway.
        int knots = 18;
        for (int k = 0; k < knots; k++) {
            float u = (k + 0.5F) / knots * 2 - 1;
            float x = u * 4.5F;
            float y = 6.0F - 0.9F * (1 - u * u);
            float thick = 0.2F + 0.1F * (1 - u * u);
            Draw.box(
                t,
                x - 0.27F,
                y - thick,
                5.5F,
                x + 0.27F,
                y + thick,
                5.5F + thick * 2,
                k % 2 == 0 ? 0xD8B880 : 0xB89058,
                0x4A3018);
        }
        for (int k = 0; k < 5; k++) {
            float u = (k + 0.5F) / 5 * 2 - 1;
            float x = u * 3.8F;
            float y = 5.75F - 0.9F * (1 - u * u);
            float sway = (float) Math.sin(time * 0.07F + k * 1.7F) * 0.18F;
            for (int fold = 0; fold < 3; fold++) {
                float x0 = x + (fold % 2 == 0 ? -0.18F : 0.02F) + sway * fold * 0.5F;
                float top = y - fold * 0.42F;
                Draw.color(t, 0xF4EEE6, 1);
                t.addVertex(x0, top, 5.86F);
                t.addVertex(x0 + 0.34F, top, 5.86F);
                Draw.color(t, 0xB8A8A0, 1);
                t.addVertex(x0 + 0.34F + sway * 0.5F, top - 0.46F, 5.86F);
                t.addVertex(x0 + sway * 0.5F, top - 0.46F, 5.86F);
            }
        }
        // The banners: long cloths that ripple down their length.
        for (int sx = -1; sx <= 1; sx += 2) {
            float x = sx * 9.6F;
            int strips = 12;
            for (int k = 0; k < strips; k++) {
                float y0 = 12.9F - k * 0.75F;
                float y1 = y0 - 0.75F;
                float w0 = (float) Math.sin(time * 0.09F + k * 0.55F + sx) * 0.05F * k;
                float w1 = (float) Math.sin(time * 0.09F + (k + 1) * 0.55F + sx) * 0.05F * (k + 1);
                int c0 = Draw.mix(0x7A0A10, 0x2A0306, 0.5F + 0.5F * (float) Math.sin(k * 0.55F + time * 0.09F));
                Draw.color(t, c0, 1);
                t.addVertex(x - 0.85F + w0, y0, 6.95F + w0);
                t.addVertex(x + 0.85F + w0, y0, 6.95F + w0);
                Draw.color(t, Draw.mix(c0, 0x000000, 0.25F), 1);
                t.addVertex(x + 0.85F + w1, y1, 6.95F + w1);
                t.addVertex(x - 0.85F + w1, y1, 6.95F + w1);
            }
        }
        t.draw();
        // The doorway: fire deep inside the hall.
        Draw.translucent();
        t.startDrawingQuads();
        float burn = 0.8F + 0.2F * (float) Math.sin(time * 0.23F) * (float) Math.sin(time * 0.071F + 1);
        Draw.color(t, 0x200004, 1);
        t.addVertex(-1.75F, 2.1F, 4.4F);
        t.addVertex(1.75F, 2.1F, 4.4F);
        Draw.color(t, Draw.mix(0x400008, 0xFF7A28, burn * 0.55F), 1);
        t.addVertex(1.75F, 6.3F, 4.4F);
        t.addVertex(-1.75F, 6.3F, 4.4F);
        Draw.color(t, Draw.mix(0xFF4A18, 0xFFC860, burn), 0.95F);
        t.addVertex(-1.2F, 2.1F, 4.45F);
        t.addVertex(1.2F, 2.1F, 4.45F);
        Draw.color(t, 0xFF2A10, 0);
        t.addVertex(0.9F, 5.6F, 4.45F);
        t.addVertex(-0.9F, 5.6F, 4.45F);
        // Windows of the upper storeys.
        for (int k = -1; k <= 1; k++) {
            Draw.color(t, Draw.mix(0xFF5A20, 0xFFC060, burn), 0.9F);
            t.addVertex(k * 1.7F - 0.45F, 10.9F, 3.03F);
            t.addVertex(k * 1.7F + 0.45F, 10.9F, 3.03F);
            t.addVertex(k * 1.7F + 0.45F, 12.0F, 3.03F);
            t.addVertex(k * 1.7F - 0.45F, 12.0F, 3.03F);
        }
        Draw.color(t, Draw.mix(0xFF5A20, 0xFFC060, burn), 0.9F);
        t.addVertex(-0.5F, 14.9F, 1.93F);
        t.addVertex(0.5F, 14.9F, 1.93F);
        t.addVertex(0.5F, 15.8F, 1.93F);
        t.addVertex(-0.5F, 15.8F, 1.93F);
        t.draw();
        // Rings of light on the spire.
        Draw.additive();
        GL11.glDepthMask(false);
        for (int k = 0; k < 3; k++) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 19.4F + k * 1.25F, 0);
            GL11.glRotatef(time * (1.2F + k * 0.5F) * (k % 2 == 0 ? 1 : -1), 0, 1, 0);
            GL11.glRotatef(8, 1, 0, 0);
            float r = 1.5F - k * 0.36F;
            Draw.ring(r - 0.07F, r + 0.07F, 40, 0xFFC860, 0.9F, 0xFFC860, 0.9F);
            Draw.ring(r - 0.5F, r + 0.5F, 40, 0xFF6A20, 0.0F, 0xFF6A20, 0.22F);
            GL11.glPopMatrix();
        }
        GL11.glDepthMask(true);
    }

    /**
     * One roof of the shrine: eaves that flare out and turn up at the corners, then the slope up to the storey
     * above, with a beam under the eaves.
     */
    private static void roof(Tessellator t, float y, float eave, float mid, float top, float rise, int lit, int dark,
        int woodLit, int woodDark) {
        Draw.box(t, -mid - 0.3F, y - 0.35F, -mid - 0.3F, mid + 0.3F, y, mid + 0.3F, woodLit, woodDark);
        Draw.frustum(t, 0, 0, y, eave, eave, y + rise * 0.3F, mid, mid, lit, dark);
        Draw.frustum(t, 0, 0, y + rise * 0.3F, mid, mid, y + rise, top, top, lit, dark);
        // The corners turn up.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                float x = sx * eave;
                float z = sz * eave;
                Draw.color(t, Draw.mix(dark, lit, 0.8F), 1);
                t.addVertex(x - sx * 1.5F, y + 0.05F, z);
                t.addVertex(x, y + 0.05F, z - sz * 1.5F);
                Draw.color(t, 0xC8782A, 1);
                t.addVertex(x + sx * 0.4F, y + 0.7F, z + sz * 0.4F);
                t.addVertex(x + sx * 0.4F, y + 0.7F, z + sz * 0.4F);
            }
        }
        // A ridge of lighter tiles along each edge of the eaves.
        Draw.box(t, -eave, y - 0.06F, eave - 0.14F, eave, y + 0.1F, eave, 0x7A1A1E, 0x2A0608);
        Draw.box(t, -eave, y - 0.06F, -eave, eave, y + 0.1F, -eave + 0.14F, 0x7A1A1E, 0x2A0608);
        Draw.box(t, eave - 0.14F, y - 0.06F, -eave, eave, y + 0.1F, eave, 0x7A1A1E, 0x2A0608);
        Draw.box(t, -eave, y - 0.06F, -eave, -eave + 0.14F, y + 0.1F, eave, 0x7A1A1E, 0x2A0608);
    }

    /** Every light of the scene, as soft glows facing the camera: the gates' lanterns, the path's, the doorway. */
    private static void lights(Tessellator t, int radius, float time) {
        Draw.additive();
        GL11.glDepthMask(false);
        t.startDrawing(GL11.GL_TRIANGLES);
        for (int i = 0; i < GATES; i++) {
            double a = Math.toRadians(360F * (i + 0.5F) / GATES);
            for (int side = -1; side <= 1; side += 2) {
                float along = side * 2.1F;
                double x = Math.sin(a) * (radius + 5.5F) + Math.cos(a) * along;
                double z = Math.cos(a) * (radius + 5.5F) - Math.sin(a) * along;
                float flicker = 0.75F + 0.25F * (float) Math.sin(time * 0.31F + i * 2.3F + side);
                Draw.glow(t, x, 5.0F, z, 1.7F, 0xFF8A30, 0.55F * flicker);
                Draw.glow(t, x, 5.0F, z, 4.2F, 0xFF5A20, 0.22F * flicker);
            }
        }
        float shrineZ = 0;
        float k1 = SHRINE_SCALE;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int k = 0; k < 2; k++) {
                float flicker = 0.8F + 0.2F * (float) Math.sin(time * 0.27F + k * 1.9F + sx);
                float z = shrineZ + (9.6F + k * 2.6F) * k1;
                Draw.glow(t, sx * 3.3F * k1, 1.9F * k1, z, 1.3F, 0xFFA040, 0.6F * flicker);
                Draw.glow(t, sx * 3.3F * k1, 1.9F * k1, z, 3.2F, 0xFF5A20, 0.2F * flicker);
            }
        }
        float burn = 0.8F + 0.2F * (float) Math.sin(time * 0.23F);
        Draw.glow(t, 0, 4.0F * k1, shrineZ + 5.6F * k1, 5.5F, 0xFF6A20, 0.45F * burn);
        Draw.glow(t, 0, 4.0F * k1, shrineZ + 5.6F * k1, 13F, 0xFF2A10, 0.22F * burn);
        Draw.glow(t, 0, 24.3F * k1, shrineZ, 3F, 0xFFD080, 0.6F);
        Draw.glow(t, 0, 21F * k1, shrineZ, 9F, 0xFF5A20, 0.16F);
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
