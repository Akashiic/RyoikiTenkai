package com.akashic.ryoiki.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.arena.ShrineCore;
import com.akashic.ryoiki.fx.BlowPose;
import com.akashic.ryoiki.fx.Frontier;
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
    private static final float FRONT_FOLLOW = 0.14F;
    /** Ticks the one struck is drawn red for. */
    private static final int HURT_FLASH = 7;

    /** A blow of a caster in a clash, as it is drawn: the pose of the body comes from {@link BlowPose}. */
    public static final class Blow {

        /**
         * Ticks the fighter stays out of its ordinary animation: the blow, then the guard for as long as the chain
         * may go on, let go over the last {@link #LET_GO}.
         */
        public static final int TICKS = 16;
        private static final float TAKE = 1.5F;
        private static final float LET_GO = 4;
        /** Ticks the blow before is still mixed into this one. */
        private static final float JOIN = 2;

        /** Which of the chain, 1 to 4. */
        public int blow;
        /** The entity it landed on, or -1, and how old the blow was when it did. */
        public int victim = -1;
        public int landedAt;
        public int age;
        /** The blow before: which, how far into it the fighter was, and how much out of its ordinary animation. */
        public int from;
        public float fromAge;
        public float fromLayer;

        /** How much the fighter is out of its ordinary animation, 0 to 1. */
        public float layer(float partialTicks) {
            float t = age + partialTicks;
            float in = fromLayer + (1 - fromLayer) * Draw.clamp01(t / TAKE);
            return Math.min(in, Draw.clamp01((TICKS - t) / LET_GO));
        }

        /** The pose of {@link BlowPose#BONES} now: this blow, with the one before still leaving it. */
        public float[] pose(float partialTicks) {
            float t = age + partialTicks;
            float[] now = BlowPose.sample(blow, t);
            if (from > 0 && t < JOIN) {
                float[] before = BlowPose.sample(from, fromAge + t);
                float k = t / JOIN;
                k = k * k * (3 - 2 * k);
                for (int i = 0; i < now.length; i++) {
                    now[i] = before[i] + (now[i] - before[i]) * k;
                }
            }
            return now;
        }
    }

    /** A blow taken by a caster in a clash, as it is drawn ({@link BlowPose#struck}). */
    public static final class Struck {

        private static final float TAKE = 0.5F;
        private static final float LET_GO = 3;

        /** 1 for a blow from the body's left, -1 from its right. */
        public int side = 1;
        public int age;

        /** How much the body is out of its ordinary animation, 0 to 1: at once, and let go as it comes to rest. */
        public float layer(float partialTicks) {
            float t = age + partialTicks;
            return Math.min(Draw.clamp01(t / TAKE), Draw.clamp01((BlowPose.STRUCK_TICKS - t) / LET_GO));
        }
    }

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
        /** Whether the domain is in a clash: its dome is black whichever domains fight in it. */
        public boolean clashing;
    }

    public final ChunkKeep keep = new ChunkKeep();
    public final Map<Integer, Dome> domes = new LinkedHashMap<>();

    /** The domain this player is in, or null. */
    public DomainType inside;
    /** The domain the arena opened as: a clash may change {@link #inside}, not what was built. */
    public DomainType openedAs;
    /**
     * Whether the arena's scene is the one a rival brought: drawn turned about, its centre {@link #turnedAt} blocks
     * before the arena's. A rival that wins a clash keeps its domain where it stood during it.
     */
    public boolean turned;
    public float turnedAt;
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
    /** Ticks of the clash left, and since it began. */
    public int clashTime;
    public int clashAge;
    /** The two domains, the two casters and their hits: the host's first. */
    public final DomainType[] clashTypes = new DomainType[2];
    public final int[] clashEntities = new int[2];
    public final int[] clashHits = new int[2];
    /** The hand sign of a rival, which is not a domain of its own. */
    public Dome rivalSign;
    /**
     * The front between the two domains: its shape, how far whoever is ahead has pushed it (-1 to 1, the host's
     * lead; it follows the score, not jumps to it), and the blows that shoved it (where along the seam, how many
     * ticks ago, and 1 for the host's or -1 for the rival's).
     */
    public Frontier front;
    public float frontPush;
    private float frontPushBefore;
    public final List<float[]> frontShoves = new ArrayList<>();
    /** The blow each caster is in, by entity id. */
    public final Map<Integer, Blow> blows = new LinkedHashMap<>();
    /** The blow each caster is taking, by entity id. */
    public final Map<Integer, Struck> strucks = new LinkedHashMap<>();
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
                openedAs = m.type;
                turned = false;
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
            case DomainWire.RIVAL: {
                // Someone answers a domain with its own: its hand sign and its cut-in, for those close to it.
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer != null && (mc.thePlayer.getEntityId() == m.casterEntity
                    || mc.thePlayer.getDistanceSq(m.x, m.y, m.z) <= (m.radius + 12.0) * (m.radius + 12.0))) {
                    Dome r = new Dome();
                    r.session = m.session;
                    r.type = m.type;
                    r.casterEntity = m.casterEntity;
                    r.x = m.x;
                    r.y = m.y;
                    r.z = m.z;
                    r.radius = m.radius;
                    r.windup = m.ticks;
                    r.windupTotal = m.ticks;
                    rivalSign = r;
                    cutIn = r;
                    DomainSounds.play(m.type, SoundCues.VOICE, 1F, 1F);
                }
                break;
            }
            case DomainWire.CLASH: {
                Dome d = domes.get(m.session);
                if (d != null) {
                    d.clashing = true;
                }
                // For those in the domain and those close enough to its dome.
                if (clash != m.session && (inside != null && session == m.session || nearDome(m.session))) {
                    clash = m.session;
                    clashTime = m.ticks;
                    clashLeft = m.ticks + 40;
                    clashAge = 0;
                    clashTypes[0] = m.type;
                    clashTypes[1] = m.rivalType;
                    clashEntities[0] = m.casterEntity;
                    clashEntities[1] = m.rivalEntity;
                    clashHits[0] = 0;
                    clashHits[1] = 0;
                    front = new Frontier(m.session, radius);
                    frontPush = 0;
                    frontPushBefore = 0;
                    frontShoves.clear();
                    int run = ++clashRun;
                    DomainSounds.track(SoundCues.CLASH_TRACK, 1F, () -> clash >= 0 && clashRun == run);
                }
                break;
            }
            case DomainWire.BLOW:
                if (clash == m.session) {
                    Minecraft mc = Minecraft.getMinecraft();
                    Blow blow = blows.get(m.casterEntity);
                    if (!m.landed) {
                        // The blow begins: the body moves, and whatever blow it was in flows into this one.
                        Blow next = new Blow();
                        next.blow = m.blow;
                        if (blow != null) {
                            next.from = blow.blow;
                            next.fromAge = blow.age;
                            next.fromLayer = blow.layer(0);
                        }
                        blows.put(m.casterEntity, next);
                        Entity attacker = mc.theWorld == null ? null : mc.theWorld.getEntityByID(m.casterEntity);
                        if (attacker != null) {
                            DomainSounds.playAt(SoundCues.BLOW, attacker.posX, attacker.posY + 1, attacker.posZ, 1F);
                        }
                        break;
                    }
                    // The fist arrives, and lands.
                    int other = clashEntities[clashEntities[0] == m.casterEntity ? 1 : 0];
                    Entity victim = mc.theWorld == null ? null : mc.theWorld.getEntityByID(other);
                    if (blow != null && blow.blow == m.blow) {
                        blow.victim = other;
                        blow.landedAt = blow.age;
                    }
                    // The game's hit is not used (it knocks back and jolts the view): what tells the blow is ours.
                    // The one struck flashes red for those who see it, and sounds hurt.
                    if (victim instanceof EntityLivingBase && victim != mc.thePlayer) {
                        ((EntityLivingBase) victim).maxHurtTime = HURT_FLASH;
                        ((EntityLivingBase) victim).hurtTime = HURT_FLASH;
                    }
                    if (victim != null && mc.theWorld != null) {
                        mc.theWorld.playSound(
                            victim.posX,
                            victim.posY,
                            victim.posZ,
                            "game.player.hurt",
                            0.8F,
                            0.9F + (m.blow % 3) * 0.08F,
                            false);
                    }
                    // Who takes it is thrown by it: from its left when the other strikes with the right hand.
                    Struck struck = new Struck();
                    struck.side = m.blow % 2 == 1 ? 1 : -1;
                    strucks.put(other, struck);
                    if (victim != null && inside != null) {
                        // The blow shoves the front, from where it landed, away from who struck.
                        double[] at = toScene(victim.posX, victim.posZ);
                        frontShoves.add(new float[] { (float) at[0], 0, clashEntities[0] == m.casterEntity ? 1 : -1 });
                    }
                    if (victim != null) {
                        DomainSounds.playAt(SoundCues.IMPACT, victim.posX, victim.posY + 1, victim.posZ, 1F);
                    }
                    // Whoever gives or takes the blow feels it; the finisher more.
                    if (mc.thePlayer != null
                        && (mc.thePlayer.getEntityId() == other || mc.thePlayer.getEntityId() == m.casterEntity)) {
                        CameraShake.INSTANCE.kick(m.blow == 4 ? 1.6F : 0.6F);
                    }
                }
                break;
            case DomainWire.CLASH_SCORE:
                if (clash == m.session) {
                    clashHits[0] = m.hostHits;
                    clashHits[1] = m.rivalHits;
                }
                break;
            case DomainWire.CLASH_END: {
                endClash(m.session);
                Dome d = domes.get(m.session);
                if (d != null) {
                    d.clashing = false;
                    d.type = m.type;
                }
                if (inside != null && session == m.session && m.result != 2) {
                    // The winner's domain, from its start. A tie is followed by the end of the domain.
                    DomainType was = inside;
                    if (m.result == 1) {
                        // The rival's domain stays where it stood in the clash, and takes the other half too.
                        turned = true;
                        turnedAt = sceneSeam() + stage(m.type);
                    }
                    inside = m.type;
                    casterEntity = m.casterEntity;
                    age = 0;
                    flash(
                        DomainRenderer.look(m.type)
                            .bandTrim());
                    CameraShake.INSTANCE.kick(2.5F);
                    if (was != m.type) {
                        DomainType won = m.type;
                        int in = m.session;
                        DomainSounds
                            .loop(SoundCues.of(won.id, SoundCues.AMBIENT), 0.6F, () -> inside == won && session == in);
                    }
                }
                break;
            }
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

    /** Whether two domains share the arena now: each is drawn in its half ({@link #sceneSeam}). */
    public boolean fused() {
        return inside != null && clash == session && clashTypes[0] != null && front != null;
    }

    /**
     * Where the two domains of a clash meet: a line across the scene, this far before its centre. The host's domain
     * lies behind the line, where its scene has what it has; the rival's before it, turned about to face it. In a
     * shrine the line is where the casters stand, before the door of the hall; in a void, through the middle.
     */
    public float sceneSeam() {
        return openedAs == DomainType.MALEVOLENT_SHRINE ? ShrineCore.CASTER_DISTANCE : 0;
    }

    /** Where a place of the world is in the arena's scene: along the seam, and before the arena's centre. */
    public double[] toScene(double worldX, double worldZ) {
        double a = Math.toRadians(yaw);
        double x = worldX - arenaX;
        double z = worldZ - arenaZ;
        return new double[] { x * Math.cos(a) + z * Math.sin(a), -x * Math.sin(a) + z * Math.cos(a) };
    }

    /**
     * The front between the two domains of a clash, now: points across the arena, for each how far along the seam
     * and how far before it ({@link Frontier#line}).
     */
    public float[] frontLine(float partialTicks) {
        float[] shoves = new float[frontShoves.size() * 3];
        for (int i = 0; i < frontShoves.size(); i++) {
            float[] shove = frontShoves.get(i);
            shoves[i * 3] = shove[0];
            shoves[i * 3 + 1] = shove[1] + partialTicks;
            shoves[i * 3 + 2] = shove[2];
        }
        float push = frontPushBefore + (frontPush - frontPushBefore) * partialTicks;
        return front.line(clashAge + partialTicks, push, shoves);
    }

    /** How far before the seam the centre of a rival's scene is put: a shrine's hall stands back from it. */
    public static float stage(DomainType type) {
        return type == DomainType.MALEVOLENT_SHRINE ? ShrineCore.CASTER_DISTANCE : 0;
    }

    /** The one domain of the arena: the host's while two share it. */
    public DomainType shown() {
        return inside;
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
            rivalSign = null;
            blows.clear();
            strucks.clear();
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
        if (clash >= 0) {
            clashAge++;
            // The front goes after the score: a seventh of the way each tick.
            frontPushBefore = frontPush;
            frontPush += ((clashHits[0] - clashHits[1]) / 16F - frontPush) * FRONT_FOLLOW;
            for (Iterator<float[]> it = frontShoves.iterator(); it.hasNext();) {
                if (++it.next()[1] >= Frontier.SHOVE_TICKS) {
                    it.remove();
                }
            }
            if (clashTime > 0) {
                clashTime--;
            }
        }
        for (Iterator<Blow> it = blows.values()
            .iterator(); it.hasNext();) {
            if (++it.next().age >= Blow.TICKS) {
                it.remove();
            }
        }
        for (Iterator<Struck> it = strucks.values()
            .iterator(); it.hasNext();) {
            if (++it.next().age >= BlowPose.STRUCK_TICKS) {
                it.remove();
            }
        }
        if (rivalSign != null && --rivalSign.windup <= 0) {
            rivalSign = null;
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
        if (cutIn != null && (cutIn.windup <= 0 || cutIn != rivalSign && !domes.containsValue(cutIn))) {
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
