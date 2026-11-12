package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlashFieldTest {

    @Test
    void aCutStartsEveryPeriodAndLastsItsLife() {
        assertTrue(SlashField.startsAt(0));
        assertTrue(SlashField.startsAt(SlashField.PERIOD * 5));
        assertFalse(SlashField.startsAt(SlashField.PERIOD * 5 + 1));
        SlashField.Slash s = new SlashField.Slash();
        assertTrue(SlashField.at(9, 3, 3 * SlashField.PERIOD, 13, s));
        assertEquals(0, s.life);
        assertTrue(SlashField.at(9, 3, 3 * SlashField.PERIOD + SlashField.LIFE - 0.5F, 13, s));
        assertTrue(s.life > 0.9F);
        assertFalse(SlashField.at(9, 3, 3 * SlashField.PERIOD + SlashField.LIFE, 13, s));
        assertFalse(SlashField.at(9, 3, 3 * SlashField.PERIOD - 0.1F, 13, s));
        assertFalse(SlashField.at(9, -1, 0, 13, s));
    }

    @Test
    void everyClientSeesTheSameCutAndEachCutIsItsOwn() {
        SlashField.Slash a = new SlashField.Slash();
        SlashField.Slash b = new SlashField.Slash();
        SlashField.at(77, 12, 12 * SlashField.PERIOD + 2, 13, a);
        SlashField.at(77, 12, 12 * SlashField.PERIOD + 2, 13, b);
        assertEquals(a.x, b.x);
        assertEquals(a.dirZ, b.dirZ);
        SlashField.at(77, 13, 13 * SlashField.PERIOD + 2, 13, b);
        assertNotEquals(a.x, b.x);
        SlashField.at(78, 12, 12 * SlashField.PERIOD + 2, 13, b);
        assertNotEquals(a.x, b.x);
    }

    @Test
    void cutsStayInsideTheArenaAndTheirDirectionIsAUnit() {
        SlashField.Slash s = new SlashField.Slash();
        for (int i = 0; i < 500; i++) {
            assertTrue(SlashField.at(5, i, i * SlashField.PERIOD + 1, 13, s));
            assertTrue(Math.hypot(s.x, s.z) <= 10.001);
            assertTrue(s.y >= 0.5F && s.y <= 2.9F);
            assertEquals(1, Math.sqrt(s.dirX * s.dirX + s.dirY * s.dirY + s.dirZ * s.dirZ), 1e-4);
        }
    }
}
