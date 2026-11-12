package com.akashic.ryoiki.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShrineCoreTest {

    @Test
    void theHallIsAHollowSquareWithATop() {
        assertTrue(ShrineCore.solid(4, 0, 0, 0), "a wall");
        assertTrue(ShrineCore.solid(-5, 3, -5, 0), "a corner");
        assertFalse(ShrineCore.solid(0, 0, 0, 0), "hollow inside");
        assertFalse(ShrineCore.solid(5, 0, 0, 0), "nothing beyond its half width");
        assertTrue(ShrineCore.solid(0, ShrineCore.HEIGHT, 0, 0), "the top");
        assertFalse(ShrineCore.solid(0, ShrineCore.HEIGHT + 1, 0, 0));
        assertFalse(ShrineCore.solid(4, -1, 0, 0));
    }

    @Test
    void turnedAnyWayItStaysWithinReachAndClosed() {
        for (float yaw = -180; yaw < 180; yaw += 7.5F) {
            int walls = 0;
            for (int dx = -12; dx < 12; dx++) {
                for (int dz = -12; dz < 12; dz++) {
                    if (ShrineCore.solid(dx, 0, dz, yaw)) {
                        walls++;
                        assertTrue(
                            dx >= -ShrineCore.REACH && dx < ShrineCore.REACH
                                && dz >= -ShrineCore.REACH
                                && dz < ShrineCore.REACH,
                            "a wall block out of reach at yaw " + yaw);
                    }
                }
            }
            assertTrue(walls >= 30 && walls <= 80, "yaw " + yaw + ": " + walls + " wall blocks");
        }
    }

    @Test
    void sceneAndWorldAreEachOthersInverse() {
        for (float yaw : new float[] { 0, 37, 90, -135, 270 }) {
            double[] w = ShrineCore.world(3, -4.5, yaw);
            double[] s = ShrineCore.scene(w[0], w[1], yaw);
            assertEquals(3, s[0], 1e-9);
            assertEquals(-4.5, s[1], 1e-9);
        }
        // Yaw 0 looks toward +Z, yaw 90 toward -X: the door, and the caster before it, are on that side.
        assertEquals(ShrineCore.CASTER_DISTANCE, ShrineCore.place(0, 0, 0, 27)[1], 1e-9);
        assertEquals(-ShrineCore.CASTER_DISTANCE, ShrineCore.place(0, 0, 90, 27)[0], 1e-9);
        assertEquals(0, ShrineCore.place(0, 0, 90, 27)[1], 1e-9);
    }

    @Test
    void nobodyLandsInTheHallOrBeyondTheLimit() {
        for (float yaw = 0; yaw < 360; yaw += 30) {
            for (int ox = -40; ox <= 40; ox += 4) {
                for (int oz = -40; oz <= 40; oz += 4) {
                    double[] at = ShrineCore.place(ox, oz, yaw, 27);
                    assertTrue(Math.hypot(at[0], at[1]) <= 27 + 1e-6);
                    double[] s = ShrineCore.scene(at[0], at[1], yaw);
                    assertTrue(
                        Math.max(Math.abs(s[0]), Math.abs(s[1])) >= ShrineCore.HALF + 1.5 - 1e-6,
                        "inside the hall: " + ox + ", " + oz + " at yaw " + yaw);
                }
            }
        }
    }
}
