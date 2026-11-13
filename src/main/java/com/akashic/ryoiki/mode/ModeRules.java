package com.akashic.ryoiki.mode;

/**
 * When a player may go into the sorcerer mode and out of it. The mode is the player's other way of being in the
 * world: no inventory, no blocks, no items, only its technique. Going in and out is free, with a breath between two
 * changes, but not in the middle of a domain: who was in the mode when it opened stays in it, and who was not stays
 * out. No Minecraft classes.
 */
public final class ModeRules {

    public static final int OK = 0;
    /** The server has the mode turned off. */
    public static final int DISABLED = 1;
    public static final int DEAD = 2;
    /** In a domain, or making the hand sign of one. */
    public static final int DOMAIN = 3;
    /** Changed a moment ago. */
    public static final int SOON = 4;
    /** Not in the mode: the techniques are the mode's. */
    public static final int NOT_IN_MODE = 5;
    /** No technique: nothing to use. */
    public static final int NO_TECHNIQUE = 6;
    /** Used a while ago, and not ready again yet. */
    public static final int RECHARGING = 7;
    /** The server gives the mode only to who has its permission, and this player has not. */
    public static final int NO_PERMISSION = 8;

    private ModeRules() {}

    /**
     * Whether a player may change to the mode it wants: {@link #OK}, or why not.
     *
     * @param want      the mode asked for
     * @param enabled   whether the server allows the mode at all
     * @param permitted whether this player has the permission the server asks for it
     * @param inDomain  whether the player is in a domain or casting one
     * @param since     ticks since its last change
     * @param cooldown  ticks there must be between two changes
     */
    public static int ask(boolean want, boolean enabled, boolean permitted, boolean alive, boolean inDomain, long since,
        int cooldown) {
        if (!alive) {
            return DEAD;
        }
        if (want && !enabled) {
            // Going out is always allowed where the mode is turned off: nobody is left stuck in it.
            return DISABLED;
        }
        if (want && !permitted) {
            // The same for who lost the permission while in the mode.
            return NO_PERMISSION;
        }
        if (inDomain) {
            return DOMAIN;
        }
        if (since < cooldown) {
            return SOON;
        }
        return OK;
    }

    /**
     * Whether a player may use one of the mode's four techniques: {@link #OK}, or why not. (For the fourth, whether
     * the domain can open where the player stands is another matter, the domain's own.)
     *
     * @param inMode       whether the player is in the sorcerer mode
     * @param hasTechnique whether it has a technique, and so a domain
     * @param left         how long until that technique is ready again, in any unit: ready at 0 or less
     */
    public static int use(boolean inMode, boolean hasTechnique, long left) {
        if (!inMode) {
            return NOT_IN_MODE;
        }
        if (!hasTechnique) {
            return NO_TECHNIQUE;
        }
        return left > 0 ? RECHARGING : OK;
    }

    /** The translation key of a refusal, to show the player; null for one that needs no words. */
    public static String key(int reason) {
        switch (reason) {
            case DISABLED:
                return "ryoikitenkai.modo.desligado";
            case DOMAIN:
                return "ryoikitenkai.modo.dominio";
            case NO_PERMISSION:
                return "ryoikitenkai.modo.permissao";
            default:
                return null;
        }
    }
}
