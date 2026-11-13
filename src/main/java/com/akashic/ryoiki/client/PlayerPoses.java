package com.akashic.ryoiki.client;

import net.minecraft.entity.player.EntityPlayer;

import com.akashic.ryoiki.fx.BlowPose;
import com.akashic.ryoiki.fx.CastPose;
import com.akashic.ryoiki.fx.Gait;

/**
 * Everything the mod asks of a player's model at once, as one pose. A model takes one pose a frame, so what is
 * layered here is layered by hand: the way the body stands, runs and jumps in the sorcerer mode underneath; over
 * it, the guard and the blows of a clash and the blow taken; over that, the gesture of a technique.
 */
final class PlayerPoses {

    private static final int BONES = BlowPose.BONES.length;
    /** Where each bone of a gesture is among the bones of the body. */
    private static final int[] GESTURE = new int[CastPose.BONES.length];
    /** The bones of the arms and of the legs, among the bones of the body. */
    private static final int ARMS_FROM = 3;
    private static final int ARMS_TO = 6;
    private static final int LEGS_FROM = 7;

    static {
        for (int i = 0; i < CastPose.BONES.length; i++) {
            for (int j = 0; j < BONES; j++) {
                if (BlowPose.BONES[j].equals(CastPose.BONES[i])) {
                    GESTURE[i] = j;
                }
            }
        }
    }

    private PlayerPoses() {}

    /**
     * Poses a player's model for this frame.
     *
     * @param fight   the guard or the blow of a clash, and how much of it shows; null for none
     * @param fighter whether the player is one of the two of a clash: its arms keep the guard between blows
     * @param gesture the gesture of a technique ({@link CastPose#BONES}), and how much of it shows; null for none
     * @return whether anything was posed
     */
    static boolean apply(EntityPlayer player, float partial, float[] fight, float fightShows, boolean fighter,
        float[] gesture, float gestureShows) {
        Stride.Track track = Stride.INSTANCE.of(player);
        float[] pose = null;
        float shows = 0;
        float moving = 0;
        if (track != null) {
            Gait.State state = track.at(partial);
            pose = Gait.sample(state);
            shows = track.weight(partial);
            moving = state.moving;
            if (fighter) {
                // Between blows the hands stay up, whatever the legs are doing.
                float[] guard = BlowPose.guard();
                for (int bone = ARMS_FROM; bone <= ARMS_TO; bone++) {
                    blend(pose, guard, bone, 0.9F);
                }
            }
        }
        if (fight != null && fightShows > 0) {
            if (pose == null) {
                pose = fight;
                shows = fightShows;
            } else {
                // A blow is thrown with the whole body from where it stands; running, the legs go on running.
                for (int bone = 0; bone < BONES; bone++) {
                    blend(pose, fight, bone, fightShows * (bone >= LEGS_FROM ? 1 - 0.85F * moving : 1));
                }
                shows = Math.max(shows, fightShows);
            }
        }
        if (gesture != null && gestureShows > 0) {
            if (pose == null) {
                FuturaBridge.INSTANCE.pose(player, CastPose.BONES, gesture, gestureShows);
                return true;
            }
            for (int i = 0; i < GESTURE.length; i++) {
                for (int axis = 0; axis < 3; axis++) {
                    int at = GESTURE[i] * 3 + axis;
                    pose[at] += (gesture[i * 3 + axis] - pose[at]) * gestureShows;
                }
            }
            shows = Math.max(shows, gestureShows);
        }
        if (pose == null || shows <= 0) {
            return false;
        }
        FuturaBridge.INSTANCE.pose(player, BlowPose.BONES, pose, shows);
        return true;
    }

    private static void blend(float[] pose, float[] over, int bone, float much) {
        for (int axis = 0; axis < 3; axis++) {
            int at = bone * 3 + axis;
            pose[at] += (over[at] - pose[at]) * much;
        }
    }
}
