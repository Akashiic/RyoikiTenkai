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
            assertTrue(score.hit(ClashScore.RIVAL, i * ClashScore.HIT_INTERVAL));
            assertEquals(ClashScore.NONE, score.full());
        }
        assertTrue(score.hit(ClashScore.RIVAL, 15 * ClashScore.HIT_INTERVAL));
        assertEquals(ClashScore.RIVAL, score.full());
        assertEquals(1F, ClashScore.bar(score.hits(ClashScore.RIVAL)));
        assertEquals(0.0625F, ClashScore.bar(1));
        // A full bar takes no more.
        assertFalse(score.hit(ClashScore.RIVAL, 10000));
    }

    @Test
    void clickingFasterAddsNothing() {
        ClashScore score = new ClashScore();
        assertTrue(score.hit(ClashScore.HOST, 100));
        assertFalse(score.hit(ClashScore.HOST, 101));
        assertFalse(score.hit(ClashScore.HOST, 100 + ClashScore.HIT_INTERVAL - 1));
        assertTrue(score.hit(ClashScore.HOST, 100 + ClashScore.HIT_INTERVAL));
        // The other side has its own pace.
        assertTrue(score.hit(ClashScore.RIVAL, 101));
        assertEquals(2, score.hits(ClashScore.HOST));
        assertEquals(1, score.hits(ClashScore.RIVAL));
    }

    @Test
    void whenTheTimeIsUpTheFullerBarWinsAndEqualBarsTie() {
        ClashScore score = new ClashScore();
        assertEquals(ClashScore.TIE, score.atTimeUp());
        score.hit(ClashScore.HOST, 0);
        assertEquals(ClashScore.HOST, score.atTimeUp());
        score.hit(ClashScore.RIVAL, 0);
        assertEquals(ClashScore.TIE, score.atTimeUp());
        score.hit(ClashScore.RIVAL, 50);
        assertEquals(ClashScore.RIVAL, score.atTimeUp());
        assertEquals(ClashScore.NONE, score.full());
    }
}
