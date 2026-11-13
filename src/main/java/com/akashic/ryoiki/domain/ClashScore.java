package com.akashic.ryoiki.domain;

/**
 * The score of a clash of two domains (docs/ESTUDO.md, section 5.4): each caster fills a bar by hitting the other,
 * 6.25 % a blow that lands (their pace is {@link ComboChain}'s). Full first wins; when the time is up, the fuller wins,
 * and equal bars break both domains. No
 * Minecraft classes.
 */
public final class ClashScore {

    /** How long a clash lasts: 15 s. */
    public static final int TICKS = 15 * 20;
    public static final int HITS_TO_WIN = 16;
    /** The caster whose domain was first. */
    public static final int HOST = 0;
    /** The caster who answered with a domain of its own. */
    public static final int RIVAL = 1;
    public static final int TIE = 2;
    public static final int NONE = -1;

    private final int[] hits = new int[2];

    /** A blow of one side that landed. Returns whether it counted (a full bar takes no more). */
    public boolean hit(int side) {
        if (hits[side] >= HITS_TO_WIN) {
            return false;
        }
        hits[side]++;
        return true;
    }

    public int hits(int side) {
        return hits[side];
    }

    /** The bar of a number of hits, from 0 to 1. */
    public static float bar(int hits) {
        return Math.min(1F, hits / (float) HITS_TO_WIN);
    }

    /** Who has a full bar, or {@link #NONE}. */
    public int full() {
        return hits[HOST] >= HITS_TO_WIN ? HOST : hits[RIVAL] >= HITS_TO_WIN ? RIVAL : NONE;
    }

    /** Who wins when the time is up. */
    public int atTimeUp() {
        return hits[HOST] > hits[RIVAL] ? HOST : hits[RIVAL] > hits[HOST] ? RIVAL : TIE;
    }
}
