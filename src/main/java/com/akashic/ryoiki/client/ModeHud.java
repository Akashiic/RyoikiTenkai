package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.mode.ModeWire;

/**
 * The foot of the screen in the sorcerer mode, in place of the game's hotbar, hearts, hunger and experience: the
 * player's life as one bar, and under it the four techniques, the last of them the domain expansion.
 *
 * <p>
 * It is drawn from one sheet of pieces ({@code textures/gui/modo.png}, made by {@code tools/hud/gerar.py}), two
 * texels to a pixel of the interface. The pieces are white or grey and take the colour of the player's technique
 * here; the places below are the sheet's, in texels, and must follow the script's.
 */
final class ModeHud {

    /** How far up from the bottom of the screen all this reaches, in pixels of the interface. */
    static final int HEIGHT = 47;

    private static final ResourceLocation SHEET = new ResourceLocation("ryoikitenkai", "textures/gui/modo.png");
    private static final float SHEET_W = 512;
    private static final float SHEET_H = 256;

    // The sheet, in texels: x, y, width, height.
    private static final int[] BAR_BASE = { 0, 0, 392, 40 };
    private static final int[] BAR_TRIM = { 0, 40, 392, 40 };
    private static final int[] BAR_FULL = { 0, 80, 392, 40 };
    private static final int[] BAR_GLASS = { 0, 120, 392, 40 };
    private static final int[] SLOT_BASE = { 0, 160, 40, 40 };
    private static final int[] SLOT_TRIM = { 40, 160, 40, 40 };
    private static final int[] LAST_BASE = { 80, 160, 52, 52 };
    private static final int[] LAST_TRIM = { 132, 160, 52, 52 };
    private static final int[] LAST_GLOW = { 184, 160, 52, 52 };
    private static final int[] SHIELD = { 236, 160, 20, 20 };
    private static final int ICONS_X = 0;
    private static final int ICONS_Y = 216;
    private static final int ICON = 32;
    /** Where life runs in the bar's piece, from empty to full, and the middle of the lozenge at its head. */
    private static final float LIFE_FROM = 42;
    private static final float LIFE_TO = 382;
    private static final float GEM_X = 22;
    private static final float GEM_Y = 20;

    /** Strips the life is drawn in: each takes its own colour, for the slope of colour and the gleam that crosses. */
    private static final int STRIPS = 34;
    /** Ticks the whole takes to come up, and the name of the mode to come and go over it. */
    private static final float OPEN = 8;
    private static final float NAME_STAYS = 70;
    private static final float NAME_FADE = 18;
    /** Ticks a place answers a press of its key. */
    private static final float PRESS = 6;

    private ModeHud() {}

    /** How much of itself the name of the mode shows, this long after the mode began: 0 to 1. */
    private static float name(float age) {
        return Math.min(1, age / OPEN) * Math.min(1, Math.max(0, (NAME_STAYS + NAME_FADE - age) / NAME_FADE));
    }

    /**
     * How far up from the bottom of the screen the chat's lowest line may come: above all this, and above the name
     * of the mode while that shows (it lets the chat down gently as it fades).
     */
    static float chatFloor(SorcererClient mode, float partialTicks) {
        float name = name(mode.sinceChange(partialTicks));
        return HEIGHT + 2 + 22 * name * name * (3 - 2 * name);
    }

    /** The colours of a technique: the trim, and the life from its head to its end. */
    private static int[] palette(DomainType technique) {
        if (technique == DomainType.INFINITE_VOID) {
            return new int[] { 0x9AD4FF, 0x3F6BFF, 0xB79BFF };
        }
        if (technique == DomainType.MALEVOLENT_SHRINE) {
            return new int[] { 0xFF6A48, 0xC8141F, 0xFF8A3C };
        }
        return new int[] { 0xCFDCFF, 0xAEB8CC, 0xE6ECF8 };
    }

    static void draw(Minecraft mc, SorcererClient mode, int w, int h, float partialTicks) {
        EntityPlayer player = mc.thePlayer;
        FontRenderer font = mc.fontRenderer;
        float age = mode.sinceChange(partialTicks);
        float t = Math.min(1, age / OPEN);
        // Comes up fast and settles: 1 - (1 - t)^3.
        float open = 1 - (1 - t) * (1 - t) * (1 - t);
        float clock = (mc.theWorld == null ? 0 : mc.theWorld.getTotalWorldTime() % 24000) + partialTicks;
        DomainType technique = mode.technique();
        int[] palette = palette(technique);
        int trim = palette[0];
        int from = palette[1];
        int to = palette[2];
        if (player.isPotionActive(Potion.wither)) {
            from = 0x2A2230;
            to = 0x6A587A;
        } else if (player.isPotionActive(Potion.poison)) {
            from = 0x3E8E2A;
            to = 0xA6D84A;
        }
        float life = mode.life(partialTicks);
        float lifeWas = Math.max(life, mode.lifeWas(partialTicks));
        // A blow whitens the trim for a moment; little life left makes it beat red.
        float struck = Math.max(0, 1 - mode.sinceHurt(partialTicks) / 5);
        boolean low = life <= 0.25F && life > 0;
        float beat = low ? 0.5F + 0.5F * MathHelper.sin(clock * 0.45F) : 0;
        int trimNow = Draw.mix(Draw.mix(trim, 0xFF3A30, beat * 0.8F), 0xFFFFFF, struck);

        GL11.glPushMatrix();
        GL11.glTranslatef(0, (1 - open) * 7, 0);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        mc.getTextureManager()
            .bindTexture(SHEET);
        // Two texels to a pixel of the interface: smooth at any scale of the interface.
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        float mid = w / 2F;
        float bx = mid - BAR_BASE[2] / 4F;
        float by = h - HEIGHT;
        piece(BAR_BASE, bx, by, 0xFFFFFF, open);
        // What the last blows took, still there for a moment, pale.
        if (lifeWas > life + 0.002F) {
            run(BAR_FULL, bx, by, life, lifeWas, 0xFFFFFF, 0xFFFFFF, 0.7F * open, clock, false);
        }
        run(BAR_FULL, bx, by, 0, life, from, to, open, clock, true);
        // Absorption: a line of gold over the life, as long as what it would take.
        float gold = Math.min(1, player.getAbsorptionAmount() / Math.max(1, player.getMaxHealth()));
        if (gold > 0) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            run(BAR_FULL, bx, by, 0, gold, 0xFFC83A, 0xFFE890, 0.55F * open, clock, false);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        if (life > 0.004F && life < 0.996F) {
            // The edge of the life: a thin light.
            float cut = LIFE_FROM + (LIFE_TO - LIFE_FROM) * life;
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            part(BAR_FULL, bx, by, cut - 3, cut + 1.5F, 0xFFFFFF, 0xFFFFFF, 0.85F * open, 0.85F * open);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        piece(BAR_GLASS, bx, by, 0xFFFFFF, open);
        piece(BAR_TRIM, bx, by, trimNow, open);

        // Armour, when there is any: a small shield past the end of the bar, and how much.
        int armor = player.getTotalArmorValue();
        float shieldX = bx + BAR_BASE[2] / 2F + 2;
        if (armor > 0) {
            piece(SHIELD, shieldX, by + 5, Draw.mix(trim, 0xFFFFFF, 0.5F), 0.9F * open);
        }

        // The four techniques, in a row under the bar: three, and the domain expansion, larger.
        float small = SLOT_BASE[2] / 2F;
        float large = LAST_BASE[2] / 2F;
        float row = 3 * small + 2 * 3 + 8 + large;
        float sx = mid - row / 2;
        float largeY = h - 2 - large;
        float smallY = largeY + (large - small) / 2;
        float[] slotX = new float[ModeWire.SLOTS];
        float[] slotY = new float[ModeWire.SLOTS];
        float[] slotSize = new float[ModeWire.SLOTS];
        for (int i = 0; i < ModeWire.SLOTS; i++) {
            boolean last = i == ModeWire.SLOTS - 1;
            slotX[i] = last ? sx + 3 * small + 2 * 3 + 8 : sx + i * (small + 3);
            slotY[i] = last ? largeY : smallY;
            slotSize[i] = last ? large : small;
            boolean has = mode.has(i);
            float recharge = mode.recharge(i, partialTicks);
            float press = Math.max(0, 1 - mode.sincePress(i, partialTicks) / PRESS);
            float cx = slotX[i] + slotSize[i] / 2;
            float cy = slotY[i] + slotSize[i] / 2;
            GL11.glPushMatrix();
            // A press of its key: the place swells a little and settles.
            float swell = 1 + 0.07F * press;
            GL11.glTranslatef(cx, cy, 0);
            GL11.glScalef(swell, swell, 1);
            GL11.glTranslatef(-cx, -cy, 0);
            piece(last ? LAST_BASE : SLOT_BASE, slotX[i], slotY[i], 0xFFFFFF, open);
            if (last && has && recharge <= 0) {
                // Ready: the domain's place breathes.
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                piece(LAST_GLOW, slotX[i], slotY[i], trim, (0.42F + 0.2F * MathHelper.sin(clock * 0.11F)) * open);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            piece(last ? LAST_TRIM : SLOT_TRIM, slotX[i], slotY[i], trim, (has ? 1 : 0.5F) * open);
            if (technique != null) {
                // Its sign: bright when the technique can be used, a shadow of itself when it cannot yet.
                float icon = ICON / 2F;
                int column = technique.ordinal() * ModeWire.SLOTS + i;
                sprite(
                    cx - icon / 2,
                    cy - icon / 2,
                    icon,
                    icon,
                    ICONS_X + column * ICON,
                    ICONS_Y,
                    ICON,
                    ICON,
                    Draw.mix(trim, 0xFFFFFF, last ? 0.85F : 0.6F),
                    (has ? recharge > 0 ? 0.45F : 1 : 0.5F) * open);
            }
            if (press > 0) {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                piece(last ? LAST_TRIM : SLOT_TRIM, slotX[i], slotY[i], 0xFFFFFF, 0.9F * press * open);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            GL11.glPopMatrix();
        }

        // Without the sheet: the shade of a recharge, falling away from the top of its place as it runs out.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tess = Tessellator.instance;
        for (int i = 0; i < ModeWire.SLOTS; i++) {
            float recharge = mode.recharge(i, partialTicks);
            if (recharge > 0) {
                float inset = 3.5F;
                float top = slotY[i] + inset;
                float bottom = slotY[i] + slotSize[i] - inset;
                tess.startDrawingQuads();
                tess.setColorRGBA_F(0.02F, 0.02F, 0.04F, 0.66F * open);
                tess.addVertex(slotX[i] + inset, bottom, 0);
                tess.addVertex(slotX[i] + slotSize[i] - inset, bottom, 0);
                tess.addVertex(slotX[i] + slotSize[i] - inset, bottom - (bottom - top) * recharge, 0);
                tess.addVertex(slotX[i] + inset, bottom - (bottom - top) * recharge, 0);
                tess.draw();
            }
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        int alpha = (int) (open * 255);
        if (alpha > 8) {
            // The life, in the lozenge at the head of the bar.
            String number = Integer.toString(MathHelper.ceiling_float_int(player.getHealth()));
            text(
                font,
                number,
                bx + GEM_X / 2,
                by + GEM_Y / 2,
                number.length() > 2 ? 0.5F : 0.75F,
                alpha << 24 | (low ? Draw.mix(0xFFFFFF, 0xFF5048, beat) : 0xFFFFFF));
            if (armor > 0) {
                text(font, Integer.toString(armor), shieldX + 16, by + 10.5F, 0.75F, alpha << 24 | 0xE8EEF8);
            }
            for (int i = 0; i < ModeWire.SLOTS; i++) {
                int seconds = mode.rechargeSeconds(i);
                if (seconds > 0) {
                    text(
                        font,
                        Integer.toString(seconds),
                        slotX[i] + slotSize[i] / 2,
                        slotY[i] + slotSize[i] / 2 + 0.5F,
                        i == ModeWire.SLOTS - 1 ? 1 : 0.75F,
                        alpha << 24 | 0xFFFFFF);
                }
                // Its key, small, in the corner.
                text(
                    font,
                    mode.slotKey(i),
                    slotX[i] + 5,
                    slotY[i] + 5.5F,
                    0.5F,
                    (int) (alpha * (mode.has(i) ? 0.95F : 0.55F)) << 24 | Draw.mix(trim, 0xFFFFFF, 0.7F));
            }
        }
        // The name of the mode and how to leave it, over the bar for a few seconds after coming in.
        float name = name(age);
        int nameAlpha = (int) (name * 255);
        if (nameAlpha > 8) {
            String title = I18n.format("ryoikitenkai.modo.nome");
            text(font, title, mid, by - 8, 1, nameAlpha << 24 | 0xFFFFFF);
            text(
                font,
                I18n.format("ryoikitenkai.modo.sair", mode.keyName()),
                mid,
                by - 18,
                0.75F,
                (int) (nameAlpha * 0.8F) << 24 | Draw.mix(trim, 0xFFFFFF, 0.5F));
        }
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** Text with its middle at a place, at a scale. */
    private static void text(FontRenderer font, String words, float x, float y, float scale, int argb) {
        if ((argb >>> 24) < 8) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1);
        font.drawStringWithShadow(words, -font.getStringWidth(words) / 2, -font.FONT_HEIGHT / 2, argb);
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** A whole piece of the sheet, at half its size in texels, in one colour. */
    private static void piece(int[] at, float x, float y, int rgb, float alpha) {
        sprite(x, y, at[2] / 2F, at[3] / 2F, at[0], at[1], at[2], at[3], rgb, alpha);
    }

    private static void sprite(float x, float y, float w, float h, float u, float v, float uw, float vh, int rgb,
        float alpha) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorRGBA_I(rgb, (int) (Math.max(0, Math.min(1, alpha)) * 255));
        tess.addVertexWithUV(x, y + h, 0, u / SHEET_W, (v + vh) / SHEET_H);
        tess.addVertexWithUV(x + w, y + h, 0, (u + uw) / SHEET_W, (v + vh) / SHEET_H);
        tess.addVertexWithUV(x + w, y, 0, (u + uw) / SHEET_W, v / SHEET_H);
        tess.addVertexWithUV(x, y, 0, u / SHEET_W, v / SHEET_H);
        tess.draw();
    }

    /**
     * The life of the bar between two fractions of it, in strips: the colour slopes from the head of the bar to its
     * end, and (for the life itself) a gleam crosses it now and then.
     */
    private static void run(int[] at, float x, float y, float fromLife, float toLife, int headRgb, int endRgb,
        float alpha, float clock, boolean gleams) {
        if (toLife <= fromLife) {
            return;
        }
        // The gleam: a soft band that takes two seconds to cross, every five.
        float band = clock % 100 / 40F * 1.3F - 0.15F;
        float last = fromLife;
        for (int i = 1; i <= STRIPS; i++) {
            float next = (float) i / STRIPS;
            if (next <= fromLife) {
                continue;
            }
            next = Math.min(next, toLife);
            part(
                at,
                x,
                y,
                LIFE_FROM + (LIFE_TO - LIFE_FROM) * last,
                LIFE_FROM + (LIFE_TO - LIFE_FROM) * next,
                lit(headRgb, endRgb, last, gleams ? band : -9),
                lit(headRgb, endRgb, next, gleams ? band : -9),
                alpha,
                alpha);
            last = next;
            if (next >= toLife) {
                break;
            }
        }
    }

    private static int lit(int headRgb, int endRgb, float along, float band) {
        float d = (along - band) / 0.07F;
        float gleam = 0.38F * (float) Math.exp(-d * d);
        return Draw.mix(Draw.mix(headRgb, endRgb, along), 0xFFFFFF, 0.08F + gleam);
    }

    /** A slice of a piece between two of its columns (texels), with a colour at each side. */
    private static void part(int[] at, float x, float y, float fromTexel, float toTexel, int leftRgb, int rightRgb,
        float leftAlpha, float rightAlpha) {
        float x0 = x + fromTexel / 2;
        float x1 = x + toTexel / 2;
        float h = at[3] / 2F;
        float u0 = (at[0] + fromTexel) / SHEET_W;
        float u1 = (at[0] + toTexel) / SHEET_W;
        float v0 = at[1] / SHEET_H;
        float v1 = (at[1] + at[3]) / SHEET_H;
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorRGBA_I(leftRgb, (int) (Math.max(0, Math.min(1, leftAlpha)) * 255));
        tess.addVertexWithUV(x0, y + h, 0, u0, v1);
        tess.setColorRGBA_I(rightRgb, (int) (Math.max(0, Math.min(1, rightAlpha)) * 255));
        tess.addVertexWithUV(x1, y + h, 0, u1, v1);
        tess.addVertexWithUV(x1, y, 0, u1, v0);
        tess.setColorRGBA_I(leftRgb, (int) (Math.max(0, Math.min(1, leftAlpha)) * 255));
        tess.addVertexWithUV(x0, y, 0, u0, v0);
        tess.draw();
    }
}
