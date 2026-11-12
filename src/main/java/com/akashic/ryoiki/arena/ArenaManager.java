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
 * The arenas of each dimension: builds them (once), keeps their chunks loaded and empties the terrain around
 * them. An arena is made of {@link BlockVeil} only, in a region where every chunk in view is empty: the server has
 * next to nothing to send when players arrive and the client has nothing to mesh. Main server thread only.
 */
public final class ArenaManager {

    public static final ArenaManager INSTANCE = new ArenaManager();

    /** Chunks emptied per tick by the background job. */
    private static final int VOID_PER_TICK = 6;

    private static final class Dimension {

        ArenaSlots slots;
        final ForgeChunkManager.Ticket[] tickets;
        final ArrayDeque<long[]> toVoid = new ArrayDeque<>();
        final boolean[] ready;
        boolean surroundingsQueued;

        Dimension(ArenaSlots slots) {
            this.slots = slots;
            this.ready = new boolean[slots.size()];
            this.tickets = new ForgeChunkManager.Ticket[slots.size()];
        }
    }

    private final Map<Integer, Dimension> dimensions = new HashMap<>();

    private ArenaManager() {}

    private Dimension dimension(WorldServer world) {
        Dimension d = dimensions.get(world.provider.dimensionId);
        if (d == null) {
            d = new Dimension(
                new ArenaSlots(
                    Config.arenaX,
                    Config.arenaZ,
                    Config.arenaY,
                    Config.maxDomains,
                    ArenaShape.chunkSpan(Config.radius)));
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
            if (!isArena(world, d.slots, slot)) {
                // Not there, or there with another radius: clear the ground and build it. Where an arena of ours
                // already stands, everything it could have reached is cleared, whoever has been in it.
                boolean ours = centre(world, d.slots, slot);
                int reach = ours ? ArenaShape.chunkSpan(ArenaShape.MAX_RADIUS) : d.slots.span;
                for (int[] c : d.slots.chunks(slot, reach)) {
                    Chunk chunk = world.theChunkProviderServer.loadChunk(c[0], c[1]);
                    if (!empty(chunk)) {
                        if (!ours && lived(chunk)) {
                            return "ryoikitenkai.erro.regiao";
                        }
                        voidChunk(world, chunk);
                    }
                }
                build(world, d.slots, slot);
            }
            if (d.tickets[slot] == null) {
                d.tickets[slot] = ForgeChunkManager
                    .requestTicket(RyoikiTenkai.instance, world, ForgeChunkManager.Type.NORMAL);
            }
            if (d.tickets[slot] != null) {
                for (int[] c : chunks) {
                    ForgeChunkManager.forceChunk(d.tickets[slot], new ChunkCoordIntPair(c[0], c[1]));
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

    /** The chunks of a slot, loaded. */
    public List<Chunk> chunks(WorldServer world, int slot) {
        List<Chunk> out = new ArrayList<>();
        for (int[] c : dimension(world).slots.chunks(slot)) {
            out.add(world.theChunkProviderServer.loadChunk(c[0], c[1]));
        }
        return out;
    }

    /** Chunks still to empty around this world's arenas. */
    public int pending(WorldServer world) {
        return dimension(world).toVoid.size();
    }

    /** Whether an arena of ours stands on this slot, whatever its radius. */
    private static boolean centre(WorldServer world, ArenaSlots slots, int slot) {
        return world.getBlock(slots.centerX(slot), slots.floorY - 1, slots.centerZ(slot)) == RyoikiTenkai.veil;
    }

    /** Whether the arena of this slot is there with the radius of the config: its shell is where it should be. */
    private static boolean isArena(WorldServer world, ArenaSlots slots, int slot) {
        int cx = slots.centerX(slot);
        int cz = slots.centerZ(slot);
        return centre(world, slots, slot) && world.getBlock(cx + Config.radius, slots.floorY, cz) == RyoikiTenkai.veil
            && world.getBlock(cx + Config.radius - 2, slots.floorY, cz) != RyoikiTenkai.veil
            && world.getBlock(cx + Config.radius + 3, slots.floorY - 1, cz) != RyoikiTenkai.veil;
    }

    /**
     * Puts the crimson shrine's solid hall in the middle of a slot's arena, turned by {@code yaw}, or takes away
     * whatever hall is there ({@code yaw} NaN). A few hundred blocks, in chunks that are always loaded.
     */
    public void setCore(WorldServer world, int slot, float yaw) {
        ArenaSlots slots = dimension(world).slots;
        int cx = slots.centerX(slot);
        int cy = slots.floorY;
        int cz = slots.centerZ(slot);
        boolean on = !Float.isNaN(yaw);
        for (int dx = -ShrineCore.REACH; dx < ShrineCore.REACH; dx++) {
            for (int dz = -ShrineCore.REACH; dz < ShrineCore.REACH; dz++) {
                for (int dy = 0; dy <= ShrineCore.HEIGHT; dy++) {
                    boolean want = on && ShrineCore.solid(dx, dy, dz, yaw);
                    boolean has = world.getBlock(cx + dx, cy + dy, cz + dz) == RyoikiTenkai.veil;
                    if (want && !has) {
                        world.setBlock(cx + dx, cy + dy, cz + dz, RyoikiTenkai.veil, BlockVeil.SHELL, 2);
                    } else if (!want && has) {
                        world.setBlockToAir(cx + dx, cy + dy, cz + dz);
                    }
                }
            }
        }
    }

    private void build(WorldServer world, ArenaSlots slots, int slot) {
        final int cx = slots.centerX(slot);
        final int cy = slots.floorY;
        final int cz = slots.centerZ(slot);
        final int[] count = { 0 };
        shape().forEach((dx, dy, dz, part) -> {
            // One floor block in sixteen gives light: enough to light the whole arena evenly, and far fewer
            // sources for the server to spread.
            int meta = part == ArenaShape.Part.FLOOR && (dx & 3) == 0 && (dz & 3) == 0 ? BlockVeil.FLOOR
                : BlockVeil.SHELL;
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
            int span = d.slots.span;
            for (int x = cx - view - span; x < cx + view + span; x++) {
                for (int z = cz - view - span; z < cz + view + span; z++) {
                    boolean own = x >= cx - span && x < cx + span && z >= cz - span && z < cz + span;
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
                if (lived(chunk)) {
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

    /**
     * Whether players have been in a chunk of real terrain. A chunk this mod emptied can get a tree or a vein back
     * from a neighbour that is generated later; that is not terrain (nothing at the bottom of the world), and it is
     * emptied again.
     */
    private static boolean lived(Chunk chunk) {
        return chunk.inhabitedTime > 0 && !chunk.getBlock(8, 0, 8)
            .isAir(chunk.worldObj, chunk.xPosition * 16 + 8, 0, chunk.zPosition * 16 + 8);
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
