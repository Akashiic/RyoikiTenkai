package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlowPoseTest {

    private static final int X = 0;
    private static final int Y = 1;

    @Test
    void aBlowStartsAndEndsInTheGuard() {
        assertArrayEquals(BlowPose.guard(), BlowPose.sample(1, 0), 1e-4F);
        assertArrayEquals(BlowPose.guard(), BlowPose.sample(1, BlowPose.TICKS), 1e-4F);
        assertArrayEquals(BlowPose.guard(), BlowPose.sample(1, 40), 1e-4F);
    }

    @Test
    void everyBlowOfTheChainStartsAndEndsInTheGuard() {
        for (int blow = 1; blow <= 4; blow++) {
            assertArrayEquals(BlowPose.guard(), BlowPose.sample(blow, 0), 1e-4F, "blow " + blow);
            assertArrayEquals(BlowPose.guard(), BlowPose.sample(blow, BlowPose.TICKS), 1e-4F, "blow " + blow);
        }
        assertTrue(BlowPose.has(1));
    }

    @Test
    void theHeadStaysOnTheTargetAndTheSolesOnTheGround() {
        for (float t = 0; t <= BlowPose.TICKS; t += 0.25F) {
            float[] p = BlowPose.sample(1, t);
            float body = BlowPose.angle(p, "AllBody", Y) + BlowPose.angle(p, "UpBody", Y);
            // The head takes back nearly all of the body's turn: it looks down the line within a few degrees.
            assertTrue(Math.abs(body + BlowPose.angle(p, "AllHead", Y)) < 3.5F, "head at " + t);
            // The lead foot points down the line and its sole is level.
            assertEquals(0, BlowPose.angle(p, "AllBody", Y) + BlowPose.angle(p, "LeftFoot", Y), 1e-3F);
            assertEquals(
                0,
                BlowPose.angle(p, "LeftLeg", X) + BlowPose.angle(p, "LeftLowerLeg", X)
                    + BlowPose.angle(p, "LeftFoot", X),
                1e-3F);
            // The rear heel is never below the ground.
            float heel = BlowPose.angle(p, "RightLeg", X) + BlowPose.angle(p, "RightLowerLeg", X)
                + BlowPose.angle(p, "RightFoot", X);
            assertTrue(heel >= 8 - 1e-3F && heel <= 26 + 1e-3F, "heel at " + t);
        }
    }

    @Test
    void theStrikePassesFromTheHipsToTheChestToTheArm() {
        float[] guard = BlowPose.guard();
        // Half way out, in time: the hips first, the chest next, the arm last.
        float hips = firstBeyondHalf("AllBody", Y, guard);
        float chest = firstBeyondHalf("UpBody", Y, guard);
        float arm = firstBeyondHalf("RightArm", X, guard);
        assertTrue(hips < chest && chest < arm + 0.3F, hips + " " + chest + " " + arm);
        // At the impact the arm is out along the line, at the height of the shoulder.
        float[] p = BlowPose.sample(1, BlowPose.IMPACT);
        assertEquals(-84, BlowPose.angle(p, "RightArm", X), 0.5F);
        assertEquals(-5, BlowPose.angle(p, "RightForeArm", X), 0.5F);
    }

    @Test
    void theFistGoesInAStraightLine() {
        float[] from = fistOf(BlowPose.sample(1, 0));
        float[] to = fistOf(BlowPose.sample(1, BlowPose.IMPACT));
        double length = Math.hypot(to[0] - from[0], to[1] - from[1]);
        assertTrue(length > 0.8, "the fist travels");
        // From when the gathering is gone to the impact: never off the line by more than a hundredth of an arm.
        for (float t = 2.4F; t <= BlowPose.IMPACT; t += 0.05F) {
            float[] f = fistOf(BlowPose.sample(1, t));
            double off = Math.abs((to[0] - from[0]) * (f[1] - from[1]) - (to[1] - from[1]) * (f[0] - from[0])) / length;
            assertTrue(off < 0.02, "off the line by " + off + " at " + t);
        }
        // And the solving gives back the angles it is asked for.
        float[] angles = BlowPose.reach(BlowPose.fist(-34, -122)[0], BlowPose.fist(-34, -122)[1]);
        assertEquals(-34, angles[0], 0.01F);
        assertEquals(-122, angles[1], 0.01F);
    }

    private static float[] fistOf(float[] pose) {
        return BlowPose.fist(BlowPose.angle(pose, "RightArm", X), BlowPose.angle(pose, "RightForeArm", X));
    }

    @Test
    void nothingJumps() {
        float[] before = BlowPose.sample(1, 0);
        for (float t = 0.05F; t <= BlowPose.TICKS; t += 0.05F) {
            float[] p = BlowPose.sample(1, t);
            for (int i = 0; i < p.length; i++) {
                // The fastest part, the forearm, opens some 120 degrees in a tick and a half, fastest as it leaves:
                // under 12 degrees in a twentieth of a tick.
                assertTrue(Math.abs(p[i] - before[i]) < 12.5F, BlowPose.BONES[i / 3] + " at " + t);
            }
            before = p;
        }
    }

    private static float firstBeyondHalf(String bone, int axis, float[] guard) {
        float from = BlowPose.angle(guard, bone, axis);
        float to = BlowPose.angle(BlowPose.sample(1, BlowPose.IMPACT), bone, axis);
        for (float t = 0; t <= BlowPose.IMPACT; t += 0.02F) {
            float now = BlowPose.angle(BlowPose.sample(1, t), bone, axis);
            if (Math.abs(now - from) >= Math.abs(to - from) / 2) {
                return t;
            }
        }
        return Float.MAX_VALUE;
    }
}
