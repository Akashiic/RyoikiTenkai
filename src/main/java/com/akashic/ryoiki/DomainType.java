package com.akashic.ryoiki;

/** The domains. Times in ticks, from the study of Jujutsu Shenanigans (docs/ESTUDO.md). */
public enum DomainType {

    /** Gojo's Infinite Void: white border, 14 s. */
    INFINITE_VOID("vazio", 14 * 20, 0xFFFFFF),
    /** Sukuna's Malevolent Shrine: black border, 18 s. */
    MALEVOLENT_SHRINE("santuario", 18 * 20, 0x050505);

    /** The hand sign before the domain opens: 1.25 s. */
    public static final int WINDUP_TICKS = 25;

    public final String id;
    public final int durationTicks;
    /** The colour of the dome those outside see. */
    public final int borderColor;

    DomainType(String id, int durationTicks, int borderColor) {
        this.id = id;
        this.durationTicks = durationTicks;
        this.borderColor = borderColor;
    }

    public static DomainType byId(String id) {
        for (DomainType t : values()) {
            if (t.id.equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }
}
