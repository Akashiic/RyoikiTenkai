package com.akashic.ryoiki.client;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.StreakField;

/**
 * The rush of the star void ({@link StreakField}), drawn: each streak a needle of light, turned to face the eye. Its
 * head is a white point; behind it the needle narrows to nothing, white near the head, then pale blue, then a deep
 * blue that is gone at the tail, inside a wider, fainter glow of the same. A streak sent in a rush is brighter,
 * and splits a little: a thread of cyan to one side and of violet to the other, as light does when it is hurried.
 */
final class StreakFx {

    private static final int WHITE = 0xFFFFFF;
    private static final int PALE = 0xBFD4FF;
    private static final int DEEP = 0x4050E8;
    private static final int CYAN = 0x50E0FF;
    private static final int VIOLET = 0xB070FF;

    /** Blocks to either side of the camera that lanes are drawn for. */
    private static final float FAR = 34;
    private static final float WIDTH = 0.05F;
    private static final StreakField.Streak STREAK = new StreakField.Streak();

    private StreakFx() {}

    /**
     * @param time ticks since the domain opened, with fractions
     * @param camX the camera, relative to the arena's centre, in the void's scene
     */
    static void draw(Tessellator t, int radius, float time, int seed, double camX, double camY, double camZ) {
        int from = (int) Math.floor((camX - FAR) / StreakField.LANE);
        int to = (int) Math.floor((camX + FAR) / StreakField.LANE);
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
            for (int lane = from; lane <= to; lane++) {
                for (int layer = 0; layer < StreakField.LAYERS; layer++) {
                    for (int moment = oldest; moment <= latest; moment++) {
                        if (StreakField.at(seed, lane, layer, moment, time, radius, STREAK)) {
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
        // From the eye to the nearest point of the needle: across that and across the needle is its width.
        double nearestZ = Math.max(s.tailZ, Math.min(s.headZ, camZ));
        double sx = s.x - camX;
        double sy = s.y - camY;
        double sz = nearestZ - camZ;
        double side = Math.sqrt(sx * sx + sy * sy);
        double distance = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (side < 1e-3) {
            return;
        }
        // One that passes right by the eye is dimmed: a needle a hand from the eye is a white wall.
        float near = 0.1F + 0.9F * Draw.clamp01(((float) distance - 1.0F) / 2.5F);
        float bright = s.shown * near * (0.5F + 0.5F * s.rush);
        float width = (float) Math.max(WIDTH * (1 + s.rush), distance * 0.0018);
        double wx = -sy / side * width;
        double wy = sx / side * width;
        if (head) {
            Draw.glow(t, s.x, s.y, s.headZ, 0.2F + 0.3F * s.rush, WHITE, bright);
            Draw.glow(t, s.x, s.y, s.headZ, 0.7F + 0.6F * s.rush, PALE, 0.4F * bright);
            return;
        }
        float mid = s.tailZ + (s.headZ - s.tailZ) * 0.72F;
        // Its glow, then itself: nothing at the tail, widening to the head.
        taper(t, s, mid, wx * 3.4, wy * 3.4, DEEP, PALE, PALE, 0.22F * bright);
        taper(t, s, mid, wx, wy, DEEP, PALE, WHITE, bright);
        if (s.rush > 0.15F) {
            // Hurried light splits: a thread of each colour, a little to either side.
            float split = s.rush * bright * 0.55F;
            thread(t, s, wx * 2.2, wy * 2.2, wx * 0.5, wy * 0.5, CYAN, split);
            thread(t, s, -wx * 2.2, -wy * 2.2, wx * 0.5, wy * 0.5, VIOLET, split);
        }
    }

    /** The needle in two lengths: from nothing at the tail to most of its width, then to all of it at the head. */
    private static void taper(Tessellator t, StreakField.Streak s, float mid, double wx, double wy, int tail,
        int middle, int head, float alpha) {
        Draw.color(t, tail, 0);
        t.addVertex(s.x, s.y, s.tailZ);
        t.addVertex(s.x, s.y, s.tailZ);
        Draw.color(t, middle, alpha * 0.55F);
        t.addVertex(s.x + wx * 0.6, s.y + wy * 0.6, mid);
        t.addVertex(s.x - wx * 0.6, s.y - wy * 0.6, mid);
        t.addVertex(s.x - wx * 0.6, s.y - wy * 0.6, mid);
        t.addVertex(s.x + wx * 0.6, s.y + wy * 0.6, mid);
        Draw.color(t, head, alpha);
        t.addVertex(s.x + wx, s.y + wy, s.headZ);
        t.addVertex(s.x - wx, s.y - wy, s.headZ);
    }

    /** A thread of one colour beside the needle, over its leading half. */
    private static void thread(Tessellator t, StreakField.Streak s, double ox, double oy, double wx, double wy,
        int color, float alpha) {
        float from = s.tailZ + (s.headZ - s.tailZ) * 0.5F;
        Draw.color(t, color, 0);
        t.addVertex(s.x + ox, s.y + oy, from);
        t.addVertex(s.x + ox, s.y + oy, from);
        Draw.color(t, color, alpha);
        t.addVertex(s.x + ox + wx, s.y + oy + wy, s.headZ);
        t.addVertex(s.x + ox - wx, s.y + oy - wy, s.headZ);
    }
}
