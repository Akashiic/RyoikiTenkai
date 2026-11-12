package com.akashic.ryoiki.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.akashic.ryoiki.DomainType;

class DomainWireTest {

    @Test
    void channelNameFitsAPluginChannel() {
        assertTrue(DomainWire.CHANNEL.length() <= 20);
    }

    @Test
    void enterRoundTrips() {
        DomainWire.Message m = DomainWire
            .read(DomainWire.enter(7, DomainType.MALEVOLENT_SHRINE, 4321, 200000, 120, -48.5, 13, 360, -12, 30, 10));
        assertEquals(DomainWire.ENTER, m.op);
        assertEquals(7, m.session);
        assertEquals(DomainType.MALEVOLENT_SHRINE, m.type);
        assertEquals(4321, m.casterEntity);
        assertEquals(200000, m.x);
        assertEquals(120, m.y);
        assertEquals(-48.5, m.z);
        assertEquals(13, m.radius);
        assertEquals(360, m.ticks);
        assertEquals(-12, m.keepChunkX);
        assertEquals(30, m.keepChunkZ);
        assertEquals(10, m.keepRadius);
    }

    @Test
    void windupDomeAndTheBareMessagesRoundTrip() {
        DomainWire.Message w = DomainWire
            .read(DomainWire.windup(1, DomainType.INFINITE_VOID, 9, 1.5, 64, -3.25, 13, 25));
        assertEquals(DomainWire.WINDUP, w.op);
        assertEquals(DomainType.INFINITE_VOID, w.type);
        assertEquals(25, w.ticks);
        assertEquals(-3.25, w.z);
        DomainWire.Message d = DomainWire.read(DomainWire.dome(2, DomainType.INFINITE_VOID, 1, 2, 3, 14, 280));
        assertEquals(DomainWire.DOME, d.op);
        assertEquals(280, d.ticks);
        assertEquals(DomainWire.EXIT, DomainWire.read(DomainWire.exit(5)).op);
        assertEquals(5, DomainWire.read(DomainWire.exit(5)).session);
        assertEquals(DomainWire.DOME_END, DomainWire.read(DomainWire.domeEnd(6)).op);
    }

    @Test
    void whatThisVersionDoesNotKnowIsIgnored() {
        assertNull(DomainWire.read(new byte[] { 99, 0, 0, 0, 1 }));
        assertNull(DomainWire.read(new byte[] { DomainWire.ENTER, 0, 0 }));
        byte[] newType = DomainWire.windup(1, DomainType.INFINITE_VOID, 9, 0, 0, 0, 13, 25);
        newType[5] = 50;
        assertNull(DomainWire.read(newType));
    }
}
