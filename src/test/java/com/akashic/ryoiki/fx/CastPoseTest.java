package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CastPoseTest {

    /** Each kind of gesture, and how long one of it lasts here. */
    private static final float[][] KINDS = { { CastPose.PULL, 76 }, { CastPose.CUT, CastPose.CUT_TICKS },
        { CastPose.RED, 24 + CastPose.RED_AFTER }, { CastPose.READY, 40 }, { CastPose.BLACK, CastPose.BLACK_TICKS } };

    private static float x(float[] pose, String bone) {
        return CastPose.angle(pose, bone, 0);
    }

    /** How far the body leans forward at the shoulders. */
    private static float lean(float[] pose) {
        return x(pose, "AllBody") + x(pose, "UpBody");
    }

    @Test
    void aGestureComesInLetsGoAndNeverJumps() {
        for (float[] kind : KINDS) {
            int k = (int) kind[0];
            float ticks = kind[1];
            assertEquals(0, CastPose.layer(0, ticks), 0);
            assertEquals(0, CastPose.layer(ticks, ticks), 0);
            assertEquals(1, CastPose.layer(ticks / 2, ticks), 1e-6);
            // No two instants a twentieth of a tick apart differ by much, in the pose or in its weight.
            float[] last = CastPose.sample(k, 0, ticks);
            float lastLayer = 0;
            for (float age = 0.05F; age <= ticks; age += 0.05F) {
                float[] now = CastPose.sample(k, age, ticks);
                for (int i = 0; i < now.length; i++) {
                    // (A fist goes from the ribs to the end of the arm in two ticks: that is fast, and still no jump.)
                    assertTrue(
                        Math.abs(now[i] - last[i]) < (k == CastPose.BLACK ? 10 : 7),
                        CastPose.BONES[i / 3] + " of " + k + " jumps at " + age);
                }
                assertTrue(Math.abs(CastPose.layer(age, ticks) - lastLayer) < 0.06F);
                last = now;
                lastLayer = CastPose.layer(age, ticks);
            }
        }
    }

    @Test
    void theHeadStaysOnTheTargetAndTheSolesOnTheGround() {
        for (float[] kind : KINDS) {
            for (float age = 0; age <= kind[1]; age += 0.5F) {
                float[] p = CastPose.sample((int) kind[0], age, kind[1]);
                float turn = CastPose.angle(p, "AllBody", 1) + CastPose.angle(p, "UpBody", 1)
                    + CastPose.angle(p, "AllHead", 1);
                assertTrue(Math.abs(turn) < 5, "the head is carried off by the body at " + age);
                assertTrue(Math.abs(lean(p) + x(p, "AllHead")) < 9, "the head hangs or tips at " + age);
                // The lead foot flat; the rear one flat or on its ball, never dug in.
                assertEquals(0, x(p, "AllBody") + x(p, "LeftLeg") + x(p, "LeftLowerLeg") + x(p, "LeftFoot"), 1e-3);
                assertTrue(x(p, "AllBody") + x(p, "RightLeg") + x(p, "RightLowerLeg") + x(p, "RightFoot") >= -1e-3);
                // And no knee or elbow bends the wrong way.
                assertTrue(x(p, "LeftLowerLeg") >= 0 && x(p, "RightLowerLeg") >= 0);
                assertTrue(x(p, "LeftForeArm") <= 0 && x(p, "RightForeArm") <= 0);
            }
        }
    }

    @Test
    void theOtherHandIsNeverLeftOnTheHip() {
        // A model may rest with a hand on its hip: the elbow bent square, the hand turned in. In a technique that
        // arm hangs or is thrown back, nearly straight, and the hand is as the arm leaves it.
        for (int kind : new int[] { CastPose.PULL, CastPose.RED, CastPose.CUT }) {
            float ticks = kind == CastPose.CUT ? CastPose.CUT_TICKS : 40;
            for (float age = 0; age <= ticks; age += 0.5F) {
                float[] p = CastPose.sample(kind, age, ticks);
                assertTrue(x(p, "LeftForeArm") > -20, "the left elbow bends at " + age + " of " + kind);
                assertArrayEquals(
                    new float[] { 0, 0, 0 },
                    new float[] { x(p, "LeftHand"), CastPose.angle(p, "LeftHand", 1),
                        CastPose.angle(p, "LeftHand", 2) },
                    0);
            }
        }
    }

    @Test
    void thePullHoldsTheHandOutAndDrawsItBackAtTheEnd() {
        float[] out = CastPose.sample(CastPose.PULL, 6, 76);
        float[] still = CastPose.sample(CastPose.PULL, 60, 76);
        // Out, for as long as the orb is guided: the arm level and straight, that shoulder forward.
        assertEquals(-84, x(out, "UpBody") + x(out, "RightArm"), 3);
        assertEquals(x(out, "RightArm"), x(still, "RightArm"), 0.5);
        assertTrue(Math.abs(x(out, "RightForeArm")) < 12);
        assertTrue(CastPose.angle(out, "UpBody", 1) < -10);
        // Drawn back: the elbow bent, the upper arm lower, the hand closed.
        float[] in = CastPose.sample(CastPose.PULL, 75.5F, 76);
        assertTrue(x(in, "RightForeArm") < -55);
        assertTrue(x(in, "RightArm") > x(out, "RightArm") + 20);
        assertTrue(CastPose.angle(in, "right_fg", 0) > 60);
    }

    @Test
    void theRedIsAThrowDrawnBackUnderTheArmAndWhippedForward() {
        float ticks = 24 + CastPose.RED_AFTER;
        float[] gathered = CastPose.sample(CastPose.RED, 20, ticks);
        float[] thrown = CastPose.sample(CastPose.RED, 24 + 9, ticks);
        // Gathered: the body leaning back and wound away, the weight behind, the arm straight up over it.
        assertTrue(lean(gathered) < -15);
        assertTrue(CastPose.angle(gathered, "AllBody", 1) + CastPose.angle(gathered, "UpBody", 1) > 15);
        assertEquals(-180, lean(gathered) + x(gathered, "RightArm"), 8);
        assertTrue(x(gathered, "RightForeArm") > -25);
        assertTrue(x(gathered, "RightLeg") > 5 && x(gathered, "LeftLeg") < -5);
        // Thrown: the body well forward over a bent lead leg and turned through; the arm level, straight, on the
        // line; the other arm thrown back.
        assertTrue(lean(thrown) > 25);
        assertTrue(CastPose.angle(thrown, "AllBody", 1) + CastPose.angle(thrown, "UpBody", 1) < -20);
        assertEquals(-88, lean(thrown) + x(thrown, "RightArm"), 2);
        assertTrue(Math.abs(x(thrown, "RightForeArm")) < 8);
        assertTrue(x(thrown, "LeftLowerLeg") > 25 && x(thrown, "LeftArm") > 40);
        // The arm points where the eyes look, higher and lower, and only so far.
        float high = lean(thrown) + x(CastPose.sample(CastPose.RED, 24 + 9, ticks, -40), "RightArm");
        float low = lean(thrown) + x(CastPose.sample(CastPose.RED, 24 + 9, ticks, 30), "RightArm");
        assertEquals(-88 - 34, high, 2);
        assertEquals(-88 + 25.5, low, 2);
        assertEquals(
            x(CastPose.sample(CastPose.RED, 24 + 9, ticks, -90), "RightArm"),
            x(CastPose.sample(CastPose.RED, 24 + 9, ticks, -70), "RightArm"),
            1e-4);
    }

    @Test
    void theBlackBlowGoesFromTheFistAtTheRibsStraightThroughAndStandsAgain() {
        float[] ready = CastPose.sample(CastPose.READY, 30, 80);
        // Squared up: the striking side behind, the fist closed at the ribs, the other hand out, the knees bent.
        assertTrue(CastPose.angle(ready, "AllBody", 1) + CastPose.angle(ready, "UpBody", 1) > 40);
        assertTrue(x(ready, "RightArm") > 30 && x(ready, "RightForeArm") < -100);
        assertTrue(CastPose.angle(ready, "right_fg", 0) > 60);
        assertTrue(x(ready, "LeftArm") < -50);
        assertTrue(x(ready, "LeftLowerLeg") > 20 && x(ready, "RightLowerLeg") > 20);
        // The blow starts from that stance.
        float[] start = CastPose.sample(CastPose.BLACK, 0, CastPose.BLACK_TICKS);
        for (int i = 0; i < start.length; i++) {
            assertEquals(ready[i], start[i], 3, CastPose.BONES[i / 3]);
        }
        // The hips have turned before the chest, and the chest before the arm is out.
        float[] early = CastPose.sample(CastPose.BLACK, 1, CastPose.BLACK_TICKS);
        assertTrue(CastPose.angle(early, "AllBody", 1) < CastPose.angle(ready, "AllBody", 1) - 15);
        assertTrue(x(early, "RightForeArm") < -100, "the arm has not left yet");
        // At the impact: the arm level and straight on the line, the body turned through and well forward, the
        // lead knee deep, the rear leg long, the other arm thrown back.
        float[] hit = CastPose.sample(CastPose.BLACK, CastPose.BLACK_IMPACT, CastPose.BLACK_TICKS);
        assertEquals(-90, lean(hit) + x(hit, "RightArm"), 2);
        assertTrue(x(hit, "RightForeArm") > -8);
        assertTrue(CastPose.angle(hit, "AllBody", 1) + CastPose.angle(hit, "UpBody", 1) < -45);
        assertTrue(lean(hit) > 30);
        assertTrue(x(hit, "LeftLowerLeg") > 45 && x(hit, "RightLeg") > 28 && x(hit, "RightLowerLeg") < 12);
        assertTrue(x(hit, "LeftArm") > 35);
        // Held there while what it did plays out.
        float[] held = CastPose.sample(CastPose.BLACK, 12, CastPose.BLACK_TICKS);
        assertEquals(x(hit, "RightArm"), x(held, "RightArm"), 3);
        // And standing again at the end.
        float[] end = CastPose.sample(CastPose.BLACK, CastPose.BLACK_TICKS - 2.5F, CastPose.BLACK_TICKS);
        for (int i = 0; i < end.length; i++) {
            assertEquals(0, end[i], 2, CastPose.BONES[i / 3]);
        }
    }

    @Test
    void theBarrageWindsUpPointsAndFlicksOncePerSlash() {
        float ticks = 31;
        assertTrue(x(CastPose.sample(CastPose.BARRAGE, 2.5F, ticks), "RightArm") < -120);
        // Down on who is before it, along the eyes: level, and lower when they look down.
        float[] level = CastPose.sample(CastPose.BARRAGE, 9, ticks, 0);
        assertTrue(x(level, "RightArm") > -100 && x(level, "RightArm") < -60);
        assertTrue(x(CastPose.sample(CastPose.BARRAGE, 9, ticks, 40), "RightArm") > x(level, "RightArm") + 25);
        // One way for one slash, the other for the next.
        float one = CastPose.angle(CastPose.sample(CastPose.BARRAGE, 8, ticks), "RightArm", 2);
        float other = CastPose.angle(CastPose.sample(CastPose.BARRAGE, 10, ticks), "RightArm", 2);
        assertTrue(Math.abs(one - other) > 20);
        float[] last = CastPose.sample(CastPose.BARRAGE, 0, ticks);
        for (float age = 0.05F; age < ticks; age += 0.05F) {
            float[] now = CastPose.sample(CastPose.BARRAGE, age, ticks);
            for (int i = 0; i < now.length; i++) {
                assertTrue(Math.abs(now[i] - last[i]) < 7, CastPose.BONES[i / 3] + " jumps at " + age);
            }
            last = now;
        }
    }

    @Test
    void theFlipIsOneWholeTurnAndThenTheStroke() {
        float ticks = CastPose.FLIP_TICKS + 10;
        assertEquals(0, x(CastPose.sample(CastPose.FLIP, 0, ticks), "AllBody"), 1e-3);
        assertEquals(180, x(CastPose.sample(CastPose.FLIP, CastPose.FLIP_TICKS / 2, ticks), "AllBody"), 1e-3);
        // Tucked in the middle of it.
        assertTrue(x(CastPose.sample(CastPose.FLIP, CastPose.FLIP_TICKS / 2, ticks), "RightLowerLeg") > 70);
        // Made: upright, and the stroke of the cut has come down.
        float[] after = CastPose.sample(CastPose.FLIP, CastPose.FLIP_TICKS + 3, ticks);
        assertEquals(0, x(after, "AllBody"), 1e-3);
        assertTrue(x(after, "RightArm") > -35 && CastPose.angle(after, "RightArm", 2) > 25);
        // Through the turn itself nothing jumps.
        float[] last = CastPose.sample(CastPose.FLIP, 0, ticks);
        for (float age = 0.05F; age < CastPose.FLIP_TICKS - 0.05F; age += 0.05F) {
            float[] now = CastPose.sample(CastPose.FLIP, age, ticks);
            for (int i = 0; i < now.length; i++) {
                assertTrue(Math.abs(now[i] - last[i]) < 7, CastPose.BONES[i / 3] + " jumps at " + age);
            }
            last = now;
        }
    }

    @Test
    void theCutGoesFromTheFarShoulderDownAndAcross() {
        float[] wound = CastPose.sample(CastPose.CUT, 2.5F, CastPose.CUT_TICKS);
        float[] struck = CastPose.sample(CastPose.CUT, 6, CastPose.CUT_TICKS);
        // Up and across the body, then low and out to its own side.
        assertTrue(x(wound, "RightArm") < -120 && CastPose.angle(wound, "RightArm", 2) < -20);
        assertTrue(x(struck, "RightArm") > -35 && CastPose.angle(struck, "RightArm", 2) > 25);
        // The chest winds one way and unwinds the other.
        assertTrue(CastPose.angle(wound, "UpBody", 1) < -18 && CastPose.angle(struck, "UpBody", 1) > 18);
    }
}
