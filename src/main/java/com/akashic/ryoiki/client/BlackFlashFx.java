package com.akashic.ryoiki.client;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.glu.GLU;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The black blow, as it is seen: on the screen of everyone near, and in the world for everyone else.
 *
 * <p>
 * It is a sequence, timed by the clock and not by the game's ticks, so that it is as sharp at any frame rate:
 * <ol>
 * <li><b>The frames of the impact</b> (160 ms). The picture of the world is reduced to two tones, hard, with no
 * greys: first red and black; then its negative for an instant; then white, with only what was dark left black,
 * the longest of them; then the negative again. The picture is the frame
 * itself, taken off the screen and drawn back through a threshold.</li>
 * <li><b>The burst</b> (to 0.7 s). Everything is burnt white; thick red rays stand out from where the blow landed,
 * and a tangle of red strokes, with black among them, covers the whole screen, a new tangle every instant. The two
 * bodies glow.</li>
 * <li><b>The black sparks</b> (to 1.9 s). The light goes; what is left of the tangle is black with a red edge, fewer
 * and fewer strokes that jump from place to place, and red beads that rise from the ground and shrink.</li>
 * </ol>
 * In the world the same happens around the place of the blow, in three dimensions, for those too far to have it on
 * their screens.
 */
public final class BlackFlashFx {

    public static final BlackFlashFx INSTANCE = new BlackFlashFx();

    /** The sequence, in milliseconds from the blow. */
    private static final float RED_FRAME = 60;
    private static final float FRAME_A = 80;
    private static final float FRAME_B = 130;
    private static final float FRAMES = 160;
    private static final float BURST = 700;
    private static final float SPARKS = 1300;
    private static final float ENDS = 1900;
    /** How often the tangle is a new one. */
    private static final float JUMPS = 70;
    /** Blocks from the blow within which it is on a screen whole, and beyond which it is not on it at all. */
    private static final float NEAR = 7;
    private static final float FAR = 26;

    private static final int RED = 0xF00A0A;
    private static final int GLOW = 0xFF2A1A;
    private static final int HOT = 0xFFF2B0;

    /** Whether the frames of the impact are taken off the screen; without, they are plain white and red. */
    private static final boolean GRAB = !"false".equals(System.getProperty("ryoiki.impactFrame"));

    /** For the development harness: the sequence held at this many milliseconds; negative lets it run. */
    public static float frozen = -1;

    private final List<Flash> flashes = new ArrayList<>();
    private int texture = -1;
    private int textureWidth;
    private int textureHeight;
    private final FloatBuffer modelview = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer projection = GLAllocation.createDirectFloatBuffer(16);
    private final IntBuffer viewport = GLAllocation.createDirectIntBuffer(16);
    private final FloatBuffer projected = GLAllocation.createDirectFloatBuffer(3);

    private static final class Flash {

        double x;
        double y;
        double z;
        float dx;
        float dy;
        float dz;
        int caster;
        int victim;
        long began;
        long seed;
        /** Only the frames of the impact: no burst, no sparks, nothing in the world. */
        boolean framesOnly;
        /** How much of it is on this screen, 0 to 1. */
        float strength;
        /** Where the blow is on the screen, 0 to 1 from its top left; the middle when it is not in sight. */
        float screenX = 0.5F;
        float screenY = 0.5F;

        float ms() {
            return frozen >= 0 ? frozen : (System.nanoTime() - began) / 1e6F;
        }
    }

    private BlackFlashFx() {}

    void clear() {
        flashes.clear();
    }

    /** For the development harness. */
    public void reset() {
        flashes.clear();
    }

    boolean active() {
        return !flashes.isEmpty();
    }

    /** The black blow lands at a place, thrown along a direction, by one entity on another. */
    public void start(double x, double y, double z, float dx, float dy, float dz, int caster, int victim) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        Flash f = new Flash();
        f.x = x;
        f.y = y;
        f.z = z;
        f.dx = dx;
        f.dy = dy;
        f.dz = dz;
        f.caster = caster;
        f.victim = victim;
        f.began = System.nanoTime();
        f.seed = Double.doubleToLongBits(x * 31 + z * 17 + y) ^ caster * 7919L;
        int own = mc.thePlayer.getEntityId();
        float distance = (float) Math.sqrt(mc.thePlayer.getDistanceSq(x, y, z));
        f.strength = own == caster || own == victim ? 1 : Draw.clamp01(1 - (distance - NEAR) / (FAR - NEAR));
        flashes.add(f);
        if (f.strength > 0) {
            CameraShake.INSTANCE.kick((own == victim ? 12 : own == caster ? 7 : 5) * f.strength);
        }
        debris(mc.theWorld, f);
    }

    /** Only the frames of an impact, for something else that lands at a place: on the screens within {@code far}. */
    void impact(double x, double y, double z, float far) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        Flash f = new Flash();
        f.x = x;
        f.y = y;
        f.z = z;
        f.framesOnly = true;
        f.began = System.nanoTime();
        f.strength = Math.sqrt(mc.thePlayer.getDistanceSq(x, y, z)) <= far ? 1 : 0;
        if (f.strength > 0) {
            flashes.add(f);
        }
    }

    /** The ground under the blow, thrown up in bits: the game's own, of whatever block is there. */
    private static void debris(World world, Flash f) {
        Random random = new Random(f.seed);
        int bx = MathHelper.floor_double(f.x);
        int bz = MathHelper.floor_double(f.z);
        for (int by = MathHelper.floor_double(f.y); by >= MathHelper.floor_double(f.y) - 4; by--) {
            Block block = world.getBlock(bx, by, bz);
            if (!block.getMaterial()
                .blocksMovement() || block.getRenderType() < 0) {
                continue;
            }
            String name = "blockcrack_" + Block.getIdFromBlock(block) + "_" + world.getBlockMetadata(bx, by, bz);
            for (int i = 0; i < 70; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double out = 0.25 + random.nextDouble() * 0.5;
                world.spawnParticle(
                    name,
                    f.x + Math.cos(a) * random.nextDouble() * 1.6,
                    by + 1.1,
                    f.z + Math.sin(a) * random.nextDouble() * 1.6,
                    Math.cos(a) * out + f.dx * 0.3,
                    0.25 + random.nextDouble() * 0.55,
                    Math.sin(a) * out + f.dz * 0.3);
            }
            return;
        }
    }

    /** Once a tick: the ones that are over go. */
    void tick() {
        for (Iterator<Flash> i = flashes.iterator(); i.hasNext();) {
            Flash f = i.next();
            if (frozen < 0 && f.ms() > (f.framesOnly ? FRAMES : ENDS)) {
                i.remove();
            }
        }
        if (Minecraft.getMinecraft().theWorld == null) {
            flashes.clear();
        }
    }

    // In the world. Places are in blocks from the camera; the caller has set the state for drawing light.

    void world(Tessellator t, Minecraft mc, double camX, double camY, double camZ, float partialTicks) {
        for (Flash f : flashes) {
            float x = (float) (f.x - camX);
            float y = (float) (f.y - camY);
            float z = (float) (f.z - camZ);
            // Where it is on this screen, for what is drawn over the screen later in the frame.
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            if (GLU.gluProject(x, y, z, modelview, projection, viewport, projected) && projected.get(2) < 1
                && viewport.get(2) > 0) {
                f.screenX = Draw.clamp01((projected.get(0) - viewport.get(0)) / viewport.get(2));
                f.screenY = Draw.clamp01(1 - (projected.get(1) - viewport.get(1)) / viewport.get(3));
            } else {
                f.screenX = 0.5F;
                f.screenY = 0.5F;
            }
            world(t, mc, f, x, y, z, camX, camY, camZ, partialTicks);
        }
    }

    private static void world(Tessellator t, Minecraft mc, Flash f, float x, float y, float z, double camX, double camY,
        double camZ, float partialTicks) {
        float ms = f.ms();
        if (ms > ENDS || f.framesOnly) {
            return;
        }
        // Right at the eye its light is the screen's own; here it is dimmed, not to burn it out twice.
        float near = 0.3F + 0.7F * Draw.clamp01(((float) Math.sqrt(x * x + y * y + z * z) - 1.5F) / 5);
        float burst = Draw.clamp01(1 - (ms - FRAMES) / (BURST - FRAMES));
        float all = Draw.clamp01(1 - ms / ENDS);
        float grown = Draw.clamp01(ms / 220);
        Draw.additive();
        t.startDrawing(GL11.GL_TRIANGLES);
        Draw.glow(t, x, y, z, 7.5F * (0.4F + 0.6F * grown), GLOW, 0.75F * all * near);
        Draw.glow(t, x, y, z, 3.4F, 0xC00010, 0.7F * all * near);
        if (ms < BURST) {
            Draw.glow(t, x, y, z, 4.2F * (0.5F + 0.5F * grown), 0xFFFFFF, 0.95F * burst * near);
        }
        // The two it is between, burnt out by it.
        for (int id : new int[] { f.caster, f.victim }) {
            Entity body = mc.theWorld.getEntityByID(id);
            if (body != null && ms < SPARKS && !(body == mc.thePlayer && mc.gameSettings.thirdPersonView == 0)) {
                float left = Draw.clamp01(1 - (ms - FRAMES) / (SPARKS - FRAMES));
                float bx = (float) (body.lastTickPosX + (body.posX - body.lastTickPosX) * partialTicks - camX);
                float by = (float) (body.boundingBox.minY + body.height * 0.55F - camY);
                float bz = (float) (body.lastTickPosZ + (body.posZ - body.lastTickPosZ) * partialTicks - camZ);
                Draw.glow(t, bx, by, bz, 1.5F, HOT, 0.85F * left);
                Draw.glow(t, bx, by + 0.45F, bz, 0.8F, 0xFFFFFF, 0.7F * left);
            }
        }
        t.draw();

        float[] d = { f.dx, f.dy, f.dz };
        float[] side = AbilityFx.cross(d, new float[] { 0, 1, 0 });
        if (AbilityFx.length(side) < 1e-3F) {
            side = new float[] { 1, 0, 0 };
        }
        AbilityFx.normalize(side);
        float[] up = AbilityFx.cross(side, d);
        AbilityFx.normalize(up);

        // The ring of the blow, across its way, and a second one after it.
        t.startDrawingQuads();
        for (int ring = 0; ring < 2; ring++) {
            float since = ms - FRAMES - ring * 140;
            float left = 1 - since / 900;
            if (since < 0 || left <= 0) {
                continue;
            }
            float wide = 0.5F + 6.5F * (float) Math.pow(since / 900, 0.55);
            for (int k = 0; k < 32; k++) {
                float a0 = k * 6.2832F / 32;
                float a1 = (k + 1) * 6.2832F / 32;
                AbilityFx.ribbon(
                    t,
                    x + (side[0] * MathHelper.cos(a0) + up[0] * MathHelper.sin(a0)) * wide,
                    y + (side[1] * MathHelper.cos(a0) + up[1] * MathHelper.sin(a0)) * wide,
                    z + (side[2] * MathHelper.cos(a0) + up[2] * MathHelper.sin(a0)) * wide,
                    x + (side[0] * MathHelper.cos(a1) + up[0] * MathHelper.sin(a1)) * wide,
                    y + (side[1] * MathHelper.cos(a1) + up[1] * MathHelper.sin(a1)) * wide,
                    z + (side[2] * MathHelper.cos(a1) + up[2] * MathHelper.sin(a1)) * wide,
                    0.2F * left,
                    0.2F * left,
                    GLOW,
                    left,
                    GLOW,
                    left);
            }
        }
        // The rays: thick and red, straight out of the blow, for the first instants of the burst.
        float rays = 1 - (ms - FRAMES) / 260;
        if (ms >= FRAMES && rays > 0) {
            Random random = new Random(f.seed + 3);
            for (int i = 0; i < 16; i++) {
                float rx = random.nextFloat() * 2 - 1 + d[0] * 0.5F;
                float ry = random.nextFloat() * 1.6F - 0.5F;
                float rz = random.nextFloat() * 2 - 1 + d[2] * 0.5F;
                float n = (float) Math.sqrt(rx * rx + ry * ry + rz * rz) + 1e-4F;
                float reach = (5 + 6 * random.nextFloat()) * (0.4F + 0.6F * (1 - rays)) / n;
                AbilityFx.ribbon(
                    t,
                    x,
                    y,
                    z,
                    x + rx * reach,
                    y + ry * reach,
                    z + rz * reach,
                    0.08F,
                    0.55F + 0.5F * random.nextFloat(),
                    GLOW,
                    rays,
                    GLOW,
                    0.85F * rays);
            }
        }
        t.draw();

        if (ms < FRAMES) {
            return;
        }
        // The tangle: strokes that wind about the place, a new set every instant. Red and many in the burst;
        // black with a red edge, and fewer and fewer, after it.
        boolean bursting = ms < BURST;
        int strokes = bursting ? 54 : ms < SPARKS ? 22 : 9;
        float reach = bursting ? 2.2F + 3.8F * (ms - FRAMES) / (BURST - FRAMES) : 6.5F;
        float fade = bursting ? 1 : ms < SPARKS ? 1 : Draw.clamp01((ENDS - ms) / (ENDS - SPARKS) * 1.6F);
        long jump = (long) (ms / JUMPS);
        float[][][] lines = new float[strokes][POINTS][3];
        boolean[] black = new boolean[strokes];
        float[] width = new float[strokes];
        for (int i = 0; i < strokes; i++) {
            Random random = new Random(mix(f.seed + i * 104729L + jump * 31L));
            black[i] = !bursting || random.nextInt(4) == 0;
            width[i] = 0.05F + 0.11F * random.nextFloat();
            float px = x + (random.nextFloat() * 2 - 1) * reach * 0.75F;
            float py = y + (random.nextFloat() * 1.5F - 0.45F) * reach * 0.6F;
            float pz = z + (random.nextFloat() * 2 - 1) * reach * 0.75F;
            float hx = random.nextFloat() * 2 - 1;
            float hy = random.nextFloat() * 2 - 1;
            float hz = random.nextFloat() * 2 - 1;
            // What turns it, itself turning slowly: a line that winds, with a hook in it now and then.
            float tx = 0;
            float ty = 0;
            float tz = 0;
            float step = 0.2F + 0.26F * random.nextFloat();
            for (int k = 0; k < POINTS; k++) {
                lines[i][k][0] = px;
                lines[i][k][1] = py;
                lines[i][k][2] = pz;
                float hook = random.nextInt(9) == 0 ? 1.6F : 0.32F;
                tx += ((random.nextFloat() * 2 - 1) * hook - tx) * 0.45F;
                ty += ((random.nextFloat() * 2 - 1) * hook - ty) * 0.45F;
                tz += ((random.nextFloat() * 2 - 1) * hook - tz) * 0.45F;
                hx += tx;
                hy += ty;
                hz += tz;
                float n = (float) Math.sqrt(hx * hx + hy * hy + hz * hz) + 1e-4F;
                hx /= n;
                hy /= n;
                hz /= n;
                px += hx * step;
                py += hy * step;
                pz += hz * step;
            }
        }
        // The red of each first, as light; then its body over that, red or black.
        Draw.translucent();
        t.startDrawingQuads();
        for (int i = 0; i < strokes; i++) {
            stroke(t, lines[i], width[i] * 2.4F, 0xFF1414, 0.55F * fade * near);
        }
        t.draw();
        t.startDrawingQuads();
        for (int i = 0; i < strokes; i++) {
            stroke(t, lines[i], width[i], black[i] ? 0x000000 : RED, Math.min(1, fade * 1.4F));
        }
        t.draw();

        // The beads: red, round, rising from about the place and shrinking as they go.
        if (ms > 420) {
            Random random = new Random(f.seed + 11);
            for (int i = 0; i < 16; i++) {
                float born = 420 + random.nextFloat() * 500;
                float life = 700 + random.nextFloat() * 650;
                float bx = (random.nextFloat() * 2 - 1) * 4.5F;
                float by = random.nextFloat() * 2.4F - 0.6F;
                float bz = (random.nextFloat() * 2 - 1) * 4.5F;
                float size = 0.1F + 0.2F * random.nextFloat();
                float age = (ms - born) / life;
                if (age <= 0 || age >= 1) {
                    continue;
                }
                float px = x + bx;
                float py = y + by + age * (1.4F + size * 4);
                float pz = z + bz;
                float shown = Math.min(1, age * 6) * (1 - age * age);
                Draw.additive();
                t.startDrawing(GL11.GL_TRIANGLES);
                Draw.glow(t, px, py, pz, size * 3.2F * shown, GLOW, 0.6F);
                t.draw();
                AbilityFx.sphere(t, px, py, pz, size * shown, ms / 40F, 1, BEAD, 0xFF2A1A);
            }
        }
        Draw.additive();
    }

    private static final int[] BEAD = { 0x8A0008, 0xD00A12, 0xF01818, 0xFF3A2A, 0xFF7A60 };

    private static final int POINTS = 18;

    /**
     * A number scrambled into another: the game's random numbers begin alike for seeds that are near each other,
     * and a tangle that should be a new one every instant would only tremble.
     */
    private static long mix(long seed) {
        seed = (seed ^ seed >>> 30) * 0xBF58476D1CE4E5B9L;
        seed = (seed ^ seed >>> 27) * 0x94D049BB133111EBL;
        return seed ^ seed >>> 31;
    }

    /** A stroke through its points, as wide as given in its middle and nothing at its ends. */
    private static void stroke(Tessellator t, float[][] line, float width, int color, float alpha) {
        int last = line.length - 1;
        float beforeX = 0;
        float beforeY = 0;
        float beforeZ = 0;
        boolean before = false;
        for (int k = 0; k <= last; k++) {
            float[] p = line[k];
            float[] from = line[Math.max(0, k - 1)];
            float[] to = line[Math.min(last, k + 1)];
            // Across the stroke here, facing the eye: across the line through its neighbours and the line of sight.
            float dx = to[0] - from[0];
            float dy = to[1] - from[1];
            float dz = to[2] - from[2];
            float wx = dy * p[2] - dz * p[1];
            float wy = dz * p[0] - dx * p[2];
            float wz = dx * p[1] - dy * p[0];
            float n = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
            float distance = (float) Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
            // A stroke that comes right up to the eye would be a bar across the whole screen: it thins out there.
            float half = (width * (float) Math.pow(Math.sin(Math.PI * k / last), 0.6) + 0.004F)
                * Draw.clamp01((distance - 0.5F) / 2.8F);
            if (n < 1e-5F || half <= 0) {
                before = false;
                continue;
            }
            wx *= half / n;
            wy *= half / n;
            wz *= half / n;
            if (before) {
                float[] q = line[k - 1];
                Draw.color(t, color, alpha);
                t.addVertex(q[0] - beforeX, q[1] - beforeY, q[2] - beforeZ);
                t.addVertex(q[0] + beforeX, q[1] + beforeY, q[2] + beforeZ);
                t.addVertex(p[0] + wx, p[1] + wy, p[2] + wz);
                t.addVertex(p[0] - wx, p[1] - wy, p[2] - wz);
            }
            beforeX = wx;
            beforeY = wy;
            beforeZ = wz;
            before = true;
        }
    }

    // On the screen.

    /** Before the game draws its own things over the world: they stay as they are, on top of this. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (flashes.isEmpty() || event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Flash f = null;
        for (Flash other : flashes) {
            if (other.strength > 0.04F && (f == null || other.began > f.began)) {
                f = other;
            }
        }
        if (f == null || f.ms() > ENDS) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int w = event.resolution.getScaledWidth();
        int h = event.resolution.getScaledHeight();
        // The game sets the screen's own projection only after this point of the frame: it is set here, as it will
        // be again.
        mc.entityRenderer.setupOverlayRendering();
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        float ms = f.ms();
        if (ms < FRAMES && f.strength > 0.45F) {
            frames(mc, w, h, ms);
        } else if (!f.framesOnly) {
            screen(f, w, h, ms);
        }
        GL11.glDepthMask(true);
        GL11.glPopAttrib();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
    }

    /**
     * The frames of the impact: the picture on the screen in two tones. First red where it was light and black
     * where it was dark; then white and black; then that turned inside out; then back.
     */
    private void frames(Minecraft mc, int w, int h, float ms) {
        if (!GRAB) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            rect(w, h, ms < RED_FRAME ? RED : ms >= FRAME_A && ms < FRAME_B ? 0xFFFFFF : 0x000000, 1);
            return;
        }
        // The white frame is white nearly all over, with only what was darkest left black; the red one and the
        // negatives keep more of the picture: the ground, the bodies, the shadows.
        boolean white = ms >= FRAME_A && ms < FRAME_B;
        twoTones(mc, w, h, white ? 0x686868 : 0x3C3C3C);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        if (ms < RED_FRAME) {
            // What is white becomes red: each colour times red.
            GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ZERO);
            rect(w, h, 0xFF0000, 1);
        } else if (ms < FRAME_A || ms >= FRAME_B) {
            // Inside out: one less each colour.
            GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);
            rect(w, h, 0xFFFFFF, 1);
        }
    }

    /**
     * Takes the picture off the screen and puts it back in black and white, with nothing in between. The picture is
     * copied into a texture; drawn back as its own brightness (a dot product in the texture stage, which is also a
     * first hardening: mid grey and under go to black); and hardened to the end by squaring and doubling it a few
     * times over, which leaves only what is under a half, to black, and what is over, to white.
     */
    private void twoTones(Minecraft mc, int w, int h, int lift) {
        int width = mc.displayWidth;
        int height = mc.displayHeight;
        // The picture is lifted first, by as much as is asked: the more, the more of it comes out white.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        rect(w, h, lift, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (texture < 0 || textureWidth != width || textureHeight != height) {
            if (texture >= 0) {
                GL11.glDeleteTextures(texture);
            }
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGB,
                width,
                height,
                0,
                GL11.GL_RGB,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
            textureWidth = width;
            textureHeight = height;
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL13.GL_DOT3_RGB);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_RGB, GL13.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_RGB, GL11.GL_SRC_COLOR);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        // The weights of red, green and blue in what the eye calls bright, as the dot product wants them: a half
        // and that much more.
        t.setColorRGBA_F(0.5F + 0.3F, 0.5F + 0.5F, 0.5F + 0.2F, 1);
        t.addVertexWithUV(0, h, 0, 0, 0);
        t.addVertexWithUV(w, h, 0, 1, 0);
        t.addVertexWithUV(w, 0, 0, 1, 1);
        t.addVertexWithUV(0, 0, 0, 0, 1);
        t.draw();
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        for (int round = 0; round < 3; round++) {
            GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_DST_COLOR);
            rect(w, h, 0xFFFFFF, 1);
            GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE);
            rect(w, h, 0xFFFFFF, 1);
        }
    }

    /** After the frames: the white of the burst, its rays, the tangle over the whole screen, and the red left. */
    private static void screen(Flash f, int w, int h, float ms) {
        float s = f.strength;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        float since = Math.max(0, ms - FRAMES);
        float burst = Draw.clamp01(1 - since / (BURST - FRAMES));
        // Burnt white, going; and red under it, going more slowly.
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        rect(w, h, 0xC00010, 0.34F * s * Draw.clamp01(1 - since / 1100));
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        rect(w, h, 0xFFFFFF, 0.9F * s * burst);
        float cx = f.screenX * w;
        float cy = f.screenY * h;
        float diagonal = (float) Math.sqrt(w * w + h * h);
        Tessellator t = Tessellator.instance;
        // The rays, from where the blow landed to past the edges of the screen.
        float rays = 1 - since / 240;
        if (rays > 0) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Random random = new Random(f.seed + 5);
            t.startDrawing(GL11.GL_TRIANGLES);
            for (int i = 0; i < 15; i++) {
                float a = random.nextFloat() * 6.2832F;
                float half = (0.03F + 0.09F * random.nextFloat()) * (0.5F + 0.5F * rays);
                t.setColorRGBA_I(RED, (int) (240 * rays * s));
                t.addVertex(cx, cy, 0);
                t.addVertex(cx + MathHelper.cos(a - half) * diagonal, cy + MathHelper.sin(a - half) * diagonal, 0);
                t.addVertex(cx + MathHelper.cos(a + half) * diagonal, cy + MathHelper.sin(a + half) * diagonal, 0);
            }
            t.draw();
        }
        // The tangle.
        boolean bursting = ms < BURST;
        int strokes = (int) ((bursting ? 18 + 30 * Math.min(1, since / 160) : ms < SPARKS ? 24 : 9) * s);
        float fade = ms < SPARKS ? 1 : Draw.clamp01((ENDS - ms) / (ENDS - SPARKS) * 1.5F);
        long jump = (long) (ms / JUMPS);
        float[][][] lines = new float[strokes][22][2];
        boolean[] black = new boolean[strokes];
        float[] width = new float[strokes];
        for (int i = 0; i < strokes; i++) {
            Random random = new Random(mix(f.seed + i * 7919L + jump * 131L));
            black[i] = !bursting || random.nextInt(5) == 0;
            width[i] = diagonal * (0.003F + 0.0075F * random.nextFloat()) * (bursting ? 1.15F : 1);
            // Anywhere on the screen, and more of them about the blow.
            float px = random.nextBoolean() ? cx + (random.nextFloat() - 0.5F) * w * 0.7F : random.nextFloat() * w;
            float py = random.nextBoolean() ? cy + (random.nextFloat() - 0.5F) * h * 0.7F : random.nextFloat() * h;
            float heading = random.nextFloat() * 6.2832F;
            float bend = 0;
            float step = diagonal * (0.014F + 0.022F * random.nextFloat());
            for (int k = 0; k < 22; k++) {
                lines[i][k][0] = px;
                lines[i][k][1] = py;
                // It winds: the bend itself drifts, and now and then it hooks.
                bend += ((random.nextFloat() * 2 - 1) * 0.75F - bend) * 0.4F;
                heading += bend + (random.nextInt(11) == 0 ? (random.nextFloat() * 2 - 1) * 2.2F : 0);
                px += MathHelper.cos(heading) * step;
                py += MathHelper.sin(heading) * step;
            }
        }
        // The red about each first; then its body over that. Laid over the picture, not added to it: added, red
        // on grass would be orange.
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        t.startDrawingQuads();
        for (int i = 0; i < strokes; i++) {
            flat(t, lines[i], width[i] * 2.3F, 0xFF1414, 0.55F * fade * s);
        }
        t.draw();
        t.startDrawingQuads();
        for (int i = 0; i < strokes; i++) {
            flat(t, lines[i], width[i], black[i] ? 0x000000 : RED, Math.min(1, fade * 1.5F) * Math.min(1, s * 1.3F));
        }
        t.draw();
    }

    /** A stroke on the screen through its points: as wide as given in its middle, nothing at its ends. */
    private static void flat(Tessellator t, float[][] line, float width, int color, float alpha) {
        int last = line.length - 1;
        int a = (int) (Draw.clamp01(alpha) * 255);
        if (a < 3) {
            return;
        }
        float beforeX = 0;
        float beforeY = 0;
        for (int k = 0; k <= last; k++) {
            // Across the stroke at this point: across the line through its neighbours.
            float[] p = line[k];
            float[] from = line[Math.max(0, k - 1)];
            float[] to = line[Math.min(last, k + 1)];
            float dx = to[0] - from[0];
            float dy = to[1] - from[1];
            float n = (float) Math.sqrt(dx * dx + dy * dy) + 1e-4F;
            float half = width * (float) Math.pow(Math.sin(Math.PI * k / last), 0.6) + 0.3F;
            float nx = -dy / n * half;
            float ny = dx / n * half;
            if (k > 0) {
                float[] q = line[k - 1];
                t.setColorRGBA_I(color, a);
                t.addVertex(q[0] - beforeX, q[1] - beforeY, 0);
                t.addVertex(q[0] + beforeX, q[1] + beforeY, 0);
                t.addVertex(p[0] + nx, p[1] + ny, 0);
                t.addVertex(p[0] - nx, p[1] - ny, 0);
            }
            beforeX = nx;
            beforeY = ny;
        }
    }

    private static void rect(int w, int h, int rgb, float alpha) {
        if (alpha <= 0.004F) {
            return;
        }
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_I(rgb, (int) (Draw.clamp01(alpha) * 255));
        t.addVertex(0, h, 0);
        t.addVertex(w, h, 0);
        t.addVertex(w, 0, 0);
        t.addVertex(0, 0, 0);
        t.draw();
    }
}
