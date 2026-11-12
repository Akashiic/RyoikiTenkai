package com.akashic.ryoiki.arena;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.ForgeChunkManager;

import com.akashic.ryoiki.Config;
import com.akashic.ryoiki.RyoikiTenkai;
import com.akashic.ryoiki.block.BlockVeil;

/**
 * The arenas of each dimension: builds them (once), keeps their four chunks loaded and empties the terrain around
 * them. An arena is made of {@link BlockVeil} only, in a region where every chunk in view is empty: the server has
 * next to nothing to send when players arrive and the client has nothing to mesh. Main server thread only.
 */
public final class ArenaManager {

    public static final ArenaManager INSTANCE = new ArenaManager();

    /** Chunks emptied per tick by the background job. */
    private static final int VOID_PER_TICK = 6;

    private static final class Dimension {

        ArenaSlots slots;
        ForgeChunkManager.Ticket ticket;
        final ArrayDeque<long[]> toVoid = new ArrayDeque<>();
        final boolean[] ready;
        boolean surroundingsQueued;

        Dimension(ArenaSlots slots) {
            this.slots = slots;
            this.ready = new boolean[slots.size()];
        }
    }

    private final Map<Integer, Dimension> dimensions = new HashMap<>();

    private ArenaManager() {}

    private Dimension dimension(WorldServer world) {
        Dimension d = dimensions.get(world.provider.dimensionId);
        if (d == null) {
            d = new Dimension(new ArenaSlots(Config.arenaX, Config.arenaZ, Config.arenaY, Config.maxDomains));
            dimensions.put(world.provider.dimensionId, d);
        }
        return d;
    }

    public ArenaSlots slots(WorldServer world) {
        return dimension(world).slots;
    }

    public ArenaShape shape() {
        return new ArenaShape(Config.radius);
    }

    /**
     * Makes a slot's arena exist and stay loaded. The first time in a world this builds it (a few thousand blocks,
     * once) and queues the emptying of the terrain around all the slots. Returns an error for the caster, or null.
     */
    public String prepare(WorldServer world, int slot) {
        Dimension d = dimension(world);
        if (!d.ready[slot]) {
            int[][] chunks = d.slots.chunks(slot);
            for (int[] c : chunks) {
                Chunk chunk = world.theChunkProviderServer.loadChunk(c[0], c[1]);
                if (!isArena(world, d.slots, slot) && !empty(chunk)) {
                    if (chunk.inhabitedTime > 0) {
                        return "ryoikitenkai.erro.regiao";
                    }
                    voidChunk(world, chunk);
                }
            }
            if (!isArena(world, d.slots, slot)) {
                build(world, d.slots, slot);
            }
            if (d.ticket == null) {
                d.ticket = ForgeChunkManager.requestTicket(RyoikiTenkai.instance, world, ForgeChunkManager.Type.NORMAL);
            }
            if (d.ticket != null) {
                for (int[] c : chunks) {
                    ForgeChunkManager.forceChunk(d.ticket, new ChunkCoordIntPair(c[0], c[1]));
                }
            }
            d.ready[slot] = true;
        }
        if (!d.surroundingsQueued) {
            d.surroundingsQueued = true;
            queueSurroundings(world, d);
        }
        return null;
    }

    /** The four chunks of a slot, loaded. */
    public List<Chunk> chunks(WorldServer world, int slot) {
        List<Chunk> out = new ArrayList<>(4);
        for (int[] c : dimension(world).slots.chunks(slot)) {
            out.add(world.theChunkProviderServer.loadChunk(c[0], c[1]));
        }
        return out;
    }

    /** Chunks still to empty around this world's arenas. */
    public int pending(WorldServer world) {
        return dimension(world).toVoid.size();
    }

    private static boolean isArena(WorldServer world, ArenaSlots slots, int slot) {
        return world.getBlock(slots.centerX(slot), slots.floorY - 1, slots.centerZ(slot)) == RyoikiTenkai.veil;
    }

    private void build(WorldServer world, ArenaSlots slots, int slot) {
        final int cx = slots.centerX(slot);
        final int cy = slots.floorY;
        final int cz = slots.centerZ(slot);
        final int[] count = { 0 };
        shape().forEach((dx, dy, dz, part) -> {
            int meta = part == ArenaShape.Part.FLOOR ? BlockVeil.FLOOR : BlockVeil.SHELL;
            world.setBlock(cx + dx, cy + dy, cz + dz, RyoikiTenkai.veil, meta, 2);
            count[0]++;
        });
        RyoikiTenkai.LOG.info(
            "Arena {} built in dimension {} at {}, {}, {} ({} blocks)",
            slot,
            world.provider.dimensionId,
            cx,
            cy,
            cz,
            count[0]);
    }

    /** Every chunk a player in an arena has in view, nearest first, except the arenas' own. */
    private void queueSurroundings(WorldServer world, Dimension d) {
        int view = world.func_73046_m()
            .getConfigurationManager()
            .getViewDistance() + 1;
        List<long[]> all = new ArrayList<>();
        for (int slot = 0; slot < d.slots.size(); slot++) {
            int cx = d.slots.centerX(slot) >> 4;
            int cz = d.slots.centerZ(slot) >> 4;
            for (int x = cx - view - 1; x <= cx + view; x++) {
                for (int z = cz - view - 1; z <= cz + view; z++) {
                    boolean own = (x == cx || x == cx - 1) && (z == cz || z == cz - 1);
                    if (!own) {
                        long dx = x - cx;
                        long dz = z - cz;
                        all.add(new long[] { x, z, dx * dx + dz * dz });
                    }
                }
            }
        }
        all.sort((a, b) -> Long.compare(a[2], b[2]));
        d.toVoid.addAll(all);
    }

    /** Once per server tick: empties a few of the queued chunks. */
    public void tick(WorldServer world) {
        Dimension d = dimensions.get(world.provider.dimensionId);
        if (d == null || d.toVoid.isEmpty()) {
            return;
        }
        int refused = 0;
        for (int i = 0; i < VOID_PER_TICK && !d.toVoid.isEmpty(); i++) {
            long[] c = d.toVoid.poll();
            int x = (int) c[0];
            int z = (int) c[1];
            boolean wasLoaded = world.theChunkProviderServer.chunkExists(x, z);
            Chunk chunk = world.theChunkProviderServer.loadChunk(x, z);
            if (!empty(chunk)) {
                if (chunk.inhabitedTime > 0) {
                    refused++;
                } else {
                    voidChunk(world, chunk);
                }
            }
            if (!wasLoaded) {
                world.theChunkProviderServer.unloadChunksIfNotNearSpawn(x, z);
            }
        }
        if (refused > 0) {
            RyoikiTenkai.LOG.warn(
                "{} chunk(s) around the arenas of dimension {} were left as they are: players have been there. "
                    + "Move the arenas (arena.x / arena.z in the config).",
                refused,
                world.provider.dimensionId);
        }
        if (d.toVoid.isEmpty()) {
            RyoikiTenkai.LOG.info("The terrain around the arenas of dimension {} is empty", world.provider.dimensionId);
        }
    }

    private static boolean empty(Chunk chunk) {
        for (ExtendedBlockStorage s : chunk.getBlockStorageArray()) {
            if (s != null) {
                return false;
            }
        }
        return true;
    }

    /** Removes everything from a chunk and marks it complete, so the server sends it at once. */
    @SuppressWarnings("unchecked")
    private static void voidChunk(WorldServer world, Chunk chunk) {
        for (TileEntity te : new ArrayList<TileEntity>(chunk.chunkTileEntityMap.values())) {
            world.removeTileEntity(te.xCoord, te.yCoord, te.zCoord);
        }
        chunk.chunkTileEntityMap.clear();
        for (List<Entity> list : chunk.entityLists) {
            for (Entity e : list) {
                e.setDead();
            }
        }
        chunk.setStorageArrays(new ExtendedBlockStorage[16]);
        Arrays.fill(chunk.precipitationHeightMap, -999);
        chunk.generateSkylightMap();
        chunk.isTerrainPopulated = true;
        chunk.isLightPopulated = true;
        chunk.field_150815_m = true;
        chunk.setChunkModified();
    }

    /** The server is stopping: nothing of this run is valid for the next. */
    public void reset() {
        dimensions.clear();
    }
}
