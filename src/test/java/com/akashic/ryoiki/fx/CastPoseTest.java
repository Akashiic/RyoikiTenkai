package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CastPoseTest {

    @Test
    void aGestureComesInAndLetsGo() {
        for (int kind : new int[] { CastPose.PULL, CastPose.CUT }) {
            assertEquals(0, CastPose.layer(kind, 0), 0);
            assertEquals(0, CastPose.layer(kind, CastPose.ticks(kind)), 0);
            assertEquals(1, CastPose.layer(kind, CastPose.ticks(kind) / 2), 1e-6);
            // It never jumps: no two instants a twentieth of a tick apart differ by much, in the pose or its weight.
            float[] last = CastPose.sample(kind, 0);
            float lastLayer = 0;
            for (float age = 0.05F; age <= CastPose.ticks(kind); age += 0.05F) {
                float[] now = CastPose.sample(kind, age);
                for (int i = 0; i < now.length; i++) {
                    assertTrue(Math.abs(now[i] - last[i]) < 6, "kind " + kind + " jumps at " + age);
                }
                assertTrue(Math.abs(CastPose.layer(kind, age) - lastLayer) < 0.06F);
                last = now;
                lastLayer = CastPose.layer(kind, age);
            }
        }
    }

    @Test
    void thePullThrowsTheHandOutAndDrawsItBack() {
        // Out: the arm level with the shoulder and straight, the chest turned to put that shoulder forward.
        float[] out = CastPose.sample(CastPose.PULL, 6);
        assertEquals(-84, CastPose.angle(out, "RightArm", 0), 3);
        assertTrue(Math.abs(CastPose.angle(out, "RightForeArm", 0)) < 12);
        assertTrue(CastPose.angle(out, "UpBody", 1) < -10);
        // Drawn back: the elbow bent, the upper arm lower.
        float[] in = CastPose.sample(CastPose.PULL, 13);
        assertTrue(CastPose.angle(in, "RightForeArm", 0) < -55);
        assertTrue(CastPose.angle(in, "RightArm", 0) > CastPose.angle(out, "RightArm", 0) + 20);
    }

    @Test
    void theCutGoesFromTheFarShoulderDownAndAcross() {
        float[] wound = CastPose.sample(CastPose.CUT, 2.5F);
        float[] struck = CastPose.sample(CastPose.CUT, 6);
        // Up and across the body, then low and out to its own side.
        assertTrue(CastPose.angle(wound, "RightArm", 0) < -120 && CastPose.angle(wound, "RightArm", 2) < -20);
        assertTrue(CastPose.angle(struck, "RightArm", 0) > -35 && CastPose.angle(struck, "RightArm", 2) > 25);
        // The chest winds one way and unwinds the other.
        assertTrue(CastPose.angle(wound, "UpBody", 1) < -18 && CastPose.angle(struck, "UpBody", 1) > 18);
    }

    @Test
    void theEyesStayOnWhatTheHandDoes() {
        for (int kind : new int[] { CastPose.PULL, CastPose.CUT }) {
            for (float age = 0; age <= CastPose.ticks(kind); age += 0.5F) {
                float[] p = CastPose.sample(kind, age);
                float turn = CastPose.angle(p, "UpBody", 1) + CastPose.angle(p, "AllHead", 1);
                assertTrue(Math.abs(turn) < 4, "the head is carried off by the chest at " + age);
            }
        }
    }
}
