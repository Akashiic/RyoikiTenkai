package com.akashic.ryoiki.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbilityMathTest {

    @Test
    void thePointDrawsInAndNeverThrowsPast() {
        double[] v = new double[3];
        // From the edge of its reach: straight at it, at the fastest.
        AbilityMath.pull(AbilityMath.PULL_RADIUS, 0, 0, v);
        assertEquals(AbilityMath.PULL_FASTEST, v[0], 1e-9);
        assertEquals(0, v[1], 1e-9);
        assertEquals(0, v[2], 1e-9);
        // From any side, the pull points at the point and is never longer than the way left.
        for (double[] d : new double[][] { { 3, 1, -2 }, { -0.5, 0.2, 0.4 }, { 0, -3.9, 0 }, { 1, 1, 1 } }) {
            AbilityMath.pull(d[0], d[1], d[2], v);
            double distance = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            double speed = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            assertTrue(speed <= distance && speed <= AbilityMath.PULL_FASTEST + 1e-9);
            assertEquals(1, (v[0] * d[0] + v[1] * d[1] + v[2] * d[2]) / (speed * distance), 1e-9);
        }
        // At the point there is nowhere to go.
        AbilityMath.pull(0.1, 0.1, -0.1, v);
        assertEquals(0, Math.abs(v[0]) + Math.abs(v[1]) + Math.abs(v[2]), 0);
    }

    @Test
    void whoIsPulledArrivesWithinThePull() {
        // Someone at the edge of the reach who does nothing about it is at the point before the pull is over.
        double x = AbilityMath.PULL_RADIUS;
        double[] v = new double[3];
        int arrived = -1;
        double last = x;
        for (int tick = 1; tick <= AbilityMath.PULL_TICKS; tick++) {
            AbilityMath.pull(-x, 0, 0, v);
            x += v[0];
            assertTrue(x <= last && x >= 0, "closer every tick, never past");
            last = x;
            if (arrived < 0 && x < 0.6) {
                arrived = tick;
            }
        }
        assertTrue(arrived > 0 && arrived <= 14, "arrived at tick " + arrived);
    }

    @Test
    void aFlyingCutCoversItsRangeAtItsSpeed() {
        assertEquals(0, AbilityMath.cutGone(0, 12), 0);
        assertEquals(AbilityMath.CUT_SPEED * 3, AbilityMath.cutGone(3, 12), 1e-9);
        assertEquals(12, AbilityMath.cutGone(100, 12), 0);
        assertEquals(4.5, AbilityMath.cutGone(100, 4.5), 0);
        assertEquals(8, AbilityMath.cutTicks(AbilityMath.CUT_RANGE));
        assertEquals(1, AbilityMath.cutTicks(0.2));
    }

    @Test
    void aLineMeetsABoxWhereItEntersIt() {
        // Straight at a box 5 away.
        assertEquals(5, AbilityMath.enters(0, 1, 0, 1, 0, 0, 5, 0, -0.5, 6, 2, 0.5), 1e-9);
        // Over it, beside it, or with it behind: never.
        assertEquals(-1, AbilityMath.enters(0, 2.5, 0, 1, 0, 0, 5, 0, -0.5, 6, 2, 0.5), 0);
        assertEquals(-1, AbilityMath.enters(0, 1, 0.6, 1, 0, 0, 5, 0, -0.5, 6, 2, 0.5), 0);
        assertEquals(-1, AbilityMath.enters(0, 1, 0, -1, 0, 0, 5, 0, -0.5, 6, 2, 0.5), 0);
        // From inside: at once.
        assertEquals(0, AbilityMath.enters(5.5, 1, 0, 0, 0, 1, 5, 0, -0.5, 6, 2, 0.5), 0);
        // Aslant: the distance along the line, not along an axis.
        double s = Math.sqrt(0.5);
        assertEquals(5 / s, AbilityMath.enters(0, 0, 0, s, 0, s, 5, -1, 4, 7, 1, 8), 1e-9);
    }
}
