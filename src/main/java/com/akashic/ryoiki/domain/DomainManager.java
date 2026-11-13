package com.akashic.ryoiki.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IntHashMap;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import com.akashic.ryoiki.Config;
import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.RyoikiTenkai;
import com.akashic.ryoiki.arena.ArenaManager;
import com.akashic.ryoiki.arena.ArenaShape;
import com.akashic.ryoiki.arena.ArenaSlots;
import com.akashic.ryoiki.arena.ShrineCore;
import com.akashic.ryoiki.domain.DomainSession.Phase;
import com.akashic.ryoiki.domain.DomainSession.ReturnPoint;
import com.akashic.ryoiki.net.DomainWire;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * The server's side of the domains. A cast is a hand sign of {@link DomainType#WINDUP_TICKS}; during it the arena's
 * chunks are sent to everyone who may be caught, so that when it opens the teleport finds them already on the client.
 * Those within the radius are then moved to the arena, each at its offset from the caster, and brought back to where
 * they stood when the domain ends. Main server thread only.
 */
public final class DomainManager {

    public static final DomainManager INSTANCE = new DomainManager();

    /** Who sees the hand sign and the dome, in blocks from where it was cast. */
    private static final double NOTICE_RANGE = 128;
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
        DomainSession host = clashHost(caster);
        if (host != null) {
            answer(host, caster, type);
            return null;
        }
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
            slot,
            caster.rotationYaw);
        sessions.add(s);
        // The crimson shrine's hall is solid: it goes up before the arena's chunks are sent.
        ArenaManager.INSTANCE.setCore(world, slot, type == DomainType.MALEVOLENT_SHRINE ? s.yaw : Float.NaN);
        byte[] windup = DomainWire
            .windup(s.id, type, caster.getEntityId(), s.x, s.y, s.z, s.radius, DomainType.WINDUP_TICKS);
        List<Chunk> arena = ArenaManager.INSTANCE.chunks(world, slot);
        for (EntityPlayerMP p : players(world)) {
            double d = distanceSq(p, s.x, s.y, s.z);
            if (d <= square(NOTICE_RANGE)) {
                send(p, windup);
                s.told.add(p.getUniqueID());
            }
            if (d <= square(s.radius + PRESEND_MARGIN)) {
                // A packet of its own for each player: one object written to two connections at the same time
                // reached one of them corrupted, and that client was dropped.
                p.playerNetServerHandler.sendPacket(new S26PacketMapChunkBulk(arena));
            }
        }
        return null;
    }

    /**
     * The domain a cast by this player would answer, and so clash with: one whose hand sign it stands in, or one
     * that caught it and opened a moment ago. A domain clashes once, with one rival.
     */
    private DomainSession clashHost(EntityPlayerMP caster) {
        UUID id = caster.getUniqueID();
        for (DomainSession s : sessions) {
            if (s.rival != null || s.clashed || s.caster.equals(id) || !caster.isEntityAlive()) {
                continue;
            }
            if (s.phase == Phase.WINDUP && s.dimension == caster.dimension
                && distanceSq(caster, s.x, s.y, s.z) <= square(s.radius)) {
                return s;
            }
            if (s.phase == Phase.ACTIVE && s.holds(id) && s.age <= Config.clashWindow) {
                return s;
            }
        }
        return null;
    }

    /** The rival's hand sign begins. The clash starts when it is done and both are in the arena. */
    private void answer(DomainSession s, EntityPlayerMP rival, DomainType type) {
        s.rival = rival.getUniqueID();
        s.rivalType = type;
        s.rivalSign = DomainType.WINDUP_TICKS;
        sendAll(
            s,
            DomainWire.rival(
                s.id,
                type,
                rival.getEntityId(),
                rival.posX,
                rival.boundingBox.minY,
                rival.posZ,
                s.radius,
                DomainType.WINDUP_TICKS));
    }

    private void beginClash(DomainSession s, WorldServer world) {
        EntityPlayerMP host = player(world, s.caster);
        EntityPlayerMP rival = player(world, s.rival);
        if (host == null || rival == null || !rival.isEntityAlive() || !s.holds(s.rival)) {
            // The rival walked out of the domain before it closed, or left: no clash.
            s.rival = null;
            return;
        }
        RyoikiTenkai.LOG.info(
            "Clash in domain {}: {} ({}) against {} ({})",
            s.id,
            host.getCommandSenderName(),
            s.type.id,
            rival.getCommandSenderName(),
            s.rivalType.id);
        s.clashed = true;
        s.clash = ClashScore.TICKS;
        s.score = new ClashScore();
        if (s.rivalType == DomainType.MALEVOLENT_SHRINE) {
            raiseRivalHall(s, world);
        }
        sendAll(
            s,
            DomainWire.clash(s.id, ClashScore.TICKS, host.getEntityId(), s.type, rival.getEntityId(), s.rivalType));
    }

    /**
     * The hall of a shrine that answers a domain is solid like any other: without it, someone thrown back in the
     * clash would end up inside what is drawn there. Whoever stands where it goes up is put out by its door.
     */
    private void raiseRivalHall(DomainSession s, WorldServer world) {
        double[] hall = ShrineCore.rival(s.type == DomainType.MALEVOLENT_SHRINE, s.yaw);
        ArenaSlots slots = ArenaManager.INSTANCE.slots(world);
        double cx = slots.centerX(s.slot);
        double cz = slots.centerZ(s.slot);
        for (UUID id : s.inside.keySet()) {
            EntityPlayerMP p = player(world, id);
            if (p == null) {
                continue;
            }
            double x = p.posX - cx - hall[0];
            double z = p.posZ - cz - hall[1];
            double[] out = ShrineCore.clear(x, z, (float) hall[2]);
            if (out[0] != x || out[1] != z) {
                teleport(p, cx + hall[0] + out[0], slots.floorY, cz + hall[1] + out[1], p.rotationYaw, p.rotationPitch);
            }
        }
        s.rivalHall = hall;
        ArenaManager.INSTANCE.setHall(world, s.slot, hall, s.radius - ArenaManager.SHELL_CLEAR, true);
    }

    /** One tick of a clash: it ends by a full bar, by the time, or by one of the two being gone. */
    private void tickClash(DomainSession s, WorldServer world) {
        EntityPlayerMP host = player(world, s.caster);
        EntityPlayerMP rival = player(world, s.rival);
        boolean hostHere = host != null && host.isEntityAlive() && s.holds(s.caster);
        boolean rivalHere = rival != null && rival.isEntityAlive() && s.holds(s.rival);
        s.clash--;
        if (hostHere && rivalHere) {
            blows(s, ClashScore.HOST, host, rival);
            blows(s, ClashScore.RIVAL, rival, host);
        }
        if (!hostHere && !rivalHere) {
            resolve(s, world, ClashScore.TIE);
        } else if (!hostHere) {
            resolve(s, world, ClashScore.RIVAL);
        } else if (!rivalHere) {
            resolve(s, world, ClashScore.HOST);
        } else if (s.score.full() != ClashScore.NONE) {
            resolve(s, world, s.score.full());
        } else if (s.clash <= 0) {
            resolve(s, world, s.score.atTimeUp());
        }
    }

    /** How close and how much in front the other must be for a blow to land. */
    private static final double REACH = 3.6;
    private static final double REACH_ANYWAY = 1.3;
    private static final double FRONT = Math.cos(Math.toRadians(60));
    /** The step into each blow and into the finisher, and how far the finisher throws the other. */
    private static final double LUNGE = 0.38;
    private static final double LUNGE_FINISHER = 0.6;
    private static final double THROW = 0.85;
    private static final double THROW_UP = 0.3;
    /** Ticks from the click to the fist arriving: the same the model takes to strike ({@code BlowPose.IMPACT}). */
    private static final int IMPACT_TICKS = 3;

    /**
     * The blows of one caster this tick. A blow is a swing of the arm: at the other, at the air, it is the same
     * click. The server sees the swing begin (the arm's progress going back to its start) and asks the chain.
     */
    private void blows(DomainSession s, int side, EntityPlayerMP attacker, EntityPlayerMP victim) {
        long tick = MinecraftServer.getServer()
            .getTickCounter();
        int progress = attacker.isSwingInProgress ? attacker.swingProgressInt : -1;
        boolean began = attacker.isSwingInProgress && (s.swing[side] < 0 || progress < s.swing[side]);
        s.swing[side] = progress;
        if (s.coming[side] > 0 && tick >= s.arrives[side]) {
            land(s, side, attacker, victim, s.coming[side]);
            s.coming[side] = 0;
        }
        ComboChain chain = s.chains[side];
        int blow = chain.tick(tick);
        if (blow == 0 && began) {
            blow = chain.click(tick);
        }
        if (blow == 0) {
            return;
        }
        // The blow begins: the step into it now, unless the other is already at arm's length. Whether it lands is
        // seen when the fist arrives.
        double yaw = Math.toRadians(attacker.rotationYaw);
        double dx = victim.posX - attacker.posX;
        double dz = victim.posZ - attacker.posZ;
        if (dx * dx + dz * dz > 1.6 * 1.6) {
            double lunge = blow == ComboChain.BLOWS ? LUNGE_FINISHER : LUNGE;
            attacker.addVelocity(-Math.sin(yaw) * lunge, 0, Math.cos(yaw) * lunge);
            attacker.velocityChanged = true;
        }
        s.coming[side] = blow;
        s.arrives[side] = tick + IMPACT_TICKS;
        sendAll(s, DomainWire.blow(s.id, attacker.getEntityId(), blow, false));
    }

    /** The fist arrives: the blow lands on the other if it is in reach and before the one who strikes. */
    private void land(DomainSession s, int side, EntityPlayerMP attacker, EntityPlayerMP victim, int blow) {
        double yaw = Math.toRadians(attacker.rotationYaw);
        double lookX = -Math.sin(yaw);
        double lookZ = Math.cos(yaw);
        double dx = victim.posX - attacker.posX;
        double dz = victim.posZ - attacker.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        boolean inReach = Math.abs(victim.posY - attacker.posY) <= 2.5
            && (distance <= REACH_ANYWAY || distance <= REACH && (dx * lookX + dz * lookZ) / distance >= FRONT);
        if (!inReach || !s.score.hit(side)) {
            return;
        }
        // Nothing of the game's own hit: no knock back and no jolt of the view, which would break the other's own
        // chain. The clients draw the blow taken. Only the finisher moves the other, and that is ours.
        if (blow == ComboChain.BLOWS) {
            victim.addVelocity(lookX * THROW, THROW_UP, lookZ * THROW);
            victim.velocityChanged = true;
        }
        sendAll(s, DomainWire.clashScore(s.id, s.score.hits(ClashScore.HOST), s.score.hits(ClashScore.RIVAL)));
        sendAll(s, DomainWire.blow(s.id, attacker.getEntityId(), blow, true));
    }

    /** The winner keeps the arena, with its own domain from the start; a tie breaks both. */
    private void resolve(DomainSession s, WorldServer world, int result) {
        s.clash = 0;
        DomainType was = s.type;
        if (result == ClashScore.RIVAL) {
            s.caster = s.rival;
            s.type = s.rivalType;
        }
        s.rival = null;
        EntityPlayerMP winner = player(world, s.caster);
        RyoikiTenkai.LOG.info(
            "Clash in domain {} over: {} ({} hits to {})",
            s.id,
            result == ClashScore.TIE || winner == null ? "a tie, both break"
                : winner.getCommandSenderName() + " wins with " + s.type.id,
            s.score.hits(ClashScore.HOST),
            s.score.hits(ClashScore.RIVAL));
        sendAll(s, DomainWire.clashEnd(s.id, result, s.type, winner == null ? 0 : winner.getEntityId()));
        if (result == ClashScore.TIE || winner == null) {
            end(s, world);
            return;
        }
        s.age = 0;
        // The hall of a shrine that lost goes; the one of a shrine that won stays where it stood in the clash.
        if (result == ClashScore.RIVAL && was == DomainType.MALEVOLENT_SHRINE) {
            // That empties the arena of halls: the winner's goes back up in the same tick.
            ArenaManager.INSTANCE.setCore(world, s.slot, Float.NaN);
            if (s.rivalHall != null) {
                ArenaManager.INSTANCE.setHall(world, s.slot, s.rivalHall, s.radius - ArenaManager.SHELL_CLEAR, true);
            }
        }
        if (result == ClashScore.HOST && s.rivalHall != null) {
            ArenaManager.INSTANCE.setHall(world, s.slot, s.rivalHall, s.radius - ArenaManager.SHELL_CLEAR, false);
            s.rivalHall = null;
        }
    }

    /** In a clash nothing hurts the two casters, and nothing pushes them. */
    @SubscribeEvent
    public void onAttacked(LivingAttackEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP && clashOf(event.entityLiving.getUniqueID()) != null) {
            event.setCanceled(true);
        }
    }

    /** The game's own blow between the two is dropped: what counts is the chain ({@link #blows}). */
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.target instanceof EntityPlayerMP && clashOf(event.target.getUniqueID()) != null) {
            event.setCanceled(true);
        }
    }

    /**
     * Whether the star void holds this player: caught in one that is not its own, complete, with no clash going on
     * or being answered. The void's sure effect: whoever it holds does nothing until it ends.
     */
    public boolean frozen(EntityPlayer player) {
        DomainSession s = sessionOf(player.getUniqueID());
        return s != null && s.type == DomainType.INFINITE_VOID
            && s.phase == Phase.ACTIVE
            && s.clash == 0
            && s.rival == null
            && s.holds(player.getUniqueID())
            && !player.getUniqueID()
                .equals(s.caster);
    }

    /** Keeps those the void holds where they are. */
    private void hold(DomainSession s, WorldServer world) {
        if (s.type != DomainType.INFINITE_VOID || s.rival != null) {
            s.held.clear();
            return;
        }
        for (UUID id : s.inside.keySet()) {
            EntityPlayerMP p = player(world, id);
            if (p == null || id.equals(s.caster)) {
                continue;
            }
            double[] at = s.held.get(id);
            if (at == null) {
                s.held.put(id, new double[] { p.posX, p.boundingBox.minY, p.posZ });
            } else if (distanceSq(p, at[0], at[1], at[2]) > 0.02) {
                teleport(p, at[0], at[1], at[2], p.rotationYaw, p.rotationPitch);
            }
        }
    }

    /**
     * In a clash the two fight with their hands: a click is a blow, never a block struck or broken. And whoever the
     * void holds does nothing with a click at all.
     */
    @SubscribeEvent
    public void onClickBlock(PlayerInteractEvent event) {
        if (!(event.entityPlayer instanceof EntityPlayerMP)) {
            return;
        }
        if (frozen(event.entityPlayer) || event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK
            && clashOf(event.entityPlayer.getUniqueID()) != null) {
            event.setCanceled(true);
        }
    }

    /** Whoever the void holds strikes nobody. */
    @SubscribeEvent
    public void onFrozenAttack(AttackEntityEvent event) {
        if (event.entityPlayer instanceof EntityPlayerMP && frozen(event.entityPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() != null && (frozen(event.getPlayer()) || clashOf(
            event.getPlayer()
                .getUniqueID())
            != null)) {
            event.setCanceled(true);
        }
    }

    /** The clash this player is one of the two casters of, or null. */
    private DomainSession clashOf(UUID player) {
        for (DomainSession s : sessions) {
            if (s.clash > 0 && (player.equals(s.caster) || player.equals(s.rival))) {
                return s;
            }
        }
        return null;
    }

    /**
     * Players who were teleported far in the same tick, and must see each other where they land. The entity tracker
     * decides who sees whom as each one moves, and with two of them moving at once it can leave one without the
     * other: each is taken out of the others' watchers and put back, once the chunks around them are being watched.
     */
    private static final class Resync {

        final Set<UUID> who;
        final int dimension;
        int age;

        Resync(Set<UUID> who, int dimension) {
            this.who = who;
            this.dimension = dimension;
        }
    }

    private static final int RESYNC_DROP = 5;
    private static final int RESYNC_ADD = 8;
    private static final int RESYNC_AGAIN = 30;

    private final List<Resync> resyncs = new ArrayList<>();

    private void resyncLater(Set<UUID> who, int dimension) {
        if (who.size() > 1) {
            resyncs.add(new Resync(new HashSet<>(who), dimension));
        }
    }

    private void tickResyncs(MinecraftServer server) {
        for (Iterator<Resync> it = resyncs.iterator(); it.hasNext();) {
            Resync r = it.next();
            r.age++;
            if (r.age != RESYNC_DROP && r.age != RESYNC_ADD && r.age != RESYNC_AGAIN) {
                continue;
            }
            WorldServer world = server.worldServerForDimension(r.dimension);
            List<EntityPlayerMP> players = new ArrayList<>();
            for (UUID id : r.who) {
                EntityPlayerMP p = player(world, id);
                if (p != null) {
                    players.add(p);
                }
            }
            IntHashMap entries = null;
            try {
                entries = ReflectionHelper.getPrivateValue(
                    EntityTracker.class,
                    world.getEntityTracker(),
                    "trackedEntityIDs",
                    "field_72794_c");
            } catch (RuntimeException e) {
                // Without the entries, only those the tracker itself knows to be missing can be put back.
            }
            for (EntityPlayerMP watcher : players) {
                for (EntityPlayerMP seen : players) {
                    if (watcher == seen) {
                        continue;
                    }
                    EntityTrackerEntry entry = entries == null ? null
                        : (EntityTrackerEntry) entries.lookup(seen.getEntityId());
                    if (entry == null) {
                        world.getEntityTracker()
                            .func_85172_a(watcher, world.getChunkFromChunkCoords(seen.chunkCoordX, seen.chunkCoordZ));
                    } else if (r.age == RESYNC_DROP) {
                        entry.removePlayerFromTracker(watcher);
                    } else {
                        entry.tryStartWachingThis(watcher);
                    }
                }
            }
            if (r.age >= RESYNC_AGAIN) {
                it.remove();
            }
        }
    }

    /** To those in the arena and those who see the dome. */
    private void sendAll(DomainSession s, byte[] data) {
        Set<UUID> all = new HashSet<>(s.told);
        all.addAll(s.inside.keySet());
        for (UUID id : all) {
            EntityPlayerMP p = anywhere(id);
            if (p != null) {
                send(p, data);
            }
        }
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
        tickResyncs(server);
        for (DomainSession s : new ArrayList<>(sessions)) {
            WorldServer world = server.worldServerForDimension(s.dimension);
            EntityPlayerMP caster = player(world, s.caster);
            if (s.rival != null && s.clash == 0 && !s.clashed) {
                // The rival's hand sign runs through the host's and, if need be, on into the arena.
                if (s.rivalSign > 0) {
                    s.rivalSign--;
                }
                if (s.rivalSign == 0 && s.phase == Phase.ACTIVE) {
                    beginClash(s, world);
                }
            }
            if (s.clash > 0) {
                // The domain's own time stands still while the two fight for it.
                dropThoseWhoLeft(s, world);
                tickClash(s, world);
                continue;
            }
            s.age++;
            if (s.phase == Phase.WINDUP) {
                if (caster == null || !caster.isEntityAlive()) {
                    end(s, world);
                } else if (s.age >= DomainType.WINDUP_TICKS) {
                    open(s, world, caster);
                }
            } else if (s.phase == Phase.ACTIVE) {
                dropThoseWhoLeft(s, world);
                hold(s, world);
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
        float yaw = s.yaw;
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
            // In the crimson shrine the caster stands before the door of the hall in the middle; in the star void, in
            // the middle itself. Everyone else keeps its offset from the caster.
            double[] at = s.type == DomainType.MALEVOLENT_SHRINE
                ? ShrineCore.place(p.posX - ox, p.posZ - oz, yaw, s.radius - 2.5)
                : shape.place(p.posX - ox, p.posZ - oz);
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
                    view,
                    yaw));
            teleport(p, cx + at[0], slots.floorY, cz + at[1], p.rotationYaw, p.rotationPitch);
            if (p.dimension != s.dimension || distanceSq(p, cx + at[0], slots.floorY, cz + at[1]) > 4) {
                // Something on the server (a world border, a region plugin) cancelled or redirected the teleport.
                // Nobody stays wherever that left them: the domain ends and everyone goes back.
                RyoikiTenkai.LOG.warn(
                    "The teleport of {} into the arena at {}, {}, {} was blocked (the player is at {}, {}, {}): "
                        + "domain cancelled. A world border or a protection plugin is in the way; move the arenas "
                        + "(arena.x / arena.z in the config).",
                    p.getCommandSenderName(),
                    cx,
                    slots.floorY,
                    cz,
                    p.posX,
                    p.posY,
                    p.posZ);
                ChatComponentTranslation message = new ChatComponentTranslation("ryoikitenkai.erro.bloqueado");
                message.getChatStyle()
                    .setColor(EnumChatFormatting.RED);
                caster.addChatMessage(message);
                end(s, world);
                return;
            }
        }
        s.phase = Phase.ACTIVE;
        s.age = 0;
        resyncLater(s.inside.keySet(), s.dimension);
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
        resyncLater(s.inside.keySet(), s.dimension);
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
        ArenaManager.INSTANCE.setCore(world, s.slot, Float.NaN);
        if (s.rivalHall != null) {
            ArenaManager.INSTANCE.setHall(world, s.slot, s.rivalHall, s.radius - ArenaManager.SHELL_CLEAR, false);
            s.rivalHall = null;
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
