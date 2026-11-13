package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlashFieldTest {

    private static final int R = 30;

    @Test
    void cutsComeInFlurries() {
        assertTrue(SlashField.flurryStartsAt(0));
        assertTrue(SlashField.flurryStartsAt(SlashField.PERIOD * 5));
        assertFalse(SlashField.flurryStartsAt(SlashField.PERIOD * 5 + 1));
        // Three cuts two ticks apart, and the next flurry right after.
        assertEquals(0, SlashField.born(0));
        assertEquals(2, SlashField.born(1));
        assertEquals(4, SlashField.born(2));
        assertEquals(SlashField.PERIOD, SlashField.born(3));
        assertEquals(0, SlashField.index(1));
        assertEquals(1, SlashField.index(2));
        assertEquals(2, SlashField.index(4));
        assertEquals(3, SlashField.index(SlashField.PERIOD));
        assertEquals(4, SlashField.index(SlashField.PERIOD + 2));
        for (int tick = 0; tick < 200; tick++) {
            assertTrue(SlashField.born(SlashField.index(tick)) <= tick);
            assertTrue(SlashField.born(SlashField.index(tick) + 1) > tick);
        }
    }

    @Test
    void aCutAndItsScarLastTheirTime() {
        SlashField.Slash s = new SlashField.Slash();
        int born = SlashField.born(7);
        assertTrue(SlashField.at(9, 7, born, R, s));
        assertEquals(0, s.age);
        assertEquals(7, s.index);
        assertTrue(SlashField.at(9, 7, born + SlashField.SCAR - 0.5F, R, s));
        assertFalse(SlashField.at(9, 7, born + SlashField.SCAR, R, s));
        assertFalse(SlashField.at(9, 7, born - 0.1F, R, s));
        assertFalse(SlashField.at(9, -1, 0, R, s));
        // The blade is across in a little over two ticks, fastest as it starts.
        assertEquals(0, SlashField.swept(0));
        assertEquals(1, SlashField.swept(SlashField.SWEEP));
        assertTrue(SlashField.swept(SlashField.SWEEP / 2) > 0.8F);
    }

    @Test
    void everyClientSeesTheSameCutAndEachCutIsItsOwn() {
        SlashField.Slash a = new SlashField.Slash();
        SlashField.Slash b = new SlashField.Slash();
        SlashField.at(4, 20, SlashField.born(20) + 1, R, a);
        SlashField.at(4, 20, SlashField.born(20) + 3, R, b);
        assertEquals(a.midX, b.midX);
        assertEquals(a.vz, b.vz);
        assertEquals(a.radius, b.radius);
        SlashField.at(4, 21, SlashField.born(21) + 1, R, b);
        assertNotEquals(a.ux, b.ux);
        SlashField.at(5, 20, SlashField.born(20) + 1, R, b);
        assertNotEquals(a.midX, b.midX);
        assertEquals(SlashField.chance(20, 3), SlashField.chance(20, 3));
        assertNotEquals(SlashField.chance(20, 3), SlashField.chance(20, 4));
    }

    @Test
    void theCutsOfAFlurryCrossAtOnePlace() {
        SlashField.Slash a = new SlashField.Slash();
        SlashField.Slash b = new SlashField.Slash();
        for (int flurry = 0; flurry < 60; flurry++) {
            for (int which = 1; which < SlashField.FLURRY; which++) {
                assertTrue(SlashField.at(6, flurry * SlashField.FLURRY, flurry * SlashField.PERIOD, R, a));
                int index = flurry * SlashField.FLURRY + which;
                assertTrue(SlashField.at(6, index, SlashField.born(index), R, b));
                // Within a few blocks of each other on the floor.
                assertTrue(Math.hypot(a.midX - b.midX, a.midZ - b.midZ) < 3.5, "flurry " + flurry);
            }
        }
    }

    @Test
    void anArcIsAPieceOfACircleInItsPlaneClearOfTheFloorAndInsideTheArena() {
        SlashField.Slash s = new SlashField.Slash();
        float[] p = new float[3];
        float[] q = new float[3];
        float[] h = new float[3];
        for (int i = 0; i < 400; i++) {
            assertTrue(SlashField.at(5, i, SlashField.born(i) + 1, R, s));
            // The two directions of its plane are units, square to each other.
            assertEquals(1, Math.sqrt(s.ux * s.ux + s.uy * s.uy + s.uz * s.uz), 1e-4);
            assertEquals(1, Math.sqrt(s.vx * s.vx + s.vy * s.vy + s.vz * s.vz), 1e-4);
            assertEquals(0, s.ux * s.vx + s.uy * s.vy + s.uz * s.vz, 1e-4);
            assertTrue(s.radius >= 3.5F && s.radius <= 7.5F);
            // Its middle is where it says.
            SlashField.point(s, 0.5F, 0, p);
            assertEquals(s.midX, p[0], 1e-4);
            assertEquals(s.midY, p[1], 1e-4);
            // Every point of it is a radius from one centre, above the floor and inside the walls.
            float cx = s.midX - s.vx * s.radius;
            float cy = s.midY - s.vy * s.radius;
            float cz = s.midZ - s.vz * s.radius;
            for (float f = 0; f <= 1; f += 0.125F) {
                SlashField.point(s, f, 0, p);
                assertEquals(s.radius, Math.sqrt(sq(p[0] - cx) + sq(p[1] - cy) + sq(p[2] - cz)), 1e-3);
                assertTrue(p[1] >= 0.3F - 1e-3F, "cut " + i + " at " + f + ": " + p[1]);
                assertTrue(Math.hypot(p[0], p[2]) < R, "cut " + i);
                // The inner edge is nearer the centre by the inset.
                SlashField.point(s, f, 0.5F, q);
                assertEquals(s.radius - 0.5, Math.sqrt(sq(q[0] - cx) + sq(q[1] - cy) + sq(q[2] - cz)), 1e-3);
                // The blade's way is along the arc: square to the line from the centre.
                SlashField.heading(s, f, h);
                assertEquals(1, Math.sqrt(sq(h[0]) + sq(h[1]) + sq(h[2])), 1e-4);
                assertEquals(0, h[0] * (p[0] - cx) + h[1] * (p[1] - cy) + h[2] * (p[2] - cz), 1e-3);
            }
            assertTrue(s.midY < 10, "cut " + i + " hangs at " + s.midY);
            // Not in the middle of the arena, where the hall would hide it.
            assertTrue(Math.hypot(s.midX, s.midZ) > 8.5, "cut " + i + " is in the hall");
        }
    }

    private static double sq(double v) {
        return v * v;
    }
}
