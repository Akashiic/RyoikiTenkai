package com.akashic.ryoiki.arena;

/**
 * The shape of a domain arena, in blocks relative to its centre: a floor disc and a dome shell around an open
 * interior of {@link #radius} blocks. The centre is a corner shared by four chunks, at the top of the floor, so
 * the whole arena fits in a square of {@link #chunkSpan} chunks to each side of it. No Minecraft classes.
 */
public final class ArenaShape {

    public static final int MAX_RADIUS = 30;
    public static final int MIN_RADIUS = 6;

    /** What a block of the arena is. */
    public enum Part {
        FLOOR,
        SHELL
    }

    /** Receives each block of the arena, relative to the centre ({@code dy = -1} is the floor layer). */
    public interface Visitor {

        void block(int dx, int dy, int dz, Part part);
    }

    public final int radius;

    /** How many chunks to each side of the centre an arena of this radius reaches (floor and shell included). */
    public static int chunkSpan(int radius) {
        return (radius + 2 + 15) / 16;
    }

    public ArenaShape(int radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS) {
            throw new IllegalArgumentException("arena radius " + radius + " not in " + MIN_RADIUS + ".." + MAX_RADIUS);
        }
        this.radius = radius;
    }

    /** Whether the block at this offset is open space players move in. */
    public boolean inside(int dx, int dy, int dz) {
        if (dy < 0) {
            return false;
        }
        double x = dx + 0.5;
        double y = dy + 0.5;
        double z = dz + 0.5;
        return x * x + y * y + z * z < (double) radius * radius;
    }

    /** The floor under the interior, and one ring more so the shell stands on it. */
    public boolean floor(int dx, int dy, int dz) {
        if (dy != -1) {
            return false;
        }
        double x = dx + 0.5;
        double z = dz + 0.5;
        double reach = radius + 2;
        return x * x + z * z < reach * reach;
    }

    /** A block outside the interior that touches it, corners included: nothing can squeeze between two of them. */
    public boolean shell(int dx, int dy, int dz) {
        if (dy < 0 || inside(dx, dy, dz)) {
            return false;
        }
        for (int ax = -1; ax <= 1; ax++) {
            for (int ay = -1; ay <= 1; ay++) {
                for (int az = -1; az <= 1; az++) {
                    if ((ax | ay | az) != 0 && inside(dx + ax, dy + ay, dz + az)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void forEach(Visitor visitor) {
        int reach = radius + 2;
        for (int dx = -reach; dx < reach; dx++) {
            for (int dz = -reach; dz < reach; dz++) {
                if (floor(dx, -1, dz)) {
                    visitor.block(dx, -1, dz, Part.FLOOR);
                }
                for (int dy = 0; dy <= reach; dy++) {
                    if (shell(dx, dy, dz)) {
                        visitor.block(dx, dy, dz, Part.SHELL);
                    }
                }
            }
        }
    }

    /**
     * Where someone standing at a horizontal offset from the caster lands in the arena: the same offset from the
     * centre, pulled in until a standing player is clear of the shell. Returns {x, z}.
     */
    public double[] place(double offsetX, double offsetZ) {
        double limit = radius - 2.5;
        double length = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        if (length > limit && length > 0) {
            double scale = limit / length;
            return new double[] { offsetX * scale, offsetZ * scale };
        }
        return new double[] { offsetX, offsetZ };
    }
}
