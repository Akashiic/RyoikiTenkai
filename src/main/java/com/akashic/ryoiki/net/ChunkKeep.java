package com.akashic.ryoiki.net;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which chunks the client keeps while its player is in a domain. The server unloads the chunks around the place the
 * player left (a chunk packet with nothing in it); the client ignores those unloads, so on the way back that terrain
 * is still in memory. A while after the domain, {@link #release} names the kept chunks that are too far from where the
 * player landed to have been sent again: those are dropped for real. No Minecraft classes; client thread only.
 */
public final class ChunkKeep {

    private boolean active;
    private int centerX;
    private int centerZ;
    private int radius;
    private final Set<Long> kept = new HashSet<>();

    public void begin(int chunkX, int chunkZ, int chunkRadius) {
        active = true;
        centerX = chunkX;
        centerZ = chunkZ;
        radius = chunkRadius;
    }

    public boolean active() {
        return active;
    }

    /** Called for each unload the server sends; true when the client must ignore it. */
    public boolean keeps(int chunkX, int chunkZ) {
        if (!active || Math.abs(chunkX - centerX) > radius || Math.abs(chunkZ - centerZ) > radius) {
            return false;
        }
        kept.add(key(chunkX, chunkZ));
        return true;
    }

    public int keptCount() {
        return kept.size();
    }

    /**
     * The kept chunks farther than {@code viewChunks} from the player: the server will not send them again, so the
     * client drops them. The ones in view are forgotten: the server sent them again and unloads them itself from now
     * on. Stops keeping. Returns {x, z} pairs.
     */
    public List<int[]> release(int playerChunkX, int playerChunkZ, int viewChunks) {
        List<int[]> out = new ArrayList<>();
        for (long k : kept) {
            int x = (int) (k >> 32);
            int z = (int) k;
            if (Math.abs(x - playerChunkX) > viewChunks || Math.abs(z - playerChunkZ) > viewChunks) {
                out.add(new int[] { x, z });
            }
        }
        clear();
        return out;
    }

    /** Everything still kept, emptied: for leaving the world. */
    public void clear() {
        active = false;
        kept.clear();
    }

    private static long key(int x, int z) {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }
}
