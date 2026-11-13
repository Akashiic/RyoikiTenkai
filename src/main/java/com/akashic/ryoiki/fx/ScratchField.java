package com.akashic.ryoiki.fx;

/**
 * The small cuts of the crimson shrine: marks of a blade in the air, everywhere, each drawn in an instant, there for
 * a moment and gone. Not the great cuts ({@link SlashField}): these are short, plain, and many, just enough to
 * trouble the view. No Minecraft classes.
 *
 * <p>
 * The air is taken in cells, {@link #CELL} blocks a side. Each cell makes a mark every {@link #PERIOD} ticks, at its
 * own moment in that time, somewhere in itself, at an angle of its own; a new place and angle each time. All of it
 * comes from the domain's number, the cell and the time, so every client draws the same marks without a message.
 */
public final class ScratchField {

    public static final float CELL = 3;
    public static final int PERIOD = 14;
    /** Ticks a mark is there. */
    public static final float LIFE = 5;
    /** Ticks the blade takes to draw it. */
    public static final float DRAW = 1.1F;
    /** Cells upward from the floor that have marks. */
    public static final int LAYERS = 3;

    private static final float MIN_LENGTH = 1.1F;
    private static final float MAX_LENGTH = 3.0F;

    /** One mark: a straight stroke from one point to another, relative to the arena's centre. */
    public static final class Scratch {

        public float fromX;
        public float fromY;
        public float fromZ;
        public float toX;
        public float toY;
        public float toZ;
        /** Ticks since the blade began it. */
        public float age;
    }

    private ScratchField() {}

    /**
     * Fills {@code out} with the mark of a cell at a time, and says whether there is one now.
     *
     * @param cellX the cell along X: the one that holds x is {@code floor(x / CELL)}; likewise Y and Z
     * @param time  ticks since the domain opened, with fractions
     */
    public static boolean at(int seed, int cellX, int cellY, int cellZ, float time, Scratch out) {
        if (cellY < 0 || cellY >= LAYERS) {
            return false;
        }
        long cell = mix(
            seed * 0x9E3779B97F4A7C15L ^ cellX * 0xC2B2AE3D27D4EB4FL
                ^ cellY * 0x165667B19E3779F9L
                ^ cellZ * 0x27D4EB2F165667C5L);
        // Each cell at its own moment of the period, so the air is never all marked or all clear at once.
        float shifted = time + unit(cell) * PERIOD;
        int turn = (int) Math.floor(shifted / PERIOD);
        float age = shifted - turn * PERIOD;
        if (age >= LIFE) {
            return false;
        }
        long h = mix(cell + turn * 0x632BE59BD9B4E019L);
        float length = MIN_LENGTH + (MAX_LENGTH - MIN_LENGTH) * unit(h >>> 12);
        // Any way round, and as often steep as level: a blade comes from anywhere.
        double around = unit(h >>> 28) * Math.PI * 2;
        double steep = (unit(h >>> 36) - 0.5) * Math.PI * 0.9;
        float dx = (float) (Math.cos(around) * Math.cos(steep));
        float dy = (float) Math.sin(steep);
        float dz = (float) (Math.sin(around) * Math.cos(steep));
        // Its middle anywhere in the cell that keeps the whole of it over the floor.
        float mx = (cellX + unit(h >>> 44)) * CELL;
        float mz = (cellZ + unit(h >>> 52)) * CELL;
        float my = (cellY + unit(h >>> 4)) * CELL;
        float half = length / 2;
        my = Math.max(my, 0.25F + Math.abs(dy) * half);
        out.fromX = mx - dx * half;
        out.fromY = my - dy * half;
        out.fromZ = mz - dz * half;
        out.toX = mx + dx * half;
        out.toY = my + dy * half;
        out.toZ = mz + dz * half;
        out.age = age;
        return true;
    }

    /** How much of the stroke is drawn, 0 to 1. */
    public static float drawn(float age) {
        return age >= DRAW ? 1 : age <= 0 ? 0 : age / DRAW;
    }

    /** How strongly the mark shows, 0 to 1: whole at once, and gone over its last two ticks. */
    public static float shown(float age) {
        float left = (LIFE - age) / 2;
        return left >= 1 ? 1 : left <= 0 ? 0 : left;
    }

    private static long mix(long z) {
        z = (z ^ z >>> 30) * 0xBF58476D1CE4E5B9L;
        z = (z ^ z >>> 27) * 0x94D049BB133111EBL;
        return z ^ z >>> 31;
    }

    private static float unit(long bits) {
        return (bits & 0xFF) / 256F;
    }
}
