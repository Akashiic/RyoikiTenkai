package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;

import org.lwjgl.opengl.GL11;

/**
 * What stands where the hotbar was while the player is in the sorcerer mode: for now only its name, on a line that
 * opens from the middle. The four techniques of the mode go here (piece M2 of the plan).
 */
final class ModeHud {

    /** Ticks the line takes to open, and the hint under the name to come and go. */
    private static final float OPEN = 9;
    private static final float HINT_FROM = 6;
    private static final float HINT_STAYS = 90;
    private static final float HINT_FADE = 20;
    private static final int LINE = 0xCFDCFF;

    private ModeHud() {}

    static void draw(Minecraft mc, SorcererClient mode, int w, int h, float partialTicks) {
        FontRenderer font = mc.fontRenderer;
        float age = mode.sinceChange(partialTicks);
        // Opens fast and settles: 1 - (1 - t)^3.
        float t = Math.min(1, age / OPEN);
        float open = 1 - (1 - t) * (1 - t) * (1 - t);
        int mid = w / 2;
        // The hotbar is 22 high at the bottom of the screen; the name sits in the middle of where it was.
        int y = h - 14;
        String name = I18n.format("ryoikitenkai.modo.nome");
        int half = font.getStringWidth(name) / 2;
        float reach = (half + 10 + 62) * open;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (int side = -1; side <= 1; side += 2) {
            float from = Math.min(reach, half + 10);
            // Each arm of the line: whole beside the name, to nothing at its end; and a dark line under it, so it
            // reads over snow and over a night sky alike.
            arm(tess, mid + side * from, mid + side * reach, y + 4.5F, 1, 0x000000, 0.55F * open);
            arm(tess, mid + side * from, mid + side * reach, y + 3.5F, 1, LINE, 0.9F * open);
        }
        tess.draw();
        if (open > 0.5F) {
            // A small lozenge where each arm begins.
            float a = (open - 0.5F) * 2;
            tess.startDrawingQuads();
            for (int side = -1; side <= 1; side += 2) {
                float x = mid + side * (half + 6);
                tess.setColorRGBA_I(LINE, (int) (a * 255));
                tess.addVertex(x - 2.5F, y + 4, 0);
                tess.addVertex(x, y + 6.5F, 0);
                tess.addVertex(x + 2.5F, y + 4, 0);
                tess.addVertex(x, y + 1.5F, 0);
            }
            tess.draw();
        }
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        int alpha = (int) (Math.min(1, age / OPEN) * 255);
        if (alpha > 8) {
            font.drawStringWithShadow(name, mid - half, y, alpha << 24 | 0xFFFFFF);
        }
        // How to leave, for a few seconds after coming in.
        float hint = Math.min(1, (age - HINT_FROM) / 8)
            * Math.min(1, Math.max(0, (HINT_FROM + HINT_STAYS + HINT_FADE - age) / HINT_FADE));
        int hintAlpha = (int) (hint * 200);
        if (hintAlpha > 8) {
            String words = I18n.format("ryoikitenkai.modo.sair", mode.keyName());
            GL11.glPushMatrix();
            GL11.glTranslatef(mid, y - 9, 0);
            GL11.glScalef(0.75F, 0.75F, 1);
            font.drawStringWithShadow(words, -font.getStringWidth(words) / 2, 0, hintAlpha << 24 | 0xCFDCFF);
            GL11.glPopMatrix();
        }
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void arm(Tessellator tess, float from, float to, float y, float height, int rgb, float alpha) {
        int whole = (int) (alpha * 255);
        if (from > to) {
            tess.setColorRGBA_I(rgb, whole);
            tess.addVertex(from, y + height, 0);
            tess.addVertex(from, y, 0);
            tess.setColorRGBA_I(rgb, 0);
            tess.addVertex(to, y, 0);
            tess.addVertex(to, y + height, 0);
        } else {
            // The same quad the other way about: the screen draws only one face.
            tess.setColorRGBA_I(rgb, 0);
            tess.addVertex(to, y + height, 0);
            tess.addVertex(to, y, 0);
            tess.setColorRGBA_I(rgb, whole);
            tess.addVertex(from, y, 0);
            tess.addVertex(from, y + height, 0);
        }
    }
}
