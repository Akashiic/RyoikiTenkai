package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StreakFieldTest {

    private static final int R = 30;
    /** The way the focus lies: north and a third of the way up, as the void's armillary hangs. */
    private static final float EX = 0;
    private static final float EY = 0.47F;
    private static final float EZ = -0.883F;

    @Test
    void theRushIsTheDomainStartingAndThenTheVoidIsStill() {
        assertEquals(1, StreakField.rush(0));
        assertEquals(1, StreakField.rush(StreakField.OPENING / 2));
        assertEquals(1, StreakField.rush(StreakField.OPENING * 0.8F));
        assertTrue(StreakField.rush(StreakField.OPENING * 0.95F) < 0.5F);
        assertEquals(0, StreakField.rush(StreakField.OPENING));
        assertEquals(0, StreakField.rush(200));
        assertEquals(0, StreakField.rush(-1));
        float before = StreakField.rush(0);
        for (float t = 0.1F; t < 80; t += 0.1F) {
            float now = StreakField.rush(t);
            assertTrue(Math.abs(now - before) < 0.06F, "a jump at " + t);
            before = now;
        }
    }

    @Test
    void aWhiteFlashJoinsTheStartingToTheComplete() {
        float opening = StreakField.OPENING;
        assertEquals(0, StreakField.flash(0));
        assertEquals(0, StreakField.flash(opening - StreakField.FLASH_IN));
        assertEquals(1, StreakField.flash(opening), 1e-4F);
        assertEquals(0, StreakField.flash(opening + StreakField.FLASH_OUT));
        assertEquals(0, StreakField.flash(200));
        // It eases in: half way through its gathering it is still faint.
        assertTrue(StreakField.flash(opening - StreakField.FLASH_IN / 2) < 0.2F);
        float before = 0;
        for (float t = 0; t < 80; t += 0.1F) {
            float now = StreakField.flash(t);
            assertTrue(now >= 0 && now <= 1 && Math.abs(now - before) < 0.03F, "a jump at " + t);
            if (t < opening) {
                assertTrue(now >= before - 1e-5F, "it only gathers until the opening ends");
            }
            before = now;
        }
    }

    @Test
    void theRushSummedIsWhatItAddsUpTo() {
        assertEquals(0, StreakField.rushed(0));
        assertEquals(10, StreakField.rushed(10));
        float sum = 0;
        for (float t = 0; t < 70; t += 0.01F) {
            sum += StreakField.rush(t) * 0.01F;
            assertEquals(sum, StreakField.rushed(t + 0.01F), 0.05F, "at " + t);
        }
        // After the opening it grows no more.
        assertEquals(StreakField.rushed(StreakField.OPENING), StreakField.rushed(300));
    }

    @Test
    void everyStreakFliesStraightOutOfTheFocusAtTheSpeedItWasSentWith() {
        StreakField.Streak a = new StreakField.Streak();
        StreakField.Streak b = new StreakField.Streak();
        float fx = EX * StreakField.FOCUS;
        float fy = EY * StreakField.FOCUS;
        float fz = EZ * StreakField.FOCUS;
        int checked = 0;
        int lanes = StreakField.lanes(R);
        for (int la = -lanes; la <= lanes; la++) {
            for (int lb = -lanes; lb <= lanes; lb++) {
                for (int moment = 0; moment < 3; moment++) {
                    float born = (moment + 1) * StreakField.PERIOD / (float) StreakField.SLOTS;
                    if (!StreakField.at(5, la, lb, moment, born + 4, R, EX, EY, EZ, a)
                        || !StreakField.at(5, la, lb, moment, born + 5, R, EX, EY, EZ, b)) {
                        continue;
                    }
                    // The head, the tail and the focus are on one line, the head the furthest out.
                    double head = Math.sqrt(sq(a.headX - fx) + sq(a.headY - fy) + sq(a.headZ - fz));
                    double tail = Math.sqrt(sq(a.tailX - fx) + sq(a.tailY - fy) + sq(a.tailZ - fz));
                    double needle = Math.sqrt(sq(a.headX - a.tailX) + sq(a.headY - a.tailY) + sq(a.headZ - a.tailZ));
                    assertEquals(head, tail + needle, 1e-2);
                    // A tick later it is its speed further from the focus, on the same line, unless it has left.
                    double later = Math.sqrt(sq(b.headX - fx) + sq(b.headY - fy) + sq(b.headZ - fz));
                    assertEquals(a.speed, b.speed);
                    assertTrue(later - head <= a.speed + 1e-2);
                    if (b.shown == 1 && a.shown == 1 && Math.abs(later - head - a.speed) < 1e-2) {
                        checked++;
                        double cross = (a.headX - fx) * (b.headY - fy) - (a.headY - fy) * (b.headX - fx);
                        assertEquals(0, cross / (head * later), 1e-4);
                    }
                    assertTrue(a.speed >= 2.4F * 0.8F && a.speed <= 2.4F * 1.2F * 1.9F + 1e-3F);
                    assertTrue(a.color >= 0 && a.color < StreakField.COLORS);
                }
            }
        }
        assertTrue(checked > 60, "streaks checked: " + checked);
    }

    @Test
    void streaksFillTheArenaFromTheFloorUpAndStayInIt() {
        StreakField.Streak s = new StreakField.Streak();
        int seen = 0;
        float highest = 0;
        float lowest = 100;
        int[] colors = new int[StreakField.COLORS];
        int lanes = StreakField.lanes(R);
        for (float time = 10; time < 40; time += 2F) {
            int latest = StreakField.moment(time);
            for (int la = -lanes; la <= lanes; la++) {
                for (int lb = -lanes; lb <= lanes; lb++) {
                    for (int moment = latest - StreakField.momentsAlive(R); moment <= latest; moment++) {
                        if (!StreakField.at(9, la, lb, moment, time, R, EX, EY, EZ, s)) {
                            continue;
                        }
                        seen++;
                        colors[s.color]++;
                        assertTrue(Math.sqrt(sq(s.headX) + sq(s.headY) + sq(s.headZ)) <= R + 4 + 1e-2, "head outside");
                        assertTrue(Math.sqrt(sq(s.tailX) + sq(s.tailY) + sq(s.tailZ)) <= R + 4 + 1e-2, "tail outside");
                        assertTrue(s.headY >= 0.15F - 1e-3F && s.tailY >= 0.15F - 1e-3F, "under the floor");
                        assertTrue(s.shown >= 0 && s.shown <= 1);
                        highest = Math.max(highest, s.tailY);
                        lowest = Math.min(lowest, s.headY);
                    }
                }
            }
        }
        // Many at any moment, of every colour, from the floor to high over the arena.
        assertTrue(seen / 15 > 150, "streaks at a moment: " + seen / 15);
        assertTrue(highest > 20 && lowest < 1, lowest + " to " + highest);
        for (int c : colors) {
            assertTrue(c > seen / 10, "a colour is rare: " + c + " of " + seen);
        }
    }

    @Test
    void streaksAreSentOnlyWhileTheDomainStartsAndEveryClientSeesTheSame() {
        int starting = sent(0, 2);
        assertTrue(starting > 300, "sent as it starts: " + starting);
        assertEquals(0, sent(8, 12));
        // A void that is fought over does not settle: with a least rush the streaks go on.
        StreakField.Streak s = new StreakField.Streak();
        int contested = 0;
        int lanes = StreakField.lanes(R);
        for (int la = -lanes; la <= lanes; la++) {
            for (int lb = -lanes; lb <= lanes; lb++) {
                if (StreakField
                    .at(3, la, lb, 20, 21.05F * StreakField.PERIOD / StreakField.SLOTS, R, EX, EY, EZ, 0.55F, s)) {
                    contested++;
                    assertEquals(0.55F, s.rush);
                }
            }
        }
        assertTrue(contested > 100, "sent in a clash: " + contested);
        assertEquals(starting, sent(0, 2));
    }

    private static int sent(int fromMoment, int toMoment) {
        StreakField.Streak s = new StreakField.Streak();
        int count = 0;
        int lanes = StreakField.lanes(R);
        for (int moment = fromMoment; moment < toMoment; moment++) {
            float justAfter = (moment + 1.05F) * StreakField.PERIOD / (float) StreakField.SLOTS;
            for (int la = -lanes; la <= lanes; la++) {
                for (int lb = -lanes; lb <= lanes; lb++) {
                    if (StreakField.at(3, la, lb, moment, justAfter, R, EX, EY, EZ, s)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static double sq(double v) {
        return v * v;
    }
}
