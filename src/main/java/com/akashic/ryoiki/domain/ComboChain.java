package com.akashic.ryoiki.domain;

/**
 * The four blows of a caster in a clash, as a chain: each click begins the next blow if the one before has had its
 * time, a click a moment too early is kept and begins as soon as it may, and a pause lets the chain drop back to the
 * first blow. The fourth is the finisher, and takes longer to recover from. No Minecraft classes.
 */
public final class ComboChain {

    public static final int BLOWS = 4;
    /** Ticks from one blow to the next. */
    public static final int GAP = 6;
    /** Ticks after the finisher before a new chain. */
    public static final int RECOVERY = 14;
    /** A click this many ticks before the next blow may begin is kept for it. */
    public static final int BUFFER = 4;
    /** Ticks without a blow after which the chain starts over. */
    public static final int DROP = 18;

    private static final long NEVER = Long.MIN_VALUE / 2;

    private int blow;
    private long last = NEVER;
    private long due = NEVER;

    /** A click at a tick. Returns the blow that begins now (1 to 4), or 0 when it is too early. */
    public int click(long tick) {
        long ready = last + pause();
        if (tick >= ready) {
            return begin(tick);
        }
        if (ready - tick <= BUFFER) {
            due = ready;
        }
        return 0;
    }

    /** Each tick: the blow of a kept click that begins now, or 0. */
    public int tick(long tick) {
        return due != NEVER && tick >= due ? begin(tick) : 0;
    }

    /** The blow last begun: 0 before the first. */
    public int blow() {
        return blow;
    }

    private int pause() {
        return blow == BLOWS ? RECOVERY : GAP;
    }

    private int begin(long tick) {
        if (blow == BLOWS || tick - last > DROP) {
            blow = 0;
        }
        blow++;
        last = tick;
        due = NEVER;
        return blow;
    }
}
