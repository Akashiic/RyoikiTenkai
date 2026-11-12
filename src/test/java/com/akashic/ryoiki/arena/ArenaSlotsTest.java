package com.akashic.ryoiki.arena;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArenaSlotsTest {

    @Test
    void centresAreChunkCornersASpacingApart() {
        ArenaSlots slots = new ArenaSlots(200005, -37, 120, 2, 1);
        assertEquals(200000, slots.centerX(0));
        assertEquals(-48, slots.centerZ(0));
        assertEquals(200000 + ArenaSlots.SPACING, slots.centerX(1));
        assertEquals(0, slots.centerX(1) & 15);
        assertArrayEquals(new int[][] { { 12499, -4 }, { 12499, -3 }, { 12500, -4 }, { 12500, -3 } }, slots.chunks(0));
        // A radius of 30 takes two chunks to each side: sixteen in all.
        ArenaSlots wide = new ArenaSlots(200000, 0, 120, 2, 2);
        assertEquals(16, wide.chunks(0).length);
        assertArrayEquals(new int[] { 12498, -2 }, wide.chunks(0)[0]);
        assertArrayEquals(new int[] { 12501, 1 }, wide.chunks(0)[15]);
    }

    @Test
    void slotsAreTakenAndGivenBack() {
        ArenaSlots slots = new ArenaSlots(0, 0, 100, 2, 1);
        assertEquals(0, slots.take("a"));
        assertEquals(1, slots.take("b"));
        assertEquals(-1, slots.take("c"));
        assertEquals(2, slots.taken());
        slots.release(0);
        assertEquals(0, slots.take("c"));
    }

    @Test
    void theArenaRegionIsTheBandOfTheSlots() {
        ArenaSlots slots = new ArenaSlots(200000, 200000, 120, 2, 2);
        assertTrue(slots.inArenaRegion(200000, 200000));
        assertTrue(slots.inArenaRegion(201024 + 13, 200000 - 13));
        assertTrue(slots.inArenaRegion(200000 - 500, 200000 + 500));
        assertFalse(slots.inArenaRegion(0, 0));
        assertFalse(slots.inArenaRegion(200000 + 1024 + 600, 200000));
        assertFalse(slots.inArenaRegion(200000, 200000 + 600));
    }
}
