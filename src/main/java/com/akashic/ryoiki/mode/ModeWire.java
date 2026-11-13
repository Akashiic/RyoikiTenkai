package com.akashic.ryoiki.mode;

/**
 * The messages of the plugin channel {@link #CHANNEL}, about the sorcerer mode. The client asks, the server decides
 * and tells: the one who asked, and everyone who sees it. No Minecraft classes.
 */
public final class ModeWire {

    public static final String CHANNEL = "ryoiki:mode";

    /** Client to server: this player wants the mode on, or off. */
    public static final byte ASK = 1;
    /** Server to client: this entity (a player) is in the mode now, or is not. */
    public static final byte STATE = 2;
    /** Server to client: what you asked was not done, and why ({@link ModeRules}). */
    public static final byte REFUSED = 3;

    /** Server to client: your technique, the domain it ends in ({@code 0} none, else the domain's number plus one). */
    public static final byte TECHNIQUE = 4;
    /**
     * Server to client: one of your four techniques is recharging: for {@code total} ticks, {@code left} of them left.
     */
    public static final byte COOLDOWN = 5;

    /** Client to server: this player uses one of its four techniques (1 to {@link #SLOTS}). */
    public static final byte USE = 6;

    /**
     * Server to client: something a technique does, to be drawn. Its {@code kind} is one of {@link AbilityMath}'s;
     * the entity is who used the technique; then a place, a direction, a size and a time, each meaning what the kind
     * makes of it.
     */
    public static final byte EFFECT = 7;

    /** The techniques of the mode: three and the domain expansion, numbered from 1. */
    public static final int SLOTS = 4;
    /** No recharge is longer than an hour. */
    public static final int LONGEST = 72000;

    /** One decoded message; which fields are set depends on {@link #op}. */
    public static final class Message {

        public byte op;
        /** ASK: the mode wanted. STATE: the mode the entity is in. */
        public boolean on;
        /** STATE. */
        public int entity;
        /** REFUSED. */
        public int reason;
        /** TECHNIQUE: 0 for none, else the number of the domain plus one. */
        public int technique;
        /** USE and COOLDOWN: which technique (1 to {@link #SLOTS}). COOLDOWN: its recharge in ticks. */
        public int slot;
        public int total;
        public int left;
        /** EFFECT. */
        public int kind;
        public double x;
        public double y;
        public double z;
        public float dx;
        public float dy;
        public float dz;
        public float size;
        public int ticks;
    }

    private ModeWire() {}

    public static byte[] ask(boolean on) {
        return new byte[] { ASK, (byte) (on ? 1 : 0) };
    }

    public static byte[] state(int entity, boolean on) {
        return new byte[] { STATE, (byte) (entity >>> 24), (byte) (entity >>> 16), (byte) (entity >>> 8), (byte) entity,
            (byte) (on ? 1 : 0) };
    }

    public static byte[] refused(int reason) {
        return new byte[] { REFUSED, (byte) reason };
    }

    public static byte[] use(int slot) {
        return new byte[] { USE, (byte) slot };
    }

    public static byte[] effect(int kind, int entity, double x, double y, double z, double dx, double dy, double dz,
        double size, int ticks) {
        return java.nio.ByteBuffer.allocate(EFFECT_LENGTH)
            .put(EFFECT)
            .put((byte) kind)
            .putInt(entity)
            .putDouble(x)
            .putDouble(y)
            .putDouble(z)
            .putFloat((float) dx)
            .putFloat((float) dy)
            .putFloat((float) dz)
            .putFloat((float) size)
            .putShort((short) ticks)
            .array();
    }

    private static final int EFFECT_LENGTH = 48;

    public static byte[] technique(int technique) {
        return new byte[] { TECHNIQUE, (byte) technique };
    }

    public static byte[] cooldown(int slot, int total, int left) {
        return new byte[] { COOLDOWN, (byte) slot, (byte) (total >>> 24), (byte) (total >>> 16), (byte) (total >>> 8),
            (byte) total, (byte) (left >>> 24), (byte) (left >>> 16), (byte) (left >>> 8), (byte) left };
    }

    private static boolean finite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static int number(byte[] data, int at) {
        return (data[at] & 0xFF) << 24 | (data[at + 1] & 0xFF) << 16 | (data[at + 2] & 0xFF) << 8 | data[at + 3] & 0xFF;
    }

    /** The message in these bytes, or null if they are not one: whatever a client sends is not to be trusted. */
    public static Message decode(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        Message m = new Message();
        m.op = data[0];
        if (m.op == ASK && data.length == 2 && (data[1] == 0 || data[1] == 1)) {
            m.on = data[1] == 1;
            return m;
        }
        if (m.op == STATE && data.length == 6 && (data[5] == 0 || data[5] == 1)) {
            m.entity = number(data, 1);
            m.on = data[5] == 1;
            return m;
        }
        if (m.op == REFUSED && data.length == 2) {
            m.reason = data[1];
            return m;
        }
        if (m.op == USE && data.length == 2 && data[1] >= 1 && data[1] <= SLOTS) {
            m.slot = data[1];
            return m;
        }
        if (m.op == EFFECT && data.length == EFFECT_LENGTH) {
            java.nio.ByteBuffer in = java.nio.ByteBuffer.wrap(data, 1, EFFECT_LENGTH - 1);
            m.kind = in.get();
            m.entity = in.getInt();
            m.x = in.getDouble();
            m.y = in.getDouble();
            m.z = in.getDouble();
            m.dx = in.getFloat();
            m.dy = in.getFloat();
            m.dz = in.getFloat();
            m.size = in.getFloat();
            m.ticks = in.getShort();
            boolean numbers = finite(m.x) && finite(
                m.y) && finite(m.z) && finite(m.dx) && finite(m.dy) && finite(m.dz) && finite(m.size);
            return numbers && m.kind >= AbilityMath.PULL && m.kind <= AbilityMath.RED_HIT && m.ticks >= 0 ? m : null;
        }
        if (m.op == TECHNIQUE && data.length == 2 && data[1] >= 0) {
            m.technique = data[1];
            return m;
        }
        if (m.op == COOLDOWN && data.length == 10) {
            m.slot = data[1];
            m.total = number(data, 2);
            m.left = number(data, 6);
            if (m.slot >= 1 && m.slot <= SLOTS && m.left >= 0 && m.left <= m.total && m.total <= LONGEST) {
                return m;
            }
        }
        return null;
    }
}
