package com.akashic.ryoiki.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.akashic.ryoiki.DomainType;

/**
 * The messages of the plugin channel {@link #CHANNEL}, server to client. Raw plugin messages (as Kunftig's), taken on
 * the client's main thread in the order they were sent, so {@link #ENTER} is always applied before the teleport
 * that follows it. No Minecraft classes.
 */
public final class DomainWire {

    public static final String CHANNEL = "ryoiki:dom";

    /** A domain is being cast nearby: the hand sign, {@code windup} ticks before it opens. */
    public static final byte WINDUP = 1;
    /** You were caught: the next teleport takes you into the arena. */
    public static final byte ENTER = 2;
    /** The domain you are in ended: the next teleport takes you back. */
    public static final byte EXIT = 3;
    /** A domain stands here (for those outside): show its dome for {@code ticks}. */
    public static final byte DOME = 4;
    /** That domain is gone. */
    public static final byte DOME_END = 5;

    /** One decoded message; which fields are set depends on {@link #op}. */
    public static final class Message {

        public byte op;
        public int session;
        public DomainType type;
        public int casterEntity;
        /** WINDUP and DOME: where it was cast. ENTER: the arena's centre. */
        public double x;
        public double y;
        public double z;
        public int radius;
        public int ticks;
        /** ENTER: the chunk the player leaves, and how many chunks around it to keep. */
        public int keepChunkX;
        public int keepChunkZ;
        public int keepRadius;
        /** ENTER: where the caster looked when the domain opened; the domain's scene is laid out behind it. */
        public float yaw;
    }

    private DomainWire() {}

    public static byte[] windup(int session, DomainType type, int casterEntity, double x, double y, double z,
        int radius, int ticks) {
        return place(WINDUP, session, type, casterEntity, x, y, z, radius, ticks);
    }

    public static byte[] dome(int session, DomainType type, double x, double y, double z, int radius, int ticks) {
        return place(DOME, session, type, 0, x, y, z, radius, ticks);
    }

    public static byte[] enter(int session, DomainType type, int casterEntity, double x, double y, double z, int radius,
        int ticks, int keepChunkX, int keepChunkZ, int keepRadius, float yaw) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(64);
            DataOutputStream out = new DataOutputStream(bytes);
            head(out, ENTER, session, type, casterEntity, x, y, z, radius, ticks);
            out.writeInt(keepChunkX);
            out.writeInt(keepChunkZ);
            out.writeByte(keepRadius);
            out.writeFloat(yaw);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] exit(int session) {
        return bare(EXIT, session);
    }

    public static byte[] domeEnd(int session) {
        return bare(DOME_END, session);
    }

    private static byte[] bare(byte op, int session) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(8);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(op);
            out.writeInt(session);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] place(byte op, int session, DomainType type, int casterEntity, double x, double y, double z,
        int radius, int ticks) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(48);
            head(new DataOutputStream(bytes), op, session, type, casterEntity, x, y, z, radius, ticks);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void head(DataOutputStream out, byte op, int session, DomainType type, int casterEntity, double x,
        double y, double z, int radius, int ticks) throws IOException {
        out.writeByte(op);
        out.writeInt(session);
        out.writeByte(type.ordinal());
        out.writeInt(casterEntity);
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(z);
        out.writeByte(radius);
        out.writeShort(ticks);
    }

    /** Decodes a message, or returns null for one this version does not know (a newer server). */
    public static Message read(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            Message m = new Message();
            m.op = in.readByte();
            m.session = in.readInt();
            if (m.op == EXIT || m.op == DOME_END) {
                return m;
            }
            if (m.op != WINDUP && m.op != ENTER && m.op != DOME) {
                return null;
            }
            int type = in.readUnsignedByte();
            if (type >= DomainType.values().length) {
                return null;
            }
            m.type = DomainType.values()[type];
            m.casterEntity = in.readInt();
            m.x = in.readDouble();
            m.y = in.readDouble();
            m.z = in.readDouble();
            m.radius = in.readUnsignedByte();
            m.ticks = in.readUnsignedShort();
            if (m.op == ENTER) {
                m.keepChunkX = in.readInt();
                m.keepChunkZ = in.readInt();
                m.keepRadius = in.readUnsignedByte();
                m.yaw = in.readFloat();
            }
            return m;
        } catch (IOException e) {
            return null;
        }
    }
}
