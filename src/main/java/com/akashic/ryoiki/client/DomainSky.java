package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.IRenderHandler;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.Frontier;

/**
 * The domain's sky, in place of the world's while the player is inside: the game calls it where it would draw its
 * own sky, before anything else, so the domain's sky is behind everything. The clouds and the rain are swapped for
 * nothing. What the world had is put back when the player leaves.
 */
final class DomainSky extends IRenderHandler {

    private static final IRenderHandler NOTHING = new IRenderHandler() {

        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {}
    };

    private WorldProvider provider;
    private IRenderHandler oldSky;
    private IRenderHandler oldClouds;
    private IRenderHandler oldWeather;
    /** Whether the game drew the sky this frame: it does not when the render distance is very short. */
    boolean drawn;

    void install(WorldProvider to) {
        if (provider == to) {
            return;
        }
        restore();
        provider = to;
        oldSky = to.getSkyRenderer();
        oldClouds = to.getCloudRenderer();
        oldWeather = to.getWeatherRenderer();
        to.setSkyRenderer(this);
        to.setCloudRenderer(NOTHING);
        to.setWeatherRenderer(NOTHING);
    }

    void restore() {
        if (provider != null) {
            if (provider.getSkyRenderer() == this) {
                provider.setSkyRenderer(oldSky);
            }
            if (provider.getCloudRenderer() == NOTHING) {
                provider.setCloudRenderer(oldClouds);
            }
            if (provider.getWeatherRenderer() == NOTHING) {
                provider.setWeatherRenderer(oldWeather);
            }
            provider = null;
        }
    }

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        ClientDomain domain = ClientDomain.INSTANCE;
        if (domain.inside == null) {
            return;
        }
        drawn = true;
        draw(domain, partialTicks, mc, false);
    }

    /** Draws the sky around the camera. With {@code behind}, only where nothing nearer was drawn. */
    static void draw(ClientDomain domain, float partialTicks, Minecraft mc, boolean behind) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);
        if (behind) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glShadeModel(GL11.GL_SMOOTH);
        // The line between the two domains of a clash is a place in the arena: the sky of each is cut by it too,
        // so the sky changes where the floor does.
        Entity view = mc.renderViewEntity;
        double x = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double y = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double z = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
        float radius = far(mc) * 0.92F;
        float time = domain.age + partialTicks;
        if (!domain.fused()) {
            GL11.glPushMatrix();
            GL11.glRotatef(-domain.yaw + (domain.turned ? 180 : 0), 0, 1, 0);
            DomainRenderer.look(domain.shown())
                .sky(radius, time);
            GL11.glPopMatrix();
        } else {
            fused(domain, radius, time, partialTicks, x, y, z);
        }
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glPopAttrib();
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** Blocks across which one sky gives way to the other, at the distance the sky is drawn. */
    private static final float BAND = 0.62F;
    /** Steps the lights of each sky fade in across that band. */
    private static final int STEPS = 6;

    /**
     * The two skies of a clash. Their ground colours are one sphere, each direction a mix of the two by how far
     * before the front the sky there is, with a slow turbulence so that the edge between them drifts and never is a
     * line. Their lights (stars, the moon, what hangs in each sky) are drawn whole on their own side and fade out
     * in steps across the same band.
     */
    private static void fused(ClientDomain domain, float radius, float time, float partialTicks, double camX,
        double camY, double camZ) {
        float[] line = domain.frontLine(partialTicks);
        double[] cam = domain.toScene(camX, camZ);
        float seam = domain.sceneSeam();
        float band = radius * BAND;
        float clashTime = domain.clashAge + partialTicks;
        // Where the front runs on the average: the band of the lights is straight, and lies along it.
        float middle = 0;
        for (int i = 1; i < line.length; i += 2) {
            middle += line[i] / (line.length / 2);
        }
        GL11.glPushMatrix();
        GL11.glRotatef(-domain.yaw, 0, 1, 0);
        GL11.glDisable(GL11.GL_BLEND);
        Draw.spheres(
            radius,
            Scene.look(domain, 0)
                .backdrop(),
            Scene.look(domain, 1)
                .backdrop(),
            (dx, dy, dz) -> {
                float before = Frontier
                    .before(line, (float) (cam[0] + dx * radius), (float) (cam[1] + dz * radius) - seam);
                double around = Math.atan2(dz, dx);
                double churn = 0.16 * Math.sin(3 * around + clashTime / 37.0 + dy * 4)
                    + 0.09 * Math.sin(7 * around - clashTime / 23.0 - dy * 9)
                    + 0.05 * Math.sin(13 * around + clashTime / 11.0 + dy * 17);
                float t = Draw.clamp01(0.5F + before / band + (float) churn);
                return t * t * (3 - 2 * t);
            });
        GL11.glPopMatrix();
        for (int part = 0; part < 2; part++) {
            // Its own side of the band whole, then fainter step by step across it.
            for (int step = 0; step <= STEPS; step++) {
                float near = middle + band * (step == 0 ? Float.NEGATIVE_INFINITY : (step - 1F) / STEPS - 0.5F);
                float far = middle + band * ((float) step / STEPS - 0.5F);
                GL11.glPushMatrix();
                GL11.glTranslated(domain.arenaX - camX, domain.arenaY - camY, domain.arenaZ - camZ);
                GL11.glRotatef(-domain.yaw, 0, 1, 0);
                // The host's lights are whole behind the band, the rival's before it.
                if (part == 0) {
                    Scene.band(domain, near, far);
                } else {
                    Scene.band(domain, -far + 2 * middle, step == 0 ? Float.POSITIVE_INFINITY : -near + 2 * middle);
                }
                GL11.glPopMatrix();
                GL11.glPushMatrix();
                GL11.glRotatef(-domain.yaw + (part == 1 ? 180 : 0), 0, 1, 0);
                Draw.fade = step == 0 ? 1 : 1 - (step - 0.5F) / STEPS;
                Scene.look(domain, part)
                    .skyFeatures(radius, time);
                GL11.glPopMatrix();
                Scene.leave();
            }
        }
        Draw.fade = 1;
    }

    /** How far the game draws, in blocks. */
    static float far(Minecraft mc) {
        return Math.max(2, mc.gameSettings.renderDistanceChunks) * 16;
    }
}
