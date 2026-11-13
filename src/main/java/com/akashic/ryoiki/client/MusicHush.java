package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The game's own music is silent while one of the owner's tracks plays: the piece that was playing is stopped and
 * none starts. Afterwards the game brings its music back by its own timer.
 */
public final class MusicHush {

    public static final MusicHush INSTANCE = new MusicHush();

    /** The game music last started, to stop it when a track begins. */
    private ISound music;

    private MusicHush() {}

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent17 event) {
        if (event.category != SoundCategory.MUSIC || DomainSounds.ours(event.sound)) {
            return;
        }
        if (DomainSounds.trackPlaying()) {
            event.result = null;
        } else {
            music = event.sound;
        }
    }

    /** Each client tick. */
    void tick() {
        if (music != null && DomainSounds.trackPlaying()) {
            Minecraft.getMinecraft()
                .getSoundHandler()
                .stopSound(music);
            music = null;
        }
    }
}
