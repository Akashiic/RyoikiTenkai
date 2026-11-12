package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;

/**
 * The cut-in: while the caster makes the hand sign, a slanted band crosses the screen of everyone near, with the
 * caster's face (from its skin), its name and the domain's. It rushes in, holds, and leaves as the domain opens.
 */
final class CutIn {

    private static final float IN_TICKS = 5;
    private static final float OUT_TICKS = 4;

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

        // The face, in a tilted frame.
        float faceX = w * 0.20F + slide;
        float size = half * 1.42F;
        t.startDrawingQuads();
        diamond(t, faceX, yc, size * 0.94F, look.bandTrim(), 1);
        diamond(t, faceX, yc, size * 0.88F, 0x000000, 1);
        t.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
        Entity caster = mc.theWorld.getEntityByID(casterEntity);
        String name = caster == null ? "" : caster.getCommandSenderName();
        if (caster instanceof AbstractClientPlayer) {
            mc.getTextureManager()
                .bindTexture(((AbstractClientPlayer) caster).getLocationSkin());
            // Old skins are 64 by 32, new ones 64 by 64: the face is 8 by 8 pixels at (8, 8) in both.
            int texHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            int texWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            float unit = texWidth <= 0 ? 1 / 64F : 1F / 64F;
            float vUnit = texHeight > 0 && texWidth > 0 && texHeight == texWidth ? 1 / 64F : 1 / 32F;
            float f = size * 0.56F;
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            face(t, faceX, yc, f, 8 * unit, 8 * vUnit, 16 * unit, 16 * vUnit);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            face(t, faceX, yc, f * 1.1F, 40 * unit, 8 * vUnit, 48 * unit, 16 * vUnit);
        }

        // The words.
        FontRenderer font = mc.fontRenderer;
        String title = I18n.format("ryoikitenkai.dominio." + domainId);
        String over = I18n.format("ryoikitenkai.expansao");
        float textX = w * 0.33F + slide;
        float scale = Math.min(h * 0.115F / font.FONT_HEIGHT, (w * 0.62F) / Math.max(1, font.getStringWidth(title)));
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

    private static void face(Tessellator t, float x, float y, float half, float u0, float v0, float u1, float v1) {
        t.startDrawingQuads();
        t.setColorRGBA_F(1, 1, 1, 1);
        t.addVertexWithUV(x - half, y - half, 0, u0, v0);
        t.addVertexWithUV(x - half, y + half, 0, u0, v1);
        t.addVertexWithUV(x + half, y + half, 0, u1, v1);
        t.addVertexWithUV(x + half, y - half, 0, u1, v0);
        t.draw();
    }
}
