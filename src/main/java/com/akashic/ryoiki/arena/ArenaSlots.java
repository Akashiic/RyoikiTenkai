package com.akashic.ryoiki.arena;

/**
 * Where the arenas are: a row of slots far from the map, one per domain at a time. A slot's centre is a chunk corner,
 * so its arena is a square of chunks around it. No Minecraft classes.
 */
public final class ArenaSlots {

    /** Blocks between two slots: far beyond any view distance, so two arenas never share a loaded chunk. */
    public static final int SPACING = 1024;

    private final int baseX;
    private final int baseZ;
    public final int floorY;
    /** Chunks to each side of a centre that an arena takes. */
    public final int span;
    private final Object[] owners;

    /**
     * @param baseX  the first slot's centre, rounded down to a chunk corner
     * @param floorY the Y of the floor's top (players stand at this Y)
     */
    public ArenaSlots(int baseX, int baseZ, int floorY, int slots, int span) {
        this.span = span;
        this.baseX = baseX & ~15;
        this.baseZ = baseZ & ~15;
        this.floorY = floorY;
        this.owners = new Object[slots];
    }

    public int size() {
        return owners.length;
    }

    public int centerX(int slot) {
        return baseX + slot * SPACING;
    }

    public int centerZ(int slot) {
        return baseZ;
    }

    /** The chunks of a slot, as {x, z} pairs. */
    public int[][] chunks(int slot) {
        return chunks(slot, span);
    }

    /** The chunks within {@code reach} chunks to each side of a slot's centre. */
    public int[][] chunks(int slot, int reach) {
        int cx = centerX(slot) >> 4;
        int cz = centerZ(slot) >> 4;
        int[][] out = new int[reach * reach * 4][];
        int n = 0;
        for (int x = cx - reach; x < cx + reach; x++) {
            for (int z = cz - reach; z < cz + reach; z++) {
                out[n++] = new int[] { x, z };
            }
        }
        return out;
    }

    /** Takes the first free slot for this owner, or returns -1 when all are taken. */
    public int take(Object owner) {
        for (int i = 0; i < owners.length; i++) {
            if (owners[i] == null) {
                owners[i] = owner;
                return i;
            }
        }
        return -1;
    }

    public void release(int slot) {
        owners[slot] = null;
    }

    public int taken() {
        int n = 0;
        for (Object o : owners) {
            if (o != null) {
                n++;
            }
        }
        return n;
    }

    /** Whether a position is inside the band of the arenas (used to rescue a player a crash left there). */
    public boolean inArenaRegion(double x, double z) {
        double half = SPACING / 2.0;
        return z >= baseZ - half && z < baseZ + half
            && x >= baseX - half
            && x < baseX + (owners.length - 1) * (double) SPACING + half;
    }
}
