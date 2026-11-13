package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GaitTest {

    private static Gait.State running(float phase, float sprint) {
        Gait.State s = new Gait.State();
        s.phase = phase;
        s.moving = 1;
        s.sprint = sprint;
        return s;
    }

    private static float sole(float[] p, String side) {
        return Gait.angle(p, "AllBody", 0) + Gait.angle(p, side + "Leg", 0)
            + Gait.angle(p, side + "LowerLeg", 0)
            + Gait.angle(p, side + "Foot", 0);
    }

    @Test
    void aStrideHasNoJumpInItAndClosesOnItself() {
        for (float sprint : new float[] { 0, 0.5F, 1 }) {
            float[] last = Gait.sample(running(0, sprint));
            for (float phase = 0.002F; phase <= 1.0001F; phase += 0.002F) {
                float[] now = Gait.sample(running(phase, sprint));
                for (int i = 0; i < now.length; i++) {
                    // A five-hundredth of a stride is a twentieth of a tick at a run.
                    assertTrue(Math.abs(now[i] - last[i]) < 3.2F, BlowPose.BONES[i / 3] + " jumps at " + phase);
                }
                last = now;
            }
            float[] first = Gait.sample(running(0, sprint));
            float[] again = Gait.sample(running(1, sprint));
            for (int i = 0; i < first.length; i++) {
                assertEquals(first[i], again[i], 1e-3);
            }
        }
    }

    @Test
    void theTwoSidesDoTheSameHalfAStrideApart() {
        String[][] pairs = { { "RightArm", "LeftArm" }, { "RightForeArm", "LeftForeArm" }, { "RightLeg", "LeftLeg" },
            { "RightLowerLeg", "LeftLowerLeg" }, { "RightFoot", "LeftFoot" } };
        for (float sprint : new float[] { 0, 1 }) {
            for (float phase = 0; phase < 1; phase += 0.05F) {
                float[] a = Gait.sample(running(phase, sprint));
                float[] b = Gait.sample(running(phase + 0.5F, sprint));
                for (String[] pair : pairs) {
                    // The mirror of a turn about X is the same turn; about Y or Z, the opposite one.
                    assertEquals(Gait.angle(a, pair[0], 0), Gait.angle(b, pair[1], 0), 1e-3);
                    assertEquals(Gait.angle(a, pair[0], 1), -Gait.angle(b, pair[1], 1), 1e-3);
                    assertEquals(Gait.angle(a, pair[0], 2), -Gait.angle(b, pair[1], 2), 1e-3);
                }
                for (String middle : new String[] { "AllBody", "UpBody", "AllHead" }) {
                    assertEquals(Gait.angle(a, middle, 0), Gait.angle(b, middle, 0), 1e-3);
                    assertEquals(Gait.angle(a, middle, 1), -Gait.angle(b, middle, 1), 1e-3);
                    assertEquals(Gait.angle(a, middle, 2), -Gait.angle(b, middle, 2), 1e-3);
                }
            }
        }
    }

    @Test
    void aFootOnTheGroundIsLevelAndAKneeNeverBendsBackward() {
        for (float sprint : new float[] { 0, 1 }) {
            // The right foot lands at 0 and is under the body a little later: its sole is level there.
            float under = sprint == 0 ? 0.2F : 0.15F;
            assertEquals(0, sole(Gait.sample(running(under, sprint)), "Right"), 0.5);
            assertEquals(0, sole(Gait.sample(running(under + 0.5F, sprint)), "Left"), 0.5);
            for (float phase = 0; phase < 1; phase += 0.01F) {
                float[] p = Gait.sample(running(phase, sprint));
                assertTrue(Gait.angle(p, "RightLowerLeg", 0) >= 0 && Gait.angle(p, "LeftLowerLeg", 0) >= 0);
            }
        }
    }

    @Test
    void theSprintLeansFoldsAndDrivesMoreThanTheJog() {
        float jogLean = 0;
        float sprintLean = 0;
        float jogFold = 0;
        float sprintFold = 0;
        float jogDrive = 0;
        float sprintDrive = 0;
        for (float phase = 0; phase < 1; phase += 0.01F) {
            float[] jog = Gait.sample(running(phase, 0));
            float[] sprint = Gait.sample(running(phase, 1));
            jogLean = Math.max(jogLean, Gait.angle(jog, "AllBody", 0) + Gait.angle(jog, "UpBody", 0));
            sprintLean = Math.max(sprintLean, Gait.angle(sprint, "AllBody", 0) + Gait.angle(sprint, "UpBody", 0));
            jogFold = Math.max(jogFold, Gait.angle(jog, "RightLowerLeg", 0));
            sprintFold = Math.max(sprintFold, Gait.angle(sprint, "RightLowerLeg", 0));
            jogDrive = Math.min(jogDrive, Gait.angle(jog, "RightLeg", 0));
            sprintDrive = Math.min(sprintDrive, Gait.angle(sprint, "RightLeg", 0) + Gait.angle(sprint, "AllBody", 0));
        }
        assertTrue(sprintLean > jogLean + 10);
        assertTrue(sprintFold > jogFold + 30);
        assertTrue(sprintDrive < jogDrive - 15);
        // The jog swings its arms against the legs; the sprint lays them back and holds them there.
        float[] jog = Gait.sample(running(0, 0));
        assertTrue(Gait.angle(jog, "RightArm", 0) > 15 && Gait.angle(jog, "LeftArm", 0) < -15);
        float[] sprint = Gait.sample(running(0, 1));
        assertTrue(Gait.angle(sprint, "RightArm", 0) > 30 && Gait.angle(sprint, "LeftArm", 0) > 30);
        // And each covers more ground in a stride than its legs alone would reach: both are runs.
        assertTrue(Gait.stride(1) > Gait.stride(0));
    }

    @Test
    void theHeadStaysOnTheWayAhead() {
        for (float sprint : new float[] { 0, 1 }) {
            for (float phase = 0; phase < 1; phase += 0.05F) {
                float[] p = Gait.sample(running(phase, sprint));
                float turn = Gait.angle(p, "AllBody", 1) + Gait.angle(p, "UpBody", 1) + Gait.angle(p, "AllHead", 1);
                assertTrue(Math.abs(turn) < 1.5, "the head turns with the body at " + phase);
                float nod = Gait.angle(p, "AllBody", 0) + Gait.angle(p, "UpBody", 0) + Gait.angle(p, "AllHead", 0);
                assertTrue(nod >= 0 && nod < 6, "the head hangs or tips back at " + phase);
            }
        }
    }

    @Test
    void standingDoesNotCareWhereTheStrideWas() {
        Gait.State a = new Gait.State();
        Gait.State b = new Gait.State();
        b.phase = 0.37F;
        b.sprint = 1;
        b.heading = 120;
        float[] pa = Gait.sample(a);
        float[] pb = Gait.sample(b);
        for (int i = 0; i < pa.length; i++) {
            assertEquals(pa[i], pb[i], 1e-4);
        }
        assertEquals(0, sole(pa, "Right"), 1e-3);
        assertEquals(0, sole(pa, "Left"), 1e-3);
    }

    @Test
    void backwardTheLegsGoTheOtherWayAndSidewaysTheyOpen() {
        Gait.State ahead = running(0, 0);
        Gait.State back = running(0, 0);
        back.heading = 180;
        Gait.State aside = running(0, 0);
        aside.heading = 90;
        // The right foot has just landed: ahead of the body going forward, behind it going backward.
        assertTrue(Gait.angle(Gait.sample(ahead), "RightLeg", 0) < -15);
        assertTrue(Gait.angle(Gait.sample(back), "RightLeg", 0) > 10);
        // To the model's right, that leg is out to the right, and hardly forward at all.
        float[] p = Gait.sample(aside);
        assertTrue(Gait.angle(p, "RightLeg", 2) > 8 && Math.abs(Gait.angle(p, "RightLeg", 0)) < 3);
    }

    @Test
    void inTheAirOneKneeGoesUpAndThenTheLegsReachForTheGround() {
        Gait.State s = running(0, 0);
        s.air = 1;
        s.rising = 0.42F;
        float[] up = Gait.sample(s);
        s.rising = -0.5F;
        float[] down = Gait.sample(s);
        // The right leg was the one in front.
        assertTrue(Gait.angle(up, "RightLeg", 0) < -55 && Gait.angle(up, "RightLowerLeg", 0) > 70);
        assertTrue(Gait.angle(up, "LeftLeg", 0) > 10);
        assertTrue(Gait.angle(down, "RightLeg", 0) > -30 && Gait.angle(down, "RightLowerLeg", 0) < 40);
        // Arms back going up; out from the sides and forward coming down.
        assertTrue(Gait.angle(up, "RightArm", 0) > 30);
        assertTrue(Gait.angle(down, "RightArm", 2) > 18 && Gait.angle(down, "LeftArm", 2) < -18);
        // And no jump in between, nor from the ground into the air.
        float[] last = up;
        for (float rising = 0.42F; rising >= -0.6F; rising -= 0.01F) {
            s.rising = rising;
            float[] now = Gait.sample(s);
            for (int i = 0; i < now.length; i++) {
                assertTrue(Math.abs(now[i] - last[i]) < 3);
            }
            last = now;
        }
        s.rising = 0.42F;
        last = Gait.sample(running(0, 0));
        for (float air = 0; air <= 1; air += 0.02F) {
            s.air = air;
            float[] now = Gait.sample(s);
            for (int i = 0; i < now.length; i++) {
                assertTrue(Math.abs(now[i] - last[i]) < 5);
            }
            last = now;
        }
    }
}
