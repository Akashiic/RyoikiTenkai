package com.akashic.ryoiki.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import com.akashic.ryoiki.fx.Gait;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Follows each player in the sorcerer mode this client sees, tick by tick, and keeps what {@link Gait} needs to
 * pose it: where it is in its stride, how fast and which way it goes, whether it is in the air. Everything is read
 * from how the player moves, the same for this client's own player and for the others.
 */
public final class Stride {

    public static final Stride INSTANCE = new Stride();

    /** For the development harness: off, the models move as they do without the mode. */
    public static boolean enabled = true;

    /** Blocks a tick at which a body counts as fully moving, and under which it counts as standing. */
    private static final float FULL = 0.13F;
    private static final float STILL = 0.02F;
    /** Ticks in the air before coming down counts as a landing. */
    private static final int FLIGHT = 5;

    private final Map<Integer, Track> tracks = new HashMap<>();

    static final class Track {

        final Gait.State now = new Gait.State();
        final Gait.State before = new Gait.State();
        private final Gait.State drawn = new Gait.State();
        /** How much of the mode's pose shows: it comes in over a few ticks after the mode begins. */
        float in;
        float inBefore;
        int airTicks;
        boolean seen;

        /** The state between the last tick and this one. */
        Gait.State at(float partial) {
            drawn.phase = before.phase + (now.phase - before.phase) * partial;
            drawn.moving = before.moving + (now.moving - before.moving) * partial;
            drawn.sprint = before.sprint + (now.sprint - before.sprint) * partial;
            drawn.heading = before.heading + MathHelper.wrapAngleTo180_float(now.heading - before.heading) * partial;
            drawn.air = before.air + (now.air - before.air) * partial;
            drawn.rising = before.rising + (now.rising - before.rising) * partial;
            drawn.landed = before.landed + (now.landed - before.landed) * partial;
            drawn.clock = before.clock + (now.clock - before.clock) * partial;
            return drawn;
        }

        float weight(float partial) {
            float w = inBefore + (in - inBefore) * partial;
            return w * w * (3 - 2 * w);
        }
    }

    private Stride() {}

    /** A player's stride, if it is in the mode and moving as the mode poses it; else null. */
    Track of(EntityPlayer player) {
        Track track = tracks.get(player.getEntityId());
        return track == null || track.in <= 0 && track.inBefore <= 0 ? null : track;
    }

    /** Whether the mode poses this player now: not while it does what its model has its own way of doing. */
    private static boolean posed(EntityPlayer player) {
        return enabled && SorcererClient.INSTANCE.inMode(player)
            && player.isEntityAlive()
            && !player.isSneaking()
            && !player.isRiding()
            && !player.isPlayerSleeping()
            && !player.isInWater()
            && !player.isOnLadder()
            && !player.capabilities.isFlying;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.theWorld == null) {
            tracks.clear();
            return;
        }
        if (mc.isGamePaused()) {
            return;
        }
        for (Track track : tracks.values()) {
            track.seen = false;
        }
        for (Object o : mc.theWorld.playerEntities) {
            EntityPlayer player = (EntityPlayer) o;
            boolean posed = posed(player);
            Track track = tracks.get(player.getEntityId());
            if (track == null) {
                if (!posed) {
                    continue;
                }
                track = new Track();
                tracks.put(player.getEntityId(), track);
            }
            track.seen = true;
            follow(mc, player, track, posed);
        }
        for (Iterator<Track> i = tracks.values()
            .iterator(); i.hasNext();) {
            Track track = i.next();
            if (!track.seen || track.in <= 0 && track.inBefore <= 0) {
                i.remove();
            }
        }
    }

    private static void follow(Minecraft mc, EntityPlayer player, Track track, boolean posed) {
        Gait.State s = track.now;
        Gait.State b = track.before;
        b.phase = s.phase;
        b.moving = s.moving;
        b.sprint = s.sprint;
        b.heading = s.heading;
        b.air = s.air;
        b.rising = s.rising;
        b.landed = s.landed;
        b.clock = s.clock;
        track.inBefore = track.in;
        track.in = Math.max(0, Math.min(1, track.in + (posed ? 0.2F : -0.25F)));

        double dx = player.posX - player.prevPosX;
        double dy = player.posY - player.prevPosY;
        double dz = player.posZ - player.prevPosZ;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        // On the ground: the game says so for this client's own player; for the others, by what is under their feet.
        boolean grounded = player.onGround || !mc.theWorld.getCollidingBoundingBoxes(
            player,
            player.boundingBox.copy()
                .offset(0, -0.12, 0))
            .isEmpty();
        s.clock++;
        s.landed++;
        if (grounded) {
            if (track.airTicks >= FLIGHT) {
                s.landed = 0;
            }
            track.airTicks = 0;
        } else {
            track.airTicks++;
        }
        s.air = Math.max(0, Math.min(1, s.air + (grounded ? -0.4F : 0.34F)));
        s.rising += ((float) dy - s.rising) * 0.6F;
        float moving = Math.max(0, Math.min(1, (speed - STILL) / (FULL - STILL)));
        s.moving += (moving - s.moving) * 0.35F;
        s.sprint += ((player.isSprinting() && speed > 0.15F ? 1 : 0) - s.sprint) * 0.25F;
        if (speed > STILL) {
            // The way it goes against the way its body faces: 0 ahead, 90 to its own right.
            float going = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
            float heading = MathHelper.wrapAngleTo180_float(going - player.renderYawOffset);
            s.heading = MathHelper
                .wrapAngleTo180_float(s.heading + MathHelper.wrapAngleTo180_float(heading - s.heading) * 0.4F);
            // The stride goes on by the ground covered, and waits while the body is in the air.
            if (s.air < 0.5F) {
                s.phase += speed / Gait.stride(s.sprint);
            }
        }
        if (s.phase > 64) {
            s.phase -= 64;
            b.phase -= 64;
        }
    }
}
