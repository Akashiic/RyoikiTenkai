package com.akashic.ryoiki.client;

import java.util.Random;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.StreakField;

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
    /**
     * Where the armillary and its black hole hang: a unit direction, a third of the way up and before the caster
     * (the scene is turned by where it looked), so whoever casts the void has it in front of it.
     */
    private static final float EYE_X = 0;
    private static final float EYE_Y = 0.47F;
    private static final float EYE_Z = 0.883F;
    /** How far across the sky a star is carried away from the armillary for each tick of full rush, in radians. */
    private static final float CARRY = 0.011F;
    /** How long a star's trail is at the height of the rush, across the sky, in radians. */
    private static final float TRAIL = 0.085F;

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
        float[] ground = backdrop(time);
        Draw.sphere(radius, (int) ground[0], (int) ground[1], (int) ground[2], ground[3]);
        skyFeatures(radius, time);
    }

    /**
     * While the domain starts the sky flares with the rush: the horizon lit violet, a wider band of it. Complete,
     * it is steady, and only breathes: the horizon a little brighter and dimmer, slowly.
     */
    @Override
    public float[] backdrop(float time) {
        float rush = StreakField.rush(time);
        float breath = 0.5F + 0.5F * (float) Math.sin(time * 0.045F);
        int horizon = Draw.mix(Draw.mix(0x141040, 0x211C62, breath), 0x5A3CD0, rush * 0.85F);
        int top = Draw.mix(0x010208, 0x0E0830, rush);
        return new float[] { top, horizon, 0x010208, 0.55F - 0.2F * rush };
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

    /**
     * The clouds of the sky: each breathes at its own pace, brighter and wider and back, and all of them flare
     * while the domain starts.
     */
    private static void nebulae(float radius, float time) {
        float flare = 1 + 1.3F * StreakField.rush(time);
        nebula(radius, time, 0, 0.62F, 0.30F, 0.72F, 0.55F, 0x5B2BD8, 0.30F * flare, 0x2A0C6B);
        nebula(radius, time, 1, -0.80F, 0.22F, 0.55F, 0.45F, 0x1C7BD9, 0.24F * flare, 0x0A2A66);
        nebula(radius, time, 2, -0.55F, 0.42F, -0.72F, 0.50F, 0xB02BC9, 0.22F * flare, 0x3A0B55);
        nebula(radius, time, 3, 0.85F, 0.12F, -0.50F, 0.40F, 0x23B8C9, 0.18F * flare, 0x0B3C55);
        nebula(radius, time, 4, 0.05F, 0.92F, 0.38F, 0.60F, 0x3340C8, 0.16F * flare, 0x101450);
    }

    private static void nebula(float radius, float time, int which, float x, float y, float z, float size, int core,
        float alpha, int rim) {
        float breath = (float) Math.sin(time * (0.031F + which * 0.007F) + which * 1.9F);
        Draw.skyDisc(
            x,
            y,
            z,
            radius * 0.97F,
            radius * size * (1 + 0.2F * breath),
            core,
            alpha * (0.62F + 0.38F * breath),
            rim,
            0);
    }

    private void stars(float radius, float time) {
        Tessellator t = Tessellator.instance;
        // While the domain starts the stars go the way the rush goes: straight out from the armillary, each drawing
        // a trail behind it. When it is complete they stay where that left them: the sky is still.
        float trail = TRAIL * StreakField.rush(time);
        float carried = CARRY * StreakField.rushed(time);
        t.startDrawingQuads();
        for (int i = 0; i < STARS; i++) {
            float x = stars[i * 5];
            float y = stars[i * 5 + 1];
            float z = stars[i * 5 + 2];
            // Away from the armillary, across the sky: the way this star is carried.
            float toward = x * EYE_X + y * EYE_Y + z * EYE_Z;
            float ox = x - EYE_X * toward;
            float oy = y - EYE_Y * toward;
            float oz = z - EYE_Z * toward;
            float out = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
            if (out > 1.0e-3F) {
                ox /= out;
                oy /= out;
                oz /= out;
                x += ox * carried;
                y += oy * carried;
                z += oz * carried;
                float unit = (float) Math.sqrt(x * x + y * y + z * z);
                x /= unit;
                y /= unit;
                z /= unit;
            }
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
            if (trail > 0.002F) {
                float w = size * 0.8F;
                Draw.color(t, tint, twinkle);
                t.addVertex((x - vx * w) * radius, (y - vy * w) * radius, (z - vz * w) * radius);
                t.addVertex((x + vx * w) * radius, (y + vy * w) * radius, (z + vz * w) * radius);
                Draw.color(t, tint, 0);
                t.addVertex((x - ox * trail) * radius, (y - oy * trail) * radius, (z - oz * trail) * radius);
                t.addVertex((x - ox * trail) * radius, (y - oy * trail) * radius, (z - oz * trail) * radius);
                Draw.color(t, tint, twinkle);
            }
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
        float rush = StreakField.rush(time);
        float size = distance * 0.36F * (0.35F + 0.65F * open);
        // Its halo flares as everything pours out of it, and afterwards beats slowly.
        float beat = 0.85F + 0.15F * (float) Math.sin(time * 0.09F);
        Draw.skyDisc(
            EYE_X,
            EYE_Y,
            EYE_Z,
            distance,
            size * (2.6F + 2.4F * rush),
            Draw.mix(0x7AA8FF, 0xFFFFFF, rush * 0.6F),
            (0.34F * open + 0.5F * rush) * beat,
            0x2030A0,
            0);
        // The rings spin up with the rush.
        time += StreakField.rushed(time) * 4;
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
        StreakFx.draw(t, radius, time, seed, EYE_X, EYE_Y, EYE_Z, camX, camY, camZ);
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
