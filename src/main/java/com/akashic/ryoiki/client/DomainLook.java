package com.akashic.ryoiki.client;

/** How one domain looks from inside. Everything is drawn in flat colour by code: the arena itself is invisible. */
interface DomainLook {

    /**
     * The sky, around the camera: called with the camera at the origin and nothing written to the depth buffer.
     *
     * @param radius how far away to draw it
     * @param time   ticks since the player entered, with the fraction
     */
    void sky(float radius, float time);

    /**
     * The sky's ground colours at a time, as {@link Draw#sphere} takes them: top, horizon, below, and the band's
     * power.
     */
    float[] backdrop(float time);

    /** What is in the sky over its ground colours, with the camera at the origin: its lights, and their reflection. */
    void skyFeatures(float radius, float time);

    /** The floor, with the origin at the arena's centre: the sky's reflection shows through it. */
    void floor(float far, int radius, float time);

    /**
     * What stands or drifts in the arena, with the origin at the arena's centre.
     *
     * @param seed the domain's number: what is random is the same for everyone in it
     * @param camX the camera, relative to the arena's centre
     */
    void props(float far, int radius, float time, int seed, double camX, double camY, double camZ);

    /** The colour the distance fades to. */
    int fogColor();

    /** The colour of the shards the domain breaks into, and of the lines between them. */
    int shardColor();

    int shardEdgeColor();

    /** The cut-in's colours: the band from one side to the other, and its trim. */
    int bandFrom();

    int bandTo();

    int bandTrim();
}
