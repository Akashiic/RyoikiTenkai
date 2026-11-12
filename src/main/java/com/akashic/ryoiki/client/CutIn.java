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
        boolean starVoid = "vazio".equals(domainId);
        float yc = h * 0.40F;
        // The star void's band is taller: its figure moves, and has the room to.
        float half = h * (starVoid ? 0.185F : 0.155F);
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
        if (starVoid) {
            // Inside the band, the void itself: arms of light turning about the caster, and stars streaming out.
            int factor = Math.max(1, Math.round((float) mc.displayHeight / h));
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(
                0,
                mc.displayHeight - (int) ((yc + half) * factor),
                mc.displayWidth,
                (int) (half * 2 * factor));
            vortex(t, w * shot(true, age)[5], yc, w, half, age);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        // Streaks rushing along it.
        Draw.additive();
        t.startDrawingQuads();
        for (int i = 0; i < (starVoid ? 8 : 22); i++) {
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
        float[] shot = shot(starVoid, age);
        // The star void's figure is there from the first frame, the band rushing in behind it; the crimson
        // shrine's comes in with its frame.
        float faceX = w * shot[5] + (starVoid ? 0 : slide);
        float size = half * 1.42F;
        if (!starVoid) {
            t.startDrawingQuads();
            diamond(t, faceX, yc, size * 0.98F, look.bandTrim(), 1);
            diamond(t, faceX, yc, size * 0.92F, Draw.mix(look.bandFrom(), 0x000000, 0.5F), 1);
            t.draw();
        }
        // A burst behind the figure at the instant the sign is complete.
        float sign = sign(age);
        float burst = Draw.clamp01((age - SIGN_START - SIGN_TICKS + 1) / 7F);
        float cut = cutFlash(starVoid, age);
        if (cut > 0) {
            // A cut: the band whites out for an instant.
            Draw.additive();
            t.startDrawingQuads();
            band(t, x0, x1, yc, half, skew, 0xFFFFFF, cut * 0.85F, look.bandTrim(), cut * 0.6F);
            t.draw();
            Draw.translucent();
        }
        if (burst > 0 && burst < 1 && !starVoid) {
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
                model(mc, (AbstractClientPlayer) caster, faceX, yc + half, half, h, shot);
            } else {
                figure(
                    mc,
                    (AbstractClientPlayer) caster,
                    domainId,
                    faceX,
                    yc + half * 0.95F,
                    half * 3.0F,
                    sign,
                    age,
                    shot);
            }
        }

        // The owner's own picture or animation, if the modpack has one for this domain, at the right end.
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        Draw.translucent();
        float room = w * 0.94F;
        if (CutInArt.has(domainId)) {
            room = CutInArt.draw(domainId, look.bandTrim(), w * 0.97F + slide, yc, half, skew, age) - slide - w * 0.02F;
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        // The words.
        FontRenderer font = mc.fontRenderer;
        String title = I18n.format("ryoikitenkai.dominio." + domainId);
        String over = I18n.format("ryoikitenkai.expansao");
        float textX = w * 0.36F + slide;
        float scale = Math
            .min(h * 0.115F / font.FONT_HEIGHT, (room - w * 0.36F) / Math.max(1, font.getStringWidth(title)));
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

    /** When the star void's cut-in cuts from one shot to the next, in ticks. */
    private static final float CUT_ONE = 6;
    private static final float CUT_TWO = 12;

    /**
     * The camera on the caster's figure, this many ticks into the hand sign: {yaw, pitch, roll, zoom, how much it
     * aims at the hand instead of the head (0 to 1), where across the screen the figure is (a fraction of its
     * width)}. The crimson shrine holds a three-quarter view and closes in slowly. The star void is three shots
     * with hard cuts between them: from behind and below, far, as the caster's arm starts up; then close on the
     * rising hand, from the other side, tilted; then the face, head on, as the sign is completed, with a jolt.
     */
    static float[] shot(boolean starVoid, float age) {
        if (!starVoid) {
            return new float[] { 22, -6, 0, 1 + 0.008F * age, 0, 0.19F };
        }
        if (age < CUT_ONE) {
            float a = age / CUT_ONE;
            return new float[] { 158 - 26 * a, -30 + 6 * a, -14 + 4 * a, 0.72F + 0.2F * a, 0, 0.15F + 0.03F * a };
        }
        if (age < CUT_TWO) {
            float a = (age - CUT_ONE) / (CUT_TWO - CUT_ONE);
            return new float[] { 64 - 18 * a, -6 - 8 * a, 15 - 6 * a, 2.1F + 0.5F * a, 1, 0.27F - 0.03F * a };
        }
        float a = Draw.easeOut((age - CUT_TWO) / 7F);
        float drift = age - CUT_TWO;
        // In close at the cut, easing back to hold the face and the hand together.
        float punch = 0.5F * (1 - a);
        return new float[] { 6 + 10 * a - drift * 0.35F, -18 + 10 * a, -8 + 11 * a, 1.75F + punch + drift * 0.012F,
            0.22F * (1 - a), 0.20F };
    }

    /** How bright the flash of a cut is, this many ticks into the hand sign (0 to 1). */
    private static float cutFlash(boolean starVoid, float age) {
        if (!starVoid) {
            return 0;
        }
        float since = age >= CUT_TWO ? age - CUT_TWO : age >= CUT_ONE ? age - CUT_ONE : 99;
        return Draw.clamp01(1 - since / 2.2F);
    }

    /** The star void behind its caster, inside the band: arms of light turning about a point, stars streaming out. */
    private static void vortex(Tessellator t, float cx, float cy, float w, float half, float age) {
        Draw.additive();
        float turn = age * 0.11F;
        t.startDrawingQuads();
        int arms = 5;
        int steps = 36;
        for (int k = 0; k < arms; k++) {
            int tint = k % 2 == 0 ? 0x9A6BFF : 0x58D0FF;
            for (int i = 0; i < steps; i++) {
                float r0 = w * 0.95F * i / steps;
                float r1 = w * 0.95F * (i + 1) / steps;
                double a0 = turn + k * Math.PI * 2 / arms + r0 * 0.011F;
                double a1 = turn + k * Math.PI * 2 / arms + r1 * 0.011F;
                float w0 = 0.22F + 0.16F * i / steps;
                float f0 = (1 - (float) i / steps) * 0.78F;
                float f1 = (1 - (float) (i + 1) / steps) * 0.78F;
                // Squashed to the band: wide, not tall.
                Draw.color(t, tint, f0);
                t.addVertex(cx + Math.cos(a0) * r0, cy + Math.sin(a0) * r0 * 0.5F, 0);
                Draw.color(t, tint, 0);
                t.addVertex(cx + Math.cos(a0 + w0) * r0, cy + Math.sin(a0 + w0) * r0 * 0.5F, 0);
                t.addVertex(cx + Math.cos(a1 + w0) * r1, cy + Math.sin(a1 + w0) * r1 * 0.5F, 0);
                Draw.color(t, tint, f1);
                t.addVertex(cx + Math.cos(a1) * r1, cy + Math.sin(a1) * r1 * 0.5F, 0);
            }
        }
        // Stars streaming out from the centre, each a short streak.
        for (int i = 0; i < 70; i++) {
            double a = Draw.hash(i, 41) * Math.PI * 2;
            float speed = 0.035F + Draw.hash(i, 42) * 0.07F;
            float phase = (Draw.hash(i, 43) + age * speed) % 1F;
            float r0 = w * 0.9F * phase * phase;
            float r1 = r0 + w * (0.01F + 0.09F * phase);
            float thick = 0.5F + phase * 1.3F;
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a) * 0.5F;
            Draw.color(t, 0xFFFFFF, 0);
            t.addVertex(cx + cos * r0 - sin * thick, cy + sin * r0 + cos * thick, 0);
            t.addVertex(cx + cos * r0 + sin * thick, cy + sin * r0 - cos * thick, 0);
            Draw.color(t, i % 3 == 0 ? 0xBFD4FF : 0xFFFFFF, 0.75F * (1 - phase * 0.4F));
            t.addVertex(cx + cos * r1 + sin * thick, cy + sin * r1 - cos * thick, 0);
            t.addVertex(cx + cos * r1 - sin * thick, cy + sin * r1 + cos * thick, 0);
        }
        // A core of light where the caster is.
        float glow = half * 2.4F;
        for (int j = 0; j < 24; j++) {
            double a0 = Math.PI * 2 * j / 24;
            double a1 = Math.PI * 2 * (j + 1) / 24;
            Draw.color(t, 0xFFFFFF, 0.7F);
            t.addVertex(cx, cy, 0);
            t.addVertex(cx, cy, 0);
            Draw.color(t, 0x4060FF, 0);
            t.addVertex(cx + Math.cos(a1) * glow, cy + Math.sin(a1) * glow, 0);
            t.addVertex(cx + Math.cos(a0) * glow, cy + Math.sin(a0) * glow, 0);
        }
        t.draw();
        Draw.translucent();
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
        float scale, float sign, float age, float[] shot) {
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
        GL11.glRotatef(shot[2], 0, 0, 1);
        GL11.glScalef(scale * shot[3], scale * shot[3], scale * shot[3]);
        float punch = 1 + 0.06F * Draw.clamp01(1 - Math.abs(age - SIGN_START - SIGN_TICKS) / 2.5F);
        GL11.glScalef(punch, punch, punch);
        GL11.glRotatef(shot[1], 1, 0, 0);
        GL11.glRotatef(180 - shot[0], 0, 1, 0);
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

    /** Where the head and the right hand of the model being drawn are, in blocks from the point it is drawn at. */
    private static float[] head = { 0, 0.1F, 0 };
    private static float[] hand = { 0, 0.1F, 0 };

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
        int screenHeight, float[] shot) {
        float scale = half * 2.7F * shot[3];
        // The neck goes a little above the middle of the band, wherever this model has it: the head rises over the
        // band and the hands, before the chest, stay inside it. The camera turns about that point.
        float neckY = bandBottom - half * 1.15F;
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
        GL11.glTranslatef(x, neckY, 200);
        GL11.glRotatef(shot[2], 0, 0, 1);
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
        caster.renderYawOffset = 0;
        caster.prevRenderYawOffset = 0;
        caster.rotationYaw = -8;
        caster.prevRotationYaw = -8;
        caster.rotationPitch = 0;
        caster.prevRotationPitch = 0;
        caster.rotationYawHead = -8;
        caster.prevRotationYawHead = -8;
        float viewY = RenderManager.instance.playerViewY;
        RenderManager.instance.playerViewY = 180;
        drawingPlayer = true;
        FuturaBridge.INSTANCE.screenDraw(true);
        try {
            // Once without drawing anything, to learn where this frame's pose puts the head and the hand (the hand
            // moves fast: last frame's place would not do); then for real, with the camera aimed by them.
            GL11.glPushMatrix();
            camera(shot);
            GL11.glColorMask(false, false, false, false);
            GL11.glDepthMask(false);
            RenderManager.instance.renderEntityWithPosYaw(caster, 0, 0, 0, 0, 1);
            GL11.glColorMask(true, true, true, true);
            GL11.glPopMatrix();
            float[] seenHead = FuturaBridge.INSTANCE.point(caster, "Head");
            if (seenHead != null) {
                head = seenHead;
            }
            float[] seenHand = FuturaBridge.INSTANCE.point(caster, "RightHandLocator");
            hand = seenHand != null ? seenHand : head;
            float aim = shot[4];
            // A draw leaves the GL state as it likes, and the unseen one may have written depth: start clean for
            // the one that shows.
            GL11.glDepthMask(true);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1, 1, 1, 1);
            camera(shot);
            GL11.glTranslatef(
                -(head[0] + (hand[0] - head[0]) * aim),
                -(head[1] + (hand[1] - head[1]) * aim),
                -(head[2] + (hand[2] - head[2]) * aim));
            RenderManager.instance.renderEntityWithPosYaw(caster, 0, 0, 0, 0, 1);
        } finally {
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

    /**
     * The light and the camera's turn, in the figure's space. The camera goes round the figure, not the figure
     * round itself: the model eases its body toward where the player faces, and would trail behind a cut.
     */
    private static void camera(float[] shot) {
        GL11.glRotatef(135, 0, 1, 0);
        RenderHelper.enableStandardItemLighting();
        GL11.glRotatef(-135, 0, 1, 0);
        GL11.glRotatef(shot[1], 1, 0, 0);
        GL11.glRotatef(-shot[0], 0, 1, 0);
    }

    private static void reset(ModelRenderer part) {
        part.rotateAngleX = 0;
        part.rotateAngleY = 0;
        part.rotateAngleZ = 0;
    }
}
