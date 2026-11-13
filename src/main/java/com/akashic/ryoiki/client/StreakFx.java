package com.akashic.ryoiki.client;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.StreakField;

/**
 * The rush of the star void ({@link StreakField}), drawn: each streak a needle of light, turned to face the eye. Its
 * head is a white point; behind it the needle narrows to nothing, white near the head, then its own colour, then a
 * deeper one that is gone at the tail, inside a wider, fainter glow of the same. The colours are the void's: white,
 * ice blue, violet and magenta. A streak sent in a rush is brighter, and splits a little: a thread of cyan to one
 * side and of violet to the other, as light does when it is hurried.
 */
final class StreakFx {

    private static final int WHITE = 0xFFFFFF;
    private static final int CYAN = 0x50E0FF;
    private static final int VIOLET = 0xB070FF;
    /** For each of the field's colours: the needle's body, and its tail. */
    private static final int[] BODY = { 0xFFFFFF, 0x9FD0FF, 0xB48CFF, 0xFF6AD8 };
    private static final int[] TAIL = { 0x6070F0, 0x3050E8, 0x6030D0, 0xA01890 };

    private static final float WIDTH = 0.055F;
    private static final StreakField.Streak STREAK = new StreakField.Streak();

    private StreakFx() {}

    /**
     * @param time ticks since the domain opened, with fractions
     * @param eyeX the way the focus lies from the arena's centre (where the armillary hangs): a unit direction
     * @param camX the camera, relative to the arena's centre, in the void's scene
     */
    static void draw(Tessellator t, int radius, float time, int seed, float eyeX, float eyeY, float eyeZ, double camX,
        double camY, double camZ) {
        int lanes = StreakField.lanes(radius);
        int latest = StreakField.moment(time);
        int oldest = latest - StreakField.momentsAlive(radius);
        Draw.additive();
        GL11.glDepthMask(false);
        // The needles, then the points of light at their heads.
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0) {
                t.startDrawingQuads();
            } else {
                t.startDrawing(GL11.GL_TRIANGLES);
            }
            for (int a = -lanes; a <= lanes; a++) {
                for (int b = -lanes; b <= lanes; b++) {
                    for (int moment = oldest; moment <= latest; moment++) {
                        if (StreakField.at(seed, a, b, moment, time, radius, eyeX, eyeY, eyeZ, STREAK)) {
                            streak(t, pass == 1, camX, camY, camZ);
                        }
                    }
                }
            }
            t.draw();
        }
        GL11.glDepthMask(true);
    }

    private static void streak(Tessellator t, boolean head, double camX, double camY, double camZ) {
        StreakField.Streak s = STREAK;
        double dx = s.headX - s.tailX;
        double dy = s.headY - s.tailY;
        double dz = s.headZ - s.tailZ;
        double length2 = dx * dx + dy * dy + dz * dz;
        // The point of the needle nearest the eye: across the line to it and across the needle is its width.
        double along = ((camX - s.tailX) * dx + (camY - s.tailY) * dy + (camZ - s.tailZ) * dz) / length2;
        along = along < 0 ? 0 : along > 1 ? 1 : along;
        double sx = s.tailX + dx * along - camX;
        double sy = s.tailY + dy * along - camY;
        double sz = s.tailZ + dz * along - camZ;
        double distance = Math.sqrt(sx * sx + sy * sy + sz * sz);
        double wx = dy * sz - dz * sy;
        double wy = dz * sx - dx * sz;
        double wz = dx * sy - dy * sx;
        double across = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (across < 1e-6) {
            return;
        }
        // One that passes right by the eye is dimmed: a needle a hand from the eye is a white wall.
        float near = 0.08F + 0.92F * Draw.clamp01(((float) distance - 1.2F) / 3F);
        float bright = s.shown * near * (0.45F + 0.55F * s.rush);
        double width = Math.max(WIDTH * (1 + 0.6F * s.rush), distance * 0.0018) / across;
        wx *= width;
        wy *= width;
        wz *= width;
        int body = BODY[s.color];
        int tail = TAIL[s.color];
        if (head) {
            Draw.glow(t, s.headX, s.headY, s.headZ, 0.18F + 0.22F * s.rush, WHITE, bright);
            Draw.glow(t, s.headX, s.headY, s.headZ, 0.6F + 0.5F * s.rush, body, 0.4F * bright);
            return;
        }
        // Its glow, then itself: nothing at the tail, widening to the head.
        taper(t, s, wx * 3.4, wy * 3.4, wz * 3.4, tail, body, body, 0.2F * bright);
        taper(t, s, wx, wy, wz, tail, body, WHITE, bright);
        if (s.rush > 0.6F) {
            // Hurried light splits: a thread of each colour, a little to either side.
            float split = (s.rush - 0.6F) * 2.5F * bright * 0.5F;
            thread(t, s, wx * 2.2, wy * 2.2, wz * 2.2, wx * 0.5, wy * 0.5, wz * 0.5, CYAN, split);
            thread(t, s, -wx * 2.2, -wy * 2.2, -wz * 2.2, wx * 0.5, wy * 0.5, wz * 0.5, VIOLET, split);
        }
    }

    /** The needle in two lengths: from nothing at the tail to most of its width, then to all of it at the head. */
    private static void taper(Tessellator t, StreakField.Streak s, double wx, double wy, double wz, int tail,
        int middle, int head, float alpha) {
        double mx = s.tailX + (s.headX - s.tailX) * 0.72;
        double my = s.tailY + (s.headY - s.tailY) * 0.72;
        double mz = s.tailZ + (s.headZ - s.tailZ) * 0.72;
        Draw.color(t, tail, 0);
        t.addVertex(s.tailX, s.tailY, s.tailZ);
        t.addVertex(s.tailX, s.tailY, s.tailZ);
        Draw.color(t, middle, alpha * 0.55F);
        t.addVertex(mx + wx * 0.6, my + wy * 0.6, mz + wz * 0.6);
        t.addVertex(mx - wx * 0.6, my - wy * 0.6, mz - wz * 0.6);
        t.addVertex(mx - wx * 0.6, my - wy * 0.6, mz - wz * 0.6);
        t.addVertex(mx + wx * 0.6, my + wy * 0.6, mz + wz * 0.6);
        Draw.color(t, head, alpha);
        t.addVertex(s.headX + wx, s.headY + wy, s.headZ + wz);
        t.addVertex(s.headX - wx, s.headY - wy, s.headZ - wz);
    }

    /** A thread of one colour beside the needle, over its leading half. */
    private static void thread(Tessellator t, StreakField.Streak s, double ox, double oy, double oz, double wx,
        double wy, double wz, int color, float alpha) {
        double fx = (s.tailX + s.headX) / 2 + ox;
        double fy = (s.tailY + s.headY) / 2 + oy;
        double fz = (s.tailZ + s.headZ) / 2 + oz;
        Draw.color(t, color, 0);
        t.addVertex(fx, fy, fz);
        t.addVertex(fx, fy, fz);
        Draw.color(t, color, alpha);
        t.addVertex(s.headX + ox + wx, s.headY + oy + wy, s.headZ + oz + wz);
        t.addVertex(s.headX + ox - wx, s.headY + oy - wy, s.headZ + oz - wz);
    }
}
