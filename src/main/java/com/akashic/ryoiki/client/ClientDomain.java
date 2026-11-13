package com.akashic.ryoiki.client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.fx.SlashField;
import com.akashic.ryoiki.fx.SoundCues;
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
    /** A sound in the world is heard up to 16 blocks times its volume. */
    private static final float SIGN_VOLUME = 3F;
    private static final float DOME_VOLUME = 4F;

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
        /** Whether this client has started the dome's sounds. */
        boolean heard;
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
    /** The caster's entity id. */
    public int casterEntity;
    /** Where the caster looked when the domain opened, in degrees: the scene is laid out behind it. */
    public float yaw;
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

    /** The session whose clash this player is in or watching from close by, or -1. */
    public int clash = -1;
    private int clashLeft;
    private int clashRun;

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
                    DomainSounds.playAt(SoundCues.SIGN, m.x, m.y + 1, m.z, SIGN_VOLUME);
                    Minecraft mc = Minecraft.getMinecraft();
                    // The cut-in is for those who may be caught: the caster and whoever is close.
                    if (mc.thePlayer != null && (mc.thePlayer.getEntityId() == m.casterEntity
                        || mc.thePlayer.getDistanceSq(m.x, m.y, m.z) <= (m.radius + 12.0) * (m.radius + 12.0))) {
                        cutIn = d;
                        // The owner's track of the whole expansion, from the hand sign to the end of the shatter, and
                        // the caster's voice over it. A clash takes the track's place.
                        Dome dome = d;
                        DomainSounds.track(
                            SoundCues.of(m.type.id, SoundCues.TRACK),
                            1F,
                            () -> clash < 0
                                && (cutIn == dome || session == dome.session && (inside != null || shatter > 0)));
                        DomainSounds.play(m.type, SoundCues.VOICE, 1F, 1F);
                    }
                } else {
                    d.windup = 0;
                    d.left = m.ticks + 40;
                    if (!d.heard && inside == null) {
                        // Seen from outside: the domain closing, then the dome's hum for as long as it stands.
                        d.heard = true;
                        Dome dome = d;
                        DomainSounds.playAt(SoundCues.of(m.type.id, SoundCues.ENTER), m.x, m.y + 1, m.z, DOME_VOLUME);
                        DomainSounds.loopAt(
                            SoundCues.DOME,
                            m.x,
                            m.y + 1,
                            m.z,
                            DOME_VOLUME,
                            () -> inside == null && domes.get(dome.session) == dome);
                    }
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
                yaw = m.yaw;
                casterEntity = m.casterEntity;
                age = 0;
                keepRadius = m.keepRadius;
                releaseIn = -1;
                keep.begin(m.keepChunkX, m.keepChunkZ, m.keepRadius);
                flash(0xFFFFFF);
                DomainSounds.play(m.type, SoundCues.ENTER, 0.7F, 1F);
                DomainType type = m.type;
                int entered = m.session;
                DomainSounds
                    .loop(SoundCues.of(type.id, SoundCues.AMBIENT), 0.6F, () -> inside == type && session == entered);
                break;
            case DomainWire.CLASH:
                // For those in the domain and those close enough to its dome to have seen its cut-in.
                if (clash != m.session && (inside != null && session == m.session || nearDome(m.session))) {
                    clash = m.session;
                    clashLeft = m.ticks + 20;
                    int run = ++clashRun;
                    DomainSounds.track(SoundCues.CLASH_TRACK, 1F, () -> clash >= 0 && clashRun == run);
                }
                break;
            case DomainWire.CLASH_END:
                endClash(m.session);
                break;
            case DomainWire.EXIT:
                endClash(m.session);
                if (inside != null && session == m.session) {
                    shatter = Shatter.TICKS;
                    shatterType = inside;
                    inside = null;
                    releaseIn = RELEASE_AFTER;
                    CameraShake.INSTANCE.kick(2.5F);
                    DomainSounds.play(SoundCues.EXIT, 0.8F, 1F);
                }
                break;
            case DomainWire.DOME_END:
                endClash(m.session);
                domes.remove(m.session);
                break;
            default:
                break;
        }
    }

    private boolean nearDome(int id) {
        Dome d = domes.get(id);
        Minecraft mc = Minecraft.getMinecraft();
        return d != null && mc.thePlayer != null
            && mc.thePlayer.getDistanceSq(d.x, d.y, d.z) <= (d.radius + 12.0) * (d.radius + 12.0);
    }

    private void endClash(int id) {
        if (clash == id) {
            clash = -1;
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
            clash = -1;
            flash = 0;
            shatter = 0;
            releaseIn = -1;
            return;
        }
        if (world == null || mc.isGamePaused()) {
            return;
        }
        MusicHush.INSTANCE.tick();
        if (flash > 0) {
            flash--;
        }
        // A clash ends by its message; failing that, by its time, or when a watcher outside walks away.
        if (clash >= 0 && (--clashLeft <= 0 || !(inside != null && session == clash) && !nearDome(clash))) {
            clash = -1;
        }
        if (shatter > 0) {
            shatter--;
        }
        if (inside != null) {
            age++;
            sky.install(world.provider);
            // Each cut of the crimson shrine jolts the view.
            if (inside == DomainType.MALEVOLENT_SHRINE && SlashField.startsAt(age)
                && age % (SlashField.PERIOD * 2) == 0) {
                CameraShake.INSTANCE.kick(0.9F);
                // The pitch varies with the cut, the same on every client.
                DomainSounds.play(inside, SoundCues.SLASH, 0.45F, 0.85F + (age / SlashField.PERIOD * 7 % 5) * 0.075F);
            }
        } else {
            sky.restore();
        }
        if (cutIn != null && (cutIn.windup <= 0 || !domes.containsValue(cutIn))) {
            cutIn = null;
        }
        if (cutIn != null) {
            // One hit on each camera cut of the cut-in.
            int signAge = cutIn.windupTotal - cutIn.windup;
            if (signAge == (int) CutIn.CUT_ONE || signAge == (int) CutIn.CUT_TWO) {
                DomainSounds.play(SoundCues.BAND_CUT, 0.6F, signAge == (int) CutIn.CUT_ONE ? 1F : 1.2F);
            }
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
