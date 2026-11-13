package com.akashic.ryoiki.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClashScoreTest {

    @Test
    void sixteenHitsFillTheBar() {
        ClashScore score = new ClashScore();
        for (int i = 0; i < 15; i++) {
            assertTrue(score.hit(ClashScore.RIVAL));
            assertEquals(ClashScore.NONE, score.full());
        }
        assertTrue(score.hit(ClashScore.RIVAL));
        assertEquals(ClashScore.RIVAL, score.full());
        assertEquals(1F, ClashScore.bar(score.hits(ClashScore.RIVAL)));
        assertEquals(0.0625F, ClashScore.bar(1));
        // A full bar takes no more.
        assertFalse(score.hit(ClashScore.RIVAL));
        assertEquals(0, score.hits(ClashScore.HOST));
    }

    @Test
    void whenTheTimeIsUpTheFullerBarWinsAndEqualBarsTie() {
        ClashScore score = new ClashScore();
        assertEquals(ClashScore.TIE, score.atTimeUp());
        score.hit(ClashScore.HOST);
        assertEquals(ClashScore.HOST, score.atTimeUp());
        score.hit(ClashScore.RIVAL);
        assertEquals(ClashScore.TIE, score.atTimeUp());
        score.hit(ClashScore.RIVAL);
        assertEquals(ClashScore.RIVAL, score.atTimeUp());
        assertEquals(ClashScore.NONE, score.full());
    }
}
