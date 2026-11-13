package com.akashic.ryoiki.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.Frontier;

/**
 * The scene, or the two scenes, of the arena this player is in. Alone, a domain has the whole arena. In a clash the
 * two are fused: a front across the arena ({@link Frontier}, about {@link ClientDomain#sceneSeam}), the host's
 * domain behind it and the rival's before it, each drawn only on its ground and the rival's turned about so that
 * what its scene has stands on its own ground, facing the front.
 */
final class Scene {

    private static final DoubleBuffer PLANE = ByteBuffer.allocateDirect(32)
        .order(ByteOrder.nativeOrder())
        .asDoubleBuffer();

    private Scene() {}

    static int parts(ClientDomain domain) {
        return domain.fused() ? 2 : 1;
    }

    static DomainLook look(ClientDomain domain, int part) {
        return DomainRenderer.look(domain.fused() ? domain.clashTypes[part] : domain.shown());
    }

    /** Whether a part's scene is drawn turned about: the rival's in a clash, and the domain of a rival that won. */
    static boolean turned(ClientDomain domain, int part) {
        return domain.fused() ? part == 1 : domain.turned;
    }

    /** How far before the arena's centre the centre of a turned scene is. */
    static float turnedAt(ClientDomain domain, int part) {
        return domain.fused() ? domain.sceneSeam() + ClientDomain.stage(domain.clashTypes[1]) : domain.turnedAt;
    }

    /** Pieces a part of the fused arena is drawn in: one for each stretch of the front, and one beyond each end. */
    static int pieces(ClientDomain domain) {
        return domain.fused() ? Frontier.SEGMENTS + 2 : 1;
    }

    /**
     * Keeps what is drawn next to one piece of a part: the ground on the part's side of one stretch of the front.
     * The front is not straight, and a clip is; so each part is drawn piece by piece, a stretch at a time, between
     * two walls across the seam and on its side of that stretch. Call with the matrix at the arena's centre, turned
     * to the scene; the piece stays where it is whatever the matrix becomes, until {@link #leave}.
     *
     * @param line the front ({@link ClientDomain#frontLine})
     */
    static void clip(ClientDomain domain, int part, int piece, float[] line) {
        if (!domain.fused()) {
            return;
        }
        float seam = domain.sceneSeam();
        int last = Frontier.SEGMENTS;
        // Behind the front for the host, before it for the rival.
        float side = part == 0 ? 1 : -1;
        if (piece == 0 || piece == last + 1) {
            // Beyond an end of the front: past its last point, on the part's side of where the front ends.
            int end = piece == 0 ? 0 : last;
            plane(0, piece == 0 ? -1 : 1, 0, piece == 0 ? line[0] : -line[last * 2]);
            plane(1, 0, -side, side * (seam + line[end * 2 + 1]));
            GL11.glDisable(GL11.GL_CLIP_PLANE2);
            return;
        }
        int i = piece - 1;
        float x0 = line[i * 2];
        float z0 = seam + line[i * 2 + 1];
        float x1 = line[i * 2 + 2];
        float z1 = seam + line[i * 2 + 3];
        float slope = (z1 - z0) / (x1 - x0);
        plane(0, 1, 0, -x0);
        plane(1, -1, 0, x1);
        // On the stretch itself: z = z0 + slope (x - x0); the host keeps what is behind it.
        plane(2, side * slope, -side, side * (z0 - slope * x0));
    }

    private static void plane(int index, float x, float z, float d) {
        PLANE.clear();
        PLANE.put(x)
            .put(0)
            .put(z)
            .put(d)
            .flip();
        GL11.glClipPlane(GL11.GL_CLIP_PLANE0 + index, PLANE);
        GL11.glEnable(GL11.GL_CLIP_PLANE0 + index);
    }

    /**
     * Keeps what is drawn next between two lines across the arena, each a number of blocks before the seam: for the
     * skies, which fade into each other across a band and have no fine edge to follow. Either may be infinite.
     */
    static void band(ClientDomain domain, float from, float to) {
        float seam = domain.sceneSeam();
        if (from > Float.NEGATIVE_INFINITY) {
            plane(0, 0, 1, -(seam + from));
        }
        if (to < Float.POSITIVE_INFINITY) {
            plane(1, 0, -1, seam + to);
        }
    }

    /** Moves the matrix from the arena's scene to a part's own, if that one is turned about. */
    static void enter(ClientDomain domain, int part) {
        if (turned(domain, part)) {
            GL11.glTranslatef(0, 0, turnedAt(domain, part));
            GL11.glRotatef(180, 0, 1, 0);
        }
    }

    static void leave() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
        GL11.glDisable(GL11.GL_CLIP_PLANE1);
        GL11.glDisable(GL11.GL_CLIP_PLANE2);
    }

    /** A place of the arena's scene, on the ground, in a part's own scene. */
    static double[] local(ClientDomain domain, int part, double x, double z) {
        return turned(domain, part) ? new double[] { -x, turnedAt(domain, part) - z } : new double[] { x, z };
    }
}
