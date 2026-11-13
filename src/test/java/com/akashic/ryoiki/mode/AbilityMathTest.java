package com.akashic.ryoiki.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbilityMathTest {

    @Test
    void thePointDrawsInAndNeverThrowsPast() {
        double[] v = new double[3];
        // From the edge of its reach: straight at it, at the fastest.
        AbilityMath.pull(AbilityMath.ORB_RADIUS, 0, 0, v);
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
        double x = AbilityMath.ORB_RADIUS;
        double[] v = new double[3];
        int arrived = -1;
        double last = x;
        for (int tick = 1; tick <= AbilityMath.ORB_TICKS; tick++) {
            AbilityMath.pull(-x, 0, 0, v);
            x += v[0];
            assertTrue(x <= last && x >= 0, "closer every tick, never past");
            last = x;
            if (arrived < 0 && x < 0.7) {
                arrived = tick;
            }
        }
        assertTrue(arrived > 0 && arrived <= 14, "arrived at tick " + arrived);
    }

    @Test
    void theOrbGoesAfterTheEyesNoFasterThanItCan() {
        double[] v = new double[3];
        // Near: all the way there. Far: a step of its speed, straight at it.
        AbilityMath.follow(1, 2, 3, 1.3, 2, 3.4, v);
        assertEquals(0.3, v[0], 1e-9);
        assertEquals(0.4, v[2], 1e-9);
        AbilityMath.follow(0, 0, 0, 30, 0, 40, v);
        assertEquals(AbilityMath.ORB_SPEED, Math.sqrt(v[0] * v[0] + v[2] * v[2]), 1e-9);
        assertEquals(0.75, v[0] / v[2], 1e-9);
        // Whoever it holds can keep up with it at its fastest only for a while: a hard swing leaves them behind.
        assertTrue(AbilityMath.PULL_FASTEST < AbilityMath.ORB_SPEED);
    }

    @Test
    void whoTheOrbLetsGoLeavesWithItsSpeed() {
        double[] v = new double[3];
        AbilityMath.thrown(0.4, 0, 0, v);
        assertEquals(0.4 * AbilityMath.ORB_THROW, v[0], 1e-9);
        assertEquals(AbilityMath.ORB_THROW_LIFT, v[1], 1e-9);
        // Never more than the most, whichever way.
        AbilityMath.thrown(3, 0, -4, v);
        assertEquals(AbilityMath.ORB_THROW_MOST, Math.sqrt(v[0] * v[0] + v[2] * v[2]), 1e-9);
        // At rest it only sets them down.
        AbilityMath.thrown(0, 0, 0, v);
        assertEquals(0, v[0], 0);
        assertEquals(AbilityMath.ORB_THROW_LIFT, v[1], 0);
    }

    @Test
    void onTheGroundTheOrbKeepsTheHeightOfABody() {
        assertEquals(64 + AbilityMath.ORB_LOW, AbilityMath.grounded(60, 64), 0);
        assertEquals(64 + AbilityMath.ORB_HIGH, AbilityMath.grounded(80, 64), 0);
        assertEquals(65.5, AbilityMath.grounded(65.5, 64), 0);
    }

    @Test
    void theRedOrbHarmsAndThrowsLessTheFartherItFlies() {
        assertEquals(6, AbilityMath.redDamage(6, 0), 1e-6);
        assertEquals(6 * AbilityMath.RED_FAR, AbilityMath.redDamage(6, AbilityMath.RED_RANGE), 1e-6);
        assertEquals(6 * AbilityMath.RED_FAR, AbilityMath.redDamage(6, 500), 1e-6);
        assertEquals(AbilityMath.RED_THROW, AbilityMath.redThrow(0), 1e-9);
        assertEquals(AbilityMath.RED_THROW_FAR, AbilityMath.redThrow(AbilityMath.RED_RANGE), 1e-9);
        float last = 7;
        for (double d = 0; d <= AbilityMath.RED_RANGE; d += 0.5) {
            float now = AbilityMath.redDamage(6, d);
            assertTrue(now < last && now > 0);
            last = now;
        }
        // It crosses its whole range in under half a second.
        assertEquals(AbilityMath.RED_RANGE, AbilityMath.redGone(10, AbilityMath.RED_RANGE), 0);
        assertEquals(AbilityMath.RED_SPEED * 3, AbilityMath.redGone(3, AbilityMath.RED_RANGE), 1e-9);
    }

    @Test
    void fromTheGroundTheRedOrbFliesNearlyLevel() {
        double[] v = new double[3];
        // Aimed level, or a little up: where it is aimed.
        AbilityMath.level(1, 0, 0, v);
        assertEquals(1, v[0], 1e-9);
        assertEquals(0, v[1], 1e-9);
        // Aimed steeply up or down: the same way ahead, no steeper than it may be, and still a unit vector.
        for (double pitch : new double[] { 60, -45, 89 }) {
            double r = Math.toRadians(pitch);
            AbilityMath.level(Math.cos(r) * 0.6, Math.sin(r), Math.cos(r) * 0.8, v);
            assertEquals(Math.signum(pitch) * AbilityMath.RED_LEVEL, v[1], 1e-9);
            assertEquals(1, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]), 1e-9);
            assertEquals(0.75, v[0] / v[2], 1e-9);
        }
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
