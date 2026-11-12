package com.akashic.ryoiki.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import com.akashic.ryoiki.Config;
import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.RyoikiTenkai;
import com.akashic.ryoiki.arena.ArenaManager;
import com.akashic.ryoiki.arena.ArenaShape;
import com.akashic.ryoiki.arena.ArenaSlots;
import com.akashic.ryoiki.domain.DomainSession.Phase;
import com.akashic.ryoiki.domain.DomainSession.ReturnPoint;
import com.akashic.ryoiki.net.DomainWire;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The server's side of the domains. A cast is a hand sign of {@link DomainType#WINDUP_TICKS}; during it the arena's
 * chunks are sent to everyone who may be caught, so that when it opens the teleport finds them already on the client.
 * Those within the radius are then moved to the arena, each at its offset from the caster, and brought back to where
 * they stood when the domain ends. Main server thread only.
 */
public final class DomainManager {

    public static final DomainManager INSTANCE = new DomainManager();

    /** Who sees the hand sign and the dome, in blocks from where it was cast. */
    private static final double NOTICE_RANGE = 96;
    /** Who gets the arena's chunks in advance: the radius and how far someone can run in during the hand sign. */
    private static final double PRESEND_MARGIN = 12;
    /** A player this far outside the arena's interior has left it (another teleport took it away). */
    private static final double LEFT_MARGIN = 8;
    private static final String RETURN_TAG = "RyoikiReturn";

    private final List<DomainSession> sessions = new ArrayList<>();
    private int nextId = 1;

    private DomainManager() {}

    public List<DomainSession> sessions() {
        return sessions;
    }

    public DomainSession sessionOf(UUID player) {
        for (DomainSession s : sessions) {
            if (s.caster.equals(player) || s.holds(player)) {
                return s;
            }
        }
        return null;
    }

    /** Starts a domain's hand sign. Returns the translation key of what stopped it, or null. */
    public String cast(EntityPlayerMP caster, DomainType type) {
        if (sessionOf(caster.getUniqueID()) != null) {
            return "ryoikitenkai.erro.dentro";
        }
        if (!caster.isEntityAlive() || !caster.onGround && !caster.capabilities.isFlying) {
            return "ryoikitenkai.erro.no_ar";
        }
        WorldServer world = caster.getServerForPlayer();
        ArenaSlots slots = ArenaManager.INSTANCE.slots(world);
        if (slots.inArenaRegion(caster.posX, caster.posZ)) {
            return "ryoikitenkai.erro.dentro";
        }
        for (DomainSession s : sessions) {
            if (s.dimension == caster.dimension && distanceSq(caster, s.x, s.y, s.z) < square(s.radius * 2 + 2)) {
                return "ryoikitenkai.erro.perto";
            }
        }
        int slot = slots.take(caster.getUniqueID());
        if (slot < 0) {
            return "ryoikitenkai.erro.cheio";
        }
        String error = ArenaManager.INSTANCE.prepare(world, slot);
        if (error != null) {
            slots.release(slot);
            return error;
        }
        DomainSession s = new DomainSession(
            nextId++,
            type,
            caster.getUniqueID(),
            caster.dimension,
            caster.posX,
            caster.boundingBox.minY,
            caster.posZ,
            Config.radius,
            slot);
        sessions.add(s);
        byte[] windup = DomainWire
            .windup(s.id, type, caster.getEntityId(), s.x, s.y, s.z, s.radius, DomainType.WINDUP_TICKS);
        S26PacketMapChunkBulk arena = new S26PacketMapChunkBulk(ArenaManager.INSTANCE.chunks(world, slot));
        for (EntityPlayerMP p : players(world)) {
            double d = distanceSq(p, s.x, s.y, s.z);
            if (d <= square(NOTICE_RANGE)) {
                send(p, windup);
                s.told.add(p.getUniqueID());
            }
            if (d <= square(s.radius + PRESEND_MARGIN)) {
                p.playerNetServerHandler.sendPacket(arena);
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        for (WorldServer world : server.worldServers) {
            ArenaManager.INSTANCE.tick(world);
        }
        for (DomainSession s : new ArrayList<>(sessions)) {
            WorldServer world = server.worldServerForDimension(s.dimension);
            EntityPlayerMP caster = player(world, s.caster);
            s.age++;
            if (s.phase == Phase.WINDUP) {
                if (caster == null || !caster.isEntityAlive()) {
                    end(s, world);
                } else if (s.age >= DomainType.WINDUP_TICKS) {
                    open(s, world, caster);
                }
            } else if (s.phase == Phase.ACTIVE) {
                dropThoseWhoLeft(s, world);
                if (caster == null || !caster.isEntityAlive() || !s.holds(s.caster) || s.age >= s.type.durationTicks) {
                    end(s, world);
                } else if (s.age % 20 == 0) {
                    tellNewcomers(s, world);
                }
            }
        }
    }

    private void open(DomainSession s, WorldServer world, EntityPlayerMP caster) {
        ArenaSlots slots = ArenaManager.INSTANCE.slots(world);
        ArenaShape shape = ArenaManager.INSTANCE.shape();
        double cx = slots.centerX(s.slot);
        double cz = slots.centerZ(s.slot);
        int view = MinecraftServer.getServer()
            .getConfigurationManager()
            .getViewDistance();
        byte[] dome = DomainWire.dome(s.id, s.type, s.x, s.y, s.z, s.radius, s.type.durationTicks);
        // The caster's position now: the domain opens around where it stands, not where the hand sign began.
        double ox = caster.posX;
        double oz = caster.posZ;
        for (EntityPlayerMP p : players(world)) {
            boolean caught = p == caster || p.isEntityAlive() && sessionOf(p.getUniqueID()) == null
                && distanceSq(p, s.x, s.y, s.z) <= square(s.radius);
            if (!caught) {
                if (distanceSq(p, s.x, s.y, s.z) <= square(NOTICE_RANGE)) {
                    send(p, dome);
                    s.told.add(p.getUniqueID());
                }
                continue;
            }
            if (p.isRiding()) {
                p.mountEntity(null);
            }
            ReturnPoint back = new ReturnPoint(
                p.dimension,
                p.posX,
                p.boundingBox.minY,
                p.posZ,
                p.rotationYaw,
                p.rotationPitch);
            s.inside.put(p.getUniqueID(), back);
            remember(p, back);
            double[] at = shape.place(p.posX - ox, p.posZ - oz);
            send(
                p,
                DomainWire.enter(
                    s.id,
                    s.type,
                    caster.getEntityId(),
                    cx,
                    slots.floorY,
                    cz,
                    s.radius,
                    s.type.durationTicks,
                    p.chunkCoordX,
                    p.chunkCoordZ,
                    view));
            teleport(p, cx + at[0], slots.floorY, cz + at[1], p.rotationYaw, p.rotationPitch);
        }
        s.phase = Phase.ACTIVE;
        s.age = 0;
    }

    /** Players another teleport, a death or a logout took out of the arena are no longer in the domain. */
    private void dropThoseWhoLeft(DomainSession s, WorldServer world) {
        ArenaSlots slots = ArenaManager.INSTANCE.slots(world);
        double cx = slots.centerX(s.slot);
        double cz = slots.centerZ(s.slot);
        for (Iterator<Map.Entry<UUID, ReturnPoint>> it = s.inside.entrySet()
            .iterator(); it.hasNext();) {
            UUID id = it.next()
                .getKey();
            EntityPlayerMP p = player(world, id);
            if (p != null && p.isEntityAlive()
                && distanceSq(p, cx, slots.floorY, cz) <= square(s.radius + LEFT_MARGIN)) {
                continue;
            }
            it.remove();
            EntityPlayerMP anywhere = anywhere(id);
            if (anywhere != null) {
                forget(anywhere);
                send(anywhere, DomainWire.exit(s.id));
            }
        }
    }

    private void tellNewcomers(DomainSession s, WorldServer world) {
        byte[] dome = null;
        for (EntityPlayerMP p : players(world)) {
            if (!s.told.contains(p.getUniqueID()) && !s.holds(p.getUniqueID())
                && distanceSq(p, s.x, s.y, s.z) <= square(NOTICE_RANGE)) {
                if (dome == null) {
                    dome = DomainWire.dome(s.id, s.type, s.x, s.y, s.z, s.radius, s.type.durationTicks - s.age);
                }
                send(p, dome);
                s.told.add(p.getUniqueID());
            }
        }
    }

    /** Ends a domain in any phase: everyone inside goes back to where it was caught. */
    public void end(DomainSession s, WorldServer world) {
        if (s.phase == Phase.ENDED) {
            return;
        }
        s.phase = Phase.ENDED;
        sessions.remove(s);
        for (Map.Entry<UUID, ReturnPoint> e : s.inside.entrySet()) {
            EntityPlayerMP p = player(world, e.getKey());
            if (p == null) {
                continue;
            }
            ReturnPoint back = e.getValue();
            forget(p);
            send(p, DomainWire.exit(s.id));
            teleport(p, back.x, back.y, back.z, p.rotationYaw, p.rotationPitch);
        }
        s.inside.clear();
        byte[] gone = DomainWire.domeEnd(s.id);
        for (UUID id : s.told) {
            EntityPlayerMP p = anywhere(id);
            if (p != null) {
                send(p, gone);
            }
        }
        ArenaManager.INSTANCE.slots(world)
            .release(s.slot);
    }

    /** The server is stopping: nobody is saved inside an arena. */
    public void endAll() {
        MinecraftServer server = MinecraftServer.getServer();
        for (DomainSession s : new ArrayList<>(sessions)) {
            end(s, server.worldServerForDimension(s.dimension));
        }
    }

    /** A player who leaves the game inside a domain is saved where it was caught. */
    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DomainSession s = sessionOf(event.player.getUniqueID());
        if (s == null) {
            return;
        }
        ReturnPoint back = s.inside.remove(event.player.getUniqueID());
        if (back != null) {
            event.player
                .setLocationAndAngles(back.x, back.y, back.z, event.player.rotationYaw, event.player.rotationPitch);
            forget(event.player);
        }
    }

    /** A crash can leave a player saved inside an arena: on its next login it goes back to where it was caught. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP p = (EntityPlayerMP) event.player;
        NBTTagCompound persisted = p.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(RETURN_TAG)) {
            return;
        }
        NBTTagCompound tag = persisted.getCompoundTag(RETURN_TAG);
        forget(p);
        if (tag.getInteger("dim") == p.dimension && ArenaManager.INSTANCE.slots(p.getServerForPlayer())
            .inArenaRegion(p.posX, p.posZ)) {
            RyoikiTenkai.LOG.info("{} was saved inside a domain arena: sent back", p.getCommandSenderName());
            teleport(
                p,
                tag.getDouble("x"),
                tag.getDouble("y"),
                tag.getDouble("z"),
                tag.getFloat("yaw"),
                tag.getFloat("pitch"));
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP) {
            DomainSession s = sessionOf(event.entityLiving.getUniqueID());
            if (s != null && s.inside.remove(event.entityLiving.getUniqueID()) != null) {
                // It respawns wherever the server says; the caster's death ends the domain on the next tick.
                forget((EntityPlayerMP) event.entityLiving);
                send((EntityPlayerMP) event.entityLiving, DomainWire.exit(s.id));
            }
        }
    }

    /** No ender pearls or chorus-like teleports inside a domain. */
    @SubscribeEvent
    public void onEnderTeleport(EnderTeleportEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP) {
            DomainSession s = sessionOf(event.entityLiving.getUniqueID());
            if (s != null && s.phase == Phase.ACTIVE) {
                event.setCanceled(true);
            }
        }
    }

    private static void teleport(EntityPlayerMP p, double x, double y, double z, float yaw, float pitch) {
        p.motionX = 0;
        p.motionY = 0;
        p.motionZ = 0;
        p.fallDistance = 0;
        p.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
    }

    private static void remember(EntityPlayerMP p, ReturnPoint back) {
        NBTTagCompound data = p.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("dim", back.dimension);
        tag.setDouble("x", back.x);
        tag.setDouble("y", back.y);
        tag.setDouble("z", back.z);
        tag.setFloat("yaw", back.yaw);
        tag.setFloat("pitch", back.pitch);
        persisted.setTag(RETURN_TAG, tag);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
    }

    private static void forget(EntityPlayer p) {
        NBTTagCompound data = p.getEntityData();
        if (data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)
                .removeTag(RETURN_TAG);
        }
    }

    private static void send(EntityPlayerMP p, byte[] data) {
        p.playerNetServerHandler.sendPacket(new S3FPacketCustomPayload(DomainWire.CHANNEL, data));
    }

    private static List<EntityPlayerMP> players(WorldServer world) {
        List<EntityPlayerMP> out = new ArrayList<>();
        for (Object o : world.playerEntities) {
            out.add((EntityPlayerMP) o);
        }
        return out;
    }

    private static EntityPlayerMP player(WorldServer world, UUID id) {
        for (Object o : world.playerEntities) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            if (p.getUniqueID()
                .equals(id)) {
                return p;
            }
        }
        return null;
    }

    private static EntityPlayerMP anywhere(UUID id) {
        for (Object o : MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            if (p.getUniqueID()
                .equals(id)) {
                return p;
            }
        }
        return null;
    }

    private static double distanceSq(EntityPlayer p, double x, double y, double z) {
        double dx = p.posX - x;
        double dy = p.boundingBox.minY - y;
        double dz = p.posZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static double square(double v) {
        return v * v;
    }
}
