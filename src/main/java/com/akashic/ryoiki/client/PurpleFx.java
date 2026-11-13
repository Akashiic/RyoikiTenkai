package com.akashic.ryoiki.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The purple: what a red orb and an idle blue one become when they meet. It hangs where the blue was for three
 * seconds, growing, a magenta sphere with a white heart in a storm of its own lightning, the ground about it lit as
 * far as it will reach; a dark shell swells out of it toward that reach; and then it bursts, and for a moment
 * everything near is white.
 */
public final class PurpleFx {

    public static final PurpleFx INSTANCE = new PurpleFx();

    private static final int[] SPHERE = { 0x7A10B0, 0xD030F0, 0xFF80FF, 0xFFE0FF, 0xFFFFFF };
    private static final int[] SHELL = { 0x12001E, 0x2A0440, 0x4A0A70, 0x7A1AA0, 0xB040E0 };
    private static final int MAGENTA = 0xE030F0;
    private static final int PINK = 0xFF70FF;
    private static final int DEEP = 0x3A0670;
    /** Ticks the burst is drawn for, and of them how many the screens near it stay white. */
    private static final int BURST = 34;
    private static final int WHITE = 9;
    private static final int BOLTS = 12;

    private final List<Purple> purples = new ArrayList<>();

    private static final class Purple {

        double x;
        double y;
        double z;
        float radius;
        int fuse;
        int age;
        /** Ticks since it burst; negative while it has not. */
        int burst = -1;
        /** Drawn here only, for the harness: it bursts by itself. */
        boolean local;
        long seed;
        float ground = Float.NaN;
    }

    private PurpleFx() {}

    void clear() {
        purples.clear();
    }

    boolean active() {
        return !purples.isEmpty();
    }

    public void start(double x, double y, double z, float radius, int fuse, boolean local) {
        Purple p = new Purple();
        p.x = x;
        p.y = y;
        p.z = z;
        p.radius = radius;
        p.fuse = fuse;
        p.local = local;
        p.seed = Double.doubleToLongBits(x * 13 + z * 7 + y);
        purples.add(p);
        // The instant the two meet: the frames of an impact, on every screen near.
        BlackFlashFx.INSTANCE.impact(x, y, z, radius * 2);
    }

    /** The one nearest a place bursts. */
    void burst(double x, double y, double z) {
        Purple nearest = null;
        double best = 16;
        for (Purple p : purples) {
            double d = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y) + (p.z - z) * (p.z - z);
            if (p.burst < 0 && d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest != null) {
            nearest.burst = 0;
            CameraShake.INSTANCE.kick(11 * felt(nearest));
        }
    }

    /** How much of it someone at this client's place feels: all of it within its reach, none three reaches away. */
    private static float felt(Purple p) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return 0;
        }
        float distance = (float) Math.sqrt(mc.thePlayer.getDistanceSq(p.x, p.y, p.z));
        return Draw.clamp01(1 - (distance - p.radius) / (p.radius * 2));
    }

    void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        for (Iterator<Purple> i = purples.iterator(); i.hasNext();) {
            Purple p = i.next();
            p.age++;
            if (p.burst >= 0) {
                p.burst++;
            } else if (p.local && p.age >= p.fuse) {
                p.burst = 0;
            } else if (p.age % 4 == 0) {
                // It shakes what is near it, more as it grows.
                CameraShake.INSTANCE.kick(1.6F * Math.min(1, (float) p.age / p.fuse) * felt(p));
            }
            if (p.burst > BURST || p.burst < 0 && p.age > p.fuse + 40 || mc.theWorld == null) {
                i.remove();
                continue;
            }
            p.ground = Float.NaN;
            int bx = MathHelper.floor_double(p.x);
            int bz = MathHelper.floor_double(p.z);
            for (int by = MathHelper.floor_double(p.y); by >= MathHelper.floor_double(p.y) - 8; by--) {
                if (mc.theWorld.getBlock(bx, by, bz)
                    .getMaterial()
                    .blocksMovement()) {
                    p.ground = by + 1;
                    break;
                }
            }
        }
    }

    // In the world. Places are in blocks from the camera; the caller has set the state for drawing light.

    void world(Tessellator t, double camX, double camY, double camZ, float partialTicks) {
        for (Purple p : purples) {
            float x = (float) (p.x - camX);
            float y = (float) (p.y - camY);
            float z = (float) (p.z - camZ);
            float ground = Float.isNaN(p.ground) ? Float.NaN : (float) (p.ground - camY) + 0.04F;
            if (p.burst >= 0) {
                burst(t, p, x, y, z, ground, p.burst + partialTicks);
            } else {
                fuse(t, p, x, y, z, ground, p.age + partialTicks);
            }
        }
    }

    private static void fuse(Tessellator t, Purple p, float x, float y, float z, float ground, float age) {
        float full = Draw.clamp01(age / p.fuse);
        float born = Draw.clamp01(age / 4);
        // Inside its light the eye would see only white: dimmed close to it.
        float near = 0.25F + 0.75F * Draw.clamp01(((float) Math.sqrt(x * x + y * y + z * z) - 1.5F) / 4);
        float size = (0.9F + 1.5F * full) * born * (1 + 0.03F * MathHelper.sin(age * 2.1F));

        // The ground it will reach: lit, and its edge marked.
        if (!Float.isNaN(ground)) {
            float beat = 0.6F + 0.4F * MathHelper.sin(age * (0.3F + 0.5F * full));
            Draw.additive();
            GL11.glPushMatrix();
            GL11.glTranslatef(x, ground, z);
            Draw.ring(0, p.radius, 48, PINK, 0.3F * born * near, MAGENTA, 0.14F * born);
            Draw.ring(p.radius - 0.3F, p.radius, 48, PINK, 0, 0xFFFFFF, 0.85F * born * beat);
            GL11.glPopMatrix();
        }
        Draw.additive();
        t.startDrawing(GL11.GL_TRIANGLES);
        Draw.glow(t, x, y, z, size * 5.5F, MAGENTA, 0.5F * born * near);
        Draw.glow(t, x, y, z, size * 2.8F, PINK, 0.6F * born * near);
        t.draw();

        // What it was made of, still winding into it for a moment: the blue and the red.
        float made = 1 - Draw.clamp01(age / 22);
        t.startDrawingQuads();
        if (made > 0) {
            for (int arc = 0; arc < 4; arc++) {
                int color = arc % 2 == 0 ? 0x3F8CFF : 0xFF2A1A;
                float tilt = 0.4F + arc * 0.8F;
                float turn = age * 0.6F + arc * 1.57F;
                float reach = size * (1.4F + 1.6F * made);
                float[] last = null;
                for (int step = 0; step <= 12; step++) {
                    float a = turn + step * 0.2F;
                    float ca = MathHelper.cos(a) * reach;
                    float sa = MathHelper.sin(a) * reach;
                    float[] at = { x + ca, y + sa * MathHelper.sin(tilt), z + sa * MathHelper.cos(tilt) };
                    if (last != null) {
                        float f0 = (step - 1) / 12F;
                        float f1 = step / 12F;
                        AbilityFx.ribbon(
                            t,
                            last[0],
                            last[1],
                            last[2],
                            at[0],
                            at[1],
                            at[2],
                            0.12F * f0,
                            0.12F * f1,
                            color,
                            made * f0,
                            color,
                            made * f1);
                    }
                    last = at;
                }
            }
        }
        // Its lightning: from it to anywhere within its reach, farther as it grows, a new set every other tick.
        float[][][] bolts = new float[BOLTS][8][3];
        for (int i = 0; i < BOLTS; i++) {
            Random random = new Random(mix(p.seed + i * 7919L + (long) (age / 2) * 104729L));
            float dx = random.nextFloat() * 2 - 1;
            float dy = random.nextFloat() * 1.4F - 0.7F;
            float dz = random.nextFloat() * 2 - 1;
            float n = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) + 1e-4F;
            float reach = p.radius * (0.3F + 0.7F * full) * (0.5F + 0.5F * random.nextFloat()) / n;
            for (int k = 0; k < 8; k++) {
                float along = k / 7F;
                float jag = k == 0 ? 0 : 0.14F * reach * n;
                bolts[i][k][0] = x + dx * (size * 0.8F / n + reach * along) + (random.nextFloat() - 0.5F) * jag;
                bolts[i][k][1] = y + dy * (size * 0.8F / n + reach * along) + (random.nextFloat() - 0.5F) * jag;
                bolts[i][k][2] = z + dz * (size * 0.8F / n + reach * along) + (random.nextFloat() - 0.5F) * jag;
            }
        }
        for (float[][] bolt : bolts) {
            for (int k = 0; k < 7; k++) {
                float f0 = 1 - k / 8F;
                float f1 = 1 - (k + 1) / 8F;
                AbilityFx.ribbon(
                    t,
                    bolt[k][0],
                    bolt[k][1],
                    bolt[k][2],
                    bolt[k + 1][0],
                    bolt[k + 1][1],
                    bolt[k + 1][2],
                    0.2F * f0,
                    0.2F * f1,
                    MAGENTA,
                    0.55F * born * near,
                    MAGENTA,
                    0.55F * born * near);
                AbilityFx.ribbon(
                    t,
                    bolt[k][0],
                    bolt[k][1],
                    bolt[k][2],
                    bolt[k + 1][0],
                    bolt[k + 1][1],
                    bolt[k + 1][2],
                    0.055F * f0,
                    0.055F * f1,
                    0xFFFFFF,
                    born,
                    0xFFD0FF,
                    born);
            }
        }
        t.draw();
        // About it, splashes of deep purple, turning.
        Draw.translucent();
        t.startDrawing(GL11.GL_TRIANGLES);
        Random random = new Random(p.seed);
        for (int i = 0; i < 11; i++) {
            float tilt = (random.nextFloat() - 0.5F) * 2.4F;
            float a = random.nextFloat() * 6.283F + age * (0.3F + 0.3F * random.nextFloat()) * (i % 2 == 0 ? 1 : -1);
            float reach = size * (1.25F + 0.6F * random.nextFloat());
            float ca = MathHelper.cos(a) * reach;
            float sa = MathHelper.sin(a) * reach;
            Draw.glow(
                t,
                x + ca * MathHelper.cos(tilt),
                y + sa * 0.85F + ca * MathHelper.sin(tilt) * 0.5F,
                z + sa * 0.6F - ca * MathHelper.sin(tilt) * 0.4F,
                size * 0.55F,
                DEEP,
                0.85F * born);
            Draw.glow(
                t,
                x + ca * MathHelper.cos(tilt),
                y + sa * 0.85F + ca * MathHelper.sin(tilt) * 0.5F,
                z + sa * 0.6F - ca * MathHelper.sin(tilt) * 0.4F,
                size * 0.3F,
                MAGENTA,
                born);
        }
        t.draw();
        AbilityFx.sphere(t, x, y, z, size, age * 2, born, SPHERE, PINK);
        // Toward the end a dark shell swells out of it, to as far as it will reach.
        float swell = Draw.clamp01((full - 0.62F) / 0.38F);
        if (swell > 0) {
            AbilityFx.sphere(t, x, y, z, size + (p.radius - size) * swell * swell, age, 0.42F * swell, SHELL, PINK);
        }
        Draw.additive();
    }

    private static void burst(Tessellator t, Purple p, float x, float y, float z, float ground, float since) {
        float left = Draw.clamp01(1 - since / BURST);
        float out = 1 - (1 - Draw.clamp01(since / 10)) * (1 - Draw.clamp01(since / 10));
        Draw.additive();
        t.startDrawing(GL11.GL_TRIANGLES);
        Draw.glow(t, x, y, z, p.radius * 2.4F * (0.3F + 0.7F * out), 0xFFFFFF, left);
        Draw.glow(t, x, y, z, p.radius * 3.6F * out, PINK, 0.7F * left);
        t.draw();
        if (!Float.isNaN(ground)) {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, ground, z);
            Draw.ring(0, p.radius, 48, 0xFFFFFF, 0.9F * left, PINK, 0.5F * left);
            float wave = p.radius * (1 + 1.6F * since / BURST);
            Draw.ring(wave - 1.2F, wave, 48, PINK, 0, 0xFFFFFF, left);
            GL11.glPopMatrix();
        }
        // What is left of it: motes of pink light, rising.
        t.startDrawing(GL11.GL_TRIANGLES);
        Random random = new Random(p.seed + 5);
        for (int i = 0; i < 40; i++) {
            float a = random.nextFloat() * 6.283F;
            float far = p.radius * (float) Math.sqrt(random.nextFloat());
            float up = random.nextFloat() * 3 + since * (0.05F + 0.1F * random.nextFloat());
            Draw.glow(
                t,
                x + MathHelper.cos(a) * far,
                (Float.isNaN(ground) ? y - 1 : ground) + up,
                z + MathHelper.sin(a) * far,
                0.12F + 0.14F * random.nextFloat(),
                i % 2 == 0 ? PINK : 0xFFFFFF,
                left * Draw.clamp01(since / 6));
        }
        t.draw();
    }

    private static long mix(long seed) {
        seed = (seed ^ seed >>> 30) * 0xBF58476D1CE4E5B9L;
        seed = (seed ^ seed >>> 27) * 0x94D049BB133111EBL;
        return seed ^ seed >>> 31;
    }

    /** On the screens near it: a breath of pink while it grows, and white when it bursts. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (purples.isEmpty() || event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        int w = event.resolution.getScaledWidth();
        int h = event.resolution.getScaledHeight();
        for (Purple p : purples) {
            float felt = felt(p);
            if (felt <= 0) {
                continue;
            }
            if (p.burst < 0) {
                int pink = (int) (46 * felt * Draw.clamp01((float) p.age / p.fuse));
                if (pink > 2) {
                    Gui.drawRect(0, 0, w, h, pink << 24 | 0xE030F0);
                }
            } else {
                float since = p.burst + event.partialTicks;
                int white = (int) (255 * felt * Draw.clamp01(1 - (since - WHITE) / (BURST - WHITE)));
                if (white > 2) {
                    Gui.drawRect(0, 0, w, h, white << 24 | 0xFFFFFF);
                }
            }
        }
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_BLEND);
    }
}
