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

    /** One decoded message; which fields are set depends on {@link #op}. */
    public static final class Message {

        public byte op;
        /** ASK: the mode wanted. STATE: the mode the entity is in. */
        public boolean on;
        /** STATE. */
        public int entity;
        /** REFUSED. */
        public int reason;
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
            m.entity = (data[1] & 0xFF) << 24 | (data[2] & 0xFF) << 16 | (data[3] & 0xFF) << 8 | data[4] & 0xFF;
            m.on = data[5] == 1;
            return m;
        }
        if (m.op == REFUSED && data.length == 2) {
            m.reason = data[1];
            return m;
        }
        return null;
    }
}
