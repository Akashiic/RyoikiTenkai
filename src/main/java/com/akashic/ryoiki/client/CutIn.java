package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.akashic.ryoiki.DomainType;

/**
 * The cut-in: while the caster makes the hand sign, a slanted band crosses the screen of everyone near, with the
 * caster's figure (its own skin) raising its arms into the domain's sign, its name and the domain's. It rushes in,
 * holds, and leaves as the domain opens.
 */
final class CutIn {

    private static final float IN_TICKS = 5;
    private static final float OUT_TICKS = 4;
    /** When the figure starts raising its arms, and how long the sign takes. */
    private static final float SIGN_START = 3;
    private static final float SIGN_TICKS = 9;

    /** One figure for each skin layout: 64 by 32 (old) and 64 by 64. */
    private static ModelBiped figureOld;
    private static ModelBiped figureNew;

    private CutIn() {}

    /**
     * @param age   ticks since the hand sign began, with the fraction
     * @param total ticks the hand sign lasts
     */
    static void draw(Minecraft mc, DomainLook look, String domainId, int casterEntity, float age, float total, int w,
        int h) {
        float in = Draw.easeOut(age / IN_TICKS);
        float out = Draw.clamp01((age - (total - OUT_TICKS)) / OUT_TICKS);
        // From off the left edge to its place, then on to the right.
        float slide = (in - 1) * w * 1.25F + out * out * w * 1.25F;
        float yc = h * 0.40F;
        float half = h * 0.155F;
        float skew = half * 0.75F;
        float x0 = -w * 0.2F + slide;
        float x1 = w * 1.2F + slide;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        Draw.translucent();
        Tessellator t = Tessellator.instance;
        // The screen dims behind the band.
        t.startDrawingQuads();
        Draw.color(t, 0x000000, 0.38F * in * (1 - out));
        t.addVertex(0, 0, 0);
        t.addVertex(0, h, 0);
        t.addVertex(w, h, 0);
        t.addVertex(w, 0, 0);
        // Its shadow, then the band, darker at one end.
        band(t, x0, x1, yc + half * 0.12F, half * 1.12F, skew, 0x000000, 0.55F, 0x000000, 0.55F);
        band(t, x0, x1, yc, half, skew, look.bandFrom(), 0.96F, look.bandTo(), 0.96F);
        t.draw();
        // Streaks rushing along it.
        Draw.additive();
        t.startDrawingQuads();
        for (int i = 0; i < 22; i++) {
            float lane = Draw.hash(i, 31) * 2 - 1;
            float y = yc + lane * half * 0.86F;
            float length = w * (0.12F + Draw.hash(i, 32) * 0.3F);
            float speed = w * (0.09F + Draw.hash(i, 33) * 0.12F);
            float span = w * 1.6F + length;
            float x = w * 1.3F - (Draw.hash(i, 34) * span + age * speed) % span + slide * 0.3F;
            // Keep to the band's slant.
            float lean = -lane * skew;
            float thick = 0.6F + Draw.hash(i, 35) * 1.6F;
            Draw.color(t, look.bandTrim(), 0);
            t.addVertex(x + lean, y - thick, 0);
            t.addVertex(x + lean, y + thick, 0);
            Draw.color(t, look.bandTrim(), 0.30F);
            t.addVertex(x + length + lean, y + thick, 0);
            t.addVertex(x + length + lean, y - thick, 0);
        }
        t.draw();
        Draw.translucent();
        // The trim along both edges.
        t.startDrawingQuads();
        float trim = Math.max(1.5F, h * 0.008F);
        edge(t, x0, x1, yc - half, skew, trim, look.bandTrim());
        edge(t, x0, x1, yc + half - trim, -skew, trim, look.bandTrim());
        edge(t, x0, x1, yc - half + trim * 2.4F, skew * 0.9F, trim * 0.4F, look.bandTrim());
        edge(t, x0, x1, yc + half - trim * 3.4F, -skew * 0.9F, trim * 0.4F, look.bandTrim());
        t.draw();

        // The caster, in a tilted frame: its own skin on a figure that makes the hand sign.
        float faceX = w * 0.19F + slide;
        float size = half * 1.42F;
        t.startDrawingQuads();
        diamond(t, faceX, yc, size * 0.98F, look.bandTrim(), 1);
        diamond(t, faceX, yc, size * 0.92F, Draw.mix(look.bandFrom(), 0x000000, 0.5F), 1);
        t.draw();
        // A burst behind the figure at the instant the sign is complete.
        float sign = sign(age);
        float burst = Draw.clamp01((age - SIGN_START - SIGN_TICKS + 1) / 7F);
        if (burst > 0 && burst < 1) {
            Draw.additive();
            t.startDrawingQuads();
            float r = size * (0.5F + burst * 2.2F);
            diamond(t, faceX, yc, r, look.bandTrim(), (1 - burst) * 0.75F);
            diamond(t, faceX, yc, r * 0.7F, 0xFFFFFF, (1 - burst) * 0.5F);
            t.draw();
            Draw.translucent();
        }
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
        Entity caster = mc.theWorld.getEntityByID(casterEntity);
        String name = caster == null ? "" : caster.getCommandSenderName();
        if (caster instanceof AbstractClientPlayer) {
            if (FuturaBridge.INSTANCE.modelled((AbstractClientPlayer) caster)) {
                // The caster's own model makes the sign (the bones are posed just before it is drawn).
                FuturaBridge.INSTANCE.pose((AbstractClientPlayer) caster, DomainType.byId(domainId), sign);
                model(mc, (AbstractClientPlayer) caster, faceX, yc + half, half, h);
            } else {
                figure(mc, (AbstractClientPlayer) caster, domainId, faceX, yc + half * 0.95F, half * 3.0F, sign, age);
            }
        }

        // The words.
        FontRenderer font = mc.fontRenderer;
        String title = I18n.format("ryoikitenkai.dominio." + domainId);
        String over = I18n.format("ryoikitenkai.expansao");
        float textX = w * 0.36F + slide;
        float scale = Math.min(h * 0.115F / font.FONT_HEIGHT, (w * 0.58F) / Math.max(1, font.getStringWidth(title)));
        int trimColor = 0xFF000000 | look.bandTrim();
        GL11.glPushMatrix();
        GL11.glTranslatef(textX, yc - scale * font.FONT_HEIGHT * 0.38F, 0);
        GL11.glScalef(scale, scale, 1);
        font.drawStringWithShadow(title, 0, 0, 0xFFFFFFFF);
        GL11.glPopMatrix();
        float small = scale * 0.36F;
        GL11.glPushMatrix();
        GL11.glTranslatef(
            textX + skew * 0.5F,
            yc - scale * font.FONT_HEIGHT * 0.38F - small * font.FONT_HEIGHT * 1.35F,
            0);
        GL11.glScalef(small, small, 1);
        font.drawStringWithShadow(over, 0, 0, trimColor);
        GL11.glPopMatrix();
        if (!name.isEmpty()) {
            GL11.glPushMatrix();
            GL11.glTranslatef(textX - skew * 0.55F, yc + scale * font.FONT_HEIGHT * 0.72F, 0);
            GL11.glScalef(small, small, 1);
            font.drawStringWithShadow(name, 0, 0, trimColor);
            GL11.glPopMatrix();
        }
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** How far the hand sign has come, 0 to 1, this many ticks into it. */
    static float sign(float age) {
        return Draw.easeOut((age - SIGN_START) / SIGN_TICKS);
    }

    /** A slanted band: its top edge leans right, its bottom edge left. */
    private static void band(Tessellator t, float x0, float x1, float yc, float half, float skew, int fromRgb,
        float fromAlpha, int toRgb, float toAlpha) {
        Draw.color(t, fromRgb, fromAlpha);
        t.addVertex(x0 + skew, yc - half, 0);
        t.addVertex(x0 - skew, yc + half, 0);
        Draw.color(t, toRgb, toAlpha);
        t.addVertex(x1 - skew, yc + half, 0);
        t.addVertex(x1 + skew, yc - half, 0);
    }

    private static void edge(Tessellator t, float x0, float x1, float y, float skew, float thick, int rgb) {
        Draw.color(t, rgb, 0.95F);
        t.addVertex(x0 + skew, y, 0);
        t.addVertex(x0 + skew, y + thick, 0);
        t.addVertex(x1 + skew, y + thick, 0);
        t.addVertex(x1 + skew, y, 0);
    }

    private static void diamond(Tessellator t, float x, float y, float r, int rgb, float alpha) {
        Draw.color(t, rgb, alpha);
        t.addVertex(x, y - r, 0);
        t.addVertex(x - r, y, 0);
        t.addVertex(x, y + r, 0);
        t.addVertex(x + r, y, 0);
    }

    /**
     * The caster's figure from the waist up, turned a little to one side, its arms going from rest to the domain's
     * hand sign as {@code sign} goes from 0 to 1.
     *
     * @param feetY where the figure's waist line sits on the screen
     * @param scale screen units per block of figure
     */
    private static void figure(Minecraft mc, AbstractClientPlayer caster, String domainId, float x, float feetY,
        float scale, float sign, float age) {
        mc.getTextureManager()
            .bindTexture(caster.getLocationSkin());
        int texHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        int texWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        boolean square = texHeight > 0 && texHeight == texWidth;
        ModelBiped model;
        if (square) {
            if (figureNew == null) {
                figureNew = new ModelBiped(0, 0, 64, 64);
            }
            model = figureNew;
        } else {
            if (figureOld == null) {
                figureOld = new ModelBiped(0, 0, 64, 32);
            }
            model = figureOld;
        }
        // The pose. Angles in radians; a negative X raises an arm forward.
        boolean shrine = "santuario".equals(domainId);
        float tremble = sign >= 1 ? (float) Math.sin(age * 2.3F) * 0.012F : 0;
        reset(model.bipedHead);
        reset(model.bipedHeadwear);
        reset(model.bipedBody);
        reset(model.bipedRightArm);
        reset(model.bipedLeftArm);
        if (shrine) {
            // Both hands brought together in front of the chest, head bowed.
            model.bipedRightArm.rotateAngleX = -1.32F * sign + tremble;
            model.bipedRightArm.rotateAngleY = -0.62F * sign;
            model.bipedLeftArm.rotateAngleX = -1.32F * sign - tremble;
            model.bipedLeftArm.rotateAngleY = 0.62F * sign;
            model.bipedHead.rotateAngleX = 0.22F * sign;
        } else {
            // One hand raised before the face, the other at rest, chin up.
            model.bipedRightArm.rotateAngleX = -2.45F * sign + tremble;
            model.bipedRightArm.rotateAngleY = -0.1F * sign;
            model.bipedRightArm.rotateAngleZ = 0.1F * sign;
            model.bipedLeftArm.rotateAngleX = 0.12F * sign;
            model.bipedLeftArm.rotateAngleZ = -0.08F;
            model.bipedHead.rotateAngleX = -0.12F * sign;
        }
        model.bipedHead.rotateAngleY = 0.18F;
        model.bipedHeadwear.rotateAngleX = model.bipedHead.rotateAngleX;
        model.bipedHeadwear.rotateAngleY = model.bipedHead.rotateAngleY;

        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glPushMatrix();
        // The model's own space has Y down, as the screen has: the waist (12 pixels below the neck) goes on feetY.
        GL11.glTranslatef(x, feetY - scale * 0.75F, 200);
        GL11.glScalef(scale, scale, scale);
        float punch = 1 + 0.06F * Draw.clamp01(1 - Math.abs(age - SIGN_START - SIGN_TICKS) / 2.5F);
        GL11.glScalef(punch, punch, punch);
        GL11.glRotatef(-8, 1, 0, 0);
        GL11.glRotatef(180 - 24, 0, 1, 0);
        RenderHelper.enableStandardItemLighting();
        GL11.glColor4f(1, 1, 1, 1);
        float unit = 0.0625F;
        model.bipedBody.render(unit);
        model.bipedRightArm.render(unit);
        model.bipedLeftArm.render(unit);
        model.bipedHead.render(unit);
        model.bipedHeadwear.render(unit);
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** The head's height above the feet in the last model drawn, in blocks: the next frame is framed by it. */
    private static float headHeight = 0.1F;

    /** Whether the cut-in is drawing a player right now: its name tag is not wanted on the band. */
    static boolean drawingPlayer;

    /**
     * The caster as the game draws it, model and all, from the waist up: turned a little to one side, cut off at the
     * band's lower edge.
     *
     * @param bandBottom the band's lower edge
     * @param half       half the band's height
     */
    private static void model(Minecraft mc, AbstractClientPlayer caster, float x, float bandBottom, float half,
        int screenHeight) {
        float scale = half * 2.7F;
        // The neck goes a little above the middle of the band, wherever this model has it: the head rises over the
        // band and the hands, before the chest, stay inside it.
        float feetY = bandBottom - half * 1.15F + scale * headHeight;
        // Nothing below the band: the legs are cut off there.
        int factor = Math.max(1, Math.round((float) mc.displayHeight / screenHeight));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, mc.displayHeight - (int) (bandBottom * factor), mc.displayWidth, mc.displayHeight);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, feetY, 200);
        GL11.glScalef(-scale, scale, scale);
        GL11.glRotatef(180, 0, 0, 1);
        float bodyYaw = caster.renderYawOffset;
        float prevBodyYaw = caster.prevRenderYawOffset;
        float yaw = caster.rotationYaw;
        float prevYaw = caster.prevRotationYaw;
        float pitch = caster.rotationPitch;
        float prevPitch = caster.prevRotationPitch;
        float headYaw = caster.rotationYawHead;
        float prevHeadYaw = caster.prevRotationYawHead;
        GL11.glRotatef(135, 0, 1, 0);
        RenderHelper.enableStandardItemLighting();
        GL11.glRotatef(-135, 0, 1, 0);
        GL11.glRotatef(-6, 1, 0, 0);
        caster.renderYawOffset = 22;
        caster.prevRenderYawOffset = 22;
        caster.rotationYaw = 14;
        caster.prevRotationYaw = 14;
        caster.rotationPitch = 0;
        caster.prevRotationPitch = 0;
        caster.rotationYawHead = 14;
        caster.prevRotationYawHead = 14;
        float viewY = RenderManager.instance.playerViewY;
        RenderManager.instance.playerViewY = 180;
        drawingPlayer = true;
        FuturaBridge.INSTANCE.screenDraw(true);
        try {
            RenderManager.instance.renderEntityWithPosYaw(caster, 0, 0, 0, 0, 1);
        } finally {
            float seen = FuturaBridge.INSTANCE.headHeight(caster);
            if (!Float.isNaN(seen) && seen > -4 && seen < 4) {
                if (Boolean.getBoolean("ryoiki.devHarness") && Math.abs(seen - headHeight) > 0.01F) {
                    com.akashic.ryoiki.RyoikiTenkai.LOG.info("cut-in: head at {} blocks above the draw point", seen);
                }
                headHeight = seen;
            }
            FuturaBridge.INSTANCE.screenDraw(false);
            drawingPlayer = false;
            RenderManager.instance.playerViewY = viewY;
            caster.renderYawOffset = bodyYaw;
            caster.prevRenderYawOffset = prevBodyYaw;
            caster.rotationYaw = yaw;
            caster.prevRotationYaw = prevYaw;
            caster.rotationPitch = pitch;
            caster.prevRotationPitch = prevPitch;
            caster.rotationYawHead = headYaw;
            caster.prevRotationYawHead = prevHeadYaw;
        }
        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void reset(ModelRenderer part) {
        part.rotateAngleX = 0;
        part.rotateAngleY = 0;
        part.rotateAngleZ = 0;
    }
}
