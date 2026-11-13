package com.akashic.ryoiki.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ComboChainTest {

    @Test
    void fourClicksInRhythmAreTheFourBlows() {
        ComboChain chain = new ComboChain();
        for (int i = 0; i < ComboChain.BLOWS; i++) {
            assertEquals(i + 1, chain.click(100 + i * ComboChain.GAP));
        }
        // The finisher takes its recovery; then a new chain.
        long finisher = 100 + 3 * ComboChain.GAP;
        assertEquals(0, chain.click(finisher + ComboChain.GAP));
        assertEquals(1, chain.click(finisher + ComboChain.RECOVERY));
    }

    @Test
    void aClickTooEarlyIsLostUnlessItIsAlmostTime() {
        ComboChain chain = new ComboChain();
        assertEquals(1, chain.click(0));
        // Much too early: lost.
        assertEquals(0, chain.click(1));
        for (int t = 2; t <= ComboChain.GAP + 2; t++) {
            assertEquals(0, chain.tick(t));
        }
        // Just too early: kept, and it begins on the tick it may.
        assertEquals(2, chain.click(ComboChain.GAP + 3));
        assertEquals(0, chain.click(2 * ComboChain.GAP + 3 - ComboChain.BUFFER));
        assertEquals(0, chain.tick(2 * ComboChain.GAP + 2));
        assertEquals(3, chain.tick(2 * ComboChain.GAP + 3));
        // A kept click is used once.
        assertEquals(0, chain.tick(2 * ComboChain.GAP + 4));
    }

    @Test
    void aPauseDropsTheChain() {
        ComboChain chain = new ComboChain();
        assertEquals(1, chain.click(0));
        assertEquals(2, chain.click(ComboChain.GAP));
        assertEquals(3, chain.click(ComboChain.GAP + ComboChain.DROP));
        assertEquals(1, chain.click(ComboChain.GAP + 2 * ComboChain.DROP + 1));
        assertEquals(1, chain.blow());
    }
}
