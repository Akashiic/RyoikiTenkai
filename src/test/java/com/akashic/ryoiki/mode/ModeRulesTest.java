package com.akashic.ryoiki.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ModeRulesTest {

    @Test
    void inAndOutAreFreeWithABreathBetween() {
        assertEquals(ModeRules.OK, ModeRules.ask(true, true, true, false, 1000, 20));
        assertEquals(ModeRules.OK, ModeRules.ask(false, true, true, false, 20, 20));
        assertEquals(ModeRules.SOON, ModeRules.ask(true, true, true, false, 19, 20));
        assertEquals(ModeRules.SOON, ModeRules.ask(false, true, true, false, 0, 20));
    }

    @Test
    void notInADomainAndNotDead() {
        // Neither way: who was in the mode when the domain opened stays in it.
        assertEquals(ModeRules.DOMAIN, ModeRules.ask(true, true, true, true, 1000, 20));
        assertEquals(ModeRules.DOMAIN, ModeRules.ask(false, true, true, true, 1000, 20));
        assertEquals(ModeRules.DEAD, ModeRules.ask(true, true, false, false, 1000, 20));
        assertEquals(ModeRules.DEAD, ModeRules.ask(false, true, false, true, 0, 20));
    }

    @Test
    void whereTheModeIsOffNobodyGoesInButAnyoneGoesOut() {
        assertEquals(ModeRules.DISABLED, ModeRules.ask(true, false, true, false, 1000, 20));
        assertEquals(ModeRules.OK, ModeRules.ask(false, false, true, false, 1000, 20));
    }

    @Test
    void theDomainIsTheModesAndWaitsItsRecharge() {
        assertEquals(ModeRules.OK, ModeRules.expand(true, true, 0));
        assertEquals(ModeRules.OK, ModeRules.expand(true, true, -500));
        assertEquals(ModeRules.RECHARGING, ModeRules.expand(true, true, 1));
        assertEquals(ModeRules.NO_TECHNIQUE, ModeRules.expand(true, false, 0));
        // Out of the mode nothing of it works, whatever else is true.
        assertEquals(ModeRules.NOT_IN_MODE, ModeRules.expand(false, true, 0));
        assertEquals(ModeRules.NOT_IN_MODE, ModeRules.expand(false, false, 99));
    }

    @Test
    void onlySomeRefusalsNeedWords() {
        assertNotNull(ModeRules.key(ModeRules.DISABLED));
        assertNotNull(ModeRules.key(ModeRules.DOMAIN));
        // Dead, or a second press right after the first: nothing to say.
        assertNull(ModeRules.key(ModeRules.SOON));
        assertNull(ModeRules.key(ModeRules.DEAD));
        assertNull(ModeRules.key(ModeRules.OK));
        // The screen already shows a recharge, an empty place and the mode: no words for those either.
        assertNull(ModeRules.key(ModeRules.RECHARGING));
        assertNull(ModeRules.key(ModeRules.NO_TECHNIQUE));
        assertNull(ModeRules.key(ModeRules.NOT_IN_MODE));
    }
}
