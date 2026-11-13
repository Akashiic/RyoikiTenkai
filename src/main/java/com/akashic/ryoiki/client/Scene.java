package com.akashic.ryoiki.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

import org.lwjgl.opengl.GL11;

/**
 * The scene, or the two scenes, of the arena this player is in. Alone, a domain has the whole arena. In a clash the
 * two are fused: a line across the arena ({@link ClientDomain#sceneSeam}), the host's domain behind it and the
 * rival's before it, each drawn only in its part and the rival's turned about so that what its scene has stands in
 * its own part, facing the line.
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

    /**
     * Keeps what is drawn next to a part's side of the line. Call with the matrix at the arena's centre, turned to
     * the scene; the line stays where it is whatever the matrix becomes, until {@link #leave}.
     */
    static void clip(ClientDomain domain, int part) {
        if (!domain.fused()) {
            return;
        }
        float seam = domain.sceneSeam();
        PLANE.clear();
        PLANE.put(0)
            .put(0)
            .put(part == 0 ? -1 : 1)
            .put(part == 0 ? seam : -seam)
            .flip();
        GL11.glClipPlane(GL11.GL_CLIP_PLANE0, PLANE);
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
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
    }

    /** A place of the arena's scene, on the ground, in a part's own scene. */
    static double[] local(ClientDomain domain, int part, double x, double z) {
        return turned(domain, part) ? new double[] { -x, turnedAt(domain, part) - z } : new double[] { x, z };
    }
}
