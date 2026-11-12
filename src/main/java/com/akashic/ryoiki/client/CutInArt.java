package com.akashic.ryoiki.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.RyoikiTenkai;

/**
 * A picture or a short animation of the owner's own choosing on the cut-in, one for each domain. The mod ships none:
 * whoever runs the modpack puts PNG frames in {@code config/ryoikitenkai/faixa/<domain id>/} (played in the order
 * of their names, at the frames per second written in an optional {@code fps.txt}, 12 otherwise), and they show in
 * a slanted panel at the right end of the band. A folder that is missing or empty shows nothing.
 */
final class CutInArt {

    private static final int MAX_FRAMES = 96;
    private static final int MAX_SIDE = 1024;

    private static final class Clip {

        int[] textures = new int[0];
        float fps = 12;
        /** Width over height of the first frame. */
        float aspect = 1;
    }

    private static final Map<String, Clip> CLIPS = new HashMap<>();

    private CutInArt() {}

    /** Whether this domain has frames to show. */
    static boolean has(String domainId) {
        return clip(domainId).textures.length > 0;
    }

    private static Clip clip(String domainId) {
        Clip clip = CLIPS.get(domainId);
        if (clip != null) {
            return clip;
        }
        clip = new Clip();
        CLIPS.put(domainId, clip);
        File dir = new File(new File(Minecraft.getMinecraft().mcDataDir, "config/ryoikitenkai/faixa"), domainId);
        File[] files = dir.listFiles(
            (d, name) -> name.toLowerCase()
                .endsWith(".png"));
        if (files == null || files.length == 0) {
            return clip;
        }
        Arrays.sort(files);
        int[] textures = new int[Math.min(files.length, MAX_FRAMES)];
        int n = 0;
        for (File file : files) {
            if (n >= textures.length) {
                break;
            }
            try {
                BufferedImage image = ImageIO.read(file);
                if (image == null || image.getWidth() > MAX_SIDE || image.getHeight() > MAX_SIDE) {
                    RyoikiTenkai.LOG
                        .warn("Cut-in frame {} skipped: not a PNG of at most {} pixels a side", file, MAX_SIDE);
                    continue;
                }
                if (n == 0) {
                    clip.aspect = (float) image.getWidth() / image.getHeight();
                }
                textures[n++] = new DynamicTexture(image).getGlTextureId();
            } catch (IOException | RuntimeException e) {
                RyoikiTenkai.LOG.warn("Cut-in frame {} could not be read", file, e);
            }
        }
        clip.textures = Arrays.copyOf(textures, n);
        File fps = new File(dir, "fps.txt");
        if (fps.isFile()) {
            try {
                float value = Float
                    .parseFloat(new String(Files.readAllBytes(fps.toPath()), StandardCharsets.UTF_8).trim());
                if (value > 0 && value <= 60) {
                    clip.fps = value;
                }
            } catch (IOException | RuntimeException e) {
                RyoikiTenkai.LOG.warn("{} is not a number of frames per second", fps);
            }
        }
        RyoikiTenkai.LOG.info("Cut-in art for {}: {} frame(s) at {} fps", domainId, n, clip.fps);
        return clip;
    }

    /**
     * Draws the domain's frame for this moment in a slanted panel.
     *
     * @param right the panel's right edge at the band's middle height
     * @param yc    the band's middle
     * @param half  half the band's height
     * @param skew  how far the band's top edge leans right of its middle
     * @param age   ticks since the hand sign began
     * @return the panel's left edge at the band's middle height (for whatever is laid out beside it)
     */
    static float draw(String domainId, int trim, float right, float yc, float half, float skew, float age) {
        Clip clip = clip(domainId);
        if (clip.textures.length == 0) {
            return right;
        }
        float inner = half * 0.88F;
        float lean = skew * 0.88F;
        float width = Math.min(inner * 2 * clip.aspect, half * 5);
        float left = right - width;
        int frame = (int) (age / 20F * clip.fps) % clip.textures.length;
        Tessellator t = Tessellator.instance;
        // A border in the band's trim, then the frame, sheared to the band's slant.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        float b = Math.max(1.2F, half * 0.035F);
        t.startDrawingQuads();
        Draw.color(t, trim, 1);
        t.addVertex(left - b + lean, yc - inner - b, 0);
        t.addVertex(left - b - lean, yc + inner + b, 0);
        t.addVertex(right + b - lean, yc + inner + b, 0);
        t.addVertex(right + b + lean, yc - inner - b, 0);
        t.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, clip.textures[frame]);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glColor4f(1, 1, 1, 1);
        t.startDrawingQuads();
        t.setColorRGBA_F(1, 1, 1, 1);
        t.addVertexWithUV(left + lean, yc - inner, 0, 0, 0);
        t.addVertexWithUV(left - lean, yc + inner, 0, 0, 1);
        t.addVertexWithUV(right - lean, yc + inner, 0, 1, 1);
        t.addVertexWithUV(right + lean, yc - inner, 0, 1, 0);
        t.draw();
        return left - b - Math.abs(lean);
    }
}
