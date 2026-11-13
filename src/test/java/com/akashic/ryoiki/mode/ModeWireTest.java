package com.akashic.ryoiki.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModeWireTest {

    @Test
    void whatIsWrittenIsRead() {
        ModeWire.Message m = ModeWire.decode(ModeWire.ask(true));
        assertEquals(ModeWire.ASK, m.op);
        assertTrue(m.on);
        assertFalse(ModeWire.decode(ModeWire.ask(false)).on);

        for (int entity : new int[] { 0, 1, 297, 65536, Integer.MAX_VALUE, -5 }) {
            m = ModeWire.decode(ModeWire.state(entity, true));
            assertEquals(ModeWire.STATE, m.op);
            assertEquals(entity, m.entity);
            assertTrue(m.on);
            assertFalse(ModeWire.decode(ModeWire.state(entity, false)).on);
        }

        m = ModeWire.decode(ModeWire.refused(ModeRules.DOMAIN));
        assertEquals(ModeWire.REFUSED, m.op);
        assertEquals(ModeRules.DOMAIN, m.reason);

        for (int technique = 0; technique <= 2; technique++) {
            m = ModeWire.decode(ModeWire.technique(technique));
            assertEquals(ModeWire.TECHNIQUE, m.op);
            assertEquals(technique, m.technique);
        }

        for (int slot = 1; slot <= ModeWire.SLOTS; slot++) {
            m = ModeWire.decode(ModeWire.use(slot));
            assertEquals(ModeWire.USE, m.op);
            assertEquals(slot, m.slot);
        }

        m = ModeWire.decode(ModeWire.effect(AbilityMath.CUT, 297, 200003.25, 121.5, -49.75, 0.6, -0.2, 0.77, 9.5, 7));
        assertEquals(ModeWire.EFFECT, m.op);
        assertEquals(AbilityMath.CUT, m.kind);
        assertEquals(297, m.entity);
        assertEquals(200003.25, m.x, 0);
        assertEquals(121.5, m.y, 0);
        assertEquals(-49.75, m.z, 0);
        assertEquals(0.6F, m.dx, 0);
        assertEquals(-0.2F, m.dy, 0);
        assertEquals(0.77F, m.dz, 0);
        assertEquals(9.5F, m.size, 0);
        assertEquals(7, m.ticks);

        m = ModeWire.decode(ModeWire.cooldown(4, 1200, 1187));
        assertEquals(ModeWire.COOLDOWN, m.op);
        assertEquals(4, m.slot);
        assertEquals(1200, m.total);
        assertEquals(1187, m.left);
        // Ready again: nothing left of nothing.
        m = ModeWire.decode(ModeWire.cooldown(1, 0, 0));
        assertEquals(0, m.total);
    }

    @Test
    void whatIsNotAMessageIsNothing() {
        // A client may send anything on the channel.
        assertNull(ModeWire.decode(null));
        assertNull(ModeWire.decode(new byte[0]));
        assertNull(ModeWire.decode(new byte[] { ModeWire.ASK }));
        assertNull(ModeWire.decode(new byte[] { ModeWire.ASK, 2 }));
        assertNull(ModeWire.decode(new byte[] { ModeWire.ASK, 1, 0 }));
        assertNull(ModeWire.decode(new byte[] { ModeWire.STATE, 0, 0, 0, 1 }));
        assertNull(ModeWire.decode(new byte[] { ModeWire.STATE, 0, 0, 0, 1, 7 }));
        assertNull(ModeWire.decode(new byte[] { 99, 1 }));
        assertNull(ModeWire.decode(new byte[] { ModeWire.TECHNIQUE, -1 }));
        assertNull(ModeWire.decode(ModeWire.use(0)));
        // An effect of no kind, at no place, or cut short.
        assertNull(ModeWire.decode(ModeWire.effect(0, 1, 0, 0, 0, 0, 0, 1, 1, 1)));
        assertNull(ModeWire.decode(ModeWire.effect(9, 1, 0, 0, 0, 0, 0, 1, 1, 1)));
        assertNull(ModeWire.decode(ModeWire.effect(AbilityMath.PULL, 1, Double.NaN, 0, 0, 0, 0, 1, 1, 1)));
        assertNull(
            ModeWire.decode(java.util.Arrays.copyOf(ModeWire.effect(AbilityMath.PULL, 1, 0, 0, 0, 0, 0, 1, 1, 1), 40)));
        assertNull(ModeWire.decode(ModeWire.use(5)));
        assertNull(ModeWire.decode(new byte[] { ModeWire.USE, 4, 0 }));
        // A recharge of a technique there is not, with more left than it lasts, or longer than any is.
        assertNull(ModeWire.decode(ModeWire.cooldown(0, 100, 50)));
        assertNull(ModeWire.decode(ModeWire.cooldown(5, 100, 50)));
        assertNull(ModeWire.decode(ModeWire.cooldown(2, 100, 101)));
        assertNull(ModeWire.decode(ModeWire.cooldown(2, 100, -1)));
        assertNull(ModeWire.decode(ModeWire.cooldown(2, ModeWire.LONGEST + 1, 5)));
        assertNotNull(ModeWire.decode(new byte[] { ModeWire.ASK, 0 }));
    }
}
