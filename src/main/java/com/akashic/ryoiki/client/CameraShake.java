package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Shakes the view: for the length of each frame the view entity looks a little off, and is put back before anything
 * else reads where it looks. Nothing is sent to the server.
 */
public final class CameraShake {

    public static final CameraShake INSTANCE = new CameraShake();

    /** Degrees, dying away every tick. */
    private float amplitude;
    private float yaw;
    private float pitch;
    private EntityLivingBase shaken;

    private CameraShake() {}

    /** A jolt of this many degrees, unless a bigger one is still going. */
    public void kick(float degrees) {
        amplitude = Math.max(amplitude, degrees);
    }

    public void stop() {
        amplitude = 0;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !Minecraft.getMinecraft()
            .isGamePaused()) {
            amplitude *= 0.72F;
            if (amplitude < 0.02F) {
                amplitude = 0;
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            EntityLivingBase view = mc.renderViewEntity;
            if (amplitude <= 0 || view == null || mc.theWorld == null) {
                return;
            }
            double time = System.nanoTime() / 1.0e9;
            yaw = amplitude * (float) (Math.sin(time * 71) * 0.6 + Math.sin(time * 113 + 1.3) * 0.4);
            pitch = amplitude * (float) (Math.sin(time * 89 + 0.7) * 0.6 + Math.sin(time * 127) * 0.4);
            shaken = view;
            turn(view, yaw, pitch);
        } else if (shaken != null) {
            turn(shaken, -yaw, -pitch);
            shaken = null;
        }
    }

    private static void turn(EntityLivingBase view, float yaw, float pitch) {
        view.rotationYaw += yaw;
        view.prevRotationYaw += yaw;
        view.rotationPitch += pitch;
        view.prevRotationPitch += pitch;
    }
}
