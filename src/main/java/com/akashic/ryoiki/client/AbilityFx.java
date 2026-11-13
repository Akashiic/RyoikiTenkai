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

import com.akashic.ryoiki.fx.BlowPose;
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

    /** Ticks the red orb's end, and each place it went through someone, go on being drawn. */
    private static final int RED_ENDS = 9;
    /** The places a red orb on its way back leaves a wake through, and the ticks the black blow is drawn for. */
    private static final int TRAIL = 9;
    private static final int BLACK_ENDS = 18;
    private static final int BOLTS = 18;
    private static final int DEEP_RED = 0xC00010;
    private static final int HOT_RED = 0xFF5A40;
    /** Ticks an orb goes on being drawn after it is over: it closes on itself. */
    private static final int PULL_CLOSES = 7;
    private static final int LOBES = 9;
    private static final int INK = 0x0A1FB8;
    private static final int NEEDLES = 40;
    private static final float NEEDLE_PERIOD = 9;
    /** The flying cut: how big its crescent is, how far it leans, and how long what it leaves stays. */
    private static final float BLADE_RADIUS = 1.05F;
    private static final float BLADE_SPAN = 0.95F;
    private static final float BLADE_LEAN = 0.6F;
    private static final int CUT_ENDS = 7;
    private static final int SCAR_STAYS = 50;
    private static final float SCAR_STEP = 0.5F;

    /** The orbs of the star void, each by the entity that guides it. */
    private final Map<Integer, Orb> orbs = new HashMap<>();
    private final List<Cut> cuts = new ArrayList<>();
    /** The red orbs of the star void, each by the entity that gathers it. */
    private final Map<Integer, Red> reds = new HashMap<>();
    /** Who waits for its red orb to come back: 0 on guard, 1 striking, and for how many ticks, by entity. */
    private final Map<Integer, int[]> punches = new HashMap<>();
    /** Who is making the gesture of a technique: its kind, how long ago it began and how long it lasts, by entity. */
    private final Map<Integer, int[]> gestures = new HashMap<>();

    private static final class Red {

        int caster;
        boolean air;
        int charge;
        int age;
        /** Let fly: ticks since, from where, which way and how far. Negative while it is gathered. */
        int flown = -1;
        double x;
        double y;
        double z;
        float dx;
        float dy;
        float dz;
        float range;
        /** Where it went through someone: x, y, z and ticks since, four numbers each. */
        final List<float[]> met = new ArrayList<>();
        final double[] metAt = new double[3 * 8];
        /** How far the hand that threw it was from where its flight begins: it leaves from the hand. */
        double offX;
        double offY;
        double offZ;
        /** On its way back: ticks since it turned (negative while it has not), where it is, was, and has been. */
        int back = -1;
        double bx;
        double by;
        double bz;
        double beforeX;
        double beforeY;
        double beforeZ;
        double toX;
        double toY;
        double toZ;
        final double[] trail = new double[3 * TRAIL];
        int trailed;
        /** 0 alone, 1 bringing someone, 2 arrived: the blow is coming. */
        int state;
        /** How it ended: 0 not yet, 1 with the black blow, 2 back with nobody. Ticks since, where, which way. */
        int ended;
        int endAge;
        boolean burst;
        double ex;
        double ey;
        double ez;
        float edx;
        float edy;
        float edz;
    }

    private static final class Orb {

        int caster;
        /** Where it is this tick and where it was the last, and how fast it goes. */
        double x;
        double y;
        double z;
        double beforeX;
        double beforeY;
        double beforeZ;
        /** Where the server last put it. */
        double toX;
        double toY;
        double toZ;
        float speed;
        float radius;
        int ticks;
        int age;
        /** Begun in the air: its caster hangs there. */
        boolean air;
        /** The height of the ground under it; NaN when there is none near. */
        float ground = Float.NaN;
        /** Ticks since it ended; negative while it lasts. */
        int closing = -1;
        final float[] needles = new float[NEEDLES * 4];
        /** The dark lobes that turn about it: for each, the tilt of its orbit, where on it, how far out, how fast. */
        final float[] lobes = new float[LOBES * 4];
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
        reds.clear();
        punches.clear();
        BlackFlashFx.INSTANCE.clear();
        orbs.clear();
        cuts.clear();
        gestures.clear();
    }

    void heard(ModeWire.Message m) {
        Minecraft mc = Minecraft.getMinecraft();
        if (m.kind == AbilityMath.PULL) {
            Orb o = new Orb();
            o.caster = m.entity;
            o.x = o.beforeX = o.toX = m.x;
            o.y = o.beforeY = o.toY = m.y;
            o.z = o.beforeZ = o.toZ = m.z;
            o.radius = m.size;
            o.ticks = m.ticks;
            o.air = m.dx > 0.5F;
            Random random = new Random(Double.doubleToLongBits(m.x * 31 + m.z) ^ m.entity);
            for (int i = 0; i < NEEDLES; i++) {
                // A direction, evenly over the sphere; where in its run the needle starts; and how long it is.
                double up = random.nextDouble() * 2 - 1;
                double around = random.nextDouble() * Math.PI * 2;
                double flat = Math.sqrt(1 - up * up);
                o.needles[i * 4] = (float) (Math.cos(around) * flat);
                o.needles[i * 4 + 1] = (float) up;
                o.needles[i * 4 + 2] = (float) (Math.sin(around) * flat);
                o.needles[i * 4 + 3] = random.nextFloat();
            }
            for (int i = 0; i < LOBES; i++) {
                o.lobes[i * 4] = (random.nextFloat() - 0.5F) * 2.4F;
                o.lobes[i * 4 + 1] = random.nextFloat() * 6.283F;
                o.lobes[i * 4 + 2] = 0.75F + random.nextFloat() * 0.75F;
                o.lobes[i * 4 + 3] = (0.32F + random.nextFloat() * 0.3F) * (i % 2 == 0 ? 1 : -1);
            }
            orbs.put(m.entity, o);
            gesture(mc, m.entity, CastPose.PULL, m.ticks + 6);
        } else if (m.kind == AbilityMath.PULL_MOVE || m.kind == AbilityMath.PULL_END) {
            Orb o = orbs.get(m.entity);
            if (o != null && o.closing < 0) {
                o.toX = m.x;
                o.toY = m.y;
                o.toZ = m.z;
                o.speed = (float) Math.sqrt(m.dx * m.dx + m.dy * m.dy + m.dz * m.dz);
                if (m.kind == AbilityMath.PULL_END) {
                    o.closing = 0;
                    // The hand that held it out comes back now.
                    int[] g = gestures.get(m.entity);
                    if (g != null && g[0] == CastPose.PULL) {
                        g[2] = Math.min(g[2], g[1] + 6);
                    }
                }
            }
        } else if (m.kind == AbilityMath.RED_CHARGE) {
            Red r = new Red();
            r.caster = m.entity;
            r.air = m.dx > 0.5F;
            r.charge = m.ticks;
            reds.put(m.entity, r);
            gesture(mc, m.entity, CastPose.RED, m.ticks + (int) CastPose.RED_AFTER, false);
        } else if (m.kind == AbilityMath.RED_FIRE) {
            Red r = reds.get(m.entity);
            if (r != null) {
                r.flown = 0;
                r.x = m.x;
                r.y = m.y;
                r.z = m.z;
                r.dx = m.dx;
                r.dy = m.dy;
                r.dz = m.dz;
                r.range = m.size;
                if (mc.thePlayer != null && mc.thePlayer.getEntityId() == m.entity) {
                    // In its own eyes: the arm comes down with the shot.
                    mc.thePlayer.swingItem();
                }
            }
        } else if (m.kind == AbilityMath.RED_HIT) {
            Red r = reds.get(m.entity);
            if (r != null && r.met.size() < 8) {
                int n = r.met.size();
                r.metAt[n * 3] = m.x;
                r.metAt[n * 3 + 1] = m.y;
                r.metAt[n * 3 + 2] = m.z;
                r.met.add(new float[] { 0 });
            }
        } else if (m.kind == AbilityMath.RED_BACK) {
            Red r = reds.get(m.entity);
            if (r != null && r.ended == 0) {
                if (r.back < 0) {
                    r.back = 0;
                    r.bx = r.beforeX = m.x;
                    r.by = r.beforeY = m.y;
                    r.bz = r.beforeZ = m.z;
                    // Who let it fly squares up for what it brings.
                    punches.put(m.entity, new int[] { 0, 0 });
                }
                r.toX = m.x;
                r.toY = m.y;
                r.toZ = m.z;
                int state = Math.round(m.dx);
                if (state == 2 && r.state != 2) {
                    punches.put(m.entity, new int[] { 1, 0 });
                }
                r.state = state;
            }
        } else if (m.kind == AbilityMath.BLACK || m.kind == AbilityMath.RED_SELF) {
            Red r = reds.get(m.entity);
            if (r != null && r.ended == 0) {
                boolean black = m.kind == AbilityMath.BLACK;
                r.ended = black ? 1 : 2;
                r.burst = black || m.dx > 0.5F;
                r.ex = m.x;
                r.ey = m.y;
                r.ez = m.z;
                r.edx = m.dx;
                r.edy = m.dy;
                r.edz = m.dz;
                int own = mc.thePlayer == null ? -1 : mc.thePlayer.getEntityId();
                int victim = Math.round(m.size);
                if (black) {
                    // Who takes it is thrown by it; and it is seen, on every screen near and in the world.
                    ClientDomain.Struck struck = new ClientDomain.Struck();
                    ClientDomain.INSTANCE.strucks.put(victim, struck);
                    BlackFlashFx.INSTANCE.start(m.x, m.y, m.z, m.dx, m.dy, m.dz, m.entity, victim);
                } else {
                    punches.remove(m.entity);
                    if (r.burst && own == m.entity) {
                        CameraShake.INSTANCE.kick(5);
                    }
                }
            }
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
            gesture(mc, m.entity, CastPose.CUT, (int) CastPose.CUT_TICKS);
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

    private void gesture(Minecraft mc, int entity, int kind, int ticks) {
        gesture(mc, entity, kind, ticks, true);
    }

    private void gesture(Minecraft mc, int entity, int kind, int ticks, boolean swing) {
        gestures.put(entity, new int[] { kind, 0, ticks });
        if (swing && mc.thePlayer != null && mc.thePlayer.getEntityId() == entity) {
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
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.entity.Entity who = mc.theWorld == null ? null : mc.theWorld.getEntityByID(entity);
        float pitch = who == null ? 0
            : who.prevRotationPitch + (who.rotationPitch - who.prevRotationPitch) * partialTicks;
        pose[0] = CastPose.sample(g[0], age, g[2], pitch);
        return CastPose.layer(age, g[2]);
    }

    /**
     * The guard, and then the punch, of who waits for its red orb to come back: the degrees of
     * {@link BlowPose#BONES}, or null; and in {@code shows} how much of it shows.
     */
    float[] punch(int entity, float partialTicks, float[] shows) {
        int[] p = punches.get(entity);
        if (p == null) {
            return null;
        }
        float age = p[1] + partialTicks;
        if (p[0] == 0) {
            shows[0] = Draw.clamp01(age / 4);
            return BlowPose.guard();
        }
        shows[0] = Draw.clamp01((BlowPose.TICKS - age) / 3);
        return BlowPose.sample(1, Math.min(age, BlowPose.TICKS));
    }

    /** For the development harness: an effect, as if the server had announced it. */
    public void show(byte[] data) {
        heard(ModeWire.decode(data));
    }

    /** Whether a player hangs in the air by an orb it began there: it does not move until the orb is over. */
    public boolean hangs(int entity) {
        Orb o = orbs.get(entity);
        Red r = reds.get(entity);
        return o != null && o.air && o.closing < 0 || r != null && r.air && r.flown < 0 && r.age <= r.charge;
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
        Minecraft mc = Minecraft.getMinecraft();
        for (Iterator<Orb> i = orbs.values()
            .iterator(); i.hasNext();) {
            Orb o = i.next();
            o.age++;
            o.beforeX = o.x;
            o.beforeY = o.y;
            o.beforeZ = o.z;
            o.x = o.toX;
            o.y = o.toY;
            o.z = o.toZ;
            if (o.closing >= 0) {
                o.closing++;
            } else if (o.age > o.ticks + 30) {
                // The server never said it was over (its caster left): it is.
                o.closing = 0;
            } else {
                rubble(mc.theWorld, o);
            }
            o.ground = Float.NaN;
            int bx = MathHelper.floor_double(o.x);
            int bz = MathHelper.floor_double(o.z);
            for (int by = MathHelper.floor_double(o.y); by >= MathHelper.floor_double(o.y) - 4; by--) {
                if (mc.theWorld.getBlock(bx, by, bz)
                    .getMaterial()
                    .blocksMovement()) {
                    o.ground = by + 1;
                    break;
                }
            }
            if (o.closing > PULL_CLOSES) {
                i.remove();
            }
        }
        for (Iterator<Red> i = reds.values()
            .iterator(); i.hasNext();) {
            Red r = i.next();
            r.age++;
            for (float[] met : r.met) {
                met[0]++;
            }
            if (r.flown >= 0) {
                r.flown++;
            }
            if (r.back >= 0 && r.ended == 0) {
                r.back++;
                System.arraycopy(r.trail, 0, r.trail, 3, 3 * (TRAIL - 1));
                r.trail[0] = r.bx;
                r.trail[1] = r.by;
                r.trail[2] = r.bz;
                r.trailed = Math.min(TRAIL, r.trailed + 1);
                r.beforeX = r.bx;
                r.beforeY = r.by;
                r.beforeZ = r.bz;
                r.bx = r.toX;
                r.by = r.toY;
                r.bz = r.toZ;
            }
            boolean over;
            if (r.ended > 0) {
                over = ++r.endAge > BLACK_ENDS;
            } else if (r.back >= 0) {
                // The server never said how it ended (its caster left).
                over = r.back > AbilityMath.RED_BACK_TICKS + 20;
            } else if (r.flown >= 0) {
                // A while after it has flown its range: time for the word that it comes back to arrive.
                over = r.flown > r.range / AbilityMath.RED_SPEED + RED_ENDS + 2;
            } else {
                // Never let fly: a while after it should have been.
                over = r.age > r.charge + 12;
            }
            if (over) {
                i.remove();
            }
        }
        for (Iterator<Map.Entry<Integer, int[]>> i = punches.entrySet()
            .iterator(); i.hasNext();) {
            Map.Entry<Integer, int[]> e = i.next();
            int[] p = e.getValue();
            p[1]++;
            Red r = reds.get(e.getKey());
            if (p[0] == 1 ? p[1] > BlowPose.TICKS : r == null || r.back < 0 || r.ended == 2) {
                i.remove();
            }
        }
        BlackFlashFx.INSTANCE.tick();
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
            if (++g[1] > g[2]) {
                i.remove();
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (orbs.isEmpty() && cuts.isEmpty() && reds.isEmpty() && !BlackFlashFx.INSTANCE.active()) {
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
        for (Orb o : orbs.values()) {
            float partial = event.partialTicks;
            orb(
                t,
                o,
                (float) (o.beforeX + (o.x - o.beforeX) * partial - camX),
                (float) (o.beforeY + (o.y - o.beforeY) * partial - camY),
                (float) (o.beforeZ + (o.z - o.beforeZ) * partial - camZ),
                (float) (o.x - o.beforeX),
                (float) (o.y - o.beforeY),
                (float) (o.z - o.beforeZ),
                partial);
        }
        for (Red r : reds.values()) {
            red(t, mc, r, camX, camY, camZ, event.partialTicks);
        }
        BlackFlashFx.INSTANCE.world(t, mc, camX, camY, camZ, event.partialTicks);
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

    private static final int[] SPHERE = { 0x1838D8, 0x2F8CFF, 0x58D6FF, 0xBFF6FF, 0xFFFFFF };
    private static final int SPHERE_RIM = 0x8FC8FF;
    private static final int[] RED_SPHERE = { 0x4A0006, 0xA8000E, 0xE60F1A, 0xFF3A2A, 0xFF9A70 };
    private static final int RED_RIM = 0xFF1A1A;
    private static final int ROSE = 0xFFD8D0;
    private static final int STACKS = 14;
    private static final int SLICES = 22;

    /**
     * A sphere at a place (blocks from the camera), coloured by how its surface faces the eye, with bands of light
     * that turn about it.
     */
    static void sphere(Tessellator t, float x, float y, float z, float radius, float age, float alpha, int[] ramp,
        int rim) {
        if (alpha <= 0.02F) {
            return;
        }
        float distance = (float) Math.sqrt(x * x + y * y + z * z);
        if (distance < radius * 1.15F) {
            // The eye is in it: there is no outside to draw.
            return;
        }
        float[][][] at = new float[STACKS + 1][SLICES + 1][3];
        int[][] color = new int[STACKS + 1][SLICES + 1];
        float[][] facing = new float[STACKS + 1][SLICES + 1];
        for (int i = 0; i <= STACKS; i++) {
            double lat = Math.PI * i / STACKS - Math.PI / 2;
            float cosLat = (float) Math.cos(lat);
            float sinLat = (float) Math.sin(lat);
            for (int j = 0; j <= SLICES; j++) {
                double lon = Math.PI * 2 * j / SLICES;
                float nx = cosLat * (float) Math.cos(lon);
                float nz = cosLat * (float) Math.sin(lon);
                float px = x + nx * radius;
                float py = y + sinLat * radius;
                float pz = z + nz * radius;
                at[i][j][0] = px;
                at[i][j][1] = py;
                at[i][j][2] = pz;
                // How squarely this bit of surface faces the eye: 1 in the middle of the disc, 0 on its edge.
                float length = (float) Math.sqrt(px * px + py * py + pz * pz);
                float f = -(nx * px + sinLat * py + nz * pz) / length;
                facing[i][j] = f;
                int c = Draw.mix(ramp[0], ramp[1], smooth((f - 0.14F) / 0.28F));
                c = Draw.mix(c, ramp[2], smooth((f - 0.40F) / 0.30F));
                c = Draw.mix(c, ramp[3], smooth((f - 0.68F) / 0.20F));
                c = Draw.mix(c, ramp[4], smooth((f - 0.86F) / 0.13F));
                c = Draw.mix(c, rim, (1 - smooth(f / 0.09F)) * 0.75F);
                // The bands: brighter and darker stripes that wind about it and turn.
                float band = MathHelper.sin((float) (4 * lon + 3 * Math.sin(2 * lat)) + age * 0.35F);
                color[i][j] = band > 0 ? Draw.mix(c, 0xFFFFFF, 0.16F * band) : Draw.mix(c, ramp[0], -0.14F * band);
            }
        }
        Draw.translucent();
        GL11.glDepthMask(true);
        t.startDrawingQuads();
        for (int i = 0; i < STACKS; i++) {
            for (int j = 0; j < SLICES; j++) {
                if (facing[i][j] < -0.05F && facing[i + 1][j] < -0.05F
                    && facing[i][j + 1] < -0.05F
                    && facing[i + 1][j + 1] < -0.05F) {
                    // The far side.
                    continue;
                }
                corner(t, at[i][j], color[i][j], alpha);
                corner(t, at[i][j + 1], color[i][j + 1], alpha);
                corner(t, at[i + 1][j + 1], color[i + 1][j + 1], alpha);
                corner(t, at[i + 1][j], color[i + 1][j], alpha);
            }
        }
        t.draw();
        GL11.glDepthMask(false);
    }

    private static void corner(Tessellator t, float[] at, int color, float alpha) {
        Draw.color(t, color, alpha);
        t.addVertex(at[0], at[1], at[2]);
    }

    private static float smooth(float v) {
        v = Draw.clamp01(v);
        return v * v * (3 - 2 * v);
    }

    /** Bits of the ground under an orb, torn up toward it: the game's own, of whatever block is there. */
    private static void rubble(World world, Orb o) {
        if (world == null || world.rand.nextInt(2) != 0) {
            return;
        }
        int bx = MathHelper.floor_double(o.x + (world.rand.nextDouble() - 0.5) * 3);
        int bz = MathHelper.floor_double(o.z + (world.rand.nextDouble() - 0.5) * 3);
        for (int by = MathHelper.floor_double(o.y); by >= MathHelper.floor_double(o.y) - 3; by--) {
            net.minecraft.block.Block block = world.getBlock(bx, by, bz);
            if (block.getMaterial()
                .blocksMovement() && block.getRenderType() >= 0) {
                String name = "blockcrack_" + net.minecraft.block.Block.getIdFromBlock(block)
                    + "_"
                    + world.getBlockMetadata(bx, by, bz);
                world.spawnParticle(
                    name,
                    bx + world.rand.nextDouble(),
                    by + 1.1,
                    bz + world.rand.nextDouble(),
                    (o.x - bx - 0.5) * 0.12,
                    0.32,
                    (o.z - bz - 0.5) * 0.12);
                return;
            }
        }
    }

    private static void orb(Tessellator t, Orb o, float x, float y, float z, float vx, float vy, float vz,
        float partialTicks) {
        float age = o.age + partialTicks;
        float open = Draw.clamp01(age / 4);
        // Over: it closes on itself, and a ring goes out from where it was.
        float closing = o.closing < 0 ? 0 : Draw.clamp01((o.closing + partialTicks) / PULL_CLOSES);
        // Whoever is inside it sees it around them, not a white screen: its light is dimmed close to the eye.
        float near = 0.16F + 0.84F * Draw.clamp01(((float) Math.sqrt(x * x + y * y + z * z) - 0.8F) / 2.6F);
        float alive = open * (1 - closing) * near;
        float beat = 0.5F + 0.5F * MathHelper.sin(age * 0.9F);
        float size = (0.62F + 0.03F * beat) * (0.35F + 0.65F * open) * (1 - closing * closing);
        float speed = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);

        // Swung through the air it leaves itself behind: paler orbs along the way it came.
        Draw.additive();
        if (speed > 0.08F) {
            t.startDrawing(GL11.GL_TRIANGLES);
            for (int ghost = 1; ghost <= 4; ghost++) {
                float back = ghost * 0.55F;
                float fade = alive * (1 - ghost / 5F) * Draw.clamp01(speed / 0.5F);
                Draw.glow(
                    t,
                    x - vx * back,
                    y - vy * back,
                    z - vz * back,
                    size * (2.4F - ghost * 0.3F),
                    BLUE,
                    0.3F * fade);
                Draw.glow(t, x - vx * back, y - vy * back, z - vz * back, size * 1.1F, ICE, 0.35F * fade);
            }
            t.draw();
        }

        // The needles: light drawn in from all around, each on its own line, faster as it nears the orb.
        t.startDrawingQuads();
        for (int i = 0; i < NEEDLES; i++) {
            float run = (age / NEEDLE_PERIOD + o.needles[i * 4 + 3]) % 1;
            if (age - run * NEEDLE_PERIOD < 0 || closing > 0 && run < closing) {
                // Not sent yet; or sent after the orb began to close.
                continue;
            }
            float head = o.radius * (1 - run) * (1 - run);
            float tail = Math.min(o.radius, head + 0.5F + 1.5F * (1 - run));
            float dx = o.needles[i * 4];
            float dy = o.needles[i * 4 + 1];
            float dz = o.needles[i * 4 + 2];
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
        // Three bright arcs that turn about it, each on its own tilt: the space it winds up.
        for (int arc = 0; arc < 3; arc++) {
            float tilt = 0.5F + arc * 1.1F;
            float turn = age * (0.42F + arc * 0.09F) + arc * 2.1F;
            float reach = size * (1.9F + 0.5F * arc);
            float[] last = null;
            for (int step = 0; step <= 10; step++) {
                float a = turn + step * 0.22F;
                float ca = MathHelper.cos(a) * reach;
                float sa = MathHelper.sin(a) * reach;
                float[] at = { x + ca, y + sa * MathHelper.sin(tilt), z + sa * MathHelper.cos(tilt) };
                if (last != null) {
                    float f0 = (step - 1) / 10F;
                    float f1 = step / 10F;
                    ribbon(
                        t,
                        last[0],
                        last[1],
                        last[2],
                        at[0],
                        at[1],
                        at[2],
                        0.05F * f0,
                        0.05F * f1,
                        WHITE,
                        0.7F * alive * f0,
                        ICE,
                        0.7F * alive * f1);
                }
                last = at;
            }
        }
        t.draw();

        // Two rings, flat, closing on it one after the other.
        for (int ring = 0; ring < 2; ring++) {
            float run = (age / 11F + ring * 0.5F) % 1;
            float radius = o.radius * (1 - run * run);
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, z);
            Draw.ring(
                Math.max(0, radius - 0.1F - 0.25F * run),
                radius,
                40,
                ICE,
                0,
                Draw.mix(ICE, BLUE, run),
                0.4F * alive * Draw.clamp01(run * 4));
            GL11.glPopMatrix();
        }

        // The light it throws: around it, and in a pool on whatever ground is under it.
        t.startDrawing(GL11.GL_TRIANGLES);
        Draw.glow(t, x, y, z, size * 5.8F, BLUE, 0.4F * alive);
        Draw.glow(t, x, y, z, size * 3.3F, 0x7FCBFF, 0.55F * alive);
        Draw.glow(t, x, y, z, size * 2.15F, 0xCFEFFF, 0.5F * alive);
        t.draw();
        if (!Float.isNaN(o.ground) && o.y - o.ground < 4) {
            float low = 1 - (float) (o.y - o.ground) / 4;
            GL11.glPushMatrix();
            GL11.glTranslatef(x, (float) (o.ground - RenderManager.renderPosY) + 0.03F, z);
            Draw.ring(0, 0.6F + 2.6F * low, 28, 0xDFF4FF, 0.42F * alive * low, 0x7FCBFF, 0);
            GL11.glPopMatrix();
        }
        // About it, dark splashes of deep blue, turning: each a hard little body with a tail.
        Draw.translucent();
        for (int i = 0; i < LOBES; i++) {
            float tilt = o.lobes[i * 4];
            float reach = size * (1.75F + 0.75F * o.lobes[i * 4 + 2]) * (1 + 0.12F * MathHelper.sin(age * 0.3F + i));
            float[] last = null;
            for (int step = 0; step <= 9; step++) {
                // The body leads; the tail is where it was a moment ago.
                float a = o.lobes[i * 4 + 1] + (age - step * 0.32F) * o.lobes[i * 4 + 3];
                float ca = MathHelper.cos(a) * reach;
                float sa = MathHelper.sin(a) * reach;
                float[] at = { x + ca * MathHelper.cos(tilt), y + sa * 0.85F + ca * MathHelper.sin(tilt) * 0.5F,
                    z + sa * 0.6F - ca * MathHelper.sin(tilt) * 0.4F };
                if (step == 0) {
                    float body = size * (0.3F + 0.12F * MathHelper.sin(age * 0.5F + i * 2));
                    t.startDrawing(GL11.GL_TRIANGLES);
                    Draw.glow(t, at[0], at[1], at[2], body * 2.1F, INK, 0.75F * alive);
                    Draw.glow(t, at[0], at[1], at[2], body * 1.1F, 0x061280, alive);
                    t.draw();
                } else {
                    float f0 = 1 - (step - 1) / 9F;
                    float f1 = 1 - step / 9F;
                    t.startDrawingQuads();
                    ribbon(
                        t,
                        last[0],
                        last[1],
                        last[2],
                        at[0],
                        at[1],
                        at[2],
                        size * 0.26F * f0,
                        size * 0.26F * f1,
                        INK,
                        0.8F * alive * f0,
                        INK,
                        0.8F * alive * f1);
                    t.draw();
                }
                last = at;
            }
        }
        // The orb itself: a sphere, with a body and an edge. From the eye, its middle is white, then the blue of
        // ice, then a deep blue where its surface turns away, and a thin light on the very edge; and its surface
        // moves.
        sphere(t, x, y, z, size * 1.5F, age, alive, SPHERE, SPHERE_RIM);

        if (closing > 0) {
            // What is left when it closes: a ring of light going out, thin and quick.
            Draw.additive();
            float out = closing * (2 - closing);
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, z);
            Draw.ring(1.8F * out, 1.8F * out + 0.4F * (1 - closing), 40, WHITE, (1 - closing) * 0.8F, ICE, 0);
            GL11.glPopMatrix();
            t.startDrawing(GL11.GL_TRIANGLES);
            Draw.glow(t, x, y, z, 1.6F * (1 - closing), WHITE, (1 - closing) * 0.9F);
            t.draw();
        }
    }

    // Repulsão.

    /** Where an entity is between the last tick and this one: x, the height of its feet, z. */
    private static double[] place(net.minecraft.entity.Entity e, float partialTicks) {
        return new double[] { e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks,
            e.boundingBox.minY + (e.posY - e.lastTickPosY) * (partialTicks - 1),
            e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks };
    }

    /**
     * Where the right hand of who uses a technique is, in blocks from the camera: its model's own hand where it has
     * a model that is drawn; else where a hand held up ({@code raised}) or pointing would be.
     */
    private static float[] hand(Minecraft mc, net.minecraft.entity.Entity caster, float partialTicks, double camX,
        double camY, double camZ, boolean raised) {
        boolean own = caster == mc.thePlayer && mc.gameSettings.thirdPersonView == 0;
        if (!own && caster instanceof net.minecraft.entity.player.EntityPlayer) {
            float[] model = FuturaBridge.INSTANCE.hand((net.minecraft.entity.player.EntityPlayer) caster, true);
            if (model != null) {
                return model;
            }
        }
        net.minecraft.util.Vec3 look = caster instanceof net.minecraft.entity.EntityLivingBase
            ? ((net.minecraft.entity.EntityLivingBase) caster).getLook(partialTicks)
            : net.minecraft.util.Vec3.createVectorHelper(0, 0, 1);
        double yaw = Math
            .toRadians(caster.prevRotationYaw + (caster.rotationYaw - caster.prevRotationYaw) * partialTicks);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        if (own) {
            // In its own eyes, which are where the camera is: up and to the right of what it looks at, in sight;
            // or low and to the right, where an arm that points would be.
            double out = raised ? 1.5 : 0.9;
            double aside = raised ? 0.72 : 0.36;
            double up = raised ? 0.5 : -0.24;
            return new float[] { (float) (look.xCoord * out + rx * aside), (float) (look.yCoord * out + up),
                (float) (look.zCoord * out + rz * aside) };
        }
        double[] at = place(caster, partialTicks);
        if (raised) {
            return new float[] { (float) (at[0] + rx * 0.36 - camX), (float) (at[1] + 2.3 - camY),
                (float) (at[2] + rz * 0.36 - camZ) };
        }
        return new float[] { (float) (at[0] + look.xCoord * 0.8 + rx * 0.34 - camX),
            (float) (at[1] + 1.42 + look.yCoord * 0.8 - camY), (float) (at[2] + look.zCoord * 0.8 + rz * 0.34 - camZ) };
    }

    private static void red(Tessellator t, Minecraft mc, Red r, double camX, double camY, double camZ,
        float partialTicks) {
        float age = r.age + partialTicks;
        net.minecraft.entity.Entity caster = mc.theWorld.getEntityByID(r.caster);
        boolean own = caster == mc.thePlayer && mc.gameSettings.thirdPersonView == 0;
        if (r.ended > 0) {
            float since = r.endAge + partialTicks;
            float ex = (float) (r.ex - camX);
            float ey = (float) (r.ey - camY);
            float ez = (float) (r.ez - camZ);
            if (r.ended == 2 && r.burst && since < RED_ENDS) {
                // Back with nobody: it breaks on who let it fly.
                float burst = 1 - since / RED_ENDS;
                Draw.additive();
                t.startDrawing(GL11.GL_TRIANGLES);
                Draw.glow(t, ex, ey, ez, 2.4F * (1.25F - burst), RED, 0.8F * burst * (own ? 0.4F : 1));
                Draw.glow(t, ex, ey, ez, 0.9F, WHITE, burst * burst * (own ? 0.3F : 1));
                t.draw();
            }
            return;
        }
        if (r.flown < 0) {
            // Gathered over the hand that is held up, wherever that is now.
            if (caster == null || age > r.charge + 2) {
                return;
            }
            float full = Draw.clamp01(age / r.charge);
            float[] h = hand(mc, caster, partialTicks, camX, camY, camZ, true);
            // Over the fingertips, not in the palm.
            float fx = h[0];
            float fy = h[1] + (own ? 0 : 0.14F + 0.2F * full);
            float fz = h[2];
            double[] at = place(caster, partialTicks);
            float bx = (float) (at[0] - camX);
            float by = (float) (at[1] + 1.0 - camY);
            float bz = (float) (at[2] - camZ);
            float size = (0.04F + 0.17F * full * full) * (own ? 0.7F : 1);
            Draw.additive();
            t.startDrawingQuads();
            // About the body: red arcs with a white wire in them, winding faster and closer as it fills.
            if (!own) {
                for (int arc = 0; arc < 3; arc++) {
                    float tilt = 0.5F + arc * 1.0F;
                    float turn = age * (0.5F + 0.6F * full + arc * 0.07F) + arc * 2.1F;
                    float reach = 1.25F - 0.4F * full + 0.12F * arc;
                    float[] last = null;
                    for (int step = 0; step <= 12; step++) {
                        float a = turn + step * 0.2F;
                        float ca = MathHelper.cos(a) * reach;
                        float sa = MathHelper.sin(a) * reach;
                        float[] p = { bx + ca, by + sa * MathHelper.sin(tilt) * 0.9F, bz + sa * MathHelper.cos(tilt) };
                        if (last != null) {
                            float f0 = (step - 1) / 12F;
                            float f1 = step / 12F;
                            ribbon(
                                t,
                                last[0],
                                last[1],
                                last[2],
                                p[0],
                                p[1],
                                p[2],
                                0.1F * f0,
                                0.1F * f1,
                                RED,
                                0.7F * full * f0,
                                RED,
                                0.7F * full * f1);
                            ribbon(
                                t,
                                last[0],
                                last[1],
                                last[2],
                                p[0],
                                p[1],
                                p[2],
                                0.025F * f0,
                                0.025F * f1,
                                WHITE,
                                0.8F * full * f0,
                                ROSE,
                                0.8F * full * f1);
                        }
                        last = p;
                    }
                }
            }
            // Into the orb: red threads from all around it, quicker as they arrive.
            Random random = new Random(r.caster * 31L + 7);
            for (int i = 0; i < 14; i++) {
                double up = random.nextDouble() * 2 - 1;
                double around = random.nextDouble() * Math.PI * 2;
                float run = (age / 7F + random.nextFloat()) % 1;
                double flat = Math.sqrt(1 - up * up);
                float dx = (float) (Math.cos(around) * flat);
                float dy = (float) up;
                float dz = (float) (Math.sin(around) * flat);
                float reach = (own ? 0.7F : 1.2F) * (0.6F + 0.4F * full);
                float head = size + reach * (1 - run) * (1 - run);
                float tail = Math.min(size + reach, head + 0.12F + 0.45F * (1 - run));
                float light = full * Draw.clamp01(run * 5) * (0.5F + 0.5F * run);
                ribbon(
                    t,
                    fx + dx * tail,
                    fy + dy * tail,
                    fz + dz * tail,
                    fx + dx * head,
                    fy + dy * head,
                    fz + dz * head,
                    0.008F,
                    0.03F,
                    RED,
                    0,
                    i % 3 == 0 ? HOT_RED : RED,
                    light);
            }
            t.draw();
            // The ground under who gathers it, red.
            GL11.glPushMatrix();
            GL11.glTranslatef(bx, (float) (at[1] - camY) + 0.04F, bz);
            Draw.ring(0, 0.9F + 2.3F * full, 28, RED, 0.6F * full, DEEP_RED, 0);
            GL11.glPopMatrix();
            t.startDrawing(GL11.GL_TRIANGLES);
            Draw.glow(t, fx, fy, fz, size * 5.5F, RED, 0.6F * full);
            Draw.glow(t, fx, fy, fz, size * 2.4F, HOT_RED, 0.75F * full);
            t.draw();
            sphere(t, fx, fy, fz, size, age * 2, Draw.clamp01(age / 3), RED_SPHERE, RED_RIM);
            return;
        }
        float flown = r.flown + partialTicks;
        float gone = (float) AbilityMath.redGone(flown, r.range);
        boolean over = gone >= r.range;
        float since = over ? flown - r.range / (float) AbilityMath.RED_SPEED : 0;
        if (caster != null && flown < 2.5F) {
            // It leaves from the hand as the arm comes down: for a moment it is carried, then it is on its line.
            float[] h = hand(mc, caster, partialTicks, camX, camY, camZ, flown < 0.6F);
            r.offX = h[0] + camX - r.x;
            r.offY = h[1] + camY - r.y;
            r.offZ = h[2] + camZ - r.z;
        }
        float carried = 1 - smooth(gone / 4.5F);
        float x = (float) (r.x - camX);
        float y = (float) (r.y - camY);
        float z = (float) (r.z - camZ);
        float hx = x + r.dx * gone + (float) r.offX * carried;
        float hy = y + r.dy * gone + (float) r.offY * carried;
        float hz = z + r.dz * gone + (float) r.offZ * carried;
        float[] d = { r.dx, r.dy, r.dz };
        float[] side = cross(d, new float[] { 0, 1, 0 });
        if (length(side) < 1e-3F) {
            side = new float[] { 1, 0, 0 };
        }
        normalize(side);
        float[] up = cross(side, d);
        normalize(up);
        Draw.additive();
        if (r.back < 0) {
            // Its wake: red where it has just been, gone a few blocks back, with a hot wire down the middle.
            t.startDrawingQuads();
            float fade = over ? Draw.clamp01(1 - since / 5) : 1;
            float tail = Math.max(0, gone - 8);
            for (int i = 0; i < 9; i++) {
                float a0 = tail + (gone - tail) * i / 9F;
                float a1 = tail + (gone - tail) * (i + 1) / 9F;
                float f0 = i / 9F;
                float f1 = (i + 1) / 9F;
                float c0 = 1 - smooth(a0 / 4.5F);
                float c1 = 1 - smooth(a1 / 4.5F);
                float x0 = x + r.dx * a0 + (float) r.offX * c0;
                float y0 = y + r.dy * a0 + (float) r.offY * c0;
                float z0 = z + r.dz * a0 + (float) r.offZ * c0;
                float x1 = x + r.dx * a1 + (float) r.offX * c1;
                float y1 = y + r.dy * a1 + (float) r.offY * c1;
                float z1 = z + r.dz * a1 + (float) r.offZ * c1;
                ribbon(
                    t,
                    x0,
                    y0,
                    z0,
                    x1,
                    y1,
                    z1,
                    0.24F * f0,
                    0.24F * f1,
                    RED,
                    0.75F * f0 * fade,
                    RED,
                    0.75F * f1 * fade);
                ribbon(
                    t,
                    x0,
                    y0,
                    z0,
                    x1,
                    y1,
                    z1,
                    0.045F * f0,
                    0.045F * f1,
                    HOT_RED,
                    0.9F * f0 * fade,
                    0xFFD0B0,
                    0.9F * f1 * fade);
            }
            // The air it pushes aside: red rings left along its way, widening as they fade.
            for (float at = 0.7F; at < gone; at += 3) {
                float old = (gone - at) / (float) AbilityMath.RED_SPEED + since;
                float left = 1 - old / 8;
                if (left <= 0) {
                    continue;
                }
                float wide = 0.3F + 0.16F * old;
                float cx = x + r.dx * at;
                float cy = y + r.dy * at;
                float cz = z + r.dz * at;
                for (int k = 0; k < 18; k++) {
                    float a0 = k * 6.2832F / 18;
                    float a1 = (k + 1) * 6.2832F / 18;
                    ribbon(
                        t,
                        cx + (side[0] * MathHelper.cos(a0) + up[0] * MathHelper.sin(a0)) * wide,
                        cy + (side[1] * MathHelper.cos(a0) + up[1] * MathHelper.sin(a0)) * wide,
                        cz + (side[2] * MathHelper.cos(a0) + up[2] * MathHelper.sin(a0)) * wide,
                        cx + (side[0] * MathHelper.cos(a1) + up[0] * MathHelper.sin(a1)) * wide,
                        cy + (side[1] * MathHelper.cos(a1) + up[1] * MathHelper.sin(a1)) * wide,
                        cz + (side[2] * MathHelper.cos(a1) + up[2] * MathHelper.sin(a1)) * wide,
                        0.035F,
                        0.035F,
                        HOT_RED,
                        0.7F * left * left,
                        RED,
                        0.7F * left * left);
                }
            }
            t.draw();
        }
        // Where it went through someone, and where its range ends: a burst, with red splinters thrown out of it.
        for (int i = 0; i <= r.met.size(); i++) {
            boolean end = i == r.met.size();
            if (end && !over) {
                continue;
            }
            float old = end ? since : r.met.get(i)[0] + partialTicks;
            float burst = 1 - old / RED_ENDS;
            if (burst <= 0) {
                continue;
            }
            float mx = end ? x + r.dx * r.range : (float) (r.metAt[i * 3] - camX);
            float my = end ? y + r.dy * r.range : (float) (r.metAt[i * 3 + 1] - camY);
            float mz = end ? z + r.dz * r.range : (float) (r.metAt[i * 3 + 2] - camZ);
            t.startDrawing(GL11.GL_TRIANGLES);
            Draw.glow(t, mx, my, mz, 1.9F * (1.25F - burst), RED, 0.8F * burst);
            Draw.glow(t, mx, my, mz, 0.6F, 0xFFD0B0, burst * burst);
            t.draw();
            t.startDrawingQuads();
            Random random = new Random(r.caster * 131L + i);
            for (int k = 0; k < 9; k++) {
                float a = random.nextFloat() * 6.2832F;
                float lift = random.nextFloat() - 0.4F;
                float reach = (0.5F + 1.6F * random.nextFloat()) * (1 - burst * burst);
                float sx = (side[0] * MathHelper.cos(a) + up[0] * lift + d[0] * 0.5F) * reach;
                float sy = (side[1] * MathHelper.cos(a) + up[1] * lift + d[1] * 0.5F + 0.3F) * reach;
                float sz = (side[2] * MathHelper.cos(a) + up[2] * lift + d[2] * 0.5F) * reach;
                ribbon(
                    t,
                    mx + sx * 0.55F,
                    my + sy * 0.55F,
                    mz + sz * 0.55F,
                    mx + sx,
                    my + sy,
                    mz + sz,
                    0.01F,
                    0.05F,
                    RED,
                    0,
                    k % 2 == 0 ? RED : HOT_RED,
                    burst);
            }
            t.draw();
        }
        if (r.back >= 0) {
            back(t, mc, r, caster, camX, camY, camZ, partialTicks);
            return;
        }
        if (!over) {
            // One that passes right by the eye is not a red screen.
            float near = 0.25F + 0.75F * Draw.clamp01(((float) Math.sqrt(hx * hx + hy * hy + hz * hz) - 0.8F) / 2);
            t.startDrawing(GL11.GL_TRIANGLES);
            Draw.glow(t, hx, hy, hz, 1.5F, RED, 0.7F * near);
            Draw.glow(t, hx, hy, hz, 0.7F, HOT_RED, 0.8F * near);
            t.draw();
            sphere(t, hx, hy, hz, 0.26F, age * 2, 1, RED_SPHERE, RED_RIM);
        }
    }

    /** The red orb on its way back to who let it fly, and that one's fist getting ready for what it brings. */
    private static void back(Tessellator t, Minecraft mc, Red r, net.minecraft.entity.Entity caster, double camX,
        double camY, double camZ, float partialTicks) {
        float age = r.age + partialTicks;
        float x = (float) (r.beforeX + (r.bx - r.beforeX) * partialTicks - camX);
        float y = (float) (r.beforeY + (r.by - r.beforeY) * partialTicks - camY);
        float z = (float) (r.beforeZ + (r.bz - r.beforeZ) * partialTicks - camZ);
        Draw.additive();
        // Its wake, through where it has been.
        t.startDrawingQuads();
        float lx = x;
        float ly = y;
        float lz = z;
        for (int i = 0; i < r.trailed; i++) {
            float px = (float) (r.trail[i * 3] - camX);
            float py = (float) (r.trail[i * 3 + 1] - camY);
            float pz = (float) (r.trail[i * 3 + 2] - camZ);
            float f0 = 1 - (float) i / TRAIL;
            float f1 = 1 - (float) (i + 1) / TRAIL;
            ribbon(t, lx, ly, lz, px, py, pz, 0.22F * f0, 0.22F * f1, RED, 0.7F * f0, RED, 0.7F * f1);
            ribbon(t, lx, ly, lz, px, py, pz, 0.04F * f0, 0.04F * f1, 0xFFD0B0, 0.9F * f0, HOT_RED, 0.9F * f1);
            lx = px;
            ly = py;
            lz = pz;
        }
        t.draw();
        if (r.state < 2) {
            float near = 0.25F + 0.75F * Draw.clamp01(((float) Math.sqrt(x * x + y * y + z * z) - 0.8F) / 2);
            t.startDrawing(GL11.GL_TRIANGLES);
            Draw.glow(t, x, y, z, 1.5F, RED, 0.7F * near);
            Draw.glow(t, x, y, z, 0.7F, HOT_RED, 0.8F * near);
            t.draw();
            if (r.state == 1) {
                // Bringing someone: black sparks about it, a taste of what is coming.
                Draw.translucent();
                t.startDrawingQuads();
                Random random = new Random(r.caster * 17L + (long) (age / 2));
                for (int k = 0; k < 6; k++) {
                    float ax = random.nextFloat() - 0.5F;
                    float ay = random.nextFloat() - 0.5F;
                    float az = random.nextFloat() - 0.5F;
                    ribbon(
                        t,
                        x + ax * 0.5F,
                        y + ay * 0.5F + 0.4F,
                        z + az * 0.5F,
                        x + ax * 2.2F,
                        y + ay * 2.2F + 0.4F,
                        z + az * 2.2F,
                        0.05F,
                        0.01F,
                        0x000000,
                        0.9F,
                        0x200000,
                        0.6F);
                }
                t.draw();
                Draw.additive();
            }
            sphere(t, x, y, z, 0.26F, age * 2, 1, RED_SPHERE, RED_RIM);
        }
        if (caster == null) {
            return;
        }
        // The fist that waits: small lights jumping about it, white and ice, brighter as the orb nears.
        float[] h = hand(mc, caster, partialTicks, camX, camY, camZ, false);
        float ready = Draw.clamp01(r.back / 6F);
        if (r.state == 2) {
            // The blow is on its way: black sparks break out of the fist, red at their edges.
            Random sparks = new Random(r.caster * 71L + (long) (age * 2) * 7919L);
            // Whoever is set before the fist has it a hand from its eyes: there the sparks are thin, not bars
            // across the screen.
            float thin = Draw.clamp01(((float) Math.sqrt(h[0] * h[0] + h[1] * h[1] + h[2] * h[2]) - 0.6F) / 2.6F);
            float[][][] bolts = new float[9][5][3];
            for (float[][] bolt : bolts) {
                float sx = sparks.nextFloat() * 2 - 1;
                float sy = sparks.nextFloat() * 2 - 1;
                float sz = sparks.nextFloat() * 2 - 1;
                float reach = 0.25F + 0.3F * sparks.nextFloat();
                for (int k = 0; k < 5; k++) {
                    bolt[k][0] = h[0] + sx * reach * k + (k == 0 ? 0 : (sparks.nextFloat() - 0.5F) * 0.3F);
                    bolt[k][1] = h[1] + sy * reach * k + (k == 0 ? 0 : (sparks.nextFloat() - 0.5F) * 0.3F);
                    bolt[k][2] = h[2] + sz * reach * k + (k == 0 ? 0 : (sparks.nextFloat() - 0.5F) * 0.3F);
                }
            }
            Draw.additive();
            t.startDrawingQuads();
            for (float[][] bolt : bolts) {
                for (int k = 0; k < 4; k++) {
                    ribbon(
                        t,
                        bolt[k][0],
                        bolt[k][1],
                        bolt[k][2],
                        bolt[k + 1][0],
                        bolt[k + 1][1],
                        bolt[k + 1][2],
                        0.16F * thin,
                        0.1F * thin,
                        RED,
                        0.6F,
                        RED,
                        0.4F);
                }
            }
            t.draw();
            Draw.translucent();
            t.startDrawingQuads();
            for (float[][] bolt : bolts) {
                for (int k = 0; k < 4; k++) {
                    ribbon(
                        t,
                        bolt[k][0],
                        bolt[k][1],
                        bolt[k][2],
                        bolt[k + 1][0],
                        bolt[k + 1][1],
                        bolt[k + 1][2],
                        0.06F * thin,
                        0.025F * thin,
                        0x000000,
                        1,
                        0x000000,
                        1);
                }
            }
            t.draw();
        }
        Draw.additive();
        t.startDrawing(GL11.GL_TRIANGLES);
        Draw.glow(t, h[0], h[1], h[2], 0.55F, ICE, 0.55F * ready);
        Random random = new Random(r.caster * 53L + (long) (age * 1.5F));
        for (int k = 0; k < 6; k++) {
            Draw.glow(
                t,
                h[0] + (random.nextFloat() - 0.5F) * 0.5F,
                h[1] + (random.nextFloat() - 0.5F) * 0.5F,
                h[2] + (random.nextFloat() - 0.5F) * 0.5F,
                0.07F + 0.07F * random.nextFloat(),
                k % 2 == 0 ? WHITE : ICE,
                ready);
        }
        t.draw();
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
    static void ribbon(Tessellator t, float ax, float ay, float az, float bx, float by, float bz, float widthA,
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

    static float[] cross(float[] a, float[] b) {
        return new float[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }

    static float length(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    static void normalize(float[] v) {
        float length = length(v);
        v[0] /= length;
        v[1] /= length;
        v[2] /= length;
    }
}
