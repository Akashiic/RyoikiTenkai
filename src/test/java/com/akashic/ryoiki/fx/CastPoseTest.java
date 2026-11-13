package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CastPoseTest {

    @Test
    void aGestureComesInAndLetsGo() {
        for (int kind : new int[] { CastPose.PULL, CastPose.CUT }) {
            float ticks = kind == CastPose.CUT ? CastPose.CUT_TICKS : 76;
            assertEquals(0, CastPose.layer(0, ticks), 0);
            assertEquals(0, CastPose.layer(ticks, ticks), 0);
            assertEquals(1, CastPose.layer(ticks / 2, ticks), 1e-6);
            // It never jumps: no two instants a twentieth of a tick apart differ by much, in the pose or its weight.
            float[] last = CastPose.sample(kind, 0, ticks);
            float lastLayer = 0;
            for (float age = 0.05F; age <= ticks; age += 0.05F) {
                float[] now = CastPose.sample(kind, age, ticks);
                for (int i = 0; i < now.length; i++) {
                    assertTrue(Math.abs(now[i] - last[i]) < 6, "kind " + kind + " jumps at " + age);
                }
                assertTrue(Math.abs(CastPose.layer(age, ticks) - lastLayer) < 0.06F);
                last = now;
                lastLayer = CastPose.layer(age, ticks);
            }
        }
    }

    @Test
    void thePullHoldsTheHandOutAndDrawsItBackAtTheEnd() {
        // Out, for as long as the orb is guided: the arm level with the shoulder and straight, the chest turned to
        // put that shoulder forward.
        float[] out = CastPose.sample(CastPose.PULL, 6, 76);
        float[] still = CastPose.sample(CastPose.PULL, 60, 76);
        assertEquals(CastPose.angle(out, "RightArm", 0), CastPose.angle(still, "RightArm", 0), 0.5);
        assertEquals(-84, CastPose.angle(out, "RightArm", 0), 3);
        assertTrue(Math.abs(CastPose.angle(out, "RightForeArm", 0)) < 12);
        assertTrue(CastPose.angle(out, "UpBody", 1) < -10);
        // Drawn back: the elbow bent, the upper arm lower.
        float[] in = CastPose.sample(CastPose.PULL, 75.5F, 76);
        assertTrue(CastPose.angle(in, "RightForeArm", 0) < -55);
        assertTrue(CastPose.angle(in, "RightArm", 0) > CastPose.angle(out, "RightArm", 0) + 20);
    }

    @Test
    void theRedIsHeldOverheadWhileItGathersAndThrownDownToTheLine() {
        float ticks = 24 + CastPose.RED_AFTER;
        float[] gathered = CastPose.sample(CastPose.RED, 20, ticks);
        float[] thrown = CastPose.sample(CastPose.RED, 24 + 9, ticks);
        // Gathered: the arm nearly straight up, the elbow hardly bent, that shoulder back.
        assertTrue(CastPose.angle(gathered, "RightArm", 0) < -150);
        assertTrue(CastPose.angle(gathered, "RightForeArm", 0) > -30);
        assertTrue(CastPose.angle(gathered, "UpBody", 1) > 6);
        // Thrown: the arm level and straight, that shoulder forward.
        assertEquals(-88, CastPose.angle(thrown, "RightArm", 0), 6);
        assertTrue(Math.abs(CastPose.angle(thrown, "RightForeArm", 0)) < 8);
        assertTrue(CastPose.angle(thrown, "UpBody", 1) < -12);
        // It points where the eyes look: higher looking up, lower looking down, and only so far.
        float level = CastPose.angle(thrown, "RightArm", 0);
        float high = CastPose.angle(CastPose.sample(CastPose.RED, 24 + 9, ticks, -40), "RightArm", 0);
        float low = CastPose.angle(CastPose.sample(CastPose.RED, 24 + 9, ticks, 30), "RightArm", 0);
        assertEquals(level - 34, high, 1);
        assertEquals(level + 25.5, low, 1);
        assertEquals(
            CastPose.angle(CastPose.sample(CastPose.RED, 24 + 9, ticks, -90), "RightArm", 0),
            CastPose.angle(CastPose.sample(CastPose.RED, 24 + 9, ticks, -70), "RightArm", 0),
            1e-4);
        // No jump anywhere in it.
        float[] last = CastPose.sample(CastPose.RED, 0, ticks);
        for (float age = 0.05F; age <= ticks; age += 0.05F) {
            float[] now = CastPose.sample(CastPose.RED, age, ticks);
            for (int i = 0; i < now.length; i++) {
                assertTrue(Math.abs(now[i] - last[i]) < 6, "jumps at " + age);
            }
            last = now;
        }
    }

    @Test
    void theCutGoesFromTheFarShoulderDownAndAcross() {
        float[] wound = CastPose.sample(CastPose.CUT, 2.5F, CastPose.CUT_TICKS);
        float[] struck = CastPose.sample(CastPose.CUT, 6, CastPose.CUT_TICKS);
        // Up and across the body, then low and out to its own side.
        assertTrue(CastPose.angle(wound, "RightArm", 0) < -120 && CastPose.angle(wound, "RightArm", 2) < -20);
        assertTrue(CastPose.angle(struck, "RightArm", 0) > -35 && CastPose.angle(struck, "RightArm", 2) > 25);
        // The chest winds one way and unwinds the other.
        assertTrue(CastPose.angle(wound, "UpBody", 1) < -18 && CastPose.angle(struck, "UpBody", 1) > 18);
    }

    @Test
    void theEyesStayOnWhatTheHandDoes() {
        for (int kind : new int[] { CastPose.PULL, CastPose.CUT }) {
            for (float age = 0; age <= CastPose.CUT_TICKS; age += 0.5F) {
                float[] p = CastPose.sample(kind, age, CastPose.CUT_TICKS);
                float turn = CastPose.angle(p, "UpBody", 1) + CastPose.angle(p, "AllHead", 1);
                assertTrue(Math.abs(turn) < 4, "the head is carried off by the chest at " + age);
            }
        }
    }
}
