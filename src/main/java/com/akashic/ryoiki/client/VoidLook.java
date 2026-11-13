package com.akashic.ryoiki.client;

import java.util.Random;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

/**
 * The star void: deep space all around, mirrored in a floor of black glass; far off, an armillary of slow rings
 * about a dark core; motes of light rising.
 */
final class VoidLook implements DomainLook {

    private static final int STARS = 1100;
    private static final int BRIGHT = 26;
    private static final int MOTES = 110;

    /** Each star: direction (3), size, phase. */
    private final float[] stars = new float[STARS * 5];
    /** Where the armillary hangs: a unit direction, north and a third of the way up. */
    private static final float EYE_X = 0;
    private static final float EYE_Y = 0.47F;
    private static final float EYE_Z = -0.883F;

    VoidLook() {
        Random random = new Random(0x52594F494B49L);
        for (int i = 0; i < STARS; i++) {
            double y = random.nextDouble();
            // More of them low, where the sky is brightest, and a band of them across it.
            y = y * y * 0.98 + 0.01;
            double a = random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            stars[i * 5] = (float) (Math.cos(a) * r);
            stars[i * 5 + 1] = (float) y;
            stars[i * 5 + 2] = (float) (Math.sin(a) * r);
            float s = random.nextFloat();
            stars[i * 5 + 3] = 0.0011F + s * s * s * 0.0034F;
            stars[i * 5 + 4] = random.nextFloat();
        }
    }

    @Override
    public void sky(float radius, float time) {
        GL11.glDisable(GL11.GL_BLEND);
        Draw.sphere(radius, 0x010208, 0x1A1650, 0x010208, 0.55F);
        skyFeatures(radius, time);
    }

    @Override
    public float[] backdrop() {
        return new float[] { 0x010208, 0x1A1650, 0x010208, 0.55F };
    }

    @Override
    public void skyFeatures(float radius, float time) {
        Draw.additive();
        for (int pass = 0; pass < 2; pass++) {
            // Once as it is, once upside down and fainter: the reflection in the floor.
            GL11.glPushMatrix();
            if (pass == 1) {
                GL11.glScalef(1, -1, 1);
                Draw.alpha = 0.4F;
            }
            nebulae(radius, time);
            stars(radius * 0.98F, time);
            armillary(radius * 0.9F, time);
            GL11.glPopMatrix();
        }
        Draw.alpha = 1;
    }

    private static void nebulae(float radius, float time) {
        float breath = 0.85F + 0.15F * (float) Math.sin(time * 0.02F);
        Draw.skyDisc(0.62F, 0.30F, 0.72F, radius * 0.97F, radius * 0.55F, 0x5B2BD8, 0.30F * breath, 0x2A0C6B, 0);
        Draw.skyDisc(-0.80F, 0.22F, 0.55F, radius * 0.97F, radius * 0.45F, 0x1C7BD9, 0.24F * breath, 0x0A2A66, 0);
        Draw.skyDisc(-0.55F, 0.42F, -0.72F, radius * 0.97F, radius * 0.50F, 0xB02BC9, 0.20F, 0x3A0B55, 0);
        Draw.skyDisc(0.85F, 0.12F, -0.50F, radius * 0.97F, radius * 0.40F, 0x23B8C9, 0.16F, 0x0B3C55, 0);
        Draw.skyDisc(0.05F, 0.92F, 0.38F, radius * 0.97F, radius * 0.60F, 0x3340C8, 0.14F, 0x101450, 0);
    }

    private void stars(float radius, float time) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        for (int i = 0; i < STARS; i++) {
            float x = stars[i * 5];
            float y = stars[i * 5 + 1];
            float z = stars[i * 5 + 2];
            float size = stars[i * 5 + 3];
            float phase = stars[i * 5 + 4];
            float twinkle = 0.6F + 0.4F * (float) Math.sin(time * (0.08F + phase * 0.14F) + phase * 40);
            int tint = phase < 0.2F ? 0xBFD4FF : phase < 0.3F ? 0xFFE2C4 : 0xFFFFFF;
            Draw.color(t, tint, twinkle);
            // Two directions across the sky at this star.
            float len = (float) Math.sqrt(x * x + z * z);
            float ux = len < 1.0e-4F ? 1 : -z / len;
            float uz = len < 1.0e-4F ? 0 : x / len;
            float vx = y * uz;
            float vy = z * ux - x * uz;
            float vz = -y * ux;
            if (i < BRIGHT) {
                // The few bright ones: a long thin cross.
                float arm = size * 9;
                float thin = size * 0.45F;
                quad(t, radius, x, y, z, ux, 0, uz, vx, vy, vz, arm, thin);
                quad(t, radius, x, y, z, ux, 0, uz, vx, vy, vz, thin, arm);
            }
            quad(t, radius, x, y, z, ux, 0, uz, vx, vy, vz, size, size);
        }
        t.draw();
    }

    private static void quad(Tessellator t, float radius, float x, float y, float z, float ux, float uy, float uz,
        float vx, float vy, float vz, float a, float b) {
        t.addVertex((x - ux * a - vx * b) * radius, (y - uy * a - vy * b) * radius, (z - uz * a - vz * b) * radius);
        t.addVertex((x + ux * a - vx * b) * radius, (y + uy * a - vy * b) * radius, (z + uz * a - vz * b) * radius);
        t.addVertex((x + ux * a + vx * b) * radius, (y + uy * a + vy * b) * radius, (z + uz * a + vz * b) * radius);
        t.addVertex((x - ux * a + vx * b) * radius, (y - uy * a + vy * b) * radius, (z - uz * a + vz * b) * radius);
    }

    /** A dark core in a white rim, and rings turning about it, each in its own plane. */
    private static void armillary(float distance, float time) {
        float open = Draw.easeOut(time / 45F);
        float size = distance * 0.2F * (0.35F + 0.65F * open);
        Draw.skyDisc(EYE_X, EYE_Y, EYE_Z, distance, size * 2.6F, 0x7AA8FF, 0.34F * open, 0x2030A0, 0);
        Draw.skyDisc(EYE_X, EYE_Y, EYE_Z, distance * 0.995F, size * 0.62F, 0xFFFFFF, 0.95F, 0xDDEBFF, 0.9F);
        GL11.glDisable(GL11.GL_BLEND);
        float keep = Draw.alpha;
        Draw.skyDisc(
            EYE_X,
            EYE_Y,
            EYE_Z,
            distance * 0.99F,
            size * 0.52F,
            Draw.mix(0x000000, 0x0B1030, 1 - keep),
            1,
            Draw.mix(0x000000, 0x0B1030, 1 - keep),
            1);
        Draw.additive();
        GL11.glPushMatrix();
        GL11.glTranslatef(EYE_X * distance * 0.985F, EYE_Y * distance * 0.985F, EYE_Z * distance * 0.985F);
        // Face the origin: the rings' planes are set from here.
        GL11.glRotatef(28, 1, 0, 0);
        GL11.glRotatef(90, 1, 0, 0);
        float[] radii = { 0.86F, 1.12F, 1.42F, 1.78F };
        for (int i = 0; i < radii.length; i++) {
            GL11.glPushMatrix();
            GL11.glRotatef(time * (0.35F + i * 0.22F) * (i % 2 == 0 ? 1 : -1) + i * 47, 0, 1, 0);
            GL11.glRotatef(18 + i * 21 + 6 * (float) Math.sin(time * 0.01F + i), 1, 0, 0);
            GL11.glRotatef(time * (0.5F - i * 0.08F), 0, 0, 1);
            float r = size * radii[i];
            float w = size * (0.022F + 0.006F * i);
            int tint = i % 2 == 0 ? 0xEAF2FF : 0x9FC4FF;
            Draw.ring(r - w, r + w, 72, tint, 0.9F * open, tint, 0.9F * open);
            Draw.ring(r - w * 4, r + w * 4, 72, tint, 0.16F * open, tint, 0.0F);
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }

    @Override
    public void floor(float far, int radius, float time) {
        Draw.translucent();
        // Black glass: darker underfoot, letting more of the mirrored sky through toward the horizon.
        Draw.ring(0, radius * 1.5F, 48, 0x02030C, 0.80F, 0x03051A, 0.66F);
        Draw.ring(radius * 1.5F, far, 48, 0x03051A, 0.66F, 0x0C0C38, 0.46F);
        Draw.additive();
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0.004F, 0);
        // Slow rings of light running out from the centre, and the edge of the domain.
        for (int i = 0; i < 5; i++) {
            float phase = (time * 0.012F + i / 5F) % 1F;
            float r = phase * radius * 1.4F;
            float fade = (1 - phase) * Draw.clamp01(phase * 6);
            Draw.ring(r - 0.5F, r, 64, 0x5B7CFF, 0, 0x8FB0FF, 0.16F * fade);
            Draw.ring(r, r + 0.06F, 64, 0xCFE0FF, 0.30F * fade, 0xCFE0FF, 0.30F * fade);
        }
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * 0.07F);
        Draw.ring(radius - 0.07F, radius + 0.07F, 96, 0xBFD4FF, 0.35F + 0.25F * pulse, 0xBFD4FF, 0.35F + 0.25F * pulse);
        Draw.ring(radius - 1.6F, radius, 96, 0x4060FF, 0, 0x7090FF, 0.12F);
        GL11.glPopMatrix();
    }

    @Override
    public void props(float far, int radius, float time, int seed, double camX, double camY, double camZ) {
        Draw.additive();
        GL11.glDepthMask(false);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLES);
        for (int i = 0; i < MOTES; i++) {
            float a = Draw.hash(i, 1) * (float) (Math.PI * 2);
            float r = (float) Math.sqrt(Draw.hash(i, 2)) * (radius + 6);
            float speed = 0.012F + Draw.hash(i, 3) * 0.03F;
            float height = 11;
            float y = (Draw.hash(i, 4) * height + time * speed) % height;
            float sway = (float) Math.sin(time * 0.03F + i) * 0.4F;
            float life = Draw.clamp01(y * 1.5F) * Draw.clamp01((height - y) * 0.35F);
            float size = 0.025F + Draw.hash(i, 5) * 0.05F;
            double x = Math.cos(a) * r + sway;
            double z = Math.sin(a) * r + Math.cos(time * 0.021F + i * 1.7F) * 0.4F;
            Draw.glow(t, x, y, z, size * 1.6F, i % 4 == 0 ? 0x9FC4FF : 0xFFFFFF, life);
            Draw.glow(t, x, y, z, size * 6, 0x7090FF, 0.22F * life);
        }
        t.draw();
        GL11.glDepthMask(true);
        StreakFx.draw(t, radius, time, seed, camX, camY, camZ);
    }

    @Override
    public int fogColor() {
        return 0x0A0A2E;
    }

    @Override
    public int shardColor() {
        return 0xF4F8FF;
    }

    @Override
    public int shardEdgeColor() {
        return 0x9FC4FF;
    }

    @Override
    public int bandFrom() {
        return 0x05071F;
    }

    @Override
    public int bandTo() {
        return 0x1B2A8C;
    }

    @Override
    public int bandTrim() {
        return 0xBFD4FF;
    }
}
