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
    /**
     * The clash of this session began, for {@code ticks}: the caster and type are the host's, and the rival's follow.
     */
    public static final byte CLASH = 6;
    /** The clash is over: who won ({@code result}), and the caster and type the domain goes on with. */
    public static final byte CLASH_END = 7;
    /** Someone answers the domain of this session with one of its own: its hand sign, at its place. */
    public static final byte RIVAL = 8;
    /** The hits of the host and of the rival so far. */
    public static final byte CLASH_SCORE = 9;

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
        /** CLASH: the rival caster and its domain. */
        public int rivalEntity;
        public DomainType rivalType;
        /** CLASH_SCORE. */
        public int hostHits;
        public int rivalHits;
        /** CLASH_END: 0 the host won, 1 the rival, 2 a tie. */
        public int result;
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

    public static byte[] rival(int session, DomainType type, int casterEntity, double x, double y, double z, int radius,
        int ticks) {
        return place(RIVAL, session, type, casterEntity, x, y, z, radius, ticks);
    }

    public static byte[] clash(int session, int ticks, int hostEntity, DomainType hostType, int rivalEntity,
        DomainType rivalType) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(20);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(CLASH);
            out.writeInt(session);
            out.writeShort(ticks);
            out.writeInt(hostEntity);
            out.writeByte(hostType.ordinal());
            out.writeInt(rivalEntity);
            out.writeByte(rivalType.ordinal());
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] clashScore(int session, int host, int rival) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(8);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(CLASH_SCORE);
            out.writeInt(session);
            out.writeByte(host);
            out.writeByte(rival);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code result} is 0 when the host won, 1 the rival, 2 a tie (and then the domain ends). */
    public static byte[] clashEnd(int session, int result, DomainType type, int casterEntity) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(12);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(CLASH_END);
            out.writeInt(session);
            out.writeByte(result);
            out.writeByte(type.ordinal());
            out.writeInt(casterEntity);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
            int types = DomainType.values().length;
            if (m.op == CLASH) {
                m.ticks = in.readUnsignedShort();
                m.casterEntity = in.readInt();
                int host = in.readUnsignedByte();
                m.rivalEntity = in.readInt();
                int rival = in.readUnsignedByte();
                if (host >= types || rival >= types) {
                    return null;
                }
                m.type = DomainType.values()[host];
                m.rivalType = DomainType.values()[rival];
                return m;
            }
            if (m.op == CLASH_SCORE) {
                m.hostHits = in.readUnsignedByte();
                m.rivalHits = in.readUnsignedByte();
                return m;
            }
            if (m.op == CLASH_END) {
                m.result = in.readUnsignedByte();
                int winner = in.readUnsignedByte();
                if (winner >= types) {
                    return null;
                }
                m.type = DomainType.values()[winner];
                m.casterEntity = in.readInt();
                return m;
            }
            if (m.op != WINDUP && m.op != ENTER && m.op != DOME && m.op != RIVAL) {
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
