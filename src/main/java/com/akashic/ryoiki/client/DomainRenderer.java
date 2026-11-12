package com.akashic.ryoiki.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.RyoikiTenkai;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the domains. Inside one, the arena is invisible blocks, so everything seen is drawn here: a closed shell
 * around the arena (the domain's sky) and its floor. From outside, a dome where the domain was cast. A first, plain
 * look: the models, the cut-in and the slashes come on top of this.
 */
public final class DomainRenderer {

    private static final int SLICES = 40;
    private static final int STACKS = 20;
    private static final int STARS = 700;

    /** Unit directions and sizes of the Infinite Void's stars, the same in every client. */
    private final float[] stars = new float[STARS * 5];

    public DomainRenderer() {
        Random random = new Random(0x52594F494B49L);
        for (int i = 0; i < STARS; i++) {
            double y = random.nextDouble() * 1.1 - 0.1;
            double a = random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            stars[i * 5] = (float) (Math.cos(a) * r);
            stars[i * 5 + 1] = (float) y;
            stars[i * 5 + 2] = (float) (Math.sin(a) * r);
            stars[i * 5 + 3] = 0.004F + random.nextFloat() * random.nextFloat() * 0.012F;
            stars[i * 5 + 4] = random.nextFloat();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        ClientDomain domain = ClientDomain.INSTANCE;
        if (domain.inside == null && domain.domes.isEmpty()) {
            return;
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        if (domain.inside != null) {
            GL11.glPushMatrix();
            GL11.glTranslated(
                domain.arenaX - RenderManager.renderPosX,
                domain.arenaY - RenderManager.renderPosY,
                domain.arenaZ - RenderManager.renderPosZ);
            interior(domain.inside, domain.radius, domain.age + event.partialTicks);
            GL11.glPopMatrix();
        }
        for (ClientDomain.Dome dome : domain.domes.values()) {
            float size;
            if (dome.windup > 0) {
                // The hand sign: the dome swells from the caster, slowly, then all at once.
                float t = 1 - (dome.windup - event.partialTicks) / Math.max(1, dome.windupTotal);
                size = dome.radius * t * t * t * t;
            } else {
                size = dome.radius;
            }
            if (size <= 0.05F) {
                continue;
            }
            GL11.glPushMatrix();
            GL11.glTranslated(
                dome.x - RenderManager.renderPosX,
                dome.y - RenderManager.renderPosY,
                dome.z - RenderManager.renderPosZ);
            int c = dome.type.borderColor;
            sphere(size, c, c, 1);
            GL11.glPopMatrix();
        }
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopAttrib();
    }

    private void interior(DomainType type, int radius, float time) {
        // Beyond the invisible shell blocks, so a camera against the wall is still inside it.
        float shell = radius + 2.2F;
        if (type == DomainType.INFINITE_VOID) {
            sphere(shell, 0x01020A, 0x10103A, 0.35F);
            floor(radius + 2, time, 0x04051A, 0x0A0C30, 0.25F);
            stars(shell - 0.2F, time);
        } else {
            sphere(shell, 0x000000, 0x1A0004, 0.2F);
            floor(radius + 2, time, 0x2A0006, 0x7A0010, 1.6F);
        }
    }

    /** A sphere whose colour goes from {@code top} at the zenith to {@code horizon} at the horizon and below. */
    private static void sphere(float radius, int top, int horizon, float bandPower) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        for (int i = 0; i < STACKS; i++) {
            double p0 = Math.PI * i / STACKS - Math.PI / 2;
            double p1 = Math.PI * (i + 1) / STACKS - Math.PI / 2;
            for (int j = 0; j < SLICES; j++) {
                double a0 = Math.PI * 2 * j / SLICES;
                double a1 = Math.PI * 2 * (j + 1) / SLICES;
                vertex(t, radius, p0, a0, top, horizon, bandPower);
                vertex(t, radius, p0, a1, top, horizon, bandPower);
                vertex(t, radius, p1, a1, top, horizon, bandPower);
                vertex(t, radius, p1, a0, top, horizon, bandPower);
            }
        }
        t.draw();
    }

    private static void vertex(Tessellator t, float radius, double pitch, double yaw, int top, int horizon,
        float bandPower) {
        double y = Math.sin(pitch);
        double r = Math.cos(pitch);
        float blend = (float) Math.pow(Math.max(0, y), bandPower);
        color(t, horizon, top, blend, 1);
        t.addVertex(Math.cos(yaw) * r * radius, y * radius, Math.sin(yaw) * r * radius);
    }

    /** The floor: rings of colour that travel outwards from the centre ({@code speed} 0 for a still floor). */
    private static void floor(float radius, float time, int dark, int bright, float speed) {
        Tessellator t = Tessellator.instance;
        int rings = 48;
        t.startDrawingQuads();
        for (int i = 0; i < rings; i++) {
            float r0 = radius * i / rings;
            float r1 = radius * (i + 1) / rings;
            float w0 = wave(r0, time, speed);
            float w1 = wave(r1, time, speed);
            for (int j = 0; j < SLICES; j++) {
                double a0 = Math.PI * 2 * j / SLICES;
                double a1 = Math.PI * 2 * (j + 1) / SLICES;
                color(t, dark, bright, w0, 1);
                t.addVertex(Math.cos(a0) * r0, 0.01, Math.sin(a0) * r0);
                t.addVertex(Math.cos(a1) * r0, 0.01, Math.sin(a1) * r0);
                color(t, dark, bright, w1, 1);
                t.addVertex(Math.cos(a1) * r1, 0.01, Math.sin(a1) * r1);
                t.addVertex(Math.cos(a0) * r1, 0.01, Math.sin(a0) * r1);
            }
        }
        t.draw();
    }

    private static float wave(float r, float time, float speed) {
        float fade = 1 - r / 18F;
        float s = (float) Math.sin(r * 1.3F - time * 0.12F * speed);
        return Math.max(0, s * s * s) * fade * 0.8F + 0.1F;
    }

    private void stars(float radius, float time) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        for (int i = 0; i < STARS; i++) {
            float x = stars[i * 5];
            float y = stars[i * 5 + 1];
            float z = stars[i * 5 + 2];
            float size = stars[i * 5 + 3];
            float twinkle = 0.55F + 0.45F * (float) Math.sin(time * 0.15F + stars[i * 5 + 4] * 40);
            // Two directions across the sphere at this star.
            float len = (float) Math.sqrt(x * x + z * z);
            float ux = len < 1.0e-4F ? 1 : -z / len;
            float uz = len < 1.0e-4F ? 0 : x / len;
            float vx = y * uz;
            float vy = z * ux - x * uz;
            float vz = -y * ux;
            t.setColorRGBA_F(twinkle, twinkle, Math.min(1, twinkle * 1.15F), 1);
            t.addVertex(
                (x - ux * size - vx * size) * radius,
                (y - vy * size) * radius,
                (z - uz * size - vz * size) * radius);
            t.addVertex(
                (x + ux * size - vx * size) * radius,
                (y - vy * size) * radius,
                (z + uz * size - vz * size) * radius);
            t.addVertex(
                (x + ux * size + vx * size) * radius,
                (y + vy * size) * radius,
                (z + uz * size + vz * size) * radius);
            t.addVertex(
                (x - ux * size + vx * size) * radius,
                (y + vy * size) * radius,
                (z - uz * size + vz * size) * radius);
        }
        t.draw();
    }

    private static void color(Tessellator t, int from, int to, float blend, float alpha) {
        blend = Math.max(0, Math.min(1, blend));
        float r = ((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * blend) / 255F;
        float g = ((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * blend) / 255F;
        float b = ((from & 255) + ((to & 255) - (from & 255)) * blend) / 255F;
        t.setColorRGBA_F(r, g, b, alpha);
    }

    /** The flash that covers the cut between the map and the arena. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        ClientDomain domain = ClientDomain.INSTANCE;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || domain.flash <= 0) {
            return;
        }
        float left = domain.flash - event.partialTicks;
        float fading = ClientDomain.FLASH_TICKS - ClientDomain.FLASH_HOLD;
        float alpha = Math.max(0, Math.min(1, left / fading));
        ScaledResolution res = event.resolution;
        int argb = (int) (alpha * 255) << 24 | domain.flashColor;
        Gui.drawRect(0, 0, res.getScaledWidth(), res.getScaledHeight(), argb);
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** No outline on the invisible blocks of an arena. */
    @SubscribeEvent
    public void onHighlight(DrawBlockHighlightEvent event) {
        MovingObjectPosition hit = event.target;
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && Minecraft.getMinecraft().theWorld.getBlock(hit.blockX, hit.blockY, hit.blockZ) == RyoikiTenkai.veil) {
            event.setCanceled(true);
        }
    }
}
