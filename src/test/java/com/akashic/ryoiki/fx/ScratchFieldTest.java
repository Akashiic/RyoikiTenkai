package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScratchFieldTest {

    @Test
    void aFifthOfTheCellsAreMarkedAtAnyMomentAndNeverAllOrNone() {
        ScratchField.Scratch s = new ScratchField.Scratch();
        for (float time = 0; time < 200; time += 13.7F) {
            int marked = 0;
            int cells = 0;
            for (int x = -8; x < 8; x++) {
                for (int z = -8; z < 8; z++) {
                    for (int y = 0; y < ScratchField.LAYERS; y++) {
                        cells++;
                        if (ScratchField.at(21, x, y, z, time, s)) {
                            marked++;
                        }
                    }
                }
            }
            float share = marked / (float) cells;
            float expected = ScratchField.LIFE / ScratchField.PERIOD;
            assertTrue(Math.abs(share - expected) < 0.05F, share + " at " + time);
        }
        assertFalse(ScratchField.at(21, 0, -1, 0, 3, s));
        assertFalse(ScratchField.at(21, 0, ScratchField.LAYERS, 0, 3, s));
    }

    @Test
    void aMarkIsShortStaysByItsCellAndOverTheFloor() {
        ScratchField.Scratch s = new ScratchField.Scratch();
        int seen = 0;
        float longest = 0;
        for (int x = -10; x < 10; x++) {
            for (int z = -10; z < 10; z++) {
                for (int y = 0; y < ScratchField.LAYERS; y++) {
                    for (float time = 0; time < 60; time += 1.3F) {
                        if (!ScratchField.at(4, x, y, z, time, s)) {
                            continue;
                        }
                        seen++;
                        double length = Math.sqrt(sq(s.toX - s.fromX) + sq(s.toY - s.fromY) + sq(s.toZ - s.fromZ));
                        assertTrue(length >= 0.7 - 1e-3 && length <= 2.3 + 1e-3, "length " + length);
                        longest = (float) Math.max(longest, length);
                        assertTrue(s.fromY >= 0.25F - 1e-3F && s.toY >= 0.25F - 1e-3F);
                        // Its middle is in its cell, across the floor.
                        float mx = (s.fromX + s.toX) / 2;
                        float mz = (s.fromZ + s.toZ) / 2;
                        assertEquals(x, (int) Math.floor(mx / ScratchField.CELL), "cell of " + mx);
                        assertEquals(z, (int) Math.floor(mz / ScratchField.CELL), "cell of " + mz);
                        assertTrue(s.age >= 0 && s.age < ScratchField.LIFE);
                    }
                }
            }
        }
        assertTrue(seen > 2000 && longest > 1.8F, seen + " marks, the longest " + longest);
    }

    @Test
    void everyClientSeesTheSameMarksAndEachTimeACellMakesAnother() {
        ScratchField.Scratch a = new ScratchField.Scratch();
        ScratchField.Scratch b = new ScratchField.Scratch();
        // Find a moment this cell is marked, then the same moment a period later.
        float time = 0;
        while (!ScratchField.at(8, 2, 1, -3, time, a)) {
            time += 0.5F;
        }
        assertTrue(ScratchField.at(8, 2, 1, -3, time, b));
        assertEquals(a.fromX, b.fromX);
        assertEquals(a.toZ, b.toZ);
        assertEquals(a.age, b.age);
        assertTrue(ScratchField.at(8, 2, 1, -3, time + ScratchField.PERIOD, b));
        assertEquals(a.age, b.age, 1e-3F);
        assertNotEquals(a.fromX, b.fromX);
        // Another domain marks the air elsewhere.
        boolean differs = !ScratchField.at(9, 2, 1, -3, time, b) || a.fromX != b.fromX;
        assertTrue(differs);
    }

    @Test
    void aMarkIsDrawnInATickAndGoneOverItsLastTwo() {
        assertEquals(0, ScratchField.drawn(0));
        assertEquals(1, ScratchField.drawn(ScratchField.DRAW));
        assertEquals(1, ScratchField.shown(0));
        assertEquals(1, ScratchField.shown(ScratchField.LIFE - 2));
        assertEquals(0.5F, ScratchField.shown(ScratchField.LIFE - 1), 1e-4F);
        assertEquals(0, ScratchField.shown(ScratchField.LIFE));
    }

    private static double sq(double v) {
        return v * v;
    }
}
