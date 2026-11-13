package com.akashic.ryoiki.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ChunkKeepTest {

    @Test
    void keepsNothingOutsideADomain() {
        ChunkKeep keep = new ChunkKeep();
        assertFalse(keep.keeps(0, 0));
        assertEquals(0, keep.keptCount());
    }

    @Test
    void keepsTheSquareAroundWhereThePlayerLeft() {
        ChunkKeep keep = new ChunkKeep();
        keep.begin(-3, 100, 8);
        assertTrue(keep.keeps(-3, 100));
        assertTrue(keep.keeps(-11, 108));
        assertTrue(keep.keeps(5, 92));
        assertFalse(keep.keeps(6, 100));
        assertFalse(keep.keeps(-3, 109));
        assertFalse(keep.keeps(12500, 12500)); // the arena's own chunks are unloaded as usual
        assertEquals(3, keep.keptCount());
    }

    @Test
    void releaseNamesOnlyTheKeptChunksOutOfViewAndStopsKeeping() {
        ChunkKeep keep = new ChunkKeep();
        keep.begin(0, 0, 8);
        keep.keeps(0, 0);
        keep.keeps(-8, -8);
        keep.keeps(8, 3);
        // The player came back 6 chunks to the east: (-8, -8) is now out of view, the others were sent again.
        List<int[]> gone = keep.release(6, 0, 8);
        assertEquals(1, gone.size());
        assertEquals(-8, gone.get(0)[0]);
        assertEquals(-8, gone.get(0)[1]);
        assertEquals(0, keep.keptCount());
        assertFalse(keep.active());
        assertFalse(keep.keeps(0, 0));
    }

    @Test
    void negativeCoordinatesSurviveTheKey() {
        ChunkKeep keep = new ChunkKeep();
        keep.begin(-5000, -7000, 2);
        keep.keeps(-5001, -7002);
        List<int[]> gone = keep.release(0, 0, 8);
        assertEquals(-5001, gone.get(0)[0]);
        assertEquals(-7002, gone.get(0)[1]);
    }
}
