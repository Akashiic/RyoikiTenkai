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
        // A recharge of a technique there is not, with more left than it lasts, or longer than any is.
        assertNull(ModeWire.decode(ModeWire.cooldown(0, 100, 50)));
        assertNull(ModeWire.decode(ModeWire.cooldown(5, 100, 50)));
        assertNull(ModeWire.decode(ModeWire.cooldown(2, 100, 101)));
        assertNull(ModeWire.decode(ModeWire.cooldown(2, 100, -1)));
        assertNull(ModeWire.decode(ModeWire.cooldown(2, ModeWire.LONGEST + 1, 5)));
        assertNotNull(ModeWire.decode(new byte[] { ModeWire.ASK, 0 }));
    }
}
