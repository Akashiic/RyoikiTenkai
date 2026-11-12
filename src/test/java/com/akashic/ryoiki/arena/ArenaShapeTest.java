package com.akashic.ryoiki.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ArenaShapeTest {

    private static long key(int x, int y, int z) {
        return (x + 64L) << 16 | (y + 64L) << 8 | (z + 64L);
    }

    @Test
    void everyRadiusFitsInTheFourChunksAroundTheCentre() {
        for (int r = ArenaShape.MIN_RADIUS; r <= ArenaShape.MAX_RADIUS; r++) {
            new ArenaShape(r).forEach((dx, dy, dz, part) -> {
                assertTrue(dx >= -16 && dx < 16 && dz >= -16 && dz < 16, "block outside the four chunks");
                assertTrue(dy >= -1 && dy <= 16);
            });
        }
        assertThrows(IllegalArgumentException.class, () -> new ArenaShape(ArenaShape.MAX_RADIUS + 1));
    }

    /** Flood from the centre through everything that is not arena: it must never get out. */
    @Test
    void theShellAndTheFloorCloseTheInterior() {
        for (int r = ArenaShape.MIN_RADIUS; r <= ArenaShape.MAX_RADIUS; r++) {
            ArenaShape shape = new ArenaShape(r);
            Set<Long> solid = new HashSet<>();
            shape.forEach((dx, dy, dz, part) -> solid.add(key(dx, dy, dz)));
            Set<Long> seen = new HashSet<>();
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[] { 0, 0, 0 });
            seen.add(key(0, 0, 0));
            int open = 0;
            while (!queue.isEmpty()) {
                int[] c = queue.poll();
                open++;
                assertTrue(
                    Math.abs(c[0]) <= r + 2 && Math.abs(c[2]) <= r + 2 && c[1] >= 0 && c[1] <= r + 2,
                    "radius " + r + ": the interior leaks at " + c[0] + ", " + c[1] + ", " + c[2]);
                // Corners too: a shell that only stops straight moves would let a player slide out diagonally.
                for (int ax = -1; ax <= 1; ax++) {
                    for (int ay = -1; ay <= 1; ay++) {
                        for (int az = -1; az <= 1; az++) {
                            int x = c[0] + ax;
                            int y = c[1] + ay;
                            int z = c[2] + az;
                            if (!solid.contains(key(x, y, z)) && seen.add(key(x, y, z))) {
                                queue.add(new int[] { x, y, z });
                            }
                        }
                    }
                }
            }
            assertTrue(open > 1000 || r < 8);
        }
    }

    @Test
    void theInteriorIsAHemisphereOfTheRadius() {
        ArenaShape shape = new ArenaShape(13);
        assertTrue(shape.inside(0, 0, 0));
        assertTrue(shape.inside(12, 0, 0));
        assertFalse(shape.inside(13, 0, 0));
        assertTrue(shape.inside(0, 12, 0));
        assertFalse(shape.inside(0, 13, 0));
        assertFalse(shape.inside(0, -1, 0));
        assertTrue(shape.floor(0, -1, 0));
        assertTrue(shape.floor(14, -1, 0));
        assertTrue(shape.shell(13, 0, 0));
        assertFalse(shape.shell(15, 0, 0));
    }

    @Test
    void placeKeepsTheOffsetAndPullsInWhoWouldLandInTheShell() {
        ArenaShape shape = new ArenaShape(13);
        double[] near = shape.place(3, -4);
        assertEquals(3, near[0], 1e-9);
        assertEquals(-4, near[1], 1e-9);
        double[] far = shape.place(13, 0);
        assertEquals(10.5, far[0], 1e-9);
        assertEquals(0, far[1], 1e-9);
        double[] diagonal = shape.place(30, 40);
        assertEquals(10.5, Math.sqrt(diagonal[0] * diagonal[0] + diagonal[1] * diagonal[1]), 1e-9);
        assertEquals(0.75, diagonal[0] / diagonal[1], 1e-9);
        // A standing player (0.6 wide, 1.8 tall) placed at the limit, in any direction, touches no shell block.
        for (int r = ArenaShape.MIN_RADIUS; r <= ArenaShape.MAX_RADIUS; r++) {
            ArenaShape each = new ArenaShape(r);
            for (int step = 0; step < 360; step++) {
                double[] at = each.place(Math.cos(Math.toRadians(step)) * 100, Math.sin(Math.toRadians(step)) * 100);
                for (int x = (int) Math.floor(at[0] - 0.3); x <= (int) Math.floor(at[0] + 0.3); x++) {
                    for (int z = (int) Math.floor(at[1] - 0.3); z <= (int) Math.floor(at[1] + 0.3); z++) {
                        assertFalse(
                            each.shell(x, 0, z) || each.shell(x, 1, z),
                            "radius " + r + ", " + step + " degrees");
                    }
                }
            }
        }
    }
}
