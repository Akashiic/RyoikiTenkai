package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.domain.ClashScore;

/**
 * A clash on the screen: the seconds left, large, at the top centre, and at its sides the bar of each caster,
 * filling toward the middle.
 */
final class ClashHud {

    private static final int NUMBER_SCALE = 4;
    private static final int BAR_HEIGHT = 7;
    private static final int GAP = 34;

    private ClashHud() {}

    static void draw(Minecraft mc, ClientDomain domain, int w, int h) {
        FontRenderer font = mc.fontRenderer;
        int mid = w / 2;
        // At the top, where nothing of the game is: the hearts and the hotbar keep the bottom.
        int y = 34;
        int seconds = (domain.clashTime + 19) / 20;
        String number = Integer.toString(seconds);
        // The last five seconds beat.
        float beat = seconds <= 5 ? 1 + 0.18F * (domain.clashTime % 20) / 20F : 1;
        GL11.glPushMatrix();
        GL11.glTranslatef(mid, y + 4, 0);
        GL11.glScalef(NUMBER_SCALE * beat, NUMBER_SCALE * beat, 1);
        int nx = -font.getStringWidth(number) / 2;
        int ny = -font.FONT_HEIGHT / 2;
        outlined(font, number, nx, ny, seconds <= 5 ? 0xFF5050 : 0xFFFFFF);
        GL11.glPopMatrix();

        String title = I18n.format("ryoikitenkai.choque");
        outlined(font, title, mid - font.getStringWidth(title) / 2, y - 26, 0xFFFFFF);

        int width = Math.max(40, Math.min(150, mid - GAP - 12));
        for (int side = 0; side < 2; side++) {
            if (domain.clashTypes[side] == null) {
                continue;
            }
            int color = DomainRenderer.look(domain.clashTypes[side])
                .bandTrim();
            float bar = ClashScore.bar(domain.clashHits[side]);
            int inner = side == 0 ? mid - GAP : mid + GAP;
            int outer = side == 0 ? inner - width : inner + width;
            int left = Math.min(inner, outer);
            int right = Math.max(inner, outer);
            Gui.drawRect(left - 1, y - 1, right + 1, y + BAR_HEIGHT + 1, 0xFF000000);
            Gui.drawRect(left, y, right, y + BAR_HEIGHT, 0xFF202020);
            int fill = Math.round(width * bar);
            // Each bar grows from its outer end toward the number: the two race to the middle.
            if (side == 0) {
                Gui.drawRect(left, y, left + fill, y + BAR_HEIGHT, 0xFF000000 | color);
            } else {
                Gui.drawRect(right - fill, y, right, y + BAR_HEIGHT, 0xFF000000 | color);
            }
            Entity caster = mc.theWorld.getEntityByID(domain.clashEntities[side]);
            String name = caster == null ? "" : caster.getCommandSenderName();
            String percent = (int) (bar * 100) + "%";
            if (side == 0) {
                outlined(font, name, left, y - 11, 0xFFFFFF);
                outlined(font, percent, right - font.getStringWidth(percent), y + BAR_HEIGHT + 3, color);
            } else {
                outlined(font, name, right - font.getStringWidth(name), y - 11, 0xFFFFFF);
                outlined(font, percent, left, y + BAR_HEIGHT + 3, color);
            }
        }
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void outlined(FontRenderer font, String text, int x, int y, int color) {
        font.drawString(text, x + 1, y, 0x000000);
        font.drawString(text, x - 1, y, 0x000000);
        font.drawString(text, x, y + 1, 0x000000);
        font.drawString(text, x, y - 1, 0x000000);
        font.drawString(text, x, y, color);
    }
}
