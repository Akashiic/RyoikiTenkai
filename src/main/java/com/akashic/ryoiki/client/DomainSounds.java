package com.akashic.ryoiki.client;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;

import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.RyoikiTenkai;
import com.akashic.ryoiki.fx.SoundCues;

/** Plays the sounds of a domain on this client. The names are in {@link SoundCues}. */
final class DomainSounds {

    private DomainSounds() {}

    private static ResourceLocation event(String name) {
        return new ResourceLocation(RyoikiTenkai.MODID, name);
    }

    /** In the listener's ears, wherever it is. */
    static void play(String name, float volume, float pitch) {
        if (!OwnerSounds.has(name)) {
            return;
        }
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(new Cue(event(name), volume, pitch));
    }

    static void play(DomainType type, String name, float volume, float pitch) {
        play(SoundCues.of(type.id, name), volume, pitch);
    }

    /** From a place in the world. */
    static void playAt(String name, double x, double y, double z, float volume) {
        if (!OwnerSounds.has(name)) {
            return;
        }
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(new Cue(event(name), volume, x, y, z));
    }

    /** A loop in the listener's ears while {@code alive} holds; it fades in and out. */
    static void loop(String name, float volume, BooleanSupplier alive) {
        if (!OwnerSounds.has(name)) {
            return;
        }
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(new Loop(event(name), volume, alive));
    }

    /** Once, in the listener's ears, from start to end unless {@code alive} fails first; then it fades out. */
    static void track(String name, float volume, BooleanSupplier alive) {
        if (!OwnerSounds.has(name)) {
            return;
        }
        Loop track = new Loop(event(name), volume, alive);
        track.once();
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(track);
    }

    /** A loop from a place in the world while {@code alive} holds. */
    static void loopAt(String name, double x, double y, double z, float volume, BooleanSupplier alive) {
        if (!OwnerSounds.has(name)) {
            return;
        }
        Loop loop = new Loop(event(name), volume, alive);
        loop.at(x, y, z);
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(loop);
    }

    private static final class Cue extends PositionedSound {

        Cue(ResourceLocation event, float volume, float pitch) {
            super(event);
            this.volume = volume;
            this.field_147663_c = pitch;
            this.field_147666_i = ISound.AttenuationType.NONE;
        }

        Cue(ResourceLocation event, float volume, double x, double y, double z) {
            super(event);
            this.volume = volume;
            this.xPosF = (float) x;
            this.yPosF = (float) y;
            this.zPosF = (float) z;
        }
    }

    private static final class Loop extends MovingSound {

        private static final float STEP = 0.08F;

        private final float full;
        private final BooleanSupplier alive;

        Loop(ResourceLocation event, float volume, BooleanSupplier alive) {
            super(event);
            this.full = volume;
            this.alive = alive;
            this.volume = 0.01F;
            this.repeat = true;
            this.field_147665_h = 0;
            this.field_147666_i = ISound.AttenuationType.NONE;
        }

        /** No repeat, and at full volume from the first sample: a track has its own start. */
        void once() {
            this.repeat = false;
            this.volume = full;
        }

        void at(double x, double y, double z) {
            this.xPosF = (float) x;
            this.yPosF = (float) y;
            this.zPosF = (float) z;
            this.field_147666_i = ISound.AttenuationType.LINEAR;
        }

        @Override
        public void update() {
            if (alive.getAsBoolean()) {
                volume = Math.min(full, volume + full * STEP);
            } else {
                volume -= full * STEP;
                if (volume <= 0) {
                    volume = 0;
                    donePlaying = true;
                }
            }
        }
    }
}
