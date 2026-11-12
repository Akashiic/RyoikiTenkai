package com.akashic.ryoiki.domain;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.akashic.ryoiki.DomainType;

/** One domain, from the hand sign to its end. Changed by {@link DomainManager} only. */
public final class DomainSession {

    public enum Phase {
        /** The hand sign: nobody has moved yet. */
        WINDUP,
        /** Open: those caught are in the arena. */
        ACTIVE,
        ENDED
    }

    /** Where a player was when the domain caught it. */
    public static final class ReturnPoint {

        public final int dimension;
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;

        public ReturnPoint(int dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public final int id;
    public final DomainType type;
    public final UUID caster;
    public final int dimension;
    /** Where it was cast: the caster's feet. */
    public final double x;
    public final double y;
    public final double z;
    public final int radius;
    public final int slot;
    /** Where the caster looked when it began the hand sign: the domain's scene is laid out by it. */
    public final float yaw;

    Phase phase = Phase.WINDUP;
    /** Ticks in the current phase. */
    int age;
    /** Who is in the arena, and where each goes back to. */
    final Map<UUID, ReturnPoint> inside = new LinkedHashMap<>();
    /** Who was told about this domain (the hand sign or the dome) and must be told when it ends. */
    final Set<UUID> told = new HashSet<>();

    DomainSession(int id, DomainType type, UUID caster, int dimension, double x, double y, double z, int radius,
        int slot, float yaw) {
        this.yaw = yaw;
        this.id = id;
        this.type = type;
        this.caster = caster;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.slot = slot;
    }

    public Phase phase() {
        return phase;
    }

    public int age() {
        return age;
    }

    public boolean holds(UUID player) {
        return inside.containsKey(player);
    }

    public int insideCount() {
        return inside.size();
    }
}
