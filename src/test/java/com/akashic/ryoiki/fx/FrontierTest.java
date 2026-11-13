package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FrontierTest {

    private static final float R = 30;

    @Test
    void theFrontMovesButKeepsWithinItsReach() {
        Frontier front = new Frontier(7, R);
        float least = 0;
        float most = 0;
        for (float t = 0; t <= 300; t += 1.5F) {
            for (float x = -R; x <= R; x += 0.5F) {
                float z = front.at(x, t, 0, null);
                assertTrue(Math.abs(z) <= Frontier.REACH, z + " at " + x + ", " + t);
                least = Math.min(least, z);
                most = Math.max(most, z);
            }
        }
        // It reaches well into both grounds: neither a line nor one-sided.
        assertTrue(most > 3 && least < -3, least + " to " + most);
    }

    @Test
    void theFrontIsOneUnbrokenCurveInSpaceAndInTime() {
        Frontier front = new Frontier(7, R);
        for (float t = 0; t <= 300; t += 7) {
            for (float x = -R; x < R; x += 0.1F) {
                // Steep at a tongue's side, never a wall: under four blocks forward for one along the seam.
                assertTrue(Math.abs(front.at(x + 0.1F, t, 0, null) - front.at(x, t, 0, null)) < 0.4F, x + ", " + t);
                // And slow enough to follow: under a fifth of a block in a tick.
                assertTrue(Math.abs(front.at(x, t + 1, 0, null) - front.at(x, t, 0, null)) < 0.2F, x + ", " + t);
            }
        }
    }

    @Test
    void whoeverIsAheadPushesTheFrontIntoTheOthersGround() {
        Frontier front = new Frontier(3, R);
        for (float x = -R; x <= R; x += 2) {
            float even = front.at(x, 50, 0, null);
            assertTrue(front.at(x, 50, 1, null) > even + Frontier.PUSH * 0.6F);
            assertTrue(front.at(x, 50, -1, null) < even - Frontier.PUSH * 0.6F);
        }
        // More in the middle than by the walls.
        float middle = front.at(0, 50, 1, null) - front.at(0, 50, 0, null);
        float wall = front.at(R, 50, 1, null) - front.at(R, 50, 0, null);
        assertEquals(Frontier.PUSH, middle, 1e-3F);
        assertTrue(wall < middle - 2);
    }

    @Test
    void aBlowShovesTheFrontAndItComesBack() {
        Frontier front = new Frontier(3, R);
        float calm = front.at(4, 80, 0, null);
        // The host struck, by x = 4, just now: the front there jumps toward the rival.
        assertTrue(front.at(4, 80, 0, new float[] { 4, 0.5F, 1 }) > calm + 2);
        assertTrue(front.at(4, 80, 0, new float[] { 4, 0.5F, -1 }) < calm - 2);
        // The humps run out along the seam: later, the place it struck is nearly still and further off is moved.
        float later = front.at(4, 80, 0, new float[] { 4, 12, 1 });
        float along = front.at(4 + 0.9F * 12, 80, 0, new float[] { 4, 12, 1 });
        assertTrue(Math.abs(later - calm) < 0.5F, "at the place: " + (later - calm));
        assertTrue(along - front.at(4 + 0.9F * 12, 80, 0, null) > 0.3F);
        assertEquals(calm, front.at(4, 80, 0, new float[] { 4, Frontier.SHOVE_TICKS, 1 }), 1e-4F);
    }

    @Test
    void theFrontIsTheSameForEveryoneInADomainAndDiffersBetweenDomains() {
        assertArrayEquals(new Frontier(11, R).line(33, 0.25F, null), new Frontier(11, R).line(33, 0.25F, null));
        assertNotEquals(new Frontier(11, R).at(0, 33, 0, null), new Frontier(12, R).at(0, 33, 0, null));
        float[] line = new Frontier(11, R).line(33, 0, null);
        assertEquals((Frontier.SEGMENTS + 1) * 2, line.length);
        assertEquals(-R, line[0]);
        assertEquals(R, line[Frontier.SEGMENTS * 2], 1e-4F);
        // A place is before or behind the front by its drawn line, and on it at its points.
        assertEquals(0, Frontier.before(line, line[10], line[11]), 1e-5F);
        assertTrue(Frontier.before(line, 0, 20) > 0 && Frontier.before(line, 0, -20) < 0);
        assertTrue(Frontier.before(line, -90, 20) > 0 && Frontier.before(line, 90, -20) < 0);
    }

    @Test
    public void effectsThinOutOverTheOthersGround() {
        // Whole on a domain's own ground and on the front, gone a reach into the other's, and never rising on the way.
        assertEquals(1, Frontier.crossing(-30, 14), 0);
        assertEquals(1, Frontier.crossing(0, 14), 0);
        assertEquals(0.5F, Frontier.crossing(7, 14), 1e-6F);
        assertEquals(0, Frontier.crossing(14, 14), 0);
        assertEquals(0, Frontier.crossing(40, 14), 0);
        float last = 1;
        for (float depth = -5; depth < 20; depth += 0.25F) {
            float now = Frontier.crossing(depth, 14);
            assertTrue(now <= last && now >= 0);
            last = now;
        }
    }
}
