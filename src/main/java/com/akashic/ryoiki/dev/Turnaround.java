package com.akashic.ryoiki.dev;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.akashic.ryoiki.client.FuturaBridge;

/**
 * Development: this player's model on a plain ground, seen from the front, from three quarters, from the right side
 * and from above at once, in a pose. To work on a pose by what it measures, not by what one angle happens to show.
 * The pose is {@code ryoiki-bones.txt} in the run directory when it is there (lines of {@code <bone> <x> <y> <z>}),
 * else the one it is given.
 */
final class Turnaround {

    /** Where each view looks from: yaw about the vertical, then pitch. */
    private static final float[][] VIEWS = { { 0, 8 }, { 45, 8 }, { 90, 8 }, { 0, 88 } };

    private static final float DROP = Float.parseFloat(System.getProperty("ryoiki.turnDrop", "1.4"));

    private Turnaround() {}

    static void draw(Minecraft mc, int w, int h, String[] bones, float[] degrees) {
        File file = new File(mc.mcDataDir, "ryoiki-bones.txt");
        if (file.isFile()) {
            try {
                List<String> names = new ArrayList<>();
                List<Float> angles = new ArrayList<>();
                for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                    String[] words = line.trim()
                        .split("\\s+");
                    if (words.length == 4) {
                        names.add(words[0]);
                        for (int k = 1; k < 4; k++) {
                            angles.add(Float.parseFloat(words[k]));
                        }
                    }
                }
                bones = names.toArray(new String[0]);
                degrees = new float[angles.size()];
                for (int i = 0; i < degrees.length; i++) {
                    degrees[i] = angles.get(i);
                }
            } catch (IOException | RuntimeException e) {
                // Half written: the next frame reads it whole.
            }
        }
        Gui.drawRect(0, 0, w, h, 0xFF3A3F47);
        int cell = w / VIEWS.length;
        for (int i = 0; i < VIEWS.length; i++) {
            Gui.drawRect(i * cell, 0, i * cell + 1, h, 0xFF22252B);
            // The line the feet stand on, and the middle of the body.
            Gui.drawRect(i * cell + 6, (int) (h * 0.86F), (i + 1) * cell - 6, (int) (h * 0.86F) + 1, 0xFF6B7380);
            Gui.drawRect(i * cell + cell / 2, 8, i * cell + cell / 2 + 1, h - 8, 0x406B7380);
        }
        GL11.glColor4f(1, 1, 1, 1);
        AbstractClientPlayer player = mc.thePlayer;
        float scale = h * 0.30F;
        for (int i = 0; i < VIEWS.length; i++) {
            if (bones != null && bones.length > 0) {
                FuturaBridge.INSTANCE.pose(player, bones, degrees, 1);
            }
            model(player, i * cell + cell / 2F, h * 0.86F, scale, VIEWS[i][0], VIEWS[i][1]);
        }
    }

    private static void model(AbstractClientPlayer player, float x, float feetY, float scale, float yaw, float pitch) {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPushMatrix();
        // The camera turns about the middle of the body, which stays where the standing model has it.
        GL11.glTranslatef(x, feetY - scale * 0.9F, 300);
        GL11.glScalef(-scale, scale, scale);
        GL11.glRotatef(180, 0, 0, 1);
        GL11.glRotatef(135, 0, 1, 0);
        RenderHelper.enableStandardItemLighting();
        GL11.glRotatef(-135, 0, 1, 0);
        GL11.glRotatef(pitch, 1, 0, 0);
        GL11.glRotatef(-yaw, 0, 1, 0);
        // Drawn for the screen, the model stands lower than its entity: this puts its feet on the line.
        GL11.glTranslatef(0, DROP - 0.9F, 0);
        float[] saved = { player.renderYawOffset, player.prevRenderYawOffset, player.rotationYaw,
            player.prevRotationYaw, player.rotationPitch, player.prevRotationPitch, player.rotationYawHead,
            player.prevRotationYawHead, player.limbSwing, player.limbSwingAmount, player.prevLimbSwingAmount };
        player.renderYawOffset = 0;
        player.prevRenderYawOffset = 0;
        player.rotationYaw = 0;
        player.prevRotationYaw = 0;
        player.rotationPitch = 0;
        player.prevRotationPitch = 0;
        player.rotationYawHead = 0;
        player.prevRotationYawHead = 0;
        player.limbSwingAmount = 0;
        player.prevLimbSwingAmount = 0;
        float viewY = RenderManager.instance.playerViewY;
        RenderManager.instance.playerViewY = 180;
        FuturaBridge.drawingOnScreen(true);
        FuturaBridge.INSTANCE.screenDraw(true);
        try {
            RenderManager.instance.renderEntityWithPosYaw(player, 0, 0, 0, 0, 1);
        } finally {
            FuturaBridge.INSTANCE.screenDraw(false);
            FuturaBridge.drawingOnScreen(false);
            RenderManager.instance.playerViewY = viewY;
            player.renderYawOffset = saved[0];
            player.prevRenderYawOffset = saved[1];
            player.rotationYaw = saved[2];
            player.prevRotationYaw = saved[3];
            player.rotationPitch = saved[4];
            player.prevRotationPitch = saved[5];
            player.rotationYawHead = saved[6];
            player.prevRotationYawHead = saved[7];
            player.limbSwing = saved[8];
            player.limbSwingAmount = saved[9];
            player.prevLimbSwingAmount = saved[10];
        }
        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
    }
}
