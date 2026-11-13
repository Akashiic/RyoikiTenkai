package com.akashic.ryoiki.client;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.opengl.GL11;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.akashic.ryoiki.RyoikiTenkai;

/**
 * A picture or a short animation of the owner's own choosing on the cut-in, one for each domain. The mod ships none:
 * whoever runs the modpack puts in {@code config/ryoikitenkai/faixa/} either an animated GIF named for the domain
 * ({@code <domain id>.gif}, played at its own speed) or a folder of PNG frames ({@code <domain id>/}, played in the
 * order of their names at the frames per second written in an optional {@code fps.txt}, 12 otherwise), and it shows
 * in a slanted panel at the right end of the band. With neither, nothing shows.
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
        File base = new File(Minecraft.getMinecraft().mcDataDir, "config/ryoikitenkai/faixa");
        File gif = new File(base, domainId + ".gif");
        if (gif.isFile()) {
            gif(gif, clip);
            RyoikiTenkai.LOG.info(
                "Cut-in art for {}: {} frame(s) at {} fps, from {}",
                domainId,
                clip.textures.length,
                clip.fps,
                gif);
            return clip;
        }
        File dir = new File(base, domainId);
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
     * Reads an animated GIF whole: each of its frames is laid over the ones before, as a GIF is meant to be played,
     * and its own timing gives the frames per second.
     */
    private static void gif(File file, Clip clip) {
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (in == null || !readers.hasNext()) {
                return;
            }
            ImageReader reader = readers.next();
            reader.setInput(in);
            int count = Math.min(reader.getNumImages(true), MAX_FRAMES);
            int width = 0;
            int height = 0;
            for (int i = 0; i < count; i++) {
                int[] box = box(reader.getImageMetadata(i));
                width = Math.max(width, box[0] + reader.getWidth(i));
                height = Math.max(height, box[1] + reader.getHeight(i));
            }
            if (count == 0 || width <= 0 || height <= 0 || width > MAX_SIDE || height > MAX_SIDE) {
                RyoikiTenkai.LOG.warn("{} skipped: not a GIF of at most {} pixels a side", file, MAX_SIDE);
                return;
            }
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] textures = new int[count];
            int hundredths = 0;
            for (int i = 0; i < count; i++) {
                int[] box = box(reader.getImageMetadata(i));
                BufferedImage frame = reader.read(i);
                BufferedImage before = null;
                if (box[3] == 3) {
                    // "Restore to previous": what was there comes back after this frame.
                    before = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    before.getGraphics()
                        .drawImage(canvas, 0, 0, null);
                }
                Graphics2D g = canvas.createGraphics();
                g.drawImage(frame, box[0], box[1], null);
                g.dispose();
                BufferedImage shown = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                shown.getGraphics()
                    .drawImage(canvas, 0, 0, null);
                textures[i] = new DynamicTexture(shown).getGlTextureId();
                hundredths += box[2] <= 0 ? 10 : box[2];
                if (box[3] == 2) {
                    // "Restore to background": the frame's own area is wiped.
                    Graphics2D wipe = canvas.createGraphics();
                    wipe.setComposite(AlphaComposite.Clear);
                    wipe.fillRect(box[0], box[1], frame.getWidth(), frame.getHeight());
                    wipe.dispose();
                } else if (before != null) {
                    canvas = before;
                }
            }
            reader.dispose();
            clip.textures = textures;
            clip.aspect = (float) width / height;
            clip.fps = Math.max(1, Math.min(60, count * 100F / Math.max(1, hundredths)));
        } catch (IOException | RuntimeException e) {
            RyoikiTenkai.LOG.warn("{} could not be read", file, e);
        }
    }

    /** A GIF frame's place and timing: {left, top, delay in hundredths of a second, disposal method}. */
    private static int[] box(IIOMetadata metadata) {
        int[] out = { 0, 0, 10, 0 };
        try {
            Node root = metadata.getAsTree("javax_imageio_gif_image_1.0");
            for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                NamedNodeMap a = n.getAttributes();
                if ("ImageDescriptor".equals(n.getNodeName())) {
                    out[0] = Integer.parseInt(
                        a.getNamedItem("imageLeftPosition")
                            .getNodeValue());
                    out[1] = Integer.parseInt(
                        a.getNamedItem("imageTopPosition")
                            .getNodeValue());
                } else if ("GraphicControlExtension".equals(n.getNodeName())) {
                    out[2] = Integer.parseInt(
                        a.getNamedItem("delayTime")
                            .getNodeValue());
                    String disposal = a.getNamedItem("disposalMethod")
                        .getNodeValue();
                    out[3] = "restoreToBackgroundColor".equals(disposal) ? 2
                        : "restoreToPrevious".equals(disposal) ? 3 : 1;
                }
            }
        } catch (RuntimeException e) {
            // A frame with no such data sits at the corner and lasts a tenth of a second.
        }
        return out;
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
