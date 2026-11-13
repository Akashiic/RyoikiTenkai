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
        assertTrue(ShrineCore.solid(7, 0, 0, 0), "the base it stands on, to the foot of its steps");
        assertFalse(ShrineCore.solid(8, 0, 0, 0), "nothing beyond that");
        assertTrue(ShrineCore.solid(0, ShrineCore.HEIGHT, 0, 0), "the top");
        assertFalse(ShrineCore.solid(0, ShrineCore.HEIGHT + 1, 0, 0));
        assertFalse(ShrineCore.solid(4, -1, 0, 0));
    }

    @Test
    void turnedAnyWayItStaysWithinReachAndClosed() {
        for (float yaw = -180; yaw < 180; yaw += 7.5F) {
            int walls = 0;
            for (int dx = -15; dx < 15; dx++) {
                for (int dz = -15; dz < 15; dz++) {
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
            assertTrue(walls >= 180 && walls <= 240, "yaw " + yaw + ": " + walls + " wall blocks");
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
                        Math.max(Math.abs(s[0]), Math.abs(s[1])) >= ShrineCore.BASE + ShrineCore.CLEAR - 1e-6,
                        "inside the hall: " + ox + ", " + oz + " at yaw " + yaw);
                }
            }
        }
    }

    @Test
    public void aRivalsHallStandsBeyondTheLineAndFacesIt() {
        // Against a void the two meet at the centre: the hall is its caster's distance before it, door toward it.
        double[] hall = ShrineCore.rival(false, 0);
        assertEquals(0, hall[0], 1e-9);
        assertEquals(ShrineCore.CASTER_DISTANCE, hall[1], 1e-9);
        assertEquals(180, hall[2], 1e-9);
        // Against a shrine they meet where its caster stands, and the two halls face each other across the line.
        hall = ShrineCore.rival(true, 0);
        assertEquals(2 * ShrineCore.CASTER_DISTANCE, hall[1], 1e-9);
        // Turned with the host: the same place in the host's own frame.
        hall = ShrineCore.rival(false, 90);
        double[] scene = ShrineCore.scene(hall[0], hall[1], 90);
        assertEquals(0, scene[0], 1e-9);
        assertEquals(ShrineCore.CASTER_DISTANCE, scene[1], 1e-9);
        // The rival's caster, who stands on the line, is before its door at the usual distance.
        hall = ShrineCore.rival(false, 37);
        double[] door = ShrineCore.scene(-hall[0], -hall[1], (float) hall[2]);
        assertEquals(0, door[0], 1e-9);
        assertEquals(ShrineCore.CASTER_DISTANCE, door[1], 1e-9);
    }

    @Test
    public void aHallAwayFromTheMiddleIsSolidAllThrough() {
        assertTrue(ShrineCore.solid(0.5, 0, 0.5, 0, true), "no inside");
        assertFalse(ShrineCore.solid(0.5, 0, 0.5, 0, false));
        assertTrue(ShrineCore.solid(4.5, 3, -4.5, 45, true) == ShrineCore.solid(4.5, 3, -4.5, 45, false));
        assertFalse(ShrineCore.solid(8.5, 0, 0.5, 0, true), "nothing beyond its base");
        assertFalse(ShrineCore.solid(0.5, ShrineCore.HEIGHT + 1, 0.5, 0, true));
    }

    @Test
    public void whoeverStandsWhereAHallGoesUpIsPutOutByItsDoor() {
        // Inside, and against a wall: out by the door side, clear of the wall, keeping the place along it.
        double door = ShrineCore.BASE + 2 * ShrineCore.CLEAR;
        double[] out = ShrineCore.clear(2, -1, 0);
        assertEquals(2, out[0], 1e-9);
        assertEquals(door, out[1], 1e-6);
        out = ShrineCore.clear(-ShrineCore.BASE - 0.5, 0, 0);
        assertEquals(door, out[1], 1e-6);
        // Clear of it already: left alone.
        out = ShrineCore.clear(0, ShrineCore.BASE + 1, 0);
        assertEquals(ShrineCore.BASE + 1, out[1], 1e-6);
        out = ShrineCore.clear(10, 10, 0);
        assertEquals(10, out[0], 1e-9);
        // The door of a hall turned about is on the other side.
        out = ShrineCore.clear(0, 0, 180);
        assertEquals(-door, out[1], 1e-6);
    }
}
