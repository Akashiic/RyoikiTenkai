package com.akashic.ryoiki.client;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.ScratchField;

/**
 * The small cuts in the air of the crimson shrine ({@link ScratchField}), drawn: a black stroke, thin, widest in its
 * middle and pointed at both ends, turned to face the eye, with a hair of pale red along one side so that it shows
 * against the dark as well as against the light. Only the cells about the camera are drawn: further off a stroke
 * this thin is nothing.
 */
final class ScratchFx {

    private static final int BLACK = 0x000000;
    private static final int RIM = 0xFF7A68;
    /** Blocks from the camera that marks are drawn to, and the nearest one may be: nearer is a bar across the eye. */
    private static final float FAR = 24;
    private static final float NEAR = 1.6F;
    private static final float WIDTH = 0.028F;
    private static final ScratchField.Scratch SCRATCH = new ScratchField.Scratch();

    private ScratchFx() {}

    /**
     * @param time ticks since the domain opened, with fractions
     * @param camX the camera, relative to the arena's centre, in the shrine's scene
     */
    static void draw(Tessellator t, int radius, float time, int seed, double camX, double camY, double camZ) {
        int reach = (int) Math.ceil(FAR / ScratchField.CELL);
        int cx = (int) Math.floor(camX / ScratchField.CELL);
        int cz = (int) Math.floor(camZ / ScratchField.CELL);
        float inside = (radius - 1.5F) * (radius - 1.5F);
        GL11.glDepthMask(false);
        // Black is laid over what is there; the rim is light, added.
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0) {
                Draw.translucent();
            } else {
                Draw.additive();
            }
            t.startDrawingQuads();
            for (int x = cx - reach; x <= cx + reach; x++) {
                for (int z = cz - reach; z <= cz + reach; z++) {
                    float midX = (x + 0.5F) * ScratchField.CELL;
                    float midZ = (z + 0.5F) * ScratchField.CELL;
                    if (midX * midX + midZ * midZ > inside) {
                        continue;
                    }
                    for (int y = 0; y < ScratchField.LAYERS; y++) {
                        if (ScratchField.at(seed, x, y, z, time, SCRATCH)) {
                            stroke(t, pass == 1, camX, camY, camZ);
                        }
                    }
                }
            }
            t.draw();
        }
        GL11.glDepthMask(true);
    }

    private static void stroke(Tessellator t, boolean rim, double camX, double camY, double camZ) {
        ScratchField.Scratch s = SCRATCH;
        float drawn = ScratchField.drawn(s.age);
        float shown = ScratchField.shown(s.age);
        // As it goes it leaves from the end it began at.
        float gone = 1 - shown;
        double ax = s.fromX + (s.toX - s.fromX) * gone * 0.6F;
        double ay = s.fromY + (s.toY - s.fromY) * gone * 0.6F;
        double az = s.fromZ + (s.toZ - s.fromZ) * gone * 0.6F;
        double bx = s.fromX + (s.toX - s.fromX) * drawn;
        double by = s.fromY + (s.toY - s.fromY) * drawn;
        double bz = s.fromZ + (s.toZ - s.fromZ) * drawn;
        double mx = (ax + bx) / 2;
        double my = (ay + by) / 2;
        double mz = (az + bz) / 2;
        double sx = mx - camX;
        double sy = my - camY;
        double sz = mz - camZ;
        double distance = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (distance < NEAR || distance > FAR) {
            return;
        }
        // Across the stroke and across the line of sight.
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double wx = dy * sz - dz * sy;
        double wy = dz * sx - dx * sz;
        double wz = dx * sy - dy * sx;
        double length = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (length < 1e-6) {
            return;
        }
        // Thin, but never thinner than the eye can see at its distance; and fainter toward the far end of sight.
        double width = Math.max(WIDTH, distance * 0.0032) / length;
        wx *= width;
        wy *= width;
        wz *= width;
        float fade = shown * Draw.clamp01((FAR - (float) distance) / 6F);
        if (rim) {
            // A hair of light along one side, half as wide again: two slivers, from each end to the middle.
            for (int end = 0; end < 2; end++) {
                Draw.color(t, RIM, 0);
                t.addVertex(end == 0 ? ax : bx, end == 0 ? ay : by, end == 0 ? az : bz);
                Draw.color(t, RIM, 0.32F * fade);
                t.addVertex(mx + wx, my + wy, mz + wz);
                t.addVertex(mx + wx * 1.5, my + wy * 1.5, mz + wz * 1.5);
                t.addVertex(mx + wx * 1.5, my + wy * 1.5, mz + wz * 1.5);
            }
            return;
        }
        // A sliver: pointed at both ends, widest in the middle.
        Draw.color(t, BLACK, 0.92F * fade);
        t.addVertex(ax, ay, az);
        t.addVertex(mx + wx, my + wy, mz + wz);
        t.addVertex(bx, by, bz);
        t.addVertex(mx - wx, my - wy, mz - wz);
    }
}
