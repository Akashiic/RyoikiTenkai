package com.akashic.ryoiki.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.CastPose;
import com.akashic.ryoiki.mode.AbilityMath;
import com.akashic.ryoiki.mode.ModeWire;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * What the techniques of the sorcerer mode look like, in the world: the server says what happened and where
 * ({@link ModeWire#EFFECT}), and this draws it for as long as it lasts. All of it is drawn by code, in the manner
 * of each domain: the star void's light and needles, the crimson shrine's blade and scars.
 */
public final class AbilityFx {

    public static final AbilityFx INSTANCE = new AbilityFx();

    private static final int ICE = 0x9AD4FF;
    private static final int BLUE = 0x3F6BFF;
    private static final int VIOLET = 0xB79BFF;
    private static final int WHITE = 0xFFFFFF;
    private static final int RED = 0xFF2A1A;
    private static final int EMBER = 0xFF6A38;

    /** Ticks a pull goes on being drawn after it is over: the point closes. */
    private static final int PULL_CLOSES = 7;
    private static final int NEEDLES = 40;
    private static final float NEEDLE_PERIOD = 9;
    /** The flying cut: how big its crescent is, how far it leans, and how long what it leaves stays. */
    private static final float BLADE_RADIUS = 1.05F;
    private static final float BLADE_SPAN = 0.95F;
    private static final float BLADE_LEAN = 0.6F;
    private static final int CUT_ENDS = 7;
    private static final int SCAR_STAYS = 50;
    private static final float SCAR_STEP = 0.5F;

    private final List<Pull> pulls = new ArrayList<>();
    private final List<Cut> cuts = new ArrayList<>();
    /** Who is making the gesture of a technique: its kind and how long ago it began, by entity. */
    private final Map<Integer, int[]> gestures = new HashMap<>();

    private static final class Pull {

        double x;
        double y;
        double z;
        float radius;
        int ticks;
        int age;
        final float[] needles = new float[NEEDLES * 4];
    }

    private static final class Cut {

        int caster;
        double x;
        double y;
        double z;
        float dx;
        float dy;
        float dz;
        float range;
        /** Where it met someone, along its way; negative while it has not. */
        float met = -1;
        int age;
        /** The tick it stopped at, by meeting someone or by the end of its way; negative while it flies. */
        int ended = -1;
        /** The ground under its way, a height for each step; NaN where there is none near. */
        float[] ground;
    }

    private AbilityFx() {}

    void clear() {
        pulls.clear();
        cuts.clear();
        gestures.clear();
    }

    void heard(ModeWire.Message m) {
        Minecraft mc = Minecraft.getMinecraft();
        if (m.kind == AbilityMath.PULL) {
            Pull p = new Pull();
            p.x = m.x;
            p.y = m.y;
            p.z = m.z;
            p.radius = m.size;
            p.ticks = m.ticks;
            Random random = new Random(Double.doubleToLongBits(m.x * 31 + m.z) ^ m.entity);
            for (int i = 0; i < NEEDLES; i++) {
                // A direction, evenly over the sphere; where in its run the needle starts; and how long it is.
                double up = random.nextDouble() * 2 - 1;
                double around = random.nextDouble() * Math.PI * 2;
                double flat = Math.sqrt(1 - up * up);
                p.needles[i * 4] = (float) (Math.cos(around) * flat);
                p.needles[i * 4 + 1] = (float) up;
                p.needles[i * 4 + 2] = (float) (Math.sin(around) * flat);
                p.needles[i * 4 + 3] = random.nextFloat();
            }
            pulls.add(p);
            gesture(mc, m.entity, CastPose.PULL);
        } else if (m.kind == AbilityMath.CUT) {
            Cut c = new Cut();
            c.caster = m.entity;
            c.x = m.x;
            c.y = m.y;
            c.z = m.z;
            c.dx = m.dx;
            c.dy = m.dy;
            c.dz = m.dz;
            c.range = m.size;
            cuts.add(c);
            gesture(mc, m.entity, CastPose.CUT);
        } else if (m.kind == AbilityMath.CUT_HIT) {
            for (int i = cuts.size() - 1; i >= 0; i--) {
                Cut c = cuts.get(i);
                if (c.caster == m.entity && c.met < 0) {
                    c.met = m.size;
                    c.ended = c.age;
                    break;
                }
            }
        }
    }

    private void gesture(Minecraft mc, int entity, int kind) {
        gestures.put(entity, new int[] { kind, 0 });
        if (mc.thePlayer != null && mc.thePlayer.getEntityId() == entity) {
            // In its own eyes: the arm goes out.
            mc.thePlayer.swingItem();
        }
    }

    /**
     * The gesture a player is making, for its model: writes the degrees of {@link CastPose#BONES} and returns how
     * much of it shows, 0 for none.
     */
    float gesture(int entity, float partialTicks, float[][] pose) {
        int[] g = gestures.get(entity);
        if (g == null) {
            return 0;
        }
        float age = g[1] + partialTicks;
        pose[0] = CastPose.sample(g[0], age);
        return CastPose.layer(g[0], age);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getMinecraft().theWorld == null) {
            clear();
            return;
        }
        for (Iterator<Pull> i = pulls.iterator(); i.hasNext();) {
            Pull p = i.next();
            if (++p.age > p.ticks + PULL_CLOSES) {
                i.remove();
            }
        }
        for (Iterator<Cut> i = cuts.iterator(); i.hasNext();) {
            Cut c = i.next();
            c.age++;
            if (c.ended < 0 && AbilityMath.cutGone(c.age, c.range) >= c.range) {
                c.ended = c.age;
            }
            if (c.ended >= 0 && c.age > c.ended + SCAR_STAYS) {
                i.remove();
            }
        }
        for (Iterator<int[]> i = gestures.values()
            .iterator(); i.hasNext();) {
            int[] g = i.next();
            if (++g[1] > CastPose.ticks(g[0])) {
                i.remove();
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (pulls.isEmpty() && cuts.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        double camX = RenderManager.renderPosX;
        double camY = RenderManager.renderPosY;
        double camZ = RenderManager.renderPosZ;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glPushMatrix();
        // Everything below is in blocks from the camera: the world's own numbers are too large for the light.
        Draw.alpha = 1;
        Draw.fade = 1;
        Draw.camera(0);
        Tessellator t = Tessellator.instance;
        for (Pull p : pulls) {
            pull(t, p, (float) (p.x - camX), (float) (p.y - camY), (float) (p.z - camZ), event.partialTicks);
        }
        for (Cut c : cuts) {
            if (c.ground == null) {
                c.ground = ground(mc.theWorld, c);
            }
            cut(t, c, (float) (c.x - camX), (float) (c.y - camY), (float) (c.z - camZ), event.partialTicks);
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glDepthMask(true);
    }

    // Atração.

    private static void pull(Tessellator t, Pull p, float x, float y, float z, float partialTicks) {
        float age = p.age + partialTicks;
        float open = Draw.clamp01(age / 3);
        // Over: the point closes on itself, and a ring goes out from where it was.
        float closing = Draw.clamp01((age - p.ticks) / PULL_CLOSES);
        float alive = open * (1 - closing);
        float beat = 0.5F + 0.5F * MathHelper.sin(age * 0.9F);

        // The needles: light drawn in from all around, each on its own line, faster as it nears the point.
        Draw.additive();
        t.startDrawingQuads();
        for (int i = 0; i < NEEDLES; i++) {
            float run = (age / NEEDLE_PERIOD + p.needles[i * 4 + 3]) % 1;
            if (age - run * NEEDLE_PERIOD < 0 || closing > 0 && run < closing) {
                // Not sent yet; or sent after the point began to close.
                continue;
            }
            float head = p.radius * (1 - run) * (1 - run);
            float tail = Math.min(p.radius, head + 0.5F + 1.5F * (1 - run));
            float dx = p.needles[i * 4];
            float dy = p.needles[i * 4 + 1];
            float dz = p.needles[i * 4 + 2];
            int color = i % 3 == 0 ? WHITE : i % 3 == 1 ? ICE : VIOLET;
            float light = alive * Draw.clamp01(run * 5) * (0.45F + 0.55F * run);
            ribbon(
                t,
                x + dx * tail,
                y + dy * tail,
                z + dz * tail,
                x + dx * head,
                y + dy * head,
                z + dz * head,
                0.012F,
                0.05F,
                color,
                0,
                color,
                light);
        }
        t.draw();

        // Two rings, flat, closing on the point one after the other.
        for (int ring = 0; ring < 2; ring++) {
            float run = (age / 11F + ring * 0.5F) % 1;
            float radius = p.radius * (1 - run * run);
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, z);
            Draw.ring(
                Math.max(0, radius - 0.1F - 0.25F * run),
                radius,
                40,
                ICE,
                0,
                Draw.mix(ICE, BLUE, run),
                0.5F * alive * Draw.clamp01(run * 4));
            GL11.glPopMatrix();
        }

        // The point: a blue light with a white heart, and inside it a hole darker than anything around.
        float size = (0.5F + 0.07F * beat) * (1 - closing * closing);
        t.startDrawing(GL11.GL_TRIANGLES);
        Draw.glow(t, x, y, z, size * 4.2F, BLUE, 0.42F * alive);
        Draw.glow(t, x, y, z, size * 2.0F, ICE, 0.75F * alive);
        Draw.glow(t, x, y, z, size * 0.95F, WHITE, alive);
        t.draw();
        Draw.translucent();
        t.startDrawing(GL11.GL_TRIANGLES);
        Draw.glow(t, x, y, z, size * 0.6F, 0x000008, 0.95F * alive);
        t.draw();

        if (closing > 0) {
            // What is left when it closes: a ring of light going out, thin and quick.
            Draw.additive();
            float out = closing * (2 - closing);
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, z);
            Draw.ring(1.6F * out, 1.6F * out + 0.35F * (1 - closing), 40, WHITE, (1 - closing) * 0.8F, ICE, 0);
            GL11.glPopMatrix();
            t.startDrawing(GL11.GL_TRIANGLES);
            Draw.glow(t, x, y, z, 1.4F * (1 - closing), WHITE, (1 - closing) * 0.9F);
            t.draw();
        }
    }

    // Talho.

    /** The height of the ground under each step of a cut's way, as far down as a blade's scar would still show. */
    private static float[] ground(World world, Cut c) {
        int steps = (int) (c.range / SCAR_STEP) + 1;
        float[] out = new float[steps];
        for (int i = 0; i < steps; i++) {
            double x = c.x + c.dx * i * SCAR_STEP;
            double y = c.y + c.dy * i * SCAR_STEP;
            double z = c.z + c.dz * i * SCAR_STEP;
            out[i] = Float.NaN;
            int bx = MathHelper.floor_double(x);
            int bz = MathHelper.floor_double(z);
            for (int by = MathHelper.floor_double(y); by >= MathHelper.floor_double(y) - 3; by--) {
                if (world.getBlock(bx, by, bz)
                    .getMaterial()
                    .blocksMovement()) {
                    out[i] = by + 1.02F;
                    break;
                }
            }
        }
        return out;
    }

    private static void cut(Tessellator t, Cut c, float x, float y, float z, float partialTicks) {
        float age = c.age + partialTicks;
        float end = c.met >= 0 ? c.met : c.range;
        float gone = Math.min(end, (float) AbilityMath.cutGone(age, c.range));
        boolean flying = c.ended < 0;
        float since = flying ? 0 : age - c.ended;

        // The blade lies in the plane of its way and of an axis that leans off the vertical: a diagonal stroke.
        float[] d = { c.dx, c.dy, c.dz };
        float[] side = cross(d, new float[] { 0, 1, 0 });
        if (length(side) < 1e-3F) {
            side = new float[] { 1, 0, 0 };
        }
        normalize(side);
        float[] up = cross(side, d);
        normalize(up);
        float cos = MathHelper.cos(BLADE_LEAN);
        float sin = MathHelper.sin(BLADE_LEAN);
        float[] axis = { up[0] * cos + side[0] * sin, up[1] * cos + side[1] * sin, up[2] * cos + side[2] * sin };

        // The scar on the ground, under where the blade has been: it glows and cools.
        Draw.additive();
        t.startDrawingQuads();
        for (int i = 0; i + 1 < c.ground.length && (i + 1) * SCAR_STEP <= gone; i++) {
            if (Float.isNaN(c.ground[i]) || Float.isNaN(c.ground[i + 1])
                || Math.abs(c.ground[i] - c.ground[i + 1]) > 0.6F) {
                continue;
            }
            // When the blade passed over this step, and how long ago that is.
            float passed = age - (i + 0.5F) * SCAR_STEP / (float) AbilityMath.CUT_SPEED;
            float warm = Draw.clamp01(1 - passed / SCAR_STAYS);
            if (warm <= 0) {
                continue;
            }
            float ax = x + c.dx * i * SCAR_STEP;
            float az = z + c.dz * i * SCAR_STEP;
            float bx = x + c.dx * (i + 1) * SCAR_STEP;
            float bz = z + c.dz * (i + 1) * SCAR_STEP;
            float ay = c.ground[i] - (float) RenderManager.renderPosY;
            float by = c.ground[i + 1] - (float) RenderManager.renderPosY;
            float fresh = Draw.clamp01(1 - passed / 6);
            flat(t, ax, ay, az, bx, by, bz, side, 0.2F, RED, 0.16F * warm * warm);
            flat(t, ax, ay, az, bx, by, bz, side, 0.045F, Draw.mix(EMBER, WHITE, fresh), 0.85F * warm);
        }
        t.draw();

        if (flying || since < 2) {
            float left = flying ? 1 : 1 - since / 2;
            // One that reaches the end of its way without meeting anyone thins out over its last blocks.
            float thin = c.met >= 0 ? 1 : Draw.clamp01((c.range - gone) / 2.5F + 0.15F);
            for (int ghost = 3; ghost >= 0; ghost--) {
                float at = gone - ghost * 0.85F;
                if (at < 0) {
                    continue;
                }
                float strength = left * thin * (float) Math.pow(0.5, ghost);
                // One that passes right by the eye is not a white screen.
                float px = x + c.dx * at;
                float py = y + c.dy * at;
                float pz = z + c.dz * at;
                strength *= 0.2F + 0.8F * Draw.clamp01(((float) Math.sqrt(px * px + py * py + pz * pz) - 1) / 2.5F);
                blade(t, px, py, pz, d, axis, strength, ghost == 0);
            }
        }

        if (c.met >= 0 && since < CUT_ENDS) {
            // Where it met someone: a flash, and three short strokes across each other.
            float hx = x + c.dx * c.met;
            float hy = y + c.dy * c.met;
            float hz = z + c.dz * c.met;
            float burst = 1 - since / CUT_ENDS;
            Draw.additive();
            t.startDrawing(GL11.GL_TRIANGLES);
            Draw.glow(t, hx, hy, hz, 2.4F * (0.4F + 0.6F * (1 - burst)), RED, 0.6F * burst);
            Draw.glow(t, hx, hy, hz, 0.9F, WHITE, burst * burst);
            t.draw();
            float reach = 0.5F + 1.3F * (1 - burst * burst);
            for (int stroke = 0; stroke < 3; stroke++) {
                float turn = BLADE_LEAN + 0.5F + stroke * 2.1F;
                float ca = MathHelper.cos(turn);
                float sa = MathHelper.sin(turn);
                float sx = (up[0] * ca + side[0] * sa) * reach;
                float sy = (up[1] * ca + side[1] * sa) * reach;
                float sz = (up[2] * ca + side[2] * sa) * reach;
                Draw.translucent();
                t.startDrawingQuads();
                ribbon(
                    t,
                    hx - sx,
                    hy - sy,
                    hz - sz,
                    hx + sx,
                    hy + sy,
                    hz + sz,
                    0.05F,
                    0.05F,
                    0x000000,
                    burst,
                    0,
                    burst);
                t.draw();
                Draw.additive();
                t.startDrawingQuads();
                ribbon(t, hx - sx, hy - sy, hz - sz, hx, hy, hz, 0.02F, 0.12F, WHITE, 0, WHITE, 0.8F * burst);
                ribbon(t, hx, hy, hz, hx + sx, hy + sy, hz + sz, 0.12F, 0.02F, WHITE, 0.8F * burst, WHITE, 0);
                t.draw();
            }
        }
    }

    /** The crescent at a place: its shadow, its red and its white edge, thick in the middle and nothing at the tips. */
    private static void blade(Tessellator t, float x, float y, float z, float[] d, float[] axis, float strength,
        boolean whole) {
        if (strength <= 0.01F) {
            return;
        }
        int steps = 14;
        float[][] outer = new float[steps + 1][3];
        float[][] inner = new float[steps + 1][3];
        float[][] dark = new float[steps + 1][3];
        float[] thick = new float[steps + 1];
        for (int i = 0; i <= steps; i++) {
            float along = (float) i / steps;
            float angle = (along * 2 - 1) * BLADE_SPAN;
            thick[i] = 0.26F * (float) Math.pow(Math.cos(angle / BLADE_SPAN * Math.PI / 2), 0.7);
            float ca = MathHelper.cos(angle);
            float sa = MathHelper.sin(angle);
            for (int k = 0; k < 3; k++) {
                // The middle of the arc leads; its centre is behind, on the blade's way.
                float base = (k == 0 ? x : k == 1 ? y : z) - d[k] * BLADE_RADIUS;
                float dir = ca * d[k] + sa * axis[k];
                outer[i][k] = base + dir * BLADE_RADIUS;
                inner[i][k] = base + dir * (BLADE_RADIUS - thick[i]);
                dark[i][k] = base + dir * (BLADE_RADIUS - thick[i] * 1.9F);
            }
        }
        if (whole) {
            // The shadow behind the edge: what the blade is, when there is no light on it.
            Draw.translucent();
            t.startDrawingQuads();
            for (int i = 0; i < steps; i++) {
                Draw.color(t, 0x000000, 0.8F * strength);
                t.addVertex(outer[i][0], outer[i][1], outer[i][2]);
                t.addVertex(outer[i + 1][0], outer[i + 1][1], outer[i + 1][2]);
                Draw.color(t, 0x0A0000, 0);
                t.addVertex(dark[i + 1][0], dark[i + 1][1], dark[i + 1][2]);
                t.addVertex(dark[i][0], dark[i][1], dark[i][2]);
            }
            t.draw();
        }
        Draw.additive();
        t.startDrawingQuads();
        for (int i = 0; i < steps; i++) {
            Draw.color(t, RED, 0.75F * strength);
            t.addVertex(outer[i][0], outer[i][1], outer[i][2]);
            t.addVertex(outer[i + 1][0], outer[i + 1][1], outer[i + 1][2]);
            Draw.color(t, RED, 0);
            t.addVertex(inner[i + 1][0], inner[i + 1][1], inner[i + 1][2]);
            t.addVertex(inner[i][0], inner[i][1], inner[i][2]);
            // The edge itself, turned to the eye so that it shows from any side: red, with a white wire in it.
            float w0 = thick[i] / 0.26F;
            float w1 = thick[i + 1] / 0.26F;
            ribbon(
                t,
                outer[i][0],
                outer[i][1],
                outer[i][2],
                outer[i + 1][0],
                outer[i + 1][1],
                outer[i + 1][2],
                0.17F * w0,
                0.17F * w1,
                RED,
                0.4F * strength,
                RED,
                0.4F * strength);
            if (whole) {
                ribbon(
                    t,
                    outer[i][0],
                    outer[i][1],
                    outer[i][2],
                    outer[i + 1][0],
                    outer[i + 1][1],
                    outer[i + 1][2],
                    0.04F * w0,
                    0.04F * w1,
                    WHITE,
                    strength,
                    WHITE,
                    strength);
            }
        }
        t.draw();
    }

    // Pieces. Places are in blocks from the camera, which is at the origin.

    /** A band of light between two places, turned to face the eye, with a width and a colour at each end. */
    private static void ribbon(Tessellator t, float ax, float ay, float az, float bx, float by, float bz, float widthA,
        float widthB, int colorA, float alphaA, int colorB, float alphaB) {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float mx = (ax + bx) / 2;
        float my = (ay + by) / 2;
        float mz = (az + bz) / 2;
        float wx = dy * mz - dz * my;
        float wy = dz * mx - dx * mz;
        float wz = dx * my - dy * mx;
        float length = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (length < 1e-6F) {
            return;
        }
        wx /= length;
        wy /= length;
        wz /= length;
        Draw.color(t, colorA, alphaA);
        t.addVertex(ax - wx * widthA, ay - wy * widthA, az - wz * widthA);
        t.addVertex(ax + wx * widthA, ay + wy * widthA, az + wz * widthA);
        Draw.color(t, colorB, alphaB);
        t.addVertex(bx + wx * widthB, by + wy * widthB, bz + wz * widthB);
        t.addVertex(bx - wx * widthB, by - wy * widthB, bz - wz * widthB);
    }

    /** A strip lying on the ground between two places, this wide to each side. */
    private static void flat(Tessellator t, float ax, float ay, float az, float bx, float by, float bz, float[] side,
        float width, int color, float alpha) {
        Draw.color(t, color, 0);
        t.addVertex(ax - side[0] * width, ay, az - side[2] * width);
        t.addVertex(bx - side[0] * width, by, bz - side[2] * width);
        Draw.color(t, color, alpha);
        t.addVertex(bx, by, bz);
        t.addVertex(ax, ay, az);
        t.addVertex(ax, ay, az);
        t.addVertex(bx, by, bz);
        Draw.color(t, color, 0);
        t.addVertex(bx + side[0] * width, by, bz + side[2] * width);
        t.addVertex(ax + side[0] * width, ay, az + side[2] * width);
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }

    private static float length(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static void normalize(float[] v) {
        float length = length(v);
        v[0] /= length;
        v[1] /= length;
        v[2] /= length;
    }
}
