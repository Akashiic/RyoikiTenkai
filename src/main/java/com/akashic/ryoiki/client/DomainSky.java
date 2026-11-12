package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.IRenderHandler;

import org.lwjgl.opengl.GL11;

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
        DomainRenderer.look(domain.inside)
            .sky(far(mc) * 0.92F, domain.age + partialTicks);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glPopAttrib();
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** How far the game draws, in blocks. */
    static float far(Minecraft mc) {
        return Math.max(2, mc.gameSettings.renderDistanceChunks) * 16;
    }
}
