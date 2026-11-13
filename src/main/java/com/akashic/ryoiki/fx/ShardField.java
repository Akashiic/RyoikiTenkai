package com.akashic.ryoiki.fx;

import java.util.Random;

/**
 * The shards a domain breaks into when it ends: triangles that together cover the whole screen (the unit square),
 * each with the way it flies off. Built from a grid whose inner corners are nudged, every cell cut in two, so the
 * pieces are uneven like broken glass but leave no gap at the first instant. No Minecraft classes.
 */
public final class ShardField {

    /** One shard: three corners in the unit square, and its flight. */
    public static final class Shard {

        public final float[] x = new float[3];
        public final float[] y = new float[3];
        public float centerX;
        public float centerY;
        /** Units of screen per unit of time. */
        public float velocityX;
        public float velocityY;
        /** Turns per unit of time, about the shard's centre. */
        public float spin;
        /** When, from 0 to 1, this shard lets go. */
        public float delay;
        /** 0 to 1, for telling one shard from the next. */
        public float shade;
    }

    public final Shard[] shards;

    public ShardField(int columns, int rows, long seed) {
        Random random = new Random(seed);
        float[][] px = new float[columns + 1][rows + 1];
        float[][] py = new float[columns + 1][rows + 1];
        for (int i = 0; i <= columns; i++) {
            for (int j = 0; j <= rows; j++) {
                boolean edgeX = i == 0 || i == columns;
                boolean edgeY = j == 0 || j == rows;
                px[i][j] = (i + (edgeX ? 0 : (random.nextFloat() - 0.5F) * 0.5F)) / columns;
                py[i][j] = (j + (edgeY ? 0 : (random.nextFloat() - 0.5F) * 0.5F)) / rows;
            }
        }
        shards = new Shard[columns * rows * 2];
        int n = 0;
        for (int i = 0; i < columns; i++) {
            for (int j = 0; j < rows; j++) {
                boolean flip = random.nextBoolean();
                // The cell's corners, counter-clockwise from the top left.
                int[][] c = { { i, j }, { i, j + 1 }, { i + 1, j + 1 }, { i + 1, j } };
                int[][] first = flip ? new int[][] { c[0], c[1], c[3] } : new int[][] { c[0], c[1], c[2] };
                int[][] second = flip ? new int[][] { c[1], c[2], c[3] } : new int[][] { c[0], c[2], c[3] };
                shards[n++] = shard(first, px, py, random);
                shards[n++] = shard(second, px, py, random);
            }
        }
    }

    private static Shard shard(int[][] corners, float[][] px, float[][] py, Random random) {
        Shard s = new Shard();
        for (int k = 0; k < 3; k++) {
            s.x[k] = px[corners[k][0]][corners[k][1]];
            s.y[k] = py[corners[k][0]][corners[k][1]];
            s.centerX += s.x[k] / 3;
            s.centerY += s.y[k] / 3;
        }
        // Away from the middle of the screen, the nearer the middle the sooner.
        float dx = s.centerX - 0.5F;
        float dy = s.centerY - 0.5F;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float push = 0.9F + random.nextFloat() * 0.9F;
        s.velocityX = (distance < 1.0e-4F ? 0 : dx / distance) * push + (random.nextFloat() - 0.5F) * 0.3F;
        s.velocityY = (distance < 1.0e-4F ? 1 : dy / distance) * push + (random.nextFloat() - 0.5F) * 0.3F;
        s.spin = (random.nextFloat() - 0.5F) * 1.6F;
        s.delay = Math.min(0.45F, distance * 0.55F + random.nextFloat() * 0.08F);
        s.shade = random.nextFloat();
        return s;
    }

    /** How far along its flight a shard is at {@code time} (0 to 1): 0 while it still holds, then easing out. */
    public static float flight(Shard s, float time) {
        if (time <= s.delay) {
            return 0;
        }
        float t = (time - s.delay) / (1 - s.delay);
        return t * t * (1.5F - 0.5F * t);
    }

    /** The area of a shard, in units of screen. */
    public static float area(Shard s) {
        return Math.abs((s.x[1] - s.x[0]) * (s.y[2] - s.y[0]) - (s.x[2] - s.x[0]) * (s.y[1] - s.y[0])) / 2;
    }
}
