package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.akashic.ryoiki.DomainType;

class SoundCuesTest {

    private static List<String> all() {
        String[] ids = new String[DomainType.values().length];
        for (DomainType t : DomainType.values()) {
            ids[t.ordinal()] = t.id;
        }
        return SoundCues.all(ids, new String[] { DomainType.MALEVOLENT_SHRINE.id });
    }

    private static byte[] read(String path) throws Exception {
        try (InputStream in = SoundCuesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0;) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    @Test
    void everySoundHasAnEventAndAFileOfTheRightChannels() throws Exception {
        String json = new String(read("/assets/ryoikitenkai/sounds.json"), StandardCharsets.UTF_8);
        List<String> all = all();
        assertEquals(11, all.size());
        for (String name : all) {
            assertTrue(json.contains("\"" + name + "\": {"), name + " is not in sounds.json");
            byte[] ogg = read("/assets/ryoikitenkai/sounds/" + name + ".ogg");
            assertEquals("OggS", new String(ogg, 0, 4, StandardCharsets.US_ASCII), name);
            // The Vorbis identification header: "\1vorbis", the version, then the channel count.
            assertEquals("vorbis", new String(ogg, 29, 6, StandardCharsets.US_ASCII), name);
            assertEquals(SoundCues.mono(name) ? 1 : 2, ogg[39], name + " channels");
        }
    }

    @Test
    void whatIsHeardFromAPlaceIsMono() {
        assertTrue(SoundCues.mono(SoundCues.SIGN));
        assertTrue(SoundCues.mono(SoundCues.DOME));
        assertTrue(SoundCues.mono(SoundCues.of("vazio", SoundCues.VOICE)));
        assertTrue(SoundCues.mono(SoundCues.of("santuario", SoundCues.ENTER)));
        assertFalse(SoundCues.mono(SoundCues.of("vazio", SoundCues.AMBIENT)));
        assertFalse(SoundCues.mono(SoundCues.EXIT));
    }

    @Test
    void ownerFilesArePlainNamesOnly() {
        assertEquals("vazio_ambiente.ogg", SoundCues.externalFile("sounds/vazio_ambiente.ogg"));
        assertNull(SoundCues.externalFile("sounds/../segredo.ogg"));
        assertNull(SoundCues.externalFile("sounds/pasta/som.ogg"));
        assertNull(SoundCues.externalFile("sounds/.ogg"));
        assertNull(SoundCues.externalFile("textures/blocks/veu.png"));
        assertNull(SoundCues.externalFile("sounds.json"));
        assertNull(SoundCues.externalFile(null));
    }
}
