package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StreakFieldTest {

    private static final int R = 30;

    @Test
    void theOpeningIsARushThatDiesAwayAndVolleysFollow() {
        assertEquals(1, StreakField.rush(0));
        assertTrue(StreakField.rush(10) > 0.5F && StreakField.rush(10) < StreakField.rush(5));
        assertEquals(0, StreakField.rush(StreakField.OPENING));
        assertEquals(0, StreakField.rush(StreakField.FIRST_VOLLEY - 1));
        // A volley rises in three ticks, holds, and falls; then calm until the next.
        assertTrue(StreakField.volleyStartsAt(StreakField.FIRST_VOLLEY));
        assertTrue(StreakField.volleyStartsAt(StreakField.FIRST_VOLLEY + 2 * StreakField.VOLLEY_PERIOD));
        assertFalse(StreakField.volleyStartsAt(StreakField.FIRST_VOLLEY + 1));
        assertFalse(StreakField.volleyStartsAt(0));
        assertEquals(0.75F, StreakField.rush(StreakField.FIRST_VOLLEY + 5), 1e-4F);
        assertEquals(0, StreakField.rush(StreakField.FIRST_VOLLEY + 30));
        assertEquals(0.75F, StreakField.rush(StreakField.FIRST_VOLLEY + StreakField.VOLLEY_PERIOD + 5), 1e-4F);
        float before = StreakField.rush(0);
        for (float t = 0.1F; t < 300; t += 0.1F) {
            float now = StreakField.rush(t);
            assertTrue(Math.abs(now - before) < 0.06F, "a jump at " + t);
            before = now;
        }
    }

    @Test
    void aStreakGoesStraightAtTheSpeedItWasSentWith() {
        StreakField.Streak a = new StreakField.Streak();
        StreakField.Streak b = new StreakField.Streak();
        int checked = 0;
        // Lanes near the middle, where the way across is long: nothing is clipped by a wall in these few ticks.
        for (int lane = -5; lane <= 5; lane++) {
            for (int layer = 0; layer < StreakField.LAYERS; layer++) {
                for (int moment = 12; moment < 60; moment++) {
                    float born = (moment + 1) * StreakField.PERIOD / (float) StreakField.SLOTS;
                    // Well inside the arena: a few ticks after it was sent, and one tick later.
                    if (!StreakField.at(5, lane, layer, moment, born + 6, R, a)
                        || !StreakField.at(5, lane, layer, moment, born + 7, R, b)) {
                        continue;
                    }
                    if (a.headZ - a.tailZ < 2.9F || b.headZ > 15) {
                        continue;
                    }
                    checked++;
                    assertEquals(a.x, b.x);
                    assertEquals(a.y, b.y);
                    assertEquals(a.speed, b.speed);
                    assertEquals(a.speed, b.headZ - a.headZ, 1e-3F);
                    assertTrue(a.speed >= 1.5F * 0.75F - 1e-3F);
                    assertEquals(lane, (int) Math.floor(a.x / StreakField.LANE));
                    assertTrue(a.y > 0.5F && a.y < 0.5F + StreakField.LAYERS * StreakField.LAYER);
                }
            }
        }
        assertTrue(checked > 40, "streaks checked: " + checked);
    }

    @Test
    void streaksStayInsideTheRoundArenaAndComeAndGoAtItsWalls() {
        StreakField.Streak s = new StreakField.Streak();
        int seen = 0;
        for (float time = 0; time < 200; time += 3.7F) {
            int latest = StreakField.moment(time);
            for (int lane = -11; lane <= 11; lane++) {
                for (int layer = 0; layer < StreakField.LAYERS; layer++) {
                    for (int moment = latest - StreakField.momentsAlive(R); moment <= latest; moment++) {
                        if (!StreakField.at(9, lane, layer, moment, time, R, s)) {
                            continue;
                        }
                        seen++;
                        assertTrue(Math.hypot(s.x, s.headZ) <= R - 1.5 + 1e-3, "head outside");
                        assertTrue(Math.hypot(s.x, s.tailZ) <= R - 1.5 + 1e-3, "tail outside");
                        assertTrue(s.headZ > s.tailZ);
                        assertTrue(s.shown >= 0 && s.shown <= 1);
                    }
                }
            }
        }
        assertTrue(seen > 300, "streaks seen: " + seen);
        assertFalse(StreakField.at(9, 0, -1, 5, 40, R, s));
        assertFalse(StreakField.at(9, 0, StreakField.LAYERS, 5, 40, R, s));
        assertFalse(StreakField.at(9, 40, 0, 5, 40, R, s));
    }

    @Test
    void aRushSendsManyTimesMoreStreaksThanTheCalm() {
        // The opening against the calm before the first volley: the streaks sent in each.
        int opening = sent(0, 6);
        int calm = sent(14, 18);
        assertTrue(opening > calm * 3, opening + " in the opening, " + calm + " in the calm");
        assertTrue(calm > 20, "the calm still flows: " + calm);
        // The same for everyone in a domain.
        assertEquals(opening, sent(0, 6));
    }

    private static int sent(int fromMoment, int toMoment) {
        StreakField.Streak s = new StreakField.Streak();
        int count = 0;
        for (int moment = fromMoment; moment < toMoment; moment++) {
            float justAfter = (moment + 1.001F) * StreakField.PERIOD / (float) StreakField.SLOTS;
            for (int lane = -9; lane <= 9; lane++) {
                for (int layer = 0; layer < StreakField.LAYERS; layer++) {
                    if (StreakField.at(3, lane, layer, moment, justAfter, R, s)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
