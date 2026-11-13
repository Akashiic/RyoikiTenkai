package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.RyoikiTenkai;
import com.akashic.ryoiki.fx.BlowPose;
import com.akashic.ryoiki.fx.Frontier;
import com.akashic.ryoiki.fx.StreakField;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the domains. Inside one, the arena is invisible blocks, so everything seen is drawn here and in
 * {@link DomainSky}: the sky, its reflection, the floor and what stands in the arena, each domain by its
 * {@link DomainLook}. From outside: the seal on the ground while the caster makes the hand sign, then the dome. On
 * the screen: the cut-in, the flash going in and the shatter coming out.
 */
public final class DomainRenderer {

    /** The dome of a clash is black whichever domains fight in it. */
    private static final int CLASH_BORDER = 0x050505;
    /** Ticks the burst of a blow that landed is drawn for. */
    private static final int IMPACT_TICKS = 5;
    /**
     * Whether a fused scene is put in a display list and replayed for each piece of its ground (else it is built
     * again for each): the same picture, far less work. {@code -Dryoiki.fusionLists=false} turns it off.
     */
    private static final boolean LISTS = !"false".equals(System.getProperty("ryoiki.fusionLists"));
    private static int lists = -1;
    /** Blocks about the line over which the fog goes from one domain's colour to the other's. */
    private static final float FOG_BLEND = 8;

    /** Ticks into the domain the caster still holds the sign, the last twelve letting it go. */
    private static final int HOLD_SIGN_TICKS = 34;
    private static final DomainLook VOID = new VoidLook();
    private static final DomainLook SHRINE = new ShrineLook();

    static DomainLook look(DomainType type) {
        return type == DomainType.MALEVOLENT_SHRINE ? SHRINE : VOID;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        ClientDomain domain = ClientDomain.INSTANCE;
        if (domain.inside == null && domain.domes.isEmpty() && domain.blows.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (domain.inside != null) {
            if (!domain.sky.drawn) {
                // The game skipped the sky (a very short render distance): draw it now, behind what is there.
                DomainSky.draw(domain, event.partialTicks, mc, true);
            }
            domain.sky.drawn = false;
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        if (domain.inside != null) {
            float time = domain.age + event.partialTicks;
            float far = DomainSky.far(mc) * 0.9F;
            double a = Math.toRadians(domain.yaw);
            double camX = RenderManager.renderPosX - domain.arenaX;
            double camZ = RenderManager.renderPosZ - domain.arenaZ;
            double sceneX = camX * Math.cos(a) + camZ * Math.sin(a);
            double sceneZ = -camX * Math.sin(a) + camZ * Math.cos(a);
            GL11.glPushMatrix();
            GL11.glTranslated(
                domain.arenaX - RenderManager.renderPosX,
                domain.arenaY + 0.01 - RenderManager.renderPosY,
                domain.arenaZ - RenderManager.renderPosZ);
            // The scene is laid out behind where the caster looked: the same for everyone in the domain.
            GL11.glRotatef(-domain.yaw, 0, 1, 0);
            float[] line = domain.fused() ? domain.frontLine(event.partialTicks) : null;
            // One domain, or the two of a clash, each on its ground.
            for (int part = 0; part < Scene.parts(domain); part++) {
                DomainLook look = Scene.look(domain, part);
                double[] cam = Scene.local(domain, part, sceneX, sceneZ);
                double camY = RenderManager.renderPosY - domain.arenaY;
                boolean turned = Scene.turned(domain, part);
                int part0 = part;
                Runnable draw = () -> {
                    GL11.glPushMatrix();
                    Scene.enter(domain, part0);
                    Draw.camera(domain.yaw + (turned ? 180 : 0));
                    // The floor does not hide what is drawn after it: the mist and the lights lie right on it.
                    GL11.glDepthMask(false);
                    look.floor(far, domain.radius, time);
                    GL11.glDepthMask(true);
                    look.props(far, domain.radius, time, domain.session, cam[0], camY, cam[1]);
                    GL11.glPopMatrix();
                };
                if (line == null) {
                    draw.run();
                    GL11.glPushMatrix();
                    Scene.enter(domain, part0);
                    look.effects(domain.radius, time, domain.session, cam[0], camY, cam[1]);
                    GL11.glPopMatrix();
                    continue;
                }
                // The scene is put together once and drawn again for each piece of its ground.
                if (LISTS) {
                    if (lists < 0) {
                        lists = GLAllocation.generateDisplayLists(2);
                    }
                    GL11.glNewList(lists + part, GL11.GL_COMPILE);
                    draw.run();
                    GL11.glEndList();
                }
                for (int piece = 0; piece < Scene.pieces(domain); piece++) {
                    Scene.clip(domain, part, piece, line);
                    if (LISTS) {
                        GL11.glCallList(lists + part);
                    } else {
                        draw.run();
                    }
                }
                Scene.leave();
            }
            if (line != null) {
                // What each domain throws through the air does not stop at the front: it goes on into the other's
                // ground, weaker the further in.
                for (int part = 0; part < 2; part++) {
                    double[] cam = Scene.local(domain, part, sceneX, sceneZ);
                    GL11.glPushMatrix();
                    Scene.enter(domain, part);
                    Draw.camera(domain.yaw + (Scene.turned(domain, part) ? 180 : 0));
                    Scene.ground(domain, part, line);
                    Scene.look(domain, part)
                        .effects(
                            domain.radius,
                            time,
                            domain.session,
                            cam[0],
                            RenderManager.renderPosY - domain.arenaY,
                            cam[1]);
                    Scene.plain();
                    GL11.glPopMatrix();
                }
                ClashFront.draw(domain, line, domain.clashAge + event.partialTicks, event.partialTicks);
            }
            GL11.glPopMatrix();
        }
        for (ClientDomain.Blow blow : domain.blows.values()) {
            int since = blow.age - blow.landedAt;
            Entity victim = blow.victim < 0 || since >= IMPACT_TICKS ? null : mc.theWorld.getEntityByID(blow.victim);
            if (victim == null || victim == mc.renderViewEntity && mc.gameSettings.thirdPersonView == 0) {
                // Seen through the eyes of who takes the blow, the burst would be on the eye itself: the jolt of the
                // camera tells it instead.
                continue;
            }
            // Where a blow lands: a burst of light at the chest, larger for the finisher.
            float t = (since + event.partialTicks) / IMPACT_TICKS;
            float size = (blow.blow == 4 ? 1.9F : 1.1F) * (0.35F + 0.65F * Draw.easeOut(t));
            int color = domain.clashTypes[0] == null ? 0xFFFFFF
                : look(domain.clashTypes[domain.clashEntities[0] == blow.victim ? 1 : 0]).bandTrim();
            Draw.additive();
            GL11.glDepthMask(false);
            Tessellator tess = Tessellator.instance;
            double x = victim.lastTickPosX + (victim.posX - victim.lastTickPosX) * event.partialTicks;
            double y = victim.lastTickPosY + (victim.posY - victim.lastTickPosY) * event.partialTicks;
            double z = victim.lastTickPosZ + (victim.posZ - victim.lastTickPosZ) * event.partialTicks;
            Draw.camera(0);
            tess.startDrawing(GL11.GL_TRIANGLES);
            Draw.glow(
                tess,
                x - RenderManager.renderPosX,
                y + victim.height * 0.62 - RenderManager.renderPosY,
                z - RenderManager.renderPosZ,
                size,
                Draw.mix(color, 0xFFFFFF, 0.5F),
                1 - t);
            tess.draw();
            GL11.glDepthMask(true);
        }
        for (ClientDomain.Dome dome : domain.domes.values()) {
            GL11.glPushMatrix();
            GL11.glTranslated(
                dome.x - RenderManager.renderPosX,
                dome.y - RenderManager.renderPosY,
                dome.z - RenderManager.renderPosZ);
            DomainLook look = look(dome.type);
            if (dome.windup > 0) {
                float t = 1 - (dome.windup - event.partialTicks) / Math.max(1, dome.windupTotal);
                seal(look, dome.radius, t, dome.windupTotal * t);
                // The dome swells from the caster, slowly, then all at once.
                float size = dome.radius * t * t * t * t;
                if (size > 0.05F) {
                    GL11.glDisable(GL11.GL_BLEND);
                    int c = dome.clashing ? CLASH_BORDER : dome.type.borderColor;
                    Draw.sphere(size, c, c, c, 1);
                }
            } else {
                GL11.glDisable(GL11.GL_BLEND);
                int c = dome.clashing ? CLASH_BORDER : dome.type.borderColor;
                Draw.sphere(dome.radius, c, Draw.mix(c, look.bandTrim(), 0.12F), c, 2);
                // Where it meets the ground, a line of light.
                Draw.additive();
                GL11.glDepthMask(false);
                GL11.glTranslatef(0, 0.03F, 0);
                float pulse = 0.6F + 0.4F * (float) Math.sin((dome.age + event.partialTicks) * 0.12F);
                Draw.ring(
                    dome.radius,
                    dome.radius + 0.12F,
                    96,
                    look.bandTrim(),
                    0.8F * pulse,
                    look.bandTrim(),
                    0.8F * pulse);
                Draw.ring(dome.radius, dome.radius + 2.2F, 96, look.bandTrim(), 0.25F * pulse, look.bandTrim(), 0);
                GL11.glDepthMask(true);
            }
            GL11.glPopMatrix();
        }
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopAttrib();
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** The seal under the caster during the hand sign: rings and rays that turn and spread as the moment nears. */
    private static void seal(DomainLook look, int radius, float progress, float ticks) {
        int rgb = look.bandTrim();
        float grow = Draw.easeOut(progress * 2.2F);
        float alpha = Draw.clamp01(progress * 6) * 0.9F;
        Draw.additive();
        GL11.glDepthMask(false);
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0.04F, 0);
        GL11.glRotatef(ticks * 7, 0, 1, 0);
        float r = 2.6F * grow;
        Draw.ring(r - 0.07F, r, 64, rgb, alpha, rgb, alpha);
        Draw.ring(r * 0.62F - 0.04F, r * 0.62F, 64, rgb, alpha, rgb, alpha);
        Draw.ring(0, r, 48, rgb, 0.22F * alpha, rgb, 0.04F * alpha);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLES);
        int rays = 12;
        for (int i = 0; i < rays; i++) {
            double a = Math.PI * 2 * i / rays;
            float inner = r * (i % 2 == 0 ? 0.62F : 0.8F);
            float outer = r * (i % 2 == 0 ? 1.55F : 1.2F);
            double w = 0.09;
            Draw.color(t, rgb, alpha);
            t.addVertex(Math.cos(a - w) * inner, 0, Math.sin(a - w) * inner);
            t.addVertex(Math.cos(a + w) * inner, 0, Math.sin(a + w) * inner);
            Draw.color(t, rgb, 0);
            t.addVertex(Math.cos(a) * outer, 0, Math.sin(a) * outer);
        }
        t.draw();
        GL11.glPopMatrix();
        // A wave on the ground that runs out to where the dome will stand.
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0.03F, 0);
        float wave = progress * progress * radius;
        Draw.ring(Math.max(0, wave - 1.2F), wave, 96, rgb, 0, rgb, 0.5F * alpha);
        GL11.glPopMatrix();
        GL11.glDepthMask(true);
    }

    /** The distance fades to the domain's own colour, not to the world's sky. */
    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors event) {
        ClientDomain domain = ClientDomain.INSTANCE;
        if (domain.inside != null) {
            int c = look(domain.shown()).fogColor();
            if (domain.fused()) {
                // The distance fades to the colour of the ground the camera is on, and to both near the front.
                double[] at = domain.toScene(RenderManager.renderPosX, RenderManager.renderPosZ);
                float before = Frontier.before(domain.frontLine(0), (float) at[0], (float) at[1] - domain.sceneSeam());
                float rival = Draw.clamp01(0.5F + before / FOG_BLEND);
                c = Draw.mix(look(domain.clashTypes[0]).fogColor(), look(domain.clashTypes[1]).fogColor(), rival);
            }
            event.red = (c >> 16 & 255) / 255F;
            event.green = (c >> 8 & 255) / 255F;
            event.blue = (c & 255) / 255F;
        }
    }

    /** On the screen: the cut-in, the flash that covers the way in and the shatter on the way out. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        ClientDomain domain = ClientDomain.INSTANCE;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        ScaledResolution res = event.resolution;
        Minecraft mc = Minecraft.getMinecraft();
        if (domain.cutIn != null && domain.cutIn.windup > 0 && mc.theWorld != null) {
            ClientDomain.Dome d = domain.cutIn;
            CutIn.draw(
                mc,
                look(d.type),
                d.type.id,
                d.casterEntity,
                d.windupTotal - d.windup + event.partialTicks,
                d.windupTotal,
                res.getScaledWidth(),
                res.getScaledHeight());
        }
        if (domain.clash >= 0 && mc.theWorld != null) {
            ClashHud.draw(mc, domain, res.getScaledWidth(), res.getScaledHeight());
        }
        if (domain.shatter > 0 && domain.shatterType != null) {
            Shatter.draw(
                look(domain.shatterType),
                domain.shatter - event.partialTicks,
                res.getScaledWidth(),
                res.getScaledHeight());
        }
        if (domain.inside == DomainType.INFINITE_VOID && !domain.fused()) {
            // The white between the void starting and the void complete.
            float white = StreakField.flash(domain.age + event.partialTicks);
            if (white > 0.004F) {
                Gui.drawRect(0, 0, res.getScaledWidth(), res.getScaledHeight(), (int) (white * 255) << 24 | 0xFFFFFF);
                GL11.glColor4f(1, 1, 1, 1);
            }
        }
        if (domain.flash > 0) {
            float left = domain.flash - event.partialTicks;
            float fading = ClientDomain.FLASH_TICKS - ClientDomain.FLASH_HOLD;
            float alpha = Math.max(0, Math.min(1, left / fading));
            int argb = (int) (alpha * 255) << 24 | domain.flashColor;
            Gui.drawRect(0, 0, res.getScaledWidth(), res.getScaledHeight(), argb);
            GL11.glColor4f(1, 1, 1, 1);
        }
    }

    /**
     * The caster makes the hand sign in the world too, where its model can: through the sign, and for a moment
     * after the domain has opened.
     */
    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (CutIn.drawingPlayer) {
            return;
        }
        ClientDomain domain = ClientDomain.INSTANCE;
        int id = event.entityPlayer.getEntityId();
        for (ClientDomain.Dome dome : domain.domes.values()) {
            if (dome.windup > 0 && dome.casterEntity == id) {
                float age = dome.windupTotal - dome.windup + event.partialRenderTick;
                FuturaBridge.INSTANCE.pose(event.entityPlayer, dome.type, CutIn.sign(age));
                return;
            }
        }
        ClientDomain.Blow blow = domain.blows.get(id);
        ClientDomain.Struck struck = domain.strucks.get(id);
        if (blow != null || struck != null) {
            // The blow it gives, or its guard; and the blow it takes on top of that.
            float partial = event.partialRenderTick;
            float[] pose = blow != null ? blow.pose(partial) : BlowPose.guard();
            float layer = blow != null ? blow.layer(partial) : 0;
            if (struck != null) {
                pose = BlowPose.struck(pose, struck.age + partial, struck.side);
                layer = Math.max(layer, struck.layer(partial));
            }
            FuturaBridge.INSTANCE.pose(event.entityPlayer, BlowPose.BONES, pose, layer);
            return;
        }
        if (domain.rivalSign != null && domain.rivalSign.casterEntity == id) {
            ClientDomain.Dome r = domain.rivalSign;
            FuturaBridge.INSTANCE.pose(
                event.entityPlayer,
                r.type,
                Draw.clamp01((r.windupTotal - r.windup + event.partialRenderTick) / 9F));
            return;
        }
        if (domain.inside != null && domain.casterEntity == id && domain.age < HOLD_SIGN_TICKS) {
            float left = HOLD_SIGN_TICKS - domain.age - event.partialRenderTick;
            FuturaBridge.INSTANCE.pose(event.entityPlayer, domain.inside, Draw.clamp01(left / 12F));
        }
    }

    /** No name tag over the figure in the cut-in. */
    @SubscribeEvent
    public void onNameTag(RenderLivingEvent.Specials.Pre event) {
        if (CutIn.drawingPlayer) {
            event.setCanceled(true);
        }
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
