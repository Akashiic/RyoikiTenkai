package com.akashic.ryoiki.domain;

/**
 * The score of a clash of two domains (docs/ESTUDO.md, section 5.4): each caster fills a bar by hitting the other,
 * 6.25 % a hit. Full first wins; when the time is up, the fuller wins, and equal bars break both domains. No
 * Minecraft classes.
 */
public final class ClashScore {

    /** How long a clash lasts: 15 s. */
    public static final int TICKS = 15 * 20;
    public static final int HITS_TO_WIN = 16;
    /** Ticks between two hits of the same caster that count: clicking faster adds nothing. */
    public static final int HIT_INTERVAL = 6;

    /** The caster whose domain was first. */
    public static final int HOST = 0;
    /** The caster who answered with a domain of its own. */
    public static final int RIVAL = 1;
    public static final int TIE = 2;
    public static final int NONE = -1;

    private final int[] hits = new int[2];
    private final long[] last = { Long.MIN_VALUE / 2, Long.MIN_VALUE / 2 };

    /** A hit by one side at a server tick. Returns whether it counted. */
    public boolean hit(int side, long tick) {
        if (hits[side] >= HITS_TO_WIN || tick - last[side] < HIT_INTERVAL) {
            return false;
        }
        last[side] = tick;
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
