package com.akashic.ryoiki.client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.fx.SlashField;
import com.akashic.ryoiki.net.ChunkKeep;
import com.akashic.ryoiki.net.DomainWire;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** What this client knows about the domains: the one its player is in, and the domes it sees. Client thread only. */
public final class ClientDomain {

    public static final ClientDomain INSTANCE = new ClientDomain();

    /** Ticks a screen flash lasts: full for {@link #FLASH_HOLD}, then fading. */
    public static final int FLASH_TICKS = 14;
    public static final int FLASH_HOLD = 3;
    /** Ticks after a domain before the chunks kept for it are given back. */
    private static final int RELEASE_AFTER = 100;

    /** A domain seen from outside: the hand sign, then the dome. */
    public static final class Dome {

        public int session;
        public int casterEntity;
        public DomainType type;
        public double x;
        public double y;
        public double z;
        public int radius;
        /** Ticks of hand sign left; 0 once it is open. */
        public int windup;
        public int windupTotal;
        /** Ticks since it opened. */
        public int age;
        int left;
    }

    public final ChunkKeep keep = new ChunkKeep();
    public final Map<Integer, Dome> domes = new LinkedHashMap<>();

    /** The domain this player is in, or null. */
    public DomainType inside;
    public int session;
    public double arenaX;
    public double arenaY;
    public double arenaZ;
    public int radius;
    /** Ticks since this player entered. */
    public int age;

    /** Ticks of screen flash left, and its colour. */
    public int flash;
    public int flashColor;
    /** Ticks left of the shatter that ends a domain, and which domain it was. */
    public int shatter;
    public DomainType shatterType;
    /** The cut-in on screen: the domain being cast near this player, or null. */
    public Dome cutIn;
    final DomainSky sky = new DomainSky();

    private int keepRadius;
    private int releaseIn = -1;
    private WorldClient world;

    private ClientDomain() {}

    public void onPayload(byte[] data) {
        DomainWire.Message m = DomainWire.read(data);
        if (m == null) {
            return;
        }
        switch (m.op) {
            case DomainWire.WINDUP:
            case DomainWire.DOME: {
                Dome d = domes.get(m.session);
                if (d == null) {
                    d = new Dome();
                    domes.put(m.session, d);
                }
                d.session = m.session;
                d.type = m.type;
                d.x = m.x;
                d.y = m.y;
                d.z = m.z;
                d.radius = m.radius;
                if (m.op == DomainWire.WINDUP) {
                    d.casterEntity = m.casterEntity;
                    d.windup = m.ticks;
                    d.windupTotal = m.ticks;
                    d.left = m.ticks + 20 * 60;
                    Minecraft mc = Minecraft.getMinecraft();
                    // The cut-in is for those who may be caught: the caster and whoever is close.
                    if (mc.thePlayer != null && (mc.thePlayer.getEntityId() == m.casterEntity
                        || mc.thePlayer.getDistanceSq(m.x, m.y, m.z) <= (m.radius + 12.0) * (m.radius + 12.0))) {
                        cutIn = d;
                    }
                } else {
                    d.windup = 0;
                    d.left = m.ticks + 40;
                }
                break;
            }
            case DomainWire.ENTER:
                domes.remove(m.session);
                inside = m.type;
                session = m.session;
                arenaX = m.x;
                arenaY = m.y;
                arenaZ = m.z;
                radius = m.radius;
                age = 0;
                keepRadius = m.keepRadius;
                releaseIn = -1;
                keep.begin(m.keepChunkX, m.keepChunkZ, m.keepRadius);
                flash(0xFFFFFF);
                break;
            case DomainWire.EXIT:
                if (inside != null && session == m.session) {
                    shatter = Shatter.TICKS;
                    shatterType = inside;
                    inside = null;
                    releaseIn = RELEASE_AFTER;
                    CameraShake.INSTANCE.kick(2.5F);
                }
                break;
            case DomainWire.DOME_END:
                domes.remove(m.session);
                break;
            default:
                break;
        }
    }

    private void flash(int color) {
        flash = FLASH_TICKS;
        flashColor = color;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != world) {
            // Another world (or none): nothing of the old one is left to keep or to draw.
            world = mc.theWorld;
            keep.clear();
            domes.clear();
            sky.restore();
            inside = null;
            cutIn = null;
            flash = 0;
            shatter = 0;
            releaseIn = -1;
            return;
        }
        if (world == null || mc.isGamePaused()) {
            return;
        }
        if (flash > 0) {
            flash--;
        }
        if (shatter > 0) {
            shatter--;
        }
        if (inside != null) {
            age++;
            sky.install(world.provider);
            // Each cut of the crimson shrine jolts the view.
            if (inside == DomainType.MALEVOLENT_SHRINE && SlashField.startsAt(age)) {
                CameraShake.INSTANCE.kick(0.9F);
            }
        } else {
            sky.restore();
        }
        if (cutIn != null && (cutIn.windup <= 0 || !domes.containsValue(cutIn))) {
            cutIn = null;
        }
        for (Iterator<Dome> it = domes.values()
            .iterator(); it.hasNext();) {
            Dome d = it.next();
            if (d.windup > 0) {
                d.windup--;
            } else {
                d.age++;
            }
            if (--d.left <= 0) {
                it.remove();
            }
        }
        if (releaseIn > 0 && --releaseIn == 0 && mc.thePlayer != null) {
            releaseIn = -1;
            for (int[] c : keep.release(mc.thePlayer.chunkCoordX, mc.thePlayer.chunkCoordZ, keepRadius)) {
                world.doPreChunk(c[0], c[1], false);
            }
        }
    }
}
