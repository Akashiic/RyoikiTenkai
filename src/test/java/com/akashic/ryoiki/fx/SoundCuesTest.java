package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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

    @Test
    void everyDomainHasItsSounds() {
        List<String> all = all();
        assertEquals(14, all.size());
        assertTrue(all.contains("clash_trilha"));
        assertTrue(all.contains("vazio_trilha"));
        assertTrue(all.contains("santuario_trilha"));
        assertTrue(all.contains("santuario_corte"));
        assertFalse(all.contains("vazio_corte"));
    }

    @Test
    void theSoundsJsonHasOnlyWhatIsThere() {
        assertEquals("{}", SoundCues.soundsJson(Arrays.asList()));
        assertEquals(
            "{\"vazio_trilha\":{\"category\":\"master\",\"sounds\":[{\"name\":\"vazio_trilha\",\"stream\":true}]},"
                + "\"saida\":{\"category\":\"player\",\"sounds\":[{\"name\":\"saida\",\"stream\":false}]}}",
            SoundCues.soundsJson(Arrays.asList("vazio_trilha", "saida")));
    }

    @Test
    void whatIsHeardFromAPlaceIsMono() {
        assertTrue(SoundCues.mono(SoundCues.SIGN));
        assertTrue(SoundCues.mono(SoundCues.DOME));
        assertFalse(SoundCues.mono(SoundCues.of("vazio", SoundCues.VOICE)));
        assertFalse(SoundCues.mono(SoundCues.CLASH_TRACK));
        assertTrue(SoundCues.streamed(SoundCues.CLASH_TRACK));
        assertTrue(SoundCues.track(SoundCues.CLASH_TRACK));
        assertFalse(SoundCues.track(SoundCues.of("vazio", SoundCues.AMBIENT)));
        assertTrue(SoundCues.mono(SoundCues.of("santuario", SoundCues.ENTER)));
        assertFalse(SoundCues.mono(SoundCues.of("vazio", SoundCues.AMBIENT)));
        assertFalse(SoundCues.mono(SoundCues.EXIT));
        assertFalse(SoundCues.mono(SoundCues.of("vazio", SoundCues.TRACK)));
        assertTrue(SoundCues.streamed(SoundCues.of("vazio", SoundCues.TRACK)));
        assertFalse(SoundCues.streamed(SoundCues.SIGN));
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
